package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.DeviceLocationInfo
import com.example.data.model.SessionLocationPoint
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.repository.DeviceLocationRepository
import com.example.data.session.FirebaseSessionManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Firestore document DTO representing a single per-session geolocation fix stored at:
 * `/users/{userId}/sessions/{sessionId}/locationPoints/{pointId}`.
 */
@Keep
@IgnoreExtraProperties
data class FirestoreSessionLocationPoint(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val deviceName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Double = 15.0,
    val verticalAccuracyMeters: Double = 0.0,
    val speedMetersPerSec: Double = 0.0,
    val bearingDegrees: Double = 0.0,
    val provider: String = "fused_gps",
    val locationAddress: String = "",
    val capturedAtMillis: Long = 0L,
    val eventNote: String = "Session Geolocation Fix",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toSessionLocationPoint(): SessionLocationPoint {
        val resolvedTime = capturedAtMillis.takeIf { it > 0L }
            ?: createdAt?.toDate()?.time
            ?: System.currentTimeMillis()
        return SessionLocationPoint(
            pointId = id.ifBlank { "pt_${sessionId}_$resolvedTime" },
            sessionId = sessionId,
            deviceName = deviceName.ifBlank { "Monitored Device" },
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters.toFloat(),
            verticalAccuracyMeters = verticalAccuracyMeters.toFloat(),
            speedMetersPerSec = speedMetersPerSec.toFloat(),
            bearingDegrees = bearingDegrees.toFloat(),
            provider = provider.ifBlank { "telemetry" },
            locationAddress = locationAddress.ifBlank { "Location Verified" },
            capturedAtMillis = resolvedTime,
            eventNote = eventNote.ifBlank { "Session Geolocation Fix" }
        )
    }
}

/**
 * Dedicated per-session Location Tracker Service that captures and persists exact geocoordinates,
 * horizontal/vertical accuracy metadata, speed/bearing, provider, and epoch timestamps in Cloud
 * Firestore (`/users/{userId}/sessions/{sessionId}/locationPoints/{pointId}`) for EACH session,
 * rather than sharing a generic global location.
 */
