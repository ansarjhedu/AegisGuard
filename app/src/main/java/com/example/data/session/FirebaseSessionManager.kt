package com.example.data.session

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.example.R
import com.example.data.model.DeviceLocationHistoryEntry
import com.example.data.model.DeviceLocationInfo
import com.example.data.model.DeviceTrackingMode
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Firestore document model representing an active device login session stored at:
 * `/users/{userId}/sessions/{sessionId}`.
 *
 * Notice:
 * - Annotated with @Keep and @IgnoreExtraProperties for R8 safety.
 * - No `Boolean` properties starting with `is` to avoid CustomClassMapper key mismatch.
 * - `@DocumentId val id` is NEVER placed inside written map payloads.
 */
@Keep
@IgnoreExtraProperties
data class FirestoreDeviceSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val deviceModel: String = "",
    val platform: String = "",
    val clientType: String = "",
    val ipAddress: String = "",
    val locationSource: String = "",
    val userEmail: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Double = 15.0,
    val locationAddress: String = "",
    val batteryLevelPercent: Long = 100L,
    val charging: Boolean = false,
    val online: Boolean = true,
    val aegisGuardInstalled: Boolean = false,
    val trackingMode: String = DeviceTrackingMode.GOOGLE_LOCATION.name,
    val pairedToken: String? = null,
    val lockedRemotely: Boolean = false,
    val signedOutRemotely: Boolean = false,
    val networkCarrierOrWifi: String = "",
    val lastSeenAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDeviceLocationInfo(currentInstallId: String): DeviceLocationInfo {
        val parsedMode = enumValues<DeviceTrackingMode>().firstOrNull { it.name == trackingMode }
            ?: DeviceTrackingMode.GOOGLE_LOCATION
        val resolvedTimestamp = (updatedAt ?: createdAt)?.toDate()?.time ?: lastSeenAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        val isLocalNode = id.isNotBlank() && id == currentInstallId

        return DeviceLocationInfo(
            deviceId = id.ifBlank { "remote-${deviceName.hashCode().toUInt()}" },
            deviceName = deviceName.ifBlank { "Active Google Session" },
            deviceModel = deviceModel.ifBlank { platform.ifBlank { "Connected Hardware" } },
            isCurrentDevice = isLocalNode,
            hasAegisGuard = aegisGuardInstalled,
            platform = platform.ifBlank { "Connected OS" },
            clientType = clientType.ifBlank { "Google Session" },
            ipAddress = ipAddress.ifBlank { "0.0.0.0" },
            locationSource = locationSource.ifBlank { "Firebase Cloud Session Manager" },
            userEmail = userEmail,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters.toFloat(),
            locationAddress = locationAddress.ifBlank { "Verified Session Location" },
            lastSeenTimestamp = resolvedTimestamp,
            batteryLevelPercent = batteryLevelPercent.toInt().coerceIn(0, 100),
            isCharging = charging,
            isOnline = online && !signedOutRemotely,
            trackingMode = parsedMode,
            pairedToken = pairedToken?.takeIf { it.isNotBlank() },
            isLockedRemotely = lockedRemotely,
            isSignedOutRemotely = signedOutRemotely,
            networkCarrierOrWifi = networkCarrierOrWifi.ifBlank { "Cloud Synced" }
        )
    }
}

/**
 * Firestore document model representing an immutable per-device location check-in stored at:
 * `/users/{userId}/locationHistory/{entryId}`.
 */
@Keep
@IgnoreExtraProperties
data class FirestoreLocationHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val locationAddress: String = "",
    val locationSource: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Double = 15.0,
    val recordedAtMillis: Long = 0L,
    val eventNote: String = "Location Check-In",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toDeviceLocationHistoryEntry(): DeviceLocationHistoryEntry {
        val resolvedTime = recordedAtMillis.takeIf { it > 0L }
            ?: createdAt?.toDate()?.time
            ?: System.currentTimeMillis()
        return DeviceLocationHistoryEntry(
            entryId = id.ifBlank { "hist_${deviceId}_${resolvedTime}" },
            deviceId = deviceId,
            deviceName = deviceName.ifBlank { "Monitored Device" },
            locationAddress = locationAddress.ifBlank { "Location Verified" },
            locationSource = locationSource.ifBlank { "Firebase Telemetry" },
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters.toFloat(),
            recordedAtMillis = resolvedTime,
            eventNote = eventNote.ifBlank { "Location Check-In" }
        )
    }
}

