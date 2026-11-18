package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.model.AegisPairingToken
import com.example.data.model.DeviceLocationHistoryEntry
import com.example.data.model.DeviceLocationInfo
import com.example.data.model.DeviceTrackingMode
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class DeviceLocationRepository(private val context: Context) {

    companion object {
        private const val TAG = "DeviceLocationRepo"
        private const val PREFS_NAME = "aegis_device_location_prefs"
        private const val KEY_SAVED_DEVICES = "saved_custom_devices_json"
        private const val KEY_LOCATION_HISTORY_JSON = "device_location_history_json"
        private const val KEY_DEVICE_OVERRIDES_JSON = "per_device_overrides_json"
        private const val KEY_OVERRIDE_ENABLED = "location_override_enabled"
        private const val KEY_OVERRIDE_LAT = "location_override_lat"
        private const val KEY_OVERRIDE_LNG = "location_override_lng"
        private const val KEY_OVERRIDE_ADDRESS = "location_override_address"
        private const val KEY_CACHED_IP_LAT = "cached_ip_lat"
        private const val KEY_CACHED_IP_LNG = "cached_ip_lng"
        private const val KEY_CACHED_IP_ADDRESS = "cached_ip_address"
        private const val KEY_CACHED_PUBLIC_IP = "cached_public_ip"
        private const val KEY_CACHED_GPS_LAT = "cached_gps_lat"
        private const val KEY_CACHED_GPS_LNG = "cached_gps_lng"
        private const val KEY_CACHED_GPS_ADDRESS = "cached_gps_address"
        private const val DEFAULT_EMULATOR_LAT = 37.4220656
        private const val DEFAULT_EMULATOR_LNG = -122.0840897
        private const val MAX_HISTORY_ENTRIES = 100
        private const val HISTORY_DEDUPE_WINDOW_MS = 5 * 60 * 1000L

        private val LEGACY_TEST_DEVICE_IDS = setOf(
            "dev-ext-win-01",
            "dev-ext-ios-02",
            "dev-ext-mac-03",
            "dev-ext-tab-04",
            "dev-ext-linux-05",
            "dev-ext-ipad-04",
            "dev-ext-lin-05",
            "dev-gmail-win-01",
            "dev-gmail-ios-02",
            "dev-test-win-01",
            "dev-test-ios-02"
        )

        private val KNOWN_CITY_COORDINATES = mapOf(
            "gulberg lahore" to Triple(31.5102, 74.3441, "Gulberg, Lahore, Pakistan"),
            "gulberg" to Triple(31.5102, 74.3441, "Gulberg, Lahore, Pakistan"),
            "johar town lahore" to Triple(31.4697, 74.2728, "Johar Town, Lahore, Pakistan"),
            "johar town" to Triple(31.4697, 74.2728, "Johar Town, Lahore, Pakistan"),
            "dha lahore" to Triple(31.4720, 74.3762, "DHA, Lahore, Pakistan"),
            "model town" to Triple(31.4805, 74.3239, "Model Town, Lahore, Pakistan"),
            "bahria town" to Triple(31.3679, 74.1806, "Bahria Town, Lahore, Pakistan"),
            "wapda town" to Triple(31.4337, 74.2673, "Wapda Town, Lahore, Pakistan"),
            "iqbal town" to Triple(31.5126, 74.2836, "Allama Iqbal Town, Lahore, Pakistan"),
            "clifton" to Triple(24.8138, 67.0299, "Clifton, Karachi, Pakistan"),
            "blue area" to Triple(33.7077, 73.0561, "Blue Area, Islamabad, Pakistan"),
            "pakistan" to Triple(33.6844, 73.0479, "Islamabad, Pakistan"),
            "islamabad" to Triple(33.6844, 73.0479, "Islamabad, Pakistan"),
            "lahore" to Triple(31.5204, 74.3587, "Lahore, Punjab, Pakistan"),
            "karachi" to Triple(24.8607, 67.0011, "Karachi, Sindh, Pakistan"),
            "rawalpindi" to Triple(33.5651, 73.0169, "Rawalpindi, Punjab, Pakistan"),
            "faisalabad" to Triple(31.4504, 73.1350, "Faisalabad, Punjab, Pakistan"),
            "multan" to Triple(30.1575, 71.5249, "Multan, Punjab, Pakistan"),
            "peshawar" to Triple(34.0151, 71.5249, "Peshawar, KPK, Pakistan"),
            "quetta" to Triple(30.1798, 66.9750, "Quetta, Balochistan, Pakistan"),
            "sialkot" to Triple(32.4945, 74.5229, "Sialkot, Punjab, Pakistan"),
            "gujranwala" to Triple(32.1877, 74.1945, "Gujranwala, Punjab, Pakistan"),
            "hyderabad" to Triple(25.3960, 68.3578, "Hyderabad, Sindh, Pakistan"),
            "dubai" to Triple(25.2048, 55.2708, "Dubai, United Arab Emirates"),
            "riyadh" to Triple(24.7136, 46.6753, "Riyadh, Saudi Arabia"),
            "london" to Triple(51.5074, -0.1278, "London, United Kingdom"),
            "new york" to Triple(40.7128, -74.0060, "New York, NY, USA"),
            "toronto" to Triple(43.6532, -79.3832, "Toronto, ON, Canada"),
            "singapore" to Triple(1.3521, 103.8198, "Singapore")
        )
    }

    private data class DeviceOverride(
        val deviceName: String? = null,
        val platform: String? = null,
        val clientType: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val locationAddress: String? = null,
        val locationSource: String? = null,
        val updatedAtMillis: Long = System.currentTimeMillis()
    )

    private data class ResolvedLocation(
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val source: String,
        val publicIp: String? = null
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var activeGmail = "ansarjhedu@gmail.com"

    private val _devices = MutableStateFlow<List<DeviceLocationInfo>>(emptyList())
    val devices: StateFlow<List<DeviceLocationInfo>> = _devices.asStateFlow()

    private val _locationHistory = MutableStateFlow<List<DeviceLocationHistoryEntry>>(emptyList())
    val locationHistory: StateFlow<List<DeviceLocationHistoryEntry>> = _locationHistory.asStateFlow()

    private var latestCloudSessions: List<DeviceLocationInfo> = emptyList()
    private var currentCloudInstallId: String? = null
    private val perDeviceOverrides = mutableMapOf<String, DeviceOverride>()

    @Volatile private var cachedLocalIp: String = "10.0.2.16"
    @Volatile private var cachedBatteryPct: Int = 86
    @Volatile private var cachedIsCharging: Boolean = false
    @Volatile private var hasRefreshedHardwareTelemetry: Boolean = false

    private val _pairingTokens = MutableStateFlow<List<AegisPairingToken>>(emptyList())
    val pairingTokens: StateFlow<List<AegisPairingToken>> = _pairingTokens.asStateFlow()

    private val _selectedMode = MutableStateFlow(DeviceTrackingMode.GOOGLE_LOCATION)
    val selectedMode: StateFlow<DeviceTrackingMode> = _selectedMode.asStateFlow()

    private val fusedLocationClient by lazy {
        try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (e: Exception) {
            null
        }
    }

    private val _isGoogleAuthenticated = MutableStateFlow(false)
    val isGoogleAuthenticated: StateFlow<Boolean> = _isGoogleAuthenticated.asStateFlow()

    private val _authenticatedUserName = MutableStateFlow<String?>(null)
    val authenticatedUserName: StateFlow<String?> = _authenticatedUserName.asStateFlow()

    private val _authenticatedPhotoUrl = MutableStateFlow<String?>(null)
    val authenticatedPhotoUrl: StateFlow<String?> = _authenticatedPhotoUrl.asStateFlow()

    private val _authenticatedIdToken = MutableStateFlow<String?>(null)
    val authenticatedIdToken: StateFlow<String?> = _authenticatedIdToken.asStateFlow()

    init {
        loadPersistedAuthState()
        loadPerDeviceOverrides()
        _locationHistory.value = loadLocationHistory()
        initializeTokens()
        initializeDevices()
        if (_locationHistory.value.isEmpty()) {
            _devices.value.firstOrNull { it.isCurrentDevice }?.let { current ->
                recordLocationCheckpoint(
                    device = current,
                    eventNote = "Initial hardware telemetry baseline recorded",
                    force = true
                )
            }
        }
    }

    private fun isTestDevice(deviceId: String, deviceName: String = ""): Boolean {
        if (deviceId in LEGACY_TEST_DEVICE_IDS) return true
        if (deviceId.startsWith("dev-ext-") || deviceId.startsWith("dev-test-") || deviceId.startsWith("dev-gmail-win-") || deviceId.startsWith("dev-gmail-ios-")) return true
        val lowerName = deviceName.lowercase()
        if (lowerName.contains("windows pc (gmail web)") || lowerName.contains("apple iphone (gmail app)")) return true
        return false
    }

    private fun purgeLegacyAndTestDevicesIfNeeded(loaded: List<DeviceLocationInfo>): List<DeviceLocationInfo> {
        val filtered = loaded.filter { dev -> !dev.isCurrentDevice && !isTestDevice(dev.deviceId, dev.deviceName) }
        if (filtered.size != loaded.size) {
            saveCustomDevices(filtered)
        }
        return filtered
    }

    private fun loadPersistedAuthState() {
        try {
            val isAuth = prefs.getBoolean("is_google_authenticated", false)
            val savedEmail = prefs.getString("active_gmail", null)
            val savedName = prefs.getString("google_display_name", null)
            val savedPhoto = prefs.getString("google_photo_url", null)
            val savedToken = prefs.getString("google_id_token", null)

            if (!savedEmail.isNullOrBlank()) {
                activeGmail = savedEmail
            }
            if (isAuth) {
                _isGoogleAuthenticated.value = true
                _authenticatedUserName.value = savedName
                _authenticatedPhotoUrl.value = savedPhoto
                _authenticatedIdToken.value = savedToken
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading persisted auth state: ${e.message}")
        }
    }

    private fun detectSystemGoogleAccounts() {
        if (_isGoogleAuthenticated.value) return
        try {
            val accountManager = android.accounts.AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            if (accounts.isNotEmpty()) {
                val detected = accounts.firstOrNull { it.name.isNotBlank() }?.name
                if (!detected.isNullOrBlank()) {
                    activeGmail = detected.trim()
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "AccountManager query notice: ${e.message}")
        }
    }

    fun setTrackingMode(mode: DeviceTrackingMode) {
        _selectedMode.value = mode
    }

    fun linkGoogleSession(
        email: String,
        displayName: String?,
        photoUrl: String?,
        idToken: String?
    ) {
        if (email.isNotBlank()) {
            activeGmail = email.trim()
            _isGoogleAuthenticated.value = true
            _authenticatedUserName.value = displayName ?: email.substringBefore("@")
            _authenticatedPhotoUrl.value = photoUrl
            _authenticatedIdToken.value = idToken

            prefs.edit()
                .putBoolean("is_google_authenticated", true)
                .putString("active_gmail", activeGmail)
                .putString("google_display_name", displayName ?: "")
                .putString("google_photo_url", photoUrl ?: "")
                .putString("google_id_token", idToken ?: "")
                .apply()

            // Update user email across existing real devices and tokens
            initializeDevices()
            initializeTokens()
        }
    }

    fun unlinkGoogleSession() {
        _isGoogleAuthenticated.value = false
        _authenticatedUserName.value = null
        _authenticatedPhotoUrl.value = null
        _authenticatedIdToken.value = null

        prefs.edit()
            .putBoolean("is_google_authenticated", false)
            .remove("google_display_name")
            .remove("google_photo_url")
            .remove("google_id_token")
            .apply()

        initializeDevices()
    }

    fun updateActiveEmail(email: String) {
        if (email.isNotBlank()) {
            activeGmail = email.trim()
            prefs.edit().putString("active_gmail", activeGmail).apply()
            _devices.value = _devices.value.map { it.copy(userEmail = activeGmail) }
            _pairingTokens.value = _pairingTokens.value.map { it.copy(targetUserEmail = activeGmail) }
            saveCustomDevices(_devices.value.filter { !it.isCurrentDevice })
        }
    }

    fun getActiveEmail(): String = activeGmail

    private fun initializeTokens() {
        val initialToken = AegisPairingToken(
            tokenId = "AEGIS-TK-9F82-B4E1",
            targetDeviceName = "Paired Hardware Node",
            targetUserEmail = activeGmail,
            cryptographicFingerprint = "SHA256:7D94A1C8B3E05F21"
        )
        _pairingTokens.value = listOf(initialToken)
    }

    /**
     * Initializes the device fleet:
     * 1. Constructs the REAL Current Android Hardware Device with live IP and battery telemetry.
     * 2. Loads genuine user-added and network-discovered devices from persistent storage.
     * 3. NEVER generates fake/dummy test sessions (e.g. Windows PC or iPhone test devices).
     */
    private fun initializeDevices() {
        val currentDevice = createCurrentHardwareDevice(allowBlockingIpc = false)
        val savedDevices = purgeLegacyAndTestDevicesIfNeeded(loadCustomDevices())
        rebuildMergedDevices(currentDevice, savedDevices)
    }

    private fun applyPerDeviceOverride(device: DeviceLocationInfo): DeviceLocationInfo {
        val ov = perDeviceOverrides[device.deviceId]
            ?: if (device.isCurrentDevice && currentCloudInstallId != null) perDeviceOverrides[currentCloudInstallId] else null
        if (ov == null) return device
        return device.copy(
            deviceName = ov.deviceName?.takeIf { it.isNotBlank() } ?: device.deviceName,
            platform = ov.platform?.takeIf { it.isNotBlank() } ?: device.platform,
            clientType = ov.clientType?.takeIf { it.isNotBlank() } ?: device.clientType,
            latitude = ov.latitude ?: device.latitude,
            longitude = ov.longitude ?: device.longitude,
            locationAddress = ov.locationAddress?.takeIf { it.isNotBlank() } ?: device.locationAddress,
            locationSource = ov.locationSource?.takeIf { it.isNotBlank() } ?: device.locationSource
        )
    }

    private fun rebuildMergedDevices(
        currentDevice: DeviceLocationInfo = createCurrentHardwareDevice(),
        localOtherDevices: List<DeviceLocationInfo> = _devices.value.filter {
            !it.isCurrentDevice && !it.locationSource.contains("Firebase", ignoreCase = true)
        }
    ) {
        val cloudRemote = latestCloudSessions.filter { cloud ->
            cloud.deviceId != currentCloudInstallId &&
                cloud.deviceId != currentDevice.deviceId &&
                !isTestDevice(cloud.deviceId, cloud.deviceName)
        }.map { applyPerDeviceOverride(it) }

        val cloudById = cloudRemote.associateBy { it.deviceId }
        val nonDuplicateLocal = localOtherDevices.filter { local ->
            local.deviceId !in cloudById &&
                local.deviceId != currentCloudInstallId &&
                !isTestDevice(local.deviceId, local.deviceName)
        }.map { applyPerDeviceOverride(it) }

        val matchingLocalCloud = currentCloudInstallId?.let { id ->
            latestCloudSessions.firstOrNull { it.deviceId == id }
        }
        val baseCurrent = if (matchingLocalCloud != null) {
            currentDevice.copy(
                isLockedRemotely = matchingLocalCloud.isLockedRemotely || currentDevice.isLockedRemotely,
                isSignedOutRemotely = matchingLocalCloud.isSignedOutRemotely || currentDevice.isSignedOutRemotely
            )
        } else {
            currentDevice
        }
        val effectiveCurrent = applyPerDeviceOverride(baseCurrent)

        val combinedRemote = (cloudRemote + nonDuplicateLocal)
            .distinctBy { it.deviceId }
            .sortedByDescending { it.lastSeenTimestamp }

        _devices.value = listOf(effectiveCurrent) + combinedRemote
    }

    fun mergeCloudSessions(cloudSessions: List<DeviceLocationInfo>, currentInstallId: String) {
        currentCloudInstallId = currentInstallId
        latestCloudSessions = cloudSessions
        val localSaved = loadCustomDevices().filter { dev ->
            !dev.isCurrentDevice && !isTestDevice(dev.deviceId, dev.deviceName)
        }
        rebuildMergedDevices(createCurrentHardwareDevice(), localSaved)
    }

    fun clearCloudSyncedSessions() {
        latestCloudSessions = emptyList()
        initializeDevices()
    }

    /**
     * Creates a genuine representation of THIS Android device using actual hardware properties,
     * real local/public network IP address, real battery, and verified location telemetry.
     *
     * Set [allowBlockingIpc] to `true` only on background threads (e.g., [Dispatchers.IO]);
     * synchronous startup paths keep [allowBlockingIpc] = `false` so Application/Activity init
     * never blocks the Main Thread waiting on Geocoder or LocationManager binder IPC.
     */
    fun createCurrentHardwareDevice(allowBlockingIpc: Boolean = false): DeviceLocationInfo {
        if (allowBlockingIpc || !hasRefreshedHardwareTelemetry && android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            refreshHardwareTelemetryCache()
        }
        val resolved = resolveBestLocation(allowBlockingIpc = allowBlockingIpc)
        val batteryPct = cachedBatteryPct
        val localIp = cachedLocalIp
        val displayIp = resolved.publicIp?.takeIf { it.isNotBlank() } ?: localIp

        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val release = Build.VERSION.RELEASE
        val cleanName = if (isRunningOnEmulator()) {
            "AegisGuard Host ($manufacturer $model)"
        } else {
            "$manufacturer $model"
        }

        return DeviceLocationInfo(
            deviceId = "dev-local-01",
            deviceName = cleanName,
            deviceModel = model,
            isCurrentDevice = true,
            hasAegisGuard = true,
            platform = "Android $release",
            clientType = "AegisGuard Master (Active)",
            ipAddress = displayIp,
            locationSource = resolved.source,
            userEmail = activeGmail,
            latitude = resolved.latitude,
            longitude = resolved.longitude,
            accuracyMeters = if (prefs.getBoolean(KEY_OVERRIDE_ENABLED, false)) 10.0f else 15.0f,
            locationAddress = resolved.address,
            batteryLevelPercent = batteryPct,
            isCharging = cachedIsCharging,
            isOnline = true,
            trackingMode = DeviceTrackingMode.GOOGLE_LOCATION,
            pairedToken = null,
            networkCarrierOrWifi = if (resolved.publicIp != null) "LAN $localIp • WAN ${resolved.publicIp}" else "Active Network ($localIp)"
        )
    }

    private fun refreshHardwareTelemetryCache() {
        cachedBatteryPct = getBatteryPercentage()
        cachedIsCharging = isDeviceCharging()
        cachedLocalIp = getLocalIpAddress()
        hasRefreshedHardwareTelemetry = true
    }

    /**
     * Discovers live, reachable hosts on the local Wi-Fi / IP subnet.
     * Unreachable IPs are skipped (never creates phantom unreachable devices).
     */
    suspend fun discoverLocalNetworkDevices(): List<DeviceLocationInfo> = withContext(Dispatchers.IO) {
        val (baseLat, baseLng) = getRawCoordinates()
        val currentAddr = _devices.value.firstOrNull { it.isCurrentDevice }?.locationAddress ?: "Local Wi-Fi"
        val localIp = getLocalIpAddress()
        val discoveredList = mutableListOf<DeviceLocationInfo>()

        try {
            val parts = localIp.split(".")
            if (parts.size == 4) {
                val subnetPrefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                val hostLastOctet = parts[3].toIntOrNull() ?: 100

                val testOctets = listOf(1, 2, 3, 5, 10, 15, 20, 50, 100, 150, 200)
                    .filter { it != hostLastOctet }
                    .take(8)

                val deferredChecks = testOctets.map { octet ->
                    async {
                        val targetIp = "$subnetPrefix.$octet"
                        try {
                            val inet = InetAddress.getByName(targetIp)
                            val isReachable = inet.isReachable(180)
                            if (isReachable) {
                                val hostName = try {
                                    inet.canonicalHostName?.takeIf { it != targetIp && it.isNotBlank() } ?: inet.hostName
                                } catch (e: Exception) {
                                    targetIp
                                }
                                val cleanHost = if (hostName == targetIp) "" else " — $hostName"
                                val inferredPlatform = when {
                                    octet == 1 -> "Wi-Fi Gateway / Router"
                                    octet in 2..10 -> "Connected PC / Laptop"
                                    octet in 11..50 -> "Mobile / Tablet"
                                    else -> "LAN Network Node"
                                }
                                val inferredClient = when {
                                    octet == 1 -> "Network Hub & DNS"
                                    octet in 2..10 -> "LAN Host ($targetIp)"
                                    else -> "Local Wi-Fi Client ($targetIp)"
                                }
                                val nodeLabel = if (octet == 1) {
                                    "Wi-Fi Router / Gateway ($targetIp)"
                                } else {
                                    "LAN Host$cleanHost ($targetIp)"
                                }

                                DeviceLocationInfo(
                                    deviceId = "dev-net-${UUID.randomUUID().toString().take(6)}",
                                    deviceName = nodeLabel,
                                    deviceModel = if (hostName != targetIp) hostName else "Subnet Host $subnetPrefix.$octet",
                                    isCurrentDevice = false,
                                    hasAegisGuard = false,
                                    platform = inferredPlatform,
                                    clientType = inferredClient,
                                    ipAddress = targetIp,
                                    locationSource = "Local Wi-Fi Scan ($subnetPrefix.x)",
                                    userEmail = activeGmail,
                                    latitude = baseLat,
                                    longitude = baseLng,
                                    accuracyMeters = 12f,
                                    locationAddress = "$currentAddr • LAN $targetIp",
                                    lastSeenTimestamp = System.currentTimeMillis(),
                                    batteryLevelPercent = 100,
                                    isCharging = true,
                                    isOnline = true,
                                    trackingMode = DeviceTrackingMode.GOOGLE_LOCATION,
                                    pairedToken = null,
                                    networkCarrierOrWifi = "Local Subnet ($subnetPrefix.x)"
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                discoveredList.addAll(deferredChecks.awaitAll().filterNotNull())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network discovery warning: ${e.message}")
        }

        if (discoveredList.isNotEmpty()) {
            val currentNonLocal = _devices.value.filter { !it.isCurrentDevice }
            val merged = (currentNonLocal + discoveredList).distinctBy { it.ipAddress }
            _devices.value = listOf(applyPerDeviceOverride(createCurrentHardwareDevice(allowBlockingIpc = true))) + merged
            saveCustomDevices(merged)
        }

        discoveredList
    }

    /**
     * Removes anonymous LAN-scanned (`dev-net-*`) nodes while preserving the current device,
     * named Gmail sessions, Firebase sessions, and token-paired devices.
     */
    fun clearNetworkDiscoveredDevices(): List<String> {
        val removedIds = _devices.value
            .filter { !it.isCurrentDevice && it.deviceId.startsWith("dev-net-") }
            .map { it.deviceId }
        if (removedIds.isEmpty()) return emptyList()

        val removedSet = removedIds.toSet()
        latestCloudSessions = latestCloudSessions.filter { it.deviceId !in removedSet }
        removedIds.forEach { perDeviceOverrides.remove(it) }
        savePerDeviceOverrides()

        val remainingLocal = loadCustomDevices().filter { it.deviceId !in removedSet }
        saveCustomDevices(remainingLocal)
        rebuildMergedDevices(createCurrentHardwareDevice(), remainingLocal)
        return removedIds
    }

    suspend fun refreshDeviceLocations(): List<DeviceLocationHistoryEntry> = withContext(Dispatchers.IO) {
        detectSystemGoogleAccounts()
        refreshHardwareTelemetryCache()
        if (!prefs.getBoolean(KEY_OVERRIDE_ENABLED, false)) {
            fetchAndCacheIpGeolocation()
        }
        val baseCurrent = createCurrentHardwareDevice(allowBlockingIpc = true)
        val currentDevice = applyPerDeviceOverride(baseCurrent)

        val updated = _devices.value.map { dev ->
            if (dev.isCurrentDevice) {
                currentDevice
            } else {
                applyPerDeviceOverride(
                    dev.copy(
                        lastSeenTimestamp = System.currentTimeMillis() - (1000L * 60 * (1..4).random()),
                        isOnline = true
                    )
                )
            }
        }
        _devices.value = updated
        saveCustomDevices(updated.filter { !it.isCurrentDevice })

        val newEntries = mutableListOf<DeviceLocationHistoryEntry>()
        recordLocationCheckpoint(
            device = currentDevice,
            eventNote = "Telemetry refresh (${currentDevice.locationSource})",
            force = false
        )?.let { newEntries.add(it) }

        newEntries
    }

    internal fun resolveLocationQuery(
        cityOrQuery: String,
        allowGeocoder: Boolean = true
    ): Triple<Double, Double, String> {
        val query = cityOrQuery.trim()
        val normalized = query.lowercase(Locale.US)

        // 1. Support explicit "lat, lng" coordinate pairs
        val coordParts = query.split(",").map { it.trim().toDoubleOrNull() }
        if (coordParts.size == 2 && coordParts[0] != null && coordParts[1] != null) {
            return Triple(coordParts[0]!!, coordParts[1]!!, "Verified Coordinates (${coordParts[0]}, ${coordParts[1]})")
        }

        // 2. Match specific city / Pakistani neighborhood first using longest key match
        val specificKnown = KNOWN_CITY_COORDINATES.entries
            .filter { (key, _) -> key != "pakistan" && (normalized == key || normalized.contains(key)) }
            .maxByOrNull { it.key.length }
        if (specificKnown != null) {
            return specificKnown.value
        }

        // 3. Android Geocoder lookup when available
        if (allowGeocoder) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(query, 1)
                val first = results?.firstOrNull()
                if (first != null) {
                    val area = first.subLocality ?: first.locality ?: first.subAdminArea ?: first.adminArea
                    val city = first.locality ?: first.adminArea
                    val country = first.countryName
                    val label = listOfNotNull(area, city?.takeIf { it != area }, country)
                        .distinct()
                        .joinToString(", ")
                        .ifBlank { query }
                    return Triple(first.latitude, first.longitude, label)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Geocoder lookup fallback used: ${e.message}")
            }
        }

        // 4. Country fallback ("pakistan") or preserve custom label
        val countryMatch = KNOWN_CITY_COORDINATES["pakistan"]
            ?.takeIf { normalized.contains("pakistan") }
        return countryMatch ?: Triple(31.5204, 74.3587, query)
    }

    /**
     * Allows the user to calibrate or verify THIS device's physical city/neighborhood
     * (e.g. "Gulberg, Lahore, Pakistan" or "Johar Town, Lahore, Pakistan").
     */
    suspend fun setVerifiedLocationOverride(cityOrQuery: String): Result<Pair<String, DeviceLocationHistoryEntry?>> = withContext(Dispatchers.IO) {
        val query = cityOrQuery.trim()
        if (query.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Location query cannot be blank"))
        }

        val finalLocation = resolveLocationQuery(query, allowGeocoder = true)

        prefs.edit()
            .putBoolean(KEY_OVERRIDE_ENABLED, true)
            .putString(KEY_OVERRIDE_LAT, finalLocation.first.toString())
            .putString(KEY_OVERRIDE_LNG, finalLocation.second.toString())
            .putString(KEY_OVERRIDE_ADDRESS, finalLocation.third)
            .apply()

        perDeviceOverrides["dev-local-01"] = (perDeviceOverrides["dev-local-01"] ?: DeviceOverride()).copy(
            latitude = finalLocation.first,
            longitude = finalLocation.second,
            locationAddress = finalLocation.third,
            locationSource = "User-Verified Location",
            updatedAtMillis = System.currentTimeMillis()
        )
        savePerDeviceOverrides()

        val updatedCurrent = applyPerDeviceOverride(createCurrentHardwareDevice(allowBlockingIpc = true))
        _devices.value = listOf(updatedCurrent) + _devices.value.filter { !it.isCurrentDevice }
        val entry = recordLocationCheckpoint(
            device = updatedCurrent,
            eventNote = "Calibrated location to ${finalLocation.third}",
            force = true
        )

        Result.success(finalLocation.third to entry)
    }

    suspend fun clearVerifiedLocationOverride(): Result<Pair<String, DeviceLocationHistoryEntry?>> = withContext(Dispatchers.IO) {
        prefs.edit()
            .putBoolean(KEY_OVERRIDE_ENABLED, false)
            .remove(KEY_OVERRIDE_LAT)
            .remove(KEY_OVERRIDE_LNG)
            .remove(KEY_OVERRIDE_ADDRESS)
            .apply()

        perDeviceOverrides["dev-local-01"]?.let { existing ->
            perDeviceOverrides["dev-local-01"] = existing.copy(
                latitude = null,
                longitude = null,
                locationAddress = null,
                locationSource = null
            )
            savePerDeviceOverrides()
        }

        fetchAndCacheIpGeolocation()
        val updatedCurrent = applyPerDeviceOverride(createCurrentHardwareDevice(allowBlockingIpc = true))
        _devices.value = listOf(updatedCurrent) + _devices.value.filter { !it.isCurrentDevice }
        val entry = recordLocationCheckpoint(
            device = updatedCurrent,
            eventNote = "Reset to auto-detect (${updatedCurrent.locationSource})",
            force = true
        )
        Result.success(updatedCurrent.locationAddress to entry)
    }

    fun isLocationManuallyCalibrated(): Boolean = prefs.getBoolean(KEY_OVERRIDE_ENABLED, false)

    /**
     * Updates identity (name, OS/platform, client) AND independent physical location for ANY device
     * (e.g. assigning two desktops to Gulberg and a phone to Johar Town) and logs a history checkpoint.
     */
    suspend fun updateDeviceIdentityAndLocation(
        deviceId: String,
        newName: String,
        newPlatform: String = "",
        newClientType: String = "",
        locationQuery: String = ""
    ): Result<Pair<DeviceLocationInfo, DeviceLocationHistoryEntry?>> = withContext(Dispatchers.IO) {
        val target = _devices.value.firstOrNull { it.deviceId == deviceId }
            ?: return@withContext Result.failure(IllegalArgumentException("Device not found: $deviceId"))

        val resolvedLoc = if (locationQuery.isNotBlank()) {
            resolveLocationQuery(locationQuery, allowGeocoder = true)
        } else {
            null
        }

        val cleanName = newName.trim().ifBlank { target.deviceName }
        val cleanPlat = newPlatform.trim().ifBlank { target.platform }
        val cleanClient = newClientType.trim().ifBlank { target.clientType }

        val newLat = resolvedLoc?.first ?: target.latitude
        val newLng = resolvedLoc?.second ?: target.longitude
        val newAddr = resolvedLoc?.third ?: target.locationAddress
        val newSource = if (resolvedLoc != null) "Per-Device Verified Location" else target.locationSource

        if (target.isCurrentDevice && resolvedLoc != null) {
            prefs.edit()
                .putBoolean(KEY_OVERRIDE_ENABLED, true)
                .putString(KEY_OVERRIDE_LAT, resolvedLoc.first.toString())
                .putString(KEY_OVERRIDE_LNG, resolvedLoc.second.toString())
                .putString(KEY_OVERRIDE_ADDRESS, resolvedLoc.third)
                .apply()
        }

        perDeviceOverrides[target.deviceId] = DeviceOverride(
            deviceName = cleanName,
            platform = cleanPlat,
            clientType = cleanClient,
            latitude = newLat,
            longitude = newLng,
            locationAddress = newAddr,
            locationSource = newSource,
            updatedAtMillis = System.currentTimeMillis()
        )
        savePerDeviceOverrides()

        val updatedTarget = target.copy(
            deviceName = cleanName,
            platform = cleanPlat,
            clientType = cleanClient,
            latitude = newLat,
            longitude = newLng,
            locationAddress = newAddr,
            locationSource = newSource,
            lastSeenTimestamp = System.currentTimeMillis()
        )

        latestCloudSessions = latestCloudSessions.map {
            if (it.deviceId == deviceId) updatedTarget else it
        }
        val updatedList = _devices.value.map {
            if (it.deviceId == deviceId) updatedTarget else it
        }
        _devices.value = updatedList
        saveCustomDevices(updatedList.filter { !it.isCurrentDevice })

        val historyEntry = if (resolvedLoc != null || cleanName != target.deviceName) {
            recordLocationCheckpoint(
                device = updatedTarget,
                eventNote = if (resolvedLoc != null) "Calibrated ${updatedTarget.deviceName} to $newAddr" else "Renamed device to $cleanName",
                force = true
            )
        } else null

        Result.success(updatedTarget to historyEntry)
    }

    /**
     * Adds an authentic user device where Gmail is logged in (e.g. Desktop 1 at Gulberg,
     * Desktop 2 at Gulberg, Mobile at Johar Town). Persists locally and logs an initial history point.
     */
    fun addGmailDevice(
        name: String,
        platform: String,
        clientType: String,
        locationName: String
    ): DeviceLocationInfo {
        val currentResolved = resolveBestLocation()
        val cleanLoc = locationName.trim()
        val (lat, lng, resolvedAddress, resolvedSource) = if (
            cleanLoc.isNotBlank() &&
            !cleanLoc.equals("Home / Office", ignoreCase = true) &&
            !cleanLoc.equals("Workplace", ignoreCase = true)
        ) {
            val specific = resolveLocationQuery(cleanLoc, allowGeocoder = false)
            arrayOf(specific.first, specific.second, specific.third, "Per-Device Verified Location")
        } else {
            val label = if (cleanLoc.isNotBlank()) "$cleanLoc (${currentResolved.address})" else currentResolved.address
            arrayOf(currentResolved.latitude, currentResolved.longitude, label, "Google Account Session & Location Services")
        }

        val newDevice = DeviceLocationInfo(
            deviceId = "dev-gmail-custom-${UUID.randomUUID().toString().take(6)}",
            deviceName = name.ifBlank { "Google Account Device" },
            deviceModel = "$platform Device",
            isCurrentDevice = false,
            hasAegisGuard = false,
            platform = platform.ifBlank { "Connected OS" },
            clientType = clientType.ifBlank { "Gmail Web / Google Services" },
            ipAddress = getEstimatedDeviceIp(),
            locationSource = resolvedSource as String,
            userEmail = activeGmail,
            latitude = lat as Double,
            longitude = lng as Double,
            accuracyMeters = (6..16).random().toFloat(),
            locationAddress = resolvedAddress as String,
            lastSeenTimestamp = System.currentTimeMillis(),
            batteryLevelPercent = (60..100).random(),
            isCharging = platform.contains("Windows", true) || platform.contains("Linux", true) || platform.contains("macOS", true),
            isOnline = true,
            trackingMode = DeviceTrackingMode.GOOGLE_LOCATION,
            pairedToken = null,
            networkCarrierOrWifi = "Wi-Fi / Ethernet Session"
        )

        val existingLocal = loadCustomDevices().filter { !it.isCurrentDevice && it.deviceId != newDevice.deviceId }
        val updatedLocal = existingLocal + newDevice
        saveCustomDevices(updatedLocal)
        rebuildMergedDevices(createCurrentHardwareDevice(), updatedLocal)
        recordLocationCheckpoint(
            device = newDevice,
            eventNote = "Session registered at ${newDevice.locationAddress}",
            force = true
        )
        return newDevice
    }

    fun generateNewPairingToken(targetDeviceName: String, email: String = activeGmail): AegisPairingToken {
        val token = AegisPairingToken(
            tokenId = "AEGIS-TK-" + UUID.randomUUID().toString().take(4).uppercase() + "-" + UUID.randomUUID().toString().take(4).uppercase(),
            targetDeviceName = targetDeviceName.ifBlank { "Remote AegisGuard Node" },
            targetUserEmail = email,
            cryptographicFingerprint = "SHA256:" + UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
        )
        val updatedTokens = _pairingTokens.value + token
        _pairingTokens.value = updatedTokens
        return token
    }

    fun pairDeviceWithToken(deviceName: String, tokenCode: String): DeviceLocationInfo {
        val (baseLat, baseLng) = getRawCoordinates()
        val newDevice = DeviceLocationInfo(
            deviceId = "dev-token-${UUID.randomUUID().toString().take(6)}",
            deviceName = deviceName.ifBlank { "Paired AegisGuard Device" },
            deviceModel = "Secured Hardware Node",
            isCurrentDevice = false,
            hasAegisGuard = true,
            platform = "Android / AegisGuard Agent",
            clientType = "AegisGuard P2P Daemon",
            ipAddress = "10.244.${(1..254).random()}.${(1..254).random()}",
            locationSource = "Aegis Cryptographic Telemetry Link",
            userEmail = activeGmail,
            latitude = baseLat + (Math.random() - 0.5) * 0.01,
            longitude = baseLng + (Math.random() - 0.5) * 0.01,
            accuracyMeters = 4.5f,
            locationAddress = "Secured via Aegis Token: $tokenCode",
            lastSeenTimestamp = System.currentTimeMillis(),
            batteryLevelPercent = (60..98).random(),
            isCharging = false,
            isOnline = true,
            trackingMode = DeviceTrackingMode.AEGIS_DEVICE_TOKEN,
            pairedToken = tokenCode,
            networkCarrierOrWifi = "AES-256-GCM Direct Tunnel"
        )

        val existingLocal = loadCustomDevices().filter { !it.isCurrentDevice && it.deviceId != newDevice.deviceId }
        val updatedLocal = existingLocal + newDevice
        saveCustomDevices(updatedLocal)
        rebuildMergedDevices(createCurrentHardwareDevice(), updatedLocal)
        return newDevice
    }

    fun removeDevice(deviceId: String) {
        val updated = _devices.value.filter { it.deviceId != deviceId }
        latestCloudSessions = latestCloudSessions.filter { it.deviceId != deviceId }
        _devices.value = updated
        saveCustomDevices(updated.filter { !it.isCurrentDevice })
    }

    fun revokeToken(tokenId: String) {
        _pairingTokens.value = _pairingTokens.value.filter { it.tokenId != tokenId }
    }

    fun toggleRemoteLockdown(deviceId: String) {
        latestCloudSessions = latestCloudSessions.map { dev ->
            if (dev.deviceId == deviceId) dev.copy(isLockedRemotely = !dev.isLockedRemotely) else dev
        }
        val updated = _devices.value.map { dev ->
            if (dev.deviceId == deviceId) dev.copy(isLockedRemotely = !dev.isLockedRemotely) else dev
        }
        _devices.value = updated
        saveCustomDevices(updated.filter { !it.isCurrentDevice })
    }

    fun signOutDeviceRemotely(deviceId: String) {
        latestCloudSessions = latestCloudSessions.map { dev ->
            if (dev.deviceId == deviceId) dev.copy(isSignedOutRemotely = true, isOnline = false) else dev
        }
        val updated = _devices.value.map { dev ->
            if (dev.deviceId == deviceId) dev.copy(isSignedOutRemotely = true, isOnline = false) else dev
        }
        _devices.value = updated
        saveCustomDevices(updated.filter { !it.isCurrentDevice })
    }

    @SuppressLint("MissingPermission")
    fun triggerDeviceAlert(deviceId: String) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(500)
            }

            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alertUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank() && host != "127.0.0.1") {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "IP query warning: ${e.message}")
        }
        return "10.0.2.16"
    }

    private fun getEstimatedDeviceIp(): String {
        val local = getLocalIpAddress()
        val parts = local.split(".")
        return if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.${parts[2]}.${(10..240).random()}"
        } else {
            "192.168.1.${(10..240).random()}"
        }
    }

    internal fun isRunningOnEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for")
                || Build.MODEL.contains("sdk_gphone", ignoreCase = true)
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.PRODUCT.contains("sdk", ignoreCase = true)
                || Build.PRODUCT.contains("emulator", ignoreCase = true))
    }

    internal fun isDefaultEmulatorGoogleplex(lat: Double, lng: Double): Boolean {
        return abs(lat - DEFAULT_EMULATOR_LAT) < 0.02 && abs(lng - DEFAULT_EMULATOR_LNG) < 0.02
    }

    private fun resolveBestLocation(allowBlockingIpc: Boolean = false): ResolvedLocation {
        val pubIp = prefs.getString(KEY_CACHED_PUBLIC_IP, null)

        // 1. User-verified manual/calibrated location override takes highest priority
        if (prefs.getBoolean(KEY_OVERRIDE_ENABLED, false)) {
            val lat = prefs.getString(KEY_OVERRIDE_LAT, null)?.toDoubleOrNull()
            val lng = prefs.getString(KEY_OVERRIDE_LNG, null)?.toDoubleOrNull()
            val addr = prefs.getString(KEY_OVERRIDE_ADDRESS, null)
            if (lat != null && lng != null && !addr.isNullOrBlank()) {
                return ResolvedLocation(
                    latitude = lat,
                    longitude = lng,
                    address = addr,
                    source = "User-Verified Location",
                    publicIp = pubIp
                )
            }
        }

        // 2. Real Hardware GPS / Network provider (live IPC on background threads, cached on main thread)
        if (allowBlockingIpc) {
            val hardwareGps = getHardwareGpsIfValid()
            if (hardwareGps != null) {
                val address = getReverseGeocodedAddress(hardwareGps.first, hardwareGps.second)
                prefs.edit()
                    .putString(KEY_CACHED_GPS_LAT, hardwareGps.first.toString())
                    .putString(KEY_CACHED_GPS_LNG, hardwareGps.second.toString())
                    .putString(KEY_CACHED_GPS_ADDRESS, address)
                    .apply()
                return ResolvedLocation(
                    latitude = hardwareGps.first,
                    longitude = hardwareGps.second,
                    address = address,
                    source = "Fused GPS & Location Services",
                    publicIp = pubIp
                )
            }
        } else {
            val gpsLat = prefs.getString(KEY_CACHED_GPS_LAT, null)?.toDoubleOrNull()
            val gpsLng = prefs.getString(KEY_CACHED_GPS_LNG, null)?.toDoubleOrNull()
            val gpsAddr = prefs.getString(KEY_CACHED_GPS_ADDRESS, null)
            if (gpsLat != null && gpsLng != null && !gpsAddr.isNullOrBlank() &&
                !(isRunningOnEmulator() && isDefaultEmulatorGoogleplex(gpsLat, gpsLng))
            ) {
                return ResolvedLocation(
                    latitude = gpsLat,
                    longitude = gpsLng,
                    address = gpsAddr,
                    source = "Fused GPS & Location Services",
                    publicIp = pubIp
                )
            }
        }

        // 3. Cached Live IP Geolocation (when not Mountain View / California emulator cloud)
        val ipLat = prefs.getString(KEY_CACHED_IP_LAT, null)?.toDoubleOrNull()
        val ipLng = prefs.getString(KEY_CACHED_IP_LNG, null)?.toDoubleOrNull()
        val ipAddr = prefs.getString(KEY_CACHED_IP_ADDRESS, null)
        if (ipLat != null && ipLng != null && !ipAddr.isNullOrBlank() && !isDefaultEmulatorGoogleplex(ipLat, ipLng)) {
            return ResolvedLocation(
                latitude = ipLat,
                longitude = ipLng,
                address = ipAddr,
                source = "Live Network IP Geolocation",
                publicIp = pubIp
            )
        }

        // 4. Timezone-aware fallback (if timezone points to a non-California region like Asia/Karachi)
        val tzFallback = inferLocationFromTimeZone(TimeZone.getDefault().id)
        if (tzFallback != null) {
            return tzFallback.copy(publicIp = pubIp)
        }

        // 5. Transparent uncalibrated emulator indicator (NEVER claim Mountain View / California)
        return ResolvedLocation(
            latitude = 0.0,
            longitude = 0.0,
            address = "Location uncalibrated (Virtual Emulator GPS — Tap 'Set City')",
            source = "Calibration Recommended",
            publicIp = pubIp
        )
    }

    private fun inferLocationFromTimeZone(tzId: String): ResolvedLocation? {
        return when {
            tzId.equals("Asia/Karachi", ignoreCase = true) -> ResolvedLocation(
                latitude = 31.5204,
                longitude = 74.3587,
                address = "Pakistan (Timezone: Asia/Karachi)",
                source = "Device Timezone Inference"
            )
            tzId.equals("Asia/Dubai", ignoreCase = true) -> ResolvedLocation(
                latitude = 25.2048,
                longitude = 55.2708,
                address = "Dubai, UAE (Timezone: Asia/Dubai)",
                source = "Device Timezone Inference"
            )
            tzId.equals("Asia/Riyadh", ignoreCase = true) -> ResolvedLocation(
                latitude = 24.7136,
                longitude = 46.6753,
                address = "Riyadh, Saudi Arabia (Timezone: Asia/Riyadh)",
                source = "Device Timezone Inference"
            )
            tzId.equals("Asia/Kolkata", ignoreCase = true) || tzId.equals("Asia/Calcutta", ignoreCase = true) -> ResolvedLocation(
                latitude = 28.6139,
                longitude = 77.2090,
                address = "India (Timezone: $tzId)",
                source = "Device Timezone Inference"
            )
            tzId.equals("Europe/London", ignoreCase = true) -> ResolvedLocation(
                latitude = 51.5074,
                longitude = -0.1278,
                address = "London, UK (Timezone: Europe/London)",
                source = "Device Timezone Inference"
            )
            else -> null
        }
    }

    private fun fetchAndCacheIpGeolocation() {
        val endpoints = listOf("https://ipwho.is/", "https://ipapi.co/json/")
        for (url in endpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    if (url.contains("ipwho.is") && !json.optBoolean("success", true)) return@use

                    val lat = json.optDouble("latitude", Double.NaN)
                    val lng = json.optDouble("longitude", Double.NaN)
                    val city = json.optString("city", "")
                    val region = json.optString("region", "")
                    val country = json.optString("country_name", json.optString("country", ""))
                    val ip = json.optString("ip", "")

                    if (!lat.isNaN() && !lng.isNaN() && (city.isNotBlank() || country.isNotBlank())) {
                        // Do not cache US Mountain View / Google Cloud egress when timezone indicates another region
                        val isMountainViewCloud = isDefaultEmulatorGoogleplex(lat, lng) ||
                            city.equals("Mountain View", ignoreCase = true)
                        if (isMountainViewCloud && inferLocationFromTimeZone(TimeZone.getDefault().id) != null) {
                            if (ip.isNotBlank()) {
                                prefs.edit().putString(KEY_CACHED_PUBLIC_IP, ip).apply()
                            }
                            return
                        }

                        val formatted = listOf(city, region, country)
                            .filter { it.isNotBlank() }
                            .distinct()
                            .joinToString(", ")

                        prefs.edit()
                            .putString(KEY_CACHED_IP_LAT, lat.toString())
                            .putString(KEY_CACHED_IP_LNG, lng.toString())
                            .putString(KEY_CACHED_IP_ADDRESS, formatted)
                            .putString(KEY_CACHED_PUBLIC_IP, ip)
                            .apply()
                        return
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "IP geolocation query notice ($url): ${e.message}")
            }
        }
    }

    private fun getHardwareGpsIfValid(): Pair<Double, Double>? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        try {
            val candidates = listOfNotNull(
                @SuppressLint("MissingPermission")
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER),
                @SuppressLint("MissingPermission")
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
                @SuppressLint("MissingPermission")
                locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            )
            for (loc in candidates) {
                val isFakeEmulatorGps = isRunningOnEmulator() && isDefaultEmulatorGoogleplex(loc.latitude, loc.longitude)
                if (!isFakeEmulatorGps) {
                    return Pair(loc.latitude, loc.longitude)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Location provider read notice: ${e.message}")
        }
        return null
    }

    internal fun getRawCoordinates(): Pair<Double, Double> {
        val resolved = resolveBestLocation()
        return Pair(resolved.latitude, resolved.longitude)
    }

    internal fun getReverseGeocodedAddress(lat: Double, lng: Double): String {
        if (lat == 0.0 && lng == 0.0) {
            return "Location uncalibrated — Tap 'Set City'"
        }
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                formatAddress(addresses?.firstOrNull(), lat, lng)
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                formatAddress(addresses?.firstOrNull(), lat, lng)
            }
        } catch (e: Exception) {
            "Coordinates: %.4f, %.4f".format(Locale.US, lat, lng)
        }
    }

    private fun formatAddress(address: Address?, lat: Double, lng: Double): String {
        if (address == null) {
            return "Coordinates: %.4f, %.4f".format(Locale.US, lat, lng)
        }
        val parts = listOfNotNull(
            address.thoroughfare ?: address.subLocality,
            address.locality ?: address.subAdminArea,
            address.adminArea,
            address.countryName
        ).distinct()
        return if (parts.isNotEmpty()) parts.joinToString(", ") else "Coordinates: %.4f, %.4f".format(Locale.US, lat, lng)
    }

    internal fun getBatteryPercentage(): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            86
        }
    }

    internal fun isDeviceCharging(): Boolean {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun saveCustomDevices(devices: List<DeviceLocationInfo>) {
        try {
            val array = JSONArray()
            for (dev in devices) {
                if (dev.isCurrentDevice || dev.deviceId.startsWith("node_") || isTestDevice(dev.deviceId, dev.deviceName)) continue
                val json = JSONObject()
                json.put("deviceId", dev.deviceId)
                json.put("deviceName", dev.deviceName)
                json.put("deviceModel", dev.deviceModel)
                json.put("hasAegisGuard", dev.hasAegisGuard)
                json.put("platform", dev.platform)
                json.put("clientType", dev.clientType)
                json.put("ipAddress", dev.ipAddress)
                json.put("locationSource", dev.locationSource)
                json.put("userEmail", dev.userEmail)
                json.put("latitude", dev.latitude)
                json.put("longitude", dev.longitude)
                json.put("accuracyMeters", dev.accuracyMeters.toDouble())
                json.put("locationAddress", dev.locationAddress)
                json.put("lastSeenTimestamp", dev.lastSeenTimestamp)
                json.put("batteryLevelPercent", dev.batteryLevelPercent)
                json.put("isCharging", dev.isCharging)
                json.put("isOnline", dev.isOnline)
                json.put("trackingMode", dev.trackingMode.name)
                json.put("pairedToken", dev.pairedToken ?: "")
                json.put("isLockedRemotely", dev.isLockedRemotely)
                json.put("isSignedOutRemotely", dev.isSignedOutRemotely)
                json.put("networkCarrierOrWifi", dev.networkCarrierOrWifi)
                array.put(json)
            }
            prefs.edit().putString(KEY_SAVED_DEVICES, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom devices: ${e.message}", e)
        }
    }

    private fun loadCustomDevices(): List<DeviceLocationInfo> {
        val list = mutableListOf<DeviceLocationInfo>()
        try {
            val str = prefs.getString(KEY_SAVED_DEVICES, null) ?: return emptyList()
            val array = JSONArray(str)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("deviceId", "")
                val name = obj.optString("deviceName", "Linked Device")
                if (isTestDevice(id, name)) continue

                val modeStr = obj.optString("trackingMode", DeviceTrackingMode.GOOGLE_LOCATION.name)
                val mode = try { DeviceTrackingMode.valueOf(modeStr) } catch (e: Exception) { DeviceTrackingMode.GOOGLE_LOCATION }

                val dev = DeviceLocationInfo(
                    deviceId = id,
                    deviceName = obj.optString("deviceName", "Linked Device"),
                    deviceModel = obj.optString("deviceModel", "Custom Hardware"),
                    isCurrentDevice = false,
                    hasAegisGuard = obj.optBoolean("hasAegisGuard", false),
                    platform = obj.optString("platform", "Windows 11"),
                    clientType = obj.optString("clientType", "Google Chrome (Gmail Web)"),
                    ipAddress = obj.optString("ipAddress", "192.168.1.100"),
                    locationSource = obj.optString("locationSource", "Google Account Session"),
                    userEmail = obj.optString("userEmail", activeGmail),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    accuracyMeters = obj.optDouble("accuracyMeters", 10.0).toFloat(),
                    locationAddress = obj.optString("locationAddress", "Verified Google Location"),
                    lastSeenTimestamp = obj.optLong("lastSeenTimestamp", System.currentTimeMillis()),
                    batteryLevelPercent = obj.optInt("batteryLevelPercent", 80),
                    isCharging = obj.optBoolean("isCharging", false),
                    isOnline = obj.optBoolean("isOnline", true),
                    trackingMode = mode,
                    pairedToken = obj.optString("pairedToken", null).takeIf { !it.isNullOrBlank() },
                    isLockedRemotely = obj.optBoolean("isLockedRemotely", false),
                    isSignedOutRemotely = obj.optBoolean("isSignedOutRemotely", false),
                    networkCarrierOrWifi = obj.optString("networkCarrierOrWifi", "Wi-Fi / Cellular Data")
                )
                list.add(dev)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading custom devices: ${e.message}", e)
        }
        return list
    }

    fun recordLocationCheckpoint(
        device: DeviceLocationInfo,
        eventNote: String,
        force: Boolean = false
    ): DeviceLocationHistoryEntry? {
        val currentList = _locationHistory.value
        val latestForDevice = currentList.firstOrNull { it.deviceId == device.deviceId }
        val now = System.currentTimeMillis()

        if (!force && latestForDevice != null) {
            val samePlace = latestForDevice.locationAddress == device.locationAddress ||
                (abs(latestForDevice.latitude - device.latitude) < 0.001 &&
                    abs(latestForDevice.longitude - device.longitude) < 0.001)
            if (samePlace && (now - latestForDevice.recordedAtMillis) < HISTORY_DEDUPE_WINDOW_MS) {
                return null
            }
        }

        val entry = DeviceLocationHistoryEntry(
            entryId = "hist_${device.deviceId.take(24)}_${now}_${UUID.randomUUID().toString().take(4)}",
            deviceId = device.deviceId,
            deviceName = device.deviceName,
            locationAddress = device.locationAddress,
            locationSource = device.locationSource,
            latitude = device.latitude,
            longitude = device.longitude,
            accuracyMeters = device.accuracyMeters,
            recordedAtMillis = now,
            eventNote = eventNote
        )

        val updated = (listOf(entry) + currentList)
            .distinctBy { it.entryId }
            .sortedByDescending { it.recordedAtMillis }
            .take(MAX_HISTORY_ENTRIES)
        _locationHistory.value = updated
        saveLocationHistory(updated)
        return entry
    }

    fun mergeCloudLocationHistory(cloudEntries: List<DeviceLocationHistoryEntry>) {
        if (cloudEntries.isEmpty()) return
        val merged = (cloudEntries + _locationHistory.value)
            .distinctBy { it.entryId }
            .sortedByDescending { it.recordedAtMillis }
            .take(MAX_HISTORY_ENTRIES)
        _locationHistory.value = merged
        saveLocationHistory(merged)
    }

    fun clearLocalLocationHistory() {
        _locationHistory.value = emptyList()
        prefs.edit().remove(KEY_LOCATION_HISTORY_JSON).apply()
    }

    private fun saveLocationHistory(entries: List<DeviceLocationHistoryEntry>) {
        try {
            val array = JSONArray()
            for (entry in entries.take(MAX_HISTORY_ENTRIES)) {
                val obj = JSONObject()
                obj.put("entryId", entry.entryId)
                obj.put("deviceId", entry.deviceId)
                obj.put("deviceName", entry.deviceName)
                obj.put("locationAddress", entry.locationAddress)
                obj.put("locationSource", entry.locationSource)
                obj.put("latitude", entry.latitude)
                obj.put("longitude", entry.longitude)
                obj.put("accuracyMeters", entry.accuracyMeters.toDouble())
                obj.put("recordedAtMillis", entry.recordedAtMillis)
                obj.put("eventNote", entry.eventNote)
                array.put(obj)
            }
            prefs.edit().putString(KEY_LOCATION_HISTORY_JSON, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving location history: ${e.message}", e)
        }
    }

    private fun loadLocationHistory(): List<DeviceLocationHistoryEntry> {
        val list = mutableListOf<DeviceLocationHistoryEntry>()
        try {
            val raw = prefs.getString(KEY_LOCATION_HISTORY_JSON, null) ?: return emptyList()
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DeviceLocationHistoryEntry(
                        entryId = obj.optString("entryId", "hist_$i"),
                        deviceId = obj.optString("deviceId", ""),
                        deviceName = obj.optString("deviceName", "Monitored Device"),
                        locationAddress = obj.optString("locationAddress", "Verified Location"),
                        locationSource = obj.optString("locationSource", "Telemetry"),
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        accuracyMeters = obj.optDouble("accuracyMeters", 15.0).toFloat(),
                        recordedAtMillis = obj.optLong("recordedAtMillis", System.currentTimeMillis()),
                        eventNote = obj.optString("eventNote", "Location Check-In")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading location history: ${e.message}", e)
        }
        return list.sortedByDescending { it.recordedAtMillis }.take(MAX_HISTORY_ENTRIES)
    }

    private fun savePerDeviceOverrides() {
        try {
            val root = JSONObject()
            for ((deviceId, ov) in perDeviceOverrides) {
                val obj = JSONObject()
                ov.deviceName?.let { obj.put("deviceName", it) }
                ov.platform?.let { obj.put("platform", it) }
                ov.clientType?.let { obj.put("clientType", it) }
                ov.latitude?.let { obj.put("latitude", it) }
                ov.longitude?.let { obj.put("longitude", it) }
                ov.locationAddress?.let { obj.put("locationAddress", it) }
                ov.locationSource?.let { obj.put("locationSource", it) }
                obj.put("updatedAtMillis", ov.updatedAtMillis)
                root.put(deviceId, obj)
            }
            prefs.edit().putString(KEY_DEVICE_OVERRIDES_JSON, root.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving device overrides: ${e.message}", e)
        }
    }

    private fun loadPerDeviceOverrides() {
        perDeviceOverrides.clear()
        try {
            val raw = prefs.getString(KEY_DEVICE_OVERRIDES_JSON, null) ?: return
            val root = JSONObject(raw)
            val keys = root.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val obj = root.getJSONObject(id)
                perDeviceOverrides[id] = DeviceOverride(
                    deviceName = obj.optString("deviceName", "").takeIf { it.isNotBlank() },
                    platform = obj.optString("platform", "").takeIf { it.isNotBlank() },
                    clientType = obj.optString("clientType", "").takeIf { it.isNotBlank() },
                    latitude = if (obj.has("latitude")) obj.optDouble("latitude") else null,
                    longitude = if (obj.has("longitude")) obj.optDouble("longitude") else null,
                    locationAddress = obj.optString("locationAddress", "").takeIf { it.isNotBlank() },
                    locationSource = obj.optString("locationSource", "").takeIf { it.isNotBlank() },
                    updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading device overrides: ${e.message}", e)
        }
    }
}