class LocationTrackerService(
    private val context: Context,
    private val deviceLocationRepository: DeviceLocationRepository,
    private val firebaseSessionManager: FirebaseSessionManager,
    injectedDb: FirebaseFirestore? = null,
    injectedAuth: FirebaseAuth? = null
) {
    companion object {
        private const val TAG = "LocationTrackerService"
        private const val MAX_POINTS_PER_SESSION = 50
        private const val FUSED_TIMEOUT_MS = 4000L
    }

    private val appContext = context.applicationContext

    private val db: FirebaseFirestore? by lazy {
        injectedDb ?: run {
            try {
                if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                    val databaseId = appContext.getString(R.string.firestore_database_id)
                    FirebaseFirestore.getInstance(databaseId)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore initialization deferred: ${e.message}")
                null
            }
        }
    }

    private val auth: FirebaseAuth? by lazy {
        injectedAuth ?: run {
            try {
                if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                    FirebaseAuth.getInstance()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "FirebaseAuth unavailable: ${e.message}")
                null
            }
        }
    }

    private val fusedLocationClient by lazy {
        try {
            LocationServices.getFusedLocationProviderClient(appContext)
        } catch (e: Exception) {
            null
        }
    }

    private val _trackedSessionPoints = MutableStateFlow<Map<String, List<SessionLocationPoint>>>(emptyMap())
    val trackedSessionPoints: StateFlow<Map<String, List<SessionLocationPoint>>> = _trackedSessionPoints.asStateFlow()

    private val _isTrackerActive = MutableStateFlow(false)
    val isTrackerActive: StateFlow<Boolean> = _isTrackerActive.asStateFlow()

    private val _lastFixSummary = MutableStateFlow<String?>(null)
    val lastFixSummary: StateFlow<String?> = _lastFixSummary.asStateFlow()

    private var periodicJob: Job? = null

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Resolves the session document ID in Firestore for any tracked device.
     * Local hardware maps to the install-scoped Firestore session ID; remote/custom devices
     * keep their own distinct sanitized session ID.
     */
    fun resolveSessionId(device: DeviceLocationInfo): String {
        val raw = if (device.isCurrentDevice || device.deviceId == "dev-local-01") {
            firebaseSessionManager.getInstallSessionId()
        } else {
            device.deviceId
        }
        return firebaseSessionManager.sanitizeSessionId(raw)
    }

    /**
     * Starts an automated periodic session location tracker loop. Runs strictly on [Dispatchers.IO]
     * and cancels cleanly when stopped.
     */
    fun startPeriodicSessionTracking(
        scope: CoroutineScope,
        intervalMs: Long = 90_000L
    ) {
        if (periodicJob?.isActive == true) return
        _isTrackerActive.value = true
        periodicJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    captureAndSyncAllSessions(reason = "Scheduled Per-Session Telemetry Fix")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.d(TAG, "Periodic tracker tick notice: ${e.message}")
                }
                delay(intervalMs)
            }
        }
    }

    fun stopPeriodicSessionTracking() {
        periodicJob?.cancel()
        periodicJob = null
        _isTrackerActive.value = false
    }

    /**
     * Captures a live hardware fix (when permitted & valid) for THIS device and preserves
     * each remote/calibrated device's own session coordinates, storing timestamped geocoordinate
     * + accuracy metadata points in Firestore for every session.
     */
    suspend fun captureAndSyncAllSessions(
        reason: String = "Manual Per-Session Fix"
    ): Result<Int> = withContext(Dispatchers.IO) {
        val hardwareFix = acquireHardwareFixIfValid()
        val fleet = deviceLocationRepository.devices.value
        if (fleet.isEmpty()) return@withContext Result.success(0)

        var recordedCount = 0
        var lastError: Throwable? = null

        for (device in fleet) {
            val res = if (device.isCurrentDevice && hardwareFix != null && !deviceLocationRepository.isLocationManuallyCalibrated()) {
                val verticalAcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hardwareFix.hasVerticalAccuracy()) {
                    hardwareFix.verticalAccuracyMeters
                } else {
                    0f
                }
                val address = deviceLocationRepository.getReverseGeocodedAddress(hardwareFix.latitude, hardwareFix.longitude)
                val point = buildPointForDevice(
                    device = device.copy(
                        latitude = hardwareFix.latitude,
                        longitude = hardwareFix.longitude,
                        accuracyMeters = hardwareFix.accuracy.takeIf { it > 0f } ?: 10f,
                        locationAddress = address
                    ),
                    provider = hardwareFix.provider ?: "fused_gps",
                    verticalAccuracyMeters = verticalAcc,
                    speedMetersPerSec = hardwareFix.speed.coerceAtLeast(0f),
                    bearingDegrees = hardwareFix.bearing.coerceIn(0f, 360f),
                    capturedAtMillis = hardwareFix.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    eventNote = reason
                )
                recordSessionLocationFix(device, explicitPoint = point, eventNote = reason)
            } else {
                recordSessionLocationFix(device, explicitPoint = null, eventNote = reason)
            }

            if (res.isSuccess) {
                recordedCount++
            } else {
                lastError = res.exceptionOrNull()
            }
        }

        if (recordedCount > 0) {
            val currentFix = _trackedSessionPoints.value[resolveSessionId(fleet.first())]?.firstOrNull()
            _lastFixSummary.value = currentFix?.let {
                "±%.1fm (%s) • %d session%s synced".format(
                    java.util.Locale.US,
                    it.accuracyMeters,
                    it.provider,
                    recordedCount,
                    if (recordedCount == 1) "" else "s"
                )
            }
            Result.success(recordedCount)
        } else if (lastError != null) {
            Result.failure(lastError)
        } else {
            Result.success(0)
        }
    }

    /**
     * Records a specific session's geocoordinates, timestamp, and accuracy metadata both locally
     * and in Cloud Firestore at `/users/{userId}/sessions/{sessionId}/locationPoints/{pointId}`,
     * alongside updating the session document and location history ledger.
     */
    suspend fun recordSessionLocationFix(
        device: DeviceLocationInfo,
        explicitPoint: SessionLocationPoint? = null,
        eventNote: String = "Session Geolocation Fix"
    ): Result<SessionLocationPoint> = withContext(Dispatchers.IO) {
        val point = explicitPoint ?: buildPointForDevice(
            device = device,
            provider = inferProviderForDevice(device),
            verticalAccuracyMeters = 0f,
            speedMetersPerSec = 0f,
            bearingDegrees = 0f,
            capturedAtMillis = System.currentTimeMillis(),
            eventNote = eventNote
        )

        // Update in-memory per-session point cache immediately
        cacheSessionPoint(point)

        // Append into local rolling history ledger when location/coordinates moved or explicit fix
        val historyEntry = deviceLocationRepository.recordLocationCheckpoint(
            device = device.copy(
                latitude = point.latitude,
                longitude = point.longitude,
                accuracyMeters = point.accuracyMeters,
                locationAddress = point.locationAddress
            ),
            eventNote = "$eventNote (±%.1fm via %s)".format(java.util.Locale.US, point.accuracyMeters, point.provider),
            force = explicitPoint != null
        )

        val uid = auth?.currentUser?.uid
        val firestore = db
        if (uid == null || firestore == null) {
            // Offline / pre-sign-in local tracking succeeded cleanly
            return@withContext Result.success(point)
        }

        val sessionId = point.sessionId
        val pointId = firebaseSessionManager.sanitizeSessionId(point.pointId)
        val docPath = "users/$uid/sessions/$sessionId/locationPoints/$pointId"

        try {
            // Ensure parent session document has latest accuracy, coordinates & timestamp
            firebaseSessionManager.upsertDeviceSession(
                device = device.copy(
                    latitude = point.latitude,
                    longitude = point.longitude,
                    accuracyMeters = point.accuracyMeters,
                    locationAddress = point.locationAddress,
                    lastSeenTimestamp = point.capturedAtMillis
                ),
                overrideSessionId = sessionId
            )

            // Write immutable per-session coordinate fix document
            firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .document(sessionId)
                .collection("locationPoints")
                .document(pointId)
                .set(buildSessionLocationPointPayload(uid, sessionId, point))
                .await()

            if (historyEntry != null) {
                firebaseSessionManager.recordLocationHistory(historyEntry)
            }

            Result.success(point)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath, appContext)
            Result.failure(e)
        }
    }

    /**
     * Streams timestamped geocoordinate fixes for a specific [sessionId] from Firestore.
     */
    fun observeSessionPoints(userId: String, sessionId: String): Flow<List<SessionLocationPoint>> {
        val firestore = db ?: return emptyFlow()
        if (userId.isBlank() || sessionId.isBlank()) return emptyFlow()
        val safeSessionId = firebaseSessionManager.sanitizeSessionId(sessionId)

        return firestore.collection("users")
            .document(userId)
            .collection("sessions")
            .document(safeSessionId)
            .collection("locationPoints")
            .orderBy("capturedAtMillis", Query.Direction.DESCENDING)
            .limit(MAX_POINTS_PER_SESSION.toLong())
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreSessionLocationPoint::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).map { it.toSessionLocationPoint() }
            }
    }

    internal fun buildPointForDevice(
        device: DeviceLocationInfo,
        provider: String,
        verticalAccuracyMeters: Float = 0f,
        speedMetersPerSec: Float = 0f,
        bearingDegrees: Float = 0f,
        capturedAtMillis: Long = System.currentTimeMillis(),
        eventNote: String = "Session Geolocation Fix"
    ): SessionLocationPoint {
        val sessionId = resolveSessionId(device)
        val pointId = "pt_${sessionId.take(20)}_${capturedAtMillis}_${UUID.randomUUID().toString().take(4)}"
        return SessionLocationPoint(
            pointId = pointId,
            sessionId = sessionId,
            deviceName = device.deviceName,
            latitude = device.latitude.coerceIn(-90.0, 90.0),
            longitude = device.longitude.coerceIn(-180.0, 180.0),
            accuracyMeters = device.accuracyMeters.coerceAtLeast(0f),
            verticalAccuracyMeters = verticalAccuracyMeters.coerceAtLeast(0f),
            speedMetersPerSec = speedMetersPerSec.coerceAtLeast(0f),
            bearingDegrees = bearingDegrees.coerceIn(0f, 360f),
            provider = provider.ifBlank { inferProviderForDevice(device) },
            locationAddress = device.locationAddress.ifBlank { "Location Verified" },
            capturedAtMillis = capturedAtMillis.coerceAtLeast(0L),
            eventNote = eventNote
        )
    }

    internal fun buildSessionLocationPointPayload(
        uid: String,
        sessionId: String,
        point: SessionLocationPoint
    ): Map<String, Any> {
        val safeSessionId = firebaseSessionManager.sanitizeSessionId(sessionId)
        return mapOf(
            "userId" to uid.take(128),
            "sessionId" to safeSessionId.take(128),
            "deviceName" to point.deviceName.ifBlank { "Monitored Device" }.take(120),
            "latitude" to point.latitude.coerceIn(-90.0, 90.0),
            "longitude" to point.longitude.coerceIn(-180.0, 180.0),
            "accuracyMeters" to point.accuracyMeters.toDouble().coerceAtLeast(0.0),
            "verticalAccuracyMeters" to point.verticalAccuracyMeters.toDouble().coerceAtLeast(0.0),
            "speedMetersPerSec" to point.speedMetersPerSec.toDouble().coerceAtLeast(0.0),
            "bearingDegrees" to point.bearingDegrees.toDouble().coerceIn(0.0, 360.0),
            "provider" to point.provider.ifBlank { "telemetry" }.take(64),
            "locationAddress" to point.locationAddress.ifBlank { "Location Verified" }.take(240),
            "capturedAtMillis" to point.capturedAtMillis.coerceAtLeast(0L),
            "eventNote" to point.eventNote.ifBlank { "Session Geolocation Fix" }.take(160),
            "createdAt" to FieldValue.serverTimestamp()
        )
    }

    internal fun cacheSessionPoint(point: SessionLocationPoint) {
        _trackedSessionPoints.update { currentMap ->
            val existing = currentMap[point.sessionId].orEmpty()
            val updated = (listOf(point) + existing)
                .distinctBy { it.pointId }
                .sortedByDescending { it.capturedAtMillis }
                .take(MAX_POINTS_PER_SESSION)
            currentMap + (point.sessionId to updated)
        }
    }

    internal fun updateCachedSessionPoints(sessionId: String, points: List<SessionLocationPoint>) {
        val safeSessionId = firebaseSessionManager.sanitizeSessionId(sessionId)
        _trackedSessionPoints.update { currentMap ->
            val existing = currentMap[safeSessionId].orEmpty()
            val merged = (points + existing)
                .distinctBy { it.pointId }
                .sortedByDescending { it.capturedAtMillis }
                .take(MAX_POINTS_PER_SESSION)
            currentMap + (safeSessionId to merged)
        }
    }

    private fun inferProviderForDevice(device: DeviceLocationInfo): String {
        val src = device.locationSource.lowercase()
        return when {
            src.contains("gps") || src.contains("fused") -> "fused_gps"
            src.contains("per-device") || src.contains("user-verified") -> "per_device_calibration"
            src.contains("ip") -> "ip_geolocation"
            src.contains("timezone") -> "timezone_inference"
            src.contains("wi-fi") || src.contains("subnet") || src.contains("lan") -> "lan_discovery"
            else -> "session_telemetry"
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun acquireHardwareFixIfValid(): Location? {
        if (!hasLocationPermission()) return null

        try {
            val client = fusedLocationClient
            if (client != null) {
                val cts = CancellationTokenSource()
                val fused = withTimeoutOrNull(FUSED_TIMEOUT_MS) {
                    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                }
                if (fused != null && !isInvalidEmulatorDefault(fused)) {
                    return fused
                }
            }
        } catch (e: SecurityException) {
            Log.d(TAG, "Location permission missing for fused provider: ${e.message}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Fused location fallback notice: ${e.message}")
        }

        return try {
            val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            listOfNotNull(
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER),
                lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ).firstOrNull { !isInvalidEmulatorDefault(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun isInvalidEmulatorDefault(loc: Location): Boolean {
        return deviceLocationRepository.isRunningOnEmulator() &&
            deviceLocationRepository.isDefaultEmulatorGoogleplex(loc.latitude, loc.longitude)
    }
}