sealed interface SessionSyncState {
    data object Unauthenticated : SessionSyncState
    data object Syncing : SessionSyncState
    data class Synced(
        val remoteSessionCount: Int,
        val totalSessionCount: Int,
        val lastSyncedAtMillis: Long
    ) : SessionSyncState
    data class Error(val message: String) : SessionSyncState
}

/**
 * Firebase Session Manager service backed by Cloud Firestore (`/users/{userId}/sessions/{sessionId}`).
 *
 * Uses the provisioned custom Firestore database (`R.string.firestore_database_id`), authenticates
 * strictly via Firebase Auth UID from Google Sign-In (Credential Manager), publishes this device's
 * active session + user-registered/discovered devices, and streams all active logins across devices
 * in real time.
 */
class FirebaseSessionManager(
    private val context: Context,
    injectedDb: FirebaseFirestore? = null,
    injectedAuth: FirebaseAuth? = null
) {
    companion object {
        private const val TAG = "FirebaseSessionManager"
        private const val PREFS_NAME = "aegis_firebase_session_prefs"
        private const val KEY_INSTALL_SESSION_ID = "install_session_id"
        private val VALID_ID_REGEX = Regex("[^a-zA-Z0-9_\\-]")
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
                Log.w(TAG, "Firestore client initialization deferred: ${e.message}")
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

    /**
     * Persistent, rule-compliant identifier (`^[a-zA-Z0-9_\-]+$`, <= 128 chars) unique to this
     * installation so reopening the app updates the same device document instead of duplicating it,
     * while each other device receives its own distinct document ID.
     */
    fun getInstallSessionId(): String {
        val existing = prefs.getString(KEY_INSTALL_SESSION_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "node_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        prefs.edit().putString(KEY_INSTALL_SESSION_ID, generated).apply()
        return generated
    }

    fun currentUserId(): String? = try {
        auth?.currentUser?.uid
    } catch (_: Exception) {
        null
    }

    fun currentUserEmail(): String? = try {
        auth?.currentUser?.email
    } catch (_: Exception) {
        null
    }

    fun authStateFlow(): Flow<FirebaseUser?> {
        val firebaseAuth = auth ?: return callbackFlow {
            trySend(null)
            awaitClose {}
        }
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { authInstance ->
                trySend(authInstance.currentUser)
            }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }
    }

    /**
     * Streams all active sessions stored in Firestore for [userId] via `Query.snapshots()`.
     * Uses `ServerTimestampBehavior.ESTIMATE` so newly written documents never expose null timestamps.
     */
    fun observeActiveSessions(userId: String): Flow<List<FirestoreDeviceSession>> {
        val firestore = db ?: return emptyFlow()
        if (userId.isBlank()) return emptyFlow()

        return firestore.collection("users")
            .document(userId)
            .collection("sessions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreDeviceSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    /**
     * Creates or updates a device session document in Firestore under `/users/{uid}/sessions/{sessionId}`.
     * Preserves immutable `userId` and `createdAt` when updating existing records.
     */
    suspend fun upsertDeviceSession(
        device: DeviceLocationInfo,
        overrideSessionId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val firestore = db
            ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        val uid = auth?.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Sign in with Google via Credential Manager to sync Firebase sessions."))

        val rawId = overrideSessionId
            ?: if (device.isCurrentDevice || device.deviceId == "dev-local-01") getInstallSessionId()
            else device.deviceId
        val sessionId = sanitizeSessionId(rawId)
        val docPath = "users/$uid/sessions/$sessionId"

        try {
            val docRef = firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .document(sessionId)

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(buildUpdatePayload(device)).await()
            } else {
                docRef.set(buildCreatePayload(uid, device)).await()
            }
            Result.success(sessionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath, appContext)
            Result.failure(e)
        }
    }

    /**
     * Publishes the current device and any locally stored non-remote devices into Firestore so all
     * devices signed into the same account appear across installs and devices.
     */
    suspend fun syncFleetToCloud(devices: List<DeviceLocationInfo>): Result<Int> = withContext(Dispatchers.IO) {
        val firestore = db
            ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        val uid = auth?.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Google Sign-In required for Firebase Session Manager sync."))

        var syncedCount = 0
        var lastError: Throwable? = null

        val installId = getInstallSessionId()
        for (dev in devices) {
            val targetId = if (dev.isCurrentDevice || dev.deviceId == "dev-local-01") installId else dev.deviceId
            val res = upsertDeviceSession(dev, overrideSessionId = targetId)
            if (res.isSuccess) {
                syncedCount++
            } else {
                lastError = res.exceptionOrNull()
            }
        }

        if (syncedCount == 0 && lastError != null) {
            Result.failure(lastError)
        } else {
            // Confirm readable collection list matches what's in Firestore
            val count = try {
                firestore.collection("users")
                    .document(uid)
                    .collection("sessions")
                    .get()
                    .await()
                    .size()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.LIST, "users/$uid/sessions", appContext)
                syncedCount
            }
            Result.success(count.coerceAtLeast(syncedCount))
        }
    }

    suspend fun updateSessionLockdown(sessionId: String, isLocked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = db
            ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        val uid = auth?.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Authentication required"))

        val safeId = sanitizeSessionId(if (sessionId == "dev-local-01") getInstallSessionId() else sessionId)
        val path = "users/$uid/sessions/$safeId"

        try {
            firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .document(safeId)
                .update(
                    mapOf(
                        "lockedRemotely" to isLocked,
                        "lastSeenAtMillis" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path, appContext)
            Result.failure(e)
        }
    }

    suspend fun terminateRemoteSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = db
            ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        val uid = auth?.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Authentication required"))

        val safeId = sanitizeSessionId(if (sessionId == "dev-local-01") getInstallSessionId() else sessionId)
        val path = "users/$uid/sessions/$safeId"

        try {
            firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .document(safeId)
                .update(
                    mapOf(
                        "signedOutRemotely" to true,
                        "online" to false,
                        "lastSeenAtMillis" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path, appContext)
            Result.failure(e)
        }
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = db
            ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        val uid = auth?.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Authentication required"))

        val safeId = sanitizeSessionId(if (sessionId == "dev-local-01") getInstallSessionId() else sessionId)
        val path = "users/$uid/sessions/$safeId"

        try {
            firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .document(safeId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path, appContext)
            Result.failure(e)
        }
    }

    /**
     * Streams up to 100 chronological location history entries stored under
     * `/users/{userId}/locationHistory`.
     */
    fun observeLocationHistory(userId: String): Flow<List<FirestoreLocationHistoryEntry>> {
        val firestore = db ?: return emptyFlow()
        if (userId.isBlank()) return emptyFlow()

        return firestore.collection("users")
            .document(userId)
            .collection("locationHistory")
            .orderBy("recordedAtMillis", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreLocationHistoryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    /**
     * Appends an immutable location check-in entry under `/users/{uid}/locationHistory/{entryId}`.
     */
    suspend fun recordLocationHistory(entry: DeviceLocationHistoryEntry): Result<String> = withContext(Dispatchers.IO) {
        val firestore = db
            ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        val uid = auth?.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Sign in with Google to record cloud location history"))

        val entryDocId = sanitizeSessionId(entry.entryId)
        val path = "users/$uid/locationHistory/$entryDocId"

        try {
            firestore.collection("users")
                .document(uid)
                .collection("locationHistory")
                .document(entryDocId)
                .set(buildLocationHistoryPayload(uid, entry))
                .await()
            Result.success(entryDocId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path, appContext)
            Result.failure(e)
        }
    }

    /**
     * Deletes all persisted cloud location history entries for the authenticated user.
     */
    suspend fun clearLocationHistory(): Result<Int> = withContext(Dispatchers.IO) {
        val firestore = db
            ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        val uid = auth?.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Authentication required"))
        val colPath = "users/$uid/locationHistory"

        try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("locationHistory")
                .limit(100)
                .get()
                .await()
            if (snapshot.isEmpty) return@withContext Result.success(0)

            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(snapshot.size())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colPath, appContext)
            Result.failure(e)
        }
    }

    internal fun buildLocationHistoryPayload(
        uid: String,
        entry: DeviceLocationHistoryEntry
    ): Map<String, Any> {
        return mapOf(
            "userId" to uid.take(128),
            "deviceId" to sanitizeSessionId(entry.deviceId).take(128),
            "deviceName" to entry.deviceName.ifBlank { "Monitored Device" }.take(120),
            "locationAddress" to entry.locationAddress.ifBlank { "Location Verified" }.take(240),
            "locationSource" to entry.locationSource.ifBlank { "Device Telemetry" }.take(120),
            "latitude" to entry.latitude.coerceIn(-90.0, 90.0),
            "longitude" to entry.longitude.coerceIn(-180.0, 180.0),
            "accuracyMeters" to entry.accuracyMeters.toDouble().coerceAtLeast(0.0),
            "recordedAtMillis" to entry.recordedAtMillis.coerceAtLeast(0L),
            "eventNote" to entry.eventNote.ifBlank { "Location Check-In" }.take(160),
            "createdAt" to FieldValue.serverTimestamp()
        )
    }

    internal fun sanitizeSessionId(raw: String): String {
        val cleaned = raw.replace(VALID_ID_REGEX, "_").take(128)
        return cleaned.ifBlank { getInstallSessionId() }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun buildCreatePayload(uid: String, device: DeviceLocationInfo): Map<String, Any> {
        val base = buildCommonFields(device).toMutableMap()
        base["userId"] = uid.take(128)
        base["lockedRemotely"] = device.isLockedRemotely
        base["signedOutRemotely"] = device.isSignedOutRemotely
        base["createdAt"] = FieldValue.serverTimestamp()
        base["updatedAt"] = FieldValue.serverTimestamp()
        return base.filterValues { it != null } as Map<String, Any>
    }

    internal fun buildUpdatePayload(device: DeviceLocationInfo): Map<String, Any> {
        val base = buildCommonFields(device).toMutableMap()
        base["updatedAt"] = FieldValue.serverTimestamp()
        // Note: `userId` and `createdAt` are immutable and omitted from partial updates;
        // `pairedToken` omitted when null to satisfy rules `!('pairedToken' in data) || ...`
        @Suppress("UNCHECKED_CAST")
        return base.filterValues { it != null } as Map<String, Any>
    }

    private fun buildCommonFields(device: DeviceLocationInfo): Map<String, Any?> {
        val lat = device.latitude.coerceIn(-90.0, 90.0)
        val lng = device.longitude.coerceIn(-180.0, 180.0)
        val acc = device.accuracyMeters.toDouble().coerceAtLeast(0.0)
        val battery = device.batteryLevelPercent.coerceIn(0, 100).toLong()
        val email = auth?.currentUser?.email?.takeIf { it.isNotBlank() } ?: device.userEmail
        val tracking = if (device.trackingMode == DeviceTrackingMode.AEGIS_DEVICE_TOKEN) {
            DeviceTrackingMode.AEGIS_DEVICE_TOKEN.name
        } else {
            DeviceTrackingMode.GOOGLE_LOCATION.name
        }

        return mapOf(
            "deviceName" to device.deviceName.ifBlank { "AegisGuard Device" }.take(120),
            "deviceModel" to device.deviceModel.ifBlank { "Connected Hardware" }.take(120),
            "platform" to device.platform.ifBlank { "Connected OS" }.take(80),
            "clientType" to device.clientType.ifBlank { "AegisGuard Session" }.take(120),
            "ipAddress" to device.ipAddress.ifBlank { "0.0.0.0" }.take(64),
            "locationSource" to device.locationSource.ifBlank { "Network Telemetry" }.take(120),
            "userEmail" to email.ifBlank { "unknown@gmail.com" }.take(200),
            "latitude" to lat,
            "longitude" to lng,
            "accuracyMeters" to acc,
            "locationAddress" to device.locationAddress.ifBlank { "Location Verified" }.take(240),
            "batteryLevelPercent" to battery,
            "charging" to device.isCharging,
            "online" to device.isOnline,
            "aegisGuardInstalled" to (device.hasAegisGuard || device.isCurrentDevice),
            "trackingMode" to tracking,
            "pairedToken" to device.pairedToken?.takeIf { it.isNotBlank() }?.take(128),
            "networkCarrierOrWifi" to device.networkCarrierOrWifi.ifBlank { "Wi-Fi / WAN" }.take(160),
            "lastSeenAtMillis" to device.lastSeenTimestamp.coerceAtLeast(0L)
        )
    }
}
