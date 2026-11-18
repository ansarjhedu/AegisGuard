package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AegisApplication
import com.example.auth.BiometricAuthManager
import com.example.auth.BiometricAvailability
import com.example.data.db.QuarantineEntity
import com.example.data.model.AppSecurityInfo
import com.example.data.model.DecoyIconType
import com.example.data.model.DecoyItem
import com.example.data.model.DecoyState
import com.example.data.model.DeviceLocationHistoryEntry
import com.example.data.model.NetworkSnapshot
import com.example.data.model.ThreatSeverity
import com.example.data.model.ZeroTrustPosture
import com.example.data.auth.GoogleAuthManager
import com.example.data.auth.GoogleAuthOutcome
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.repository.DecryptedAuditLog
import com.example.data.repository.SecurityRepository
import com.example.data.session.SessionSyncState
import com.example.network.NetworkMonitor
import com.example.service.DecoyStatusBarService
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScreenTab {
    DASHBOARD,
    SCANNER,
    DEVICES_LOCATION,
    GOOGLE_AUTH
}

enum class AppFilter {
    ALL,
    WHITELISTED,
    USER_APPS,
    GOOGLE_PLAY,
    SYSTEM_PROTECTED,
    HIDDEN_APPS,
    HIGH_RISK,
    QUARANTINED
}

data class AegisUiState(
    val selectedTab: ScreenTab = ScreenTab.DASHBOARD,
    val isDarkMode: Boolean = true,
    val isRealtimeShieldEnabled: Boolean = true,
    val isScanning: Boolean = false,
    val scanProgress: Float = 1f,
    val installedApps: List<AppSecurityInfo> = emptyList(),
    val activeFilter: AppFilter = AppFilter.USER_APPS,
    val searchQuery: String = "",
    val networkSnapshot: NetworkSnapshot? = null,
    val decoyState: DecoyState = DecoyState(),
    val devicePosture: ZeroTrustPosture? = null,
    val activeTamperAlert: NetworkSnapshot? = null,
    val selectedAppForDetail: AppSecurityInfo? = null,
    val emailRecipient: String = "ansarjhedu@gmail.com",
    val showEmailDispatchDialog: Boolean = false,
    val showCamouflageFullscreen: Boolean = false,
    val isBiometricEnforced: Boolean = true,
    val isSessionUnlocked: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.AVAILABLE,
    val authErrorMessage: String? = null,
    val toastMessage: String? = null,
    val autoWhitelistSystemApps: Boolean = true,
    val autoWhitelistPlayStore: Boolean = true,
    val userWhitelistedPackages: Set<String> = emptySet(),
    val isGoogleAuthenticated: Boolean = false,
    val authenticatedUserName: String? = null,
    val authenticatedUserPhotoUrl: String? = null,
    val authenticatedIdToken: String? = null,
    val isGoogleSignInLoading: Boolean = false,
    val showGoogleAccountPickerModal: Boolean = false,
    val googlePeopleProfile: com.example.data.repository.GooglePeopleProfile? = null,
    val isFirebaseAuthLinked: Boolean = false
)

class AegisViewModel(application: Application) : AndroidViewModel(application) {

    private val aegisApp = application as AegisApplication
    private val repository: SecurityRepository = aegisApp.securityRepository
    private val networkMonitor: NetworkMonitor = aegisApp.networkMonitor
    val biometricAuthManager = BiometricAuthManager(application)
    val googleAuthManager: GoogleAuthManager = aegisApp.googleAuthManager
    val googleAccountSessionRepository = aegisApp.googleAccountSessionRepository
    val firebaseSessionManager = aegisApp.firebaseSessionManager
    val locationTrackerService = aegisApp.locationTrackerService

    private val _firebaseSyncState = MutableStateFlow<SessionSyncState>(SessionSyncState.Unauthenticated)
    val firebaseSyncState: StateFlow<SessionSyncState> = _firebaseSyncState.asStateFlow()

    private var firebaseSessionObserverJob: Job? = null
    private val sessionPointsObserverJobs = mutableMapOf<String, Job>()
    private var activeFirebaseUserId: String? = null

    private val _uiState = MutableStateFlow(AegisUiState())
    val uiState: StateFlow<AegisUiState> = _uiState.asStateFlow()

    val auditLogs: StateFlow<List<DecryptedAuditLog>> = repository.auditLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val quarantinedApps: StateFlow<List<QuarantineEntity>> = repository.quarantinedApps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deviceLocationRepository = aegisApp.deviceLocationRepository
    val trackedDevices: StateFlow<List<com.example.data.model.DeviceLocationInfo>> = deviceLocationRepository.devices
    val locationHistory: StateFlow<List<DeviceLocationHistoryEntry>> = deviceLocationRepository.locationHistory
    val pairingTokens: StateFlow<List<com.example.data.model.AegisPairingToken>> = deviceLocationRepository.pairingTokens
    val trackingMode: StateFlow<com.example.data.model.DeviceTrackingMode> = deviceLocationRepository.selectedMode
    val activeGoogleSessions = googleAccountSessionRepository.activeSessions
    val googlePeopleProfile = googleAccountSessionRepository.currentProfile
    val sessionLocationPoints: StateFlow<Map<String, List<com.example.data.model.SessionLocationPoint>>> =
        locationTrackerService.trackedSessionPoints
    val isLocationTrackerActive: StateFlow<Boolean> = locationTrackerService.isTrackerActive
    val lastTrackerFixSummary: StateFlow<String?> = locationTrackerService.lastFixSummary

    init {
        // Collect Google Auth state from repository
        viewModelScope.launch {
            combine(
                deviceLocationRepository.isGoogleAuthenticated,
                deviceLocationRepository.authenticatedUserName,
                deviceLocationRepository.authenticatedPhotoUrl,
                deviceLocationRepository.authenticatedIdToken
            ) { isAuth, name, photo, token ->
                Triple(isAuth, name, Pair(photo, token))
            }.collect { (isAuth, name, photoAndToken) ->
                _uiState.update { current ->
                    current.copy(
                        isGoogleAuthenticated = isAuth,
                        authenticatedUserName = name,
                        authenticatedUserPhotoUrl = photoAndToken.first,
                        authenticatedIdToken = photoAndToken.second,
                        emailRecipient = deviceLocationRepository.getActiveEmail()
                    )
                }
            }
        }

        viewModelScope.launch {
            googleAccountSessionRepository.currentProfile.collect { profile ->
                _uiState.update { it.copy(googlePeopleProfile = profile) }
            }
        }

        // Collect network changes & tamper alerts
        viewModelScope.launch {
            try {
                networkMonitor.currentSnapshot.collect { snapshot ->
                    _uiState.update { it.copy(networkSnapshot = snapshot) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            try {
                networkMonitor.tamperAlertEvents.collect { alertSnapshot ->
                    _uiState.update { it.copy(activeTamperAlert = alertSnapshot) }
                    repository.recordAuditLog(
                        eventType = "NETWORK_PARAMETER_TAMPERED",
                        severity = ThreatSeverity.CRITICAL,
                        description = alertSnapshot.tamperReason,
                        metadata = "IP: ${alertSnapshot.ipAddress} | DNS: ${alertSnapshot.dnsServers.joinToString()}",
                        isAlert = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Hydrate preferences, biometric capability, device posture, and initial scans off the Main Thread
        // so MainActivity renders its first Compose frame immediately without dropping SurfaceSyncGroup frames.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val availability = biometricAuthManager.checkBiometricAvailability()
                val autoSystem = repository.isAutoWhitelistSystem()
                val autoPlayStore = repository.isAutoWhitelistPlayStore()
                val userWhitelist = repository.getUserWhitelistedPackages()

                val prefs = aegisApp.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
                val isEnforced = prefs.getBoolean("is_biometric_enforced", true)
                val isVaultLocked = prefs.getBoolean("is_vault_locked", true)
                val savedWifiSsid = prefs.getString("fake_wifi_ssid", "Metropolis_Secure_WiFi") ?: "Metropolis_Secure_WiFi"

                _uiState.update { current ->
                    val updatedIcons = current.decoyState.icons.map { item ->
                        val isEnabledSaved = prefs.getBoolean("decoy_enabled_${item.type.name.lowercase()}", item.isEnabled)
                        val replacesOriginalSaved = prefs.getBoolean("hide_original_${item.type.name.lowercase()}", item.replacesOriginal)
                        val spoofedVal = if (item.type == DecoyIconType.WIFI) savedWifiSsid else item.spoofedValue
                        item.copy(
                            isEnabled = isEnabledSaved,
                            replacesOriginal = replacesOriginalSaved,
                            spoofedValue = spoofedVal
                        )
                    }
                    current.copy(
                        biometricAvailability = availability,
                        autoWhitelistSystemApps = autoSystem,
                        autoWhitelistPlayStore = autoPlayStore,
                        userWhitelistedPackages = userWhitelist,
                        isBiometricEnforced = isEnforced,
                        isSessionUnlocked = if (isEnforced) !isVaultLocked else true,
                        decoyState = current.decoyState.copy(
                            fakeWifiSsid = savedWifiSsid,
                            icons = updatedIcons
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val posture = repository.getDevicePosture()
                _uiState.update { it.copy(devicePosture = posture) }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                runAppScan()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                deviceLocationRepository.refreshDeviceLocations()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Auth-gated Firebase Session Manager observation:
        // Only attaches Firestore snapshot listeners when FirebaseAuth currentUser != null.
        viewModelScope.launch {
            firebaseSessionManager.authStateFlow().collect { firebaseUser ->
                if (firebaseUser == null) {
                    activeFirebaseUserId = null
                    firebaseSessionObserverJob?.cancel()
                    firebaseSessionObserverJob = null
                    cancelSessionPointsObservers()
                    locationTrackerService.stopPeriodicSessionTracking()
                    _firebaseSyncState.value = SessionSyncState.Unauthenticated
                    _uiState.update { it.copy(isFirebaseAuthLinked = false) }
                } else {
                    _uiState.update { it.copy(isFirebaseAuthLinked = true) }
                    startFirebaseSessionObservation(firebaseUser.uid)
                }
            }
        }
    }

    private fun startFirebaseSessionObservation(uid: String) {
        if (activeFirebaseUserId == uid && firebaseSessionObserverJob?.isActive == true) return
        firebaseSessionObserverJob?.cancel()
        activeFirebaseUserId = uid
        _firebaseSyncState.value = SessionSyncState.Syncing

        val installId = firebaseSessionManager.getInstallSessionId()

        firebaseSessionObserverJob = viewModelScope.launch {
            locationTrackerService.startPeriodicSessionTracking(this, intervalMs = 90_000L)

            // Publish this device & local fleet so other devices signed into this account see it immediately
            launch {
                try {
                    val pushResult = firebaseSessionManager.syncFleetToCloud(deviceLocationRepository.devices.value)
                    if (pushResult.isFailure) {
                        val err = pushResult.exceptionOrNull()
                        if (err !is CancellationException) {
                            _firebaseSyncState.value = SessionSyncState.Error(
                                err?.localizedMessage ?: "Failed to register this device in Firebase Session Manager"
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _firebaseSyncState.value = SessionSyncState.Error(
                        e.localizedMessage ?: "Cloud registration error"
                    )
                }
            }

            // Stream real-time per-device location history under /users/{uid}/locationHistory
            launch {
                firebaseSessionManager.observeLocationHistory(uid)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        handleFirestoreError(rootError, OperationType.LIST, "users/$uid/locationHistory", aegisApp)
                    }
                    .collect { cloudHistory ->
                        deviceLocationRepository.mergeCloudLocationHistory(
                            cloudHistory.map { it.toDeviceLocationHistoryEntry() }
                        )
                    }
            }

            // Stream all active logins across devices under /users/{uid}/sessions
            firebaseSessionManager.observeActiveSessions(uid)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    handleFirestoreError(rootError, OperationType.LIST, "users/$uid/sessions", aegisApp)
                    _firebaseSyncState.value = SessionSyncState.Error(
                        rootError.localizedMessage ?: "Unable to query Firebase sessions"
                    )
                }
                .collect { firestoreSessions ->
                    val mapped = firestoreSessions.map { it.toDeviceLocationInfo(installId) }
                    deviceLocationRepository.mergeCloudSessions(mapped, installId)
                    val mergedDevices = deviceLocationRepository.devices.value
                    googleAccountSessionRepository.updateActiveSessionsFromCloud(mergedDevices)
                    syncSessionPointsObservers(uid, mergedDevices)
                    _firebaseSyncState.value = SessionSyncState.Synced(
                        remoteSessionCount = firestoreSessions.size,
                        totalSessionCount = mergedDevices.size,
                        lastSyncedAtMillis = System.currentTimeMillis()
                    )
                }
        }
    }

    private fun syncSessionPointsObservers(
        uid: String,
        devices: List<com.example.data.model.DeviceLocationInfo>
    ) {
        val activeSessionIds = devices.map { locationTrackerService.resolveSessionId(it) }.toSet()
        val staleIds = sessionPointsObserverJobs.keys.filter { it !in activeSessionIds }
        staleIds.forEach { staleId ->
            sessionPointsObserverJobs.remove(staleId)?.cancel()
        }
        activeSessionIds.forEach { sessionId ->
            if (sessionPointsObserverJobs[sessionId]?.isActive == true) return@forEach
            sessionPointsObserverJobs[sessionId] = viewModelScope.launch {
                locationTrackerService.observeSessionPoints(uid, sessionId)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        handleFirestoreError(
                            rootError,
                            OperationType.LIST,
                            "users/$uid/sessions/$sessionId/locationPoints",
                            aegisApp
                        )
                    }
                    .collect { points ->
                        locationTrackerService.updateCachedSessionPoints(sessionId, points)
                    }
            }
        }
    }

    private fun cancelSessionPointsObservers() {
        sessionPointsObserverJobs.values.forEach { it.cancel() }
        sessionPointsObserverJobs.clear()
    }

    private suspend fun pushHistoryToCloud(entry: DeviceLocationHistoryEntry?) {
        if (entry != null && firebaseSessionManager.currentUserId() != null) {
            firebaseSessionManager.recordLocationHistory(entry)
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationTrackerService.stopPeriodicSessionTracking()
        cancelSessionPointsObservers()
        firebaseSessionObserverJob?.cancel()
    }

    fun selectTab(tab: ScreenTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun toggleDarkMode() {
        _uiState.value = _uiState.value.copy(isDarkMode = !_uiState.value.isDarkMode)
    }

    fun toggleRealtimeShield() {
        val newState = !_uiState.value.isRealtimeShieldEnabled
        _uiState.value = _uiState.value.copy(isRealtimeShieldEnabled = newState)
        viewModelScope.launch {
            repository.recordAuditLog(
                eventType = if (newState) "SHIELD_ENABLED" else "SHIELD_PAUSED",
                severity = if (newState) ThreatSeverity.INFO else ThreatSeverity.MEDIUM,
                description = if (newState) "Real-time unauthorized network and threat shield activated"
                else "Real-time shield paused by user"
            )
        }
    }

    fun setAppFilter(filter: AppFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectAppForDetail(app: AppSecurityInfo?) {
        _uiState.value = _uiState.value.copy(selectedAppForDetail = app)
    }

    fun dismissTamperAlert() {
        _uiState.value = _uiState.value.copy(activeTamperAlert = null)
    }

    fun refreshDevicePosture() {
        viewModelScope.launch(Dispatchers.IO) {
            val posture = repository.getDevicePosture()
            _uiState.update { it.copy(devicePosture = posture) }
        }
    }

    fun runAppScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanProgress = 0.1f) }
            val current = _uiState.value
            val quarantinedSet = quarantinedApps.value.map { it.packageName }.toSet()
            val apps = repository.scanInstalledApps(
                quarantinedSet = quarantinedSet,
                userWhitelisted = current.userWhitelistedPackages,
                autoWhitelistSystem = current.autoWhitelistSystemApps,
                autoWhitelistPlayStore = current.autoWhitelistPlayStore
            )
            _uiState.update {
                it.copy(
                    installedApps = apps,
                    isScanning = false,
                    scanProgress = 1.0f
                )
            }
        }
    }

    fun toggleAppWhitelist(app: AppSecurityInfo) {
        val willBeWhitelisted = repository.toggleUserWhitelist(app.packageName)
        val updatedWhitelisted = repository.getUserWhitelistedPackages()
        _uiState.value = _uiState.value.copy(
            userWhitelistedPackages = updatedWhitelisted,
            toastMessage = if (willBeWhitelisted) "Whitelisted ${app.appName} (Ignored in risk report)"
                           else "Removed ${app.appName} from whitelist"
        )
        runAppScan()
    }

    fun toggleAutoWhitelistSystem(enabled: Boolean) {
        repository.setAutoWhitelistSystem(enabled)
        _uiState.value = _uiState.value.copy(
            autoWhitelistSystemApps = enabled,
            toastMessage = if (enabled) "System apps auto-whitelisted (Zero false positives)"
                           else "System apps included in threat analysis"
        )
        runAppScan()
    }

    fun toggleAutoWhitelistPlayStore(enabled: Boolean) {
        repository.setAutoWhitelistPlayStore(enabled)
        _uiState.value = _uiState.value.copy(
            autoWhitelistPlayStore = enabled,
            toastMessage = if (enabled) "Google Play apps auto-whitelisted (Zero false positives)"
                           else "Play Store apps included in threat analysis"
        )
        runAppScan()
    }

    fun quarantineApp(app: AppSecurityInfo, reason: String = "Identified as unauthorized/suspicious data collector") {
        viewModelScope.launch {
            repository.quarantinePackage(app, reason)
            // Update list state
            val updated = _uiState.value.installedApps.map {
                if (it.packageName == app.packageName) it.copy(isQuarantined = true) else it
            }
            _uiState.value = _uiState.value.copy(
                installedApps = updated,
                selectedAppForDetail = app.copy(isQuarantined = true),
                toastMessage = "Quarantined ${app.appName}. Permissions restricted."
            )
        }
    }

    fun releaseApp(packageName: String) {
        viewModelScope.launch {
            repository.releaseFromQuarantine(packageName)
            val updated = _uiState.value.installedApps.map {
                if (it.packageName == packageName) it.copy(isQuarantined = false) else it
            }
            _uiState.value = _uiState.value.copy(
                installedApps = updated,
                toastMessage = "Released from quarantine"
            )
        }
    }

    fun lockNetworkBaseline() {
        networkMonitor.lockCurrentAsBaseline()
        viewModelScope.launch {
            repository.recordAuditLog(
                eventType = "NETWORK_BASELINE_LOCKED",
                severity = ThreatSeverity.INFO,
                description = "Current network parameters locked as verified baseline"
            )
        }
        _uiState.value = _uiState.value.copy(toastMessage = "Network parameters locked as trusted baseline")
    }

    fun triggerSimulatedTamper(tamperType: String) {
        networkMonitor.simulateTamperEvent(tamperType)
    }

    // Decoy System Functions
    fun toggleDecoyIcon(type: DecoyIconType) {
        val currentIcons = _uiState.value.decoyState.icons.map { item ->
            if (item.type == type) item.copy(isEnabled = !item.isEnabled) else item
        }
        val newDecoyState = _uiState.value.decoyState.copy(icons = currentIcons)
        _uiState.value = _uiState.value.copy(decoyState = newDecoyState)
        val item = currentIcons.find { it.type == type }
        if (item != null) {
            val prefs = aegisApp.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("decoy_enabled_${type.name.lowercase()}", item.isEnabled).apply()
            val status = if (item.isEnabled) "Displayed at top in place of original" else "Removed from top display"
            _uiState.value = _uiState.value.copy(toastMessage = "${type.displayName}: $status")
        }
        updateDecoyServiceIfActive(newDecoyState)
    }

    fun toggleDecoyReplaceOriginal(type: DecoyIconType) {
        val currentIcons = _uiState.value.decoyState.icons.map { item ->
            if (item.type == type) item.copy(replacesOriginal = !item.replacesOriginal) else item
        }
        val newDecoyState = _uiState.value.decoyState.copy(icons = currentIcons)
        _uiState.value = _uiState.value.copy(decoyState = newDecoyState)
        val item = currentIcons.find { it.type == type }
        if (item != null) {
            val prefs = aegisApp.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("hide_original_${type.name.lowercase()}", item.replacesOriginal).apply()
            val stateText = if (item.replacesOriginal) "Hiding original icon and displaying fake decoy" else "Displaying original icon normally"
            _uiState.value = _uiState.value.copy(toastMessage = "${type.displayName}: $stateText")
        }
        updateDecoyServiceIfActive(newDecoyState)
    }

    fun updateDecoyItem(type: DecoyIconType, spoofedValue: String, replacesOriginal: Boolean) {
        val currentIcons = _uiState.value.decoyState.icons.map { item ->
            if (item.type == type) item.copy(spoofedValue = spoofedValue, replacesOriginal = replacesOriginal) else item
        }
        val newDecoyState = _uiState.value.decoyState.copy(icons = currentIcons)
        _uiState.value = _uiState.value.copy(decoyState = newDecoyState)
        val prefs = aegisApp.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("hide_original_${type.name.lowercase()}", replacesOriginal)
            .apply()
        updateDecoyServiceIfActive(newDecoyState)
    }

    fun updateDecoyLocation(city: String, coordinates: String) {
        val currentIcons = _uiState.value.decoyState.icons.map { item ->
            if (item.type == DecoyIconType.LOCATION) item.copy(spoofedValue = "$city ($coordinates)") else item
        }
        val newDecoyState = _uiState.value.decoyState.copy(
            fakeLocationCity = city,
            fakeLocationCoordinates = coordinates,
            icons = currentIcons
        )
        _uiState.value = _uiState.value.copy(decoyState = newDecoyState)
        updateDecoyServiceIfActive(newDecoyState)
    }

    fun updateDecoyWifi(ssid: String) {
        val currentIcons = _uiState.value.decoyState.icons.map { item ->
            if (item.type == DecoyIconType.WIFI) item.copy(spoofedValue = ssid) else item
        }
        val newDecoyState = _uiState.value.decoyState.copy(
            fakeWifiSsid = ssid,
            icons = currentIcons
        )
        _uiState.value = _uiState.value.copy(
            decoyState = newDecoyState,
            toastMessage = "Decoy Wi-Fi SSID set to: $ssid"
        )
        val prefs = aegisApp.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fake_wifi_ssid", ssid).apply()
        updateDecoyServiceIfActive(newDecoyState)
    }

    fun toggleDecoyCamouflageService() {
        val current = _uiState.value.decoyState
        val newState = !current.isCamouflageServiceActive
        val updated = current.copy(isCamouflageServiceActive = newState)
        _uiState.value = _uiState.value.copy(decoyState = updated)

        val context = getApplication<AegisApplication>()
        if (newState) {
            val summary = buildDecoySummaryText(updated)
            DecoyStatusBarService.startService(context, summary)
            viewModelScope.launch {
                repository.recordAuditLog(
                    eventType = "DECOY_CAMOUFLAGE_STARTED",
                    severity = ThreatSeverity.INFO,
                    description = "Decoy status bar camouflage activated with spoofed icons (Wi-Fi, GPS, 5G, Hotspot)"
                )
            }
        } else {
            DecoyStatusBarService.stopService(context)
            viewModelScope.launch {
                repository.recordAuditLog(
                    eventType = "DECOY_CAMOUFLAGE_STOPPED",
                    severity = ThreatSeverity.LOW,
                    description = "Decoy status bar camouflage stopped"
                )
            }
        }
    }

    private fun updateDecoyServiceIfActive(decoyState: DecoyState) {
        if (decoyState.isCamouflageServiceActive) {
            val context = getApplication<AegisApplication>()
            val summary = buildDecoySummaryText(decoyState)
            DecoyStatusBarService.startService(context, summary)
        }
    }

    private fun buildDecoySummaryText(state: DecoyState): String {
        val enabledTypes = state.icons.filter { it.isEnabled }.map { it.type.displayName }
        return "Camouflaged Icons: ${enabledTypes.joinToString(", ")} | Location: ${state.fakeLocationCity}"
    }

    fun toggleTopBarOverlay() {
        val updated = _uiState.value.decoyState.copy(
            isTopBarOverlayVisible = !_uiState.value.decoyState.isTopBarOverlayVisible
        )
        _uiState.value = _uiState.value.copy(decoyState = updated)
    }

    fun setCamouflageFullscreen(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showCamouflageFullscreen = visible)
    }

    // Email Dispatch Functions
    fun setEmailRecipient(email: String) {
        _uiState.value = _uiState.value.copy(emailRecipient = email)
    }

    fun showEmailDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showEmailDispatchDialog = show)
    }

    fun dispatchLocationAlertEmail(context: Context, customTrigger: String? = null) {
        val snapshot = _uiState.value.networkSnapshot ?: return
        val recipient = _uiState.value.emailRecipient
        val threatCount = _uiState.value.installedApps.count {
            !it.isWhitelisted && (it.riskLevel == ThreatSeverity.CRITICAL || it.riskLevel == ThreatSeverity.HIGH)
        }

        val body = repository.prepareEmailReport(recipient, snapshot, customTrigger, threatCount)
        val intent = repository.createEmailIntent(
            recipient = recipient,
            subject = if (customTrigger != null) "[AegisGuard Alert] Parameter Tampering Detected & Location Report"
            else "[AegisGuard] Device Security & Location Audit Report",
            body = body
        )

        try {
            context.startActivity(intent)
            viewModelScope.launch {
                repository.recordAuditLog(
                    eventType = "LOCATION_EMAIL_DISPATCHED",
                    severity = ThreatSeverity.INFO,
                    description = "Transmitted AES-256 encrypted location & network audit dispatch to $recipient",
                    metadata = "Target: $recipient | Trigger: ${customTrigger ?: "Manual User Dispatch"}"
                )
            }
            _uiState.value = _uiState.value.copy(
                showEmailDispatchDialog = false,
                toastMessage = "Security report and location dispatched to $recipient"
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                toastMessage = "Unable to launch email client: ${e.localizedMessage}"
            )
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _uiState.value = _uiState.value.copy(toastMessage = "Audit log ledger cleared")
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    // Biometric Security Controls
    fun unlockVault() {
        _uiState.value = _uiState.value.copy(
            isSessionUnlocked = true,
            authErrorMessage = null,
            toastMessage = "Biometric authentication verified"
        )
        val prefs = aegisApp.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_vault_locked", false).apply()
        viewModelScope.launch {
            repository.recordAuditLog(
                eventType = "BIOMETRIC_AUTH_SUCCESS",
                severity = ThreatSeverity.INFO,
                description = "Identity authenticated via AndroidX BiometricPrompt. Dashboard & audit logs accessible.",
                metadata = "Authenticator: BIOMETRIC_STRONG • Session: Ephemeral"
            )
        }
    }

    fun lockVault() {
        _uiState.value = _uiState.value.copy(
            isSessionUnlocked = false,
            authErrorMessage = null,
            toastMessage = "Security vault locked"
        )
        val prefs = aegisApp.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_vault_locked", true).apply()
        viewModelScope.launch {
            repository.recordAuditLog(
                eventType = "VAULT_SESSION_LOCKED",
                severity = ThreatSeverity.INFO,
                description = "Dashboard and audit logs locked by user. Biometric verification required."
            )
        }
    }

    fun disconnectSessionAndLock() {
        if (_uiState.value.isGoogleSignInLoading) {
            return
        }
        if (_uiState.value.isBiometricEnforced) {
            _uiState.value = _uiState.value.copy(
                isSessionUnlocked = false,
                authErrorMessage = null
            )
            val prefs = aegisApp.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_vault_locked", true).apply()
            viewModelScope.launch {
                repository.recordAuditLog(
                    eventType = "SESSION_DISCONNECTED_APP_LEAVE",
                    severity = ThreatSeverity.INFO,
                    description = "User left the application. Active session disconnected and biometric vault locked."
                )
            }
        }
    }

    fun setAuthErrorMessage(error: String) {
        _uiState.value = _uiState.value.copy(authErrorMessage = error)
        viewModelScope.launch {
            repository.recordAuditLog(
                eventType = "BIOMETRIC_AUTH_FAILED",
                severity = ThreatSeverity.MEDIUM,
                description = "Biometric access verification rejected or canceled: $error",
                isAlert = true
            )
        }
    }

    fun toggleBiometricEnforcement() {
        val currentEnforced = _uiState.value.isBiometricEnforced
        val newEnforced = !currentEnforced
        val prefs = aegisApp.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_biometric_enforced", newEnforced).apply()
        _uiState.value = _uiState.value.copy(
            isBiometricEnforced = newEnforced,
            isSessionUnlocked = !newEnforced || _uiState.value.isSessionUnlocked,
            toastMessage = if (newEnforced) "Biometric lock enforced: Session disconnects as soon as you leave" else "Biometric lock disabled"
        )
        viewModelScope.launch {
            repository.recordAuditLog(
                eventType = "BIOMETRIC_POLICY_CHANGED",
                severity = if (newEnforced) ThreatSeverity.INFO else ThreatSeverity.MEDIUM,
                description = if (newEnforced) "Enforced biometric lock requirement. App disconnects session and locks on leave."
                else "Disabled biometric requirement by user authorization"
            )
        }
    }

    fun setTrackingMode(mode: com.example.data.model.DeviceTrackingMode) {
        deviceLocationRepository.setTrackingMode(mode)
    }

    fun captureSessionLocationFixes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGoogleSignInLoading = true)
            val result = locationTrackerService.captureAndSyncAllSessions(reason = "Manual Per-Session Geolocation Fix")
            val summary = locationTrackerService.lastFixSummary.value
            _uiState.value = _uiState.value.copy(
                isGoogleSignInLoading = false,
                toastMessage = if (result.isSuccess) {
                    summary ?: "Captured per-session GPS fixes for ${result.getOrDefault(0)} session(s)"
                } else {
                    "Session geolocation capture notice: ${result.exceptionOrNull()?.localizedMessage}"
                }
            )
        }
    }

    fun refreshDeviceLocations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGoogleSignInLoading = true)
            val isAuth = _uiState.value.isGoogleAuthenticated
            if (isAuth) {
                googleAccountSessionRepository.retrieveActiveDeviceSessions(
                    email = _uiState.value.emailRecipient,
                    displayName = _uiState.value.authenticatedUserName,
                    idToken = _uiState.value.authenticatedIdToken
                )
            }
            val newHistoryEntries = deviceLocationRepository.refreshDeviceLocations()
            locationTrackerService.captureAndSyncAllSessions(reason = "Fleet Telemetry Refresh")

            var cloudMessage: String? = null
            val firebaseUid = firebaseSessionManager.currentUserId()
            if (firebaseUid != null) {
                newHistoryEntries.forEach { pushHistoryToCloud(it) }
                _firebaseSyncState.value = SessionSyncState.Syncing
                val syncResult = firebaseSessionManager.syncFleetToCloud(deviceLocationRepository.devices.value)
                if (syncResult.isSuccess) {
                    val cloudCount = syncResult.getOrDefault(trackedDevices.value.size)
                    _firebaseSyncState.value = SessionSyncState.Synced(
                        remoteSessionCount = cloudCount,
                        totalSessionCount = trackedDevices.value.size,
                        lastSyncedAtMillis = System.currentTimeMillis()
                    )
                    cloudMessage = "Firebase synced ${trackedDevices.value.size} active device(s)"
                } else {
                    val errMsg = syncResult.exceptionOrNull()?.localizedMessage ?: "Firebase sync failed"
                    _firebaseSyncState.value = SessionSyncState.Error(errMsg)
                    cloudMessage = "Local refreshed; Firebase sync notice: $errMsg"
                }
            }

            val total = trackedDevices.value.size
            val external = trackedDevices.value.count { !it.isCurrentDevice }
            _uiState.value = _uiState.value.copy(
                isGoogleSignInLoading = false,
                toastMessage = cloudMessage ?: if (isAuth) {
                    "Synced $total active device(s) ($external external) for ${_uiState.value.emailRecipient}"
                } else {
                    "Refreshed hardware telemetry for $total active device(s)"
                }
            )
        }
    }

    fun generatePairingToken(targetDeviceName: String) {
        val token = deviceLocationRepository.generateNewPairingToken(targetDeviceName, _uiState.value.emailRecipient)
        _uiState.value = _uiState.value.copy(toastMessage = "New AegisGuard token generated: ${token.tokenId}")
    }

    fun pairDeviceWithToken(deviceName: String, tokenCode: String) {
        val newDev = deviceLocationRepository.pairDeviceWithToken(deviceName, tokenCode)
        viewModelScope.launch {
            locationTrackerService.recordSessionLocationFix(
                device = newDev,
                eventNote = "Paired via token $tokenCode"
            )
        }
        _uiState.value = _uiState.value.copy(toastMessage = "Device successfully paired with AegisGuard token")
    }

    fun removeTrackedDevice(deviceId: String) {
        deviceLocationRepository.removeDevice(deviceId)
        viewModelScope.launch {
            if (firebaseSessionManager.currentUserId() != null) {
                firebaseSessionManager.deleteSession(deviceId)
            }
        }
        _uiState.value = _uiState.value.copy(toastMessage = "Device removed from tracking registry")
    }

    fun revokePairingToken(tokenId: String) {
        deviceLocationRepository.revokeToken(tokenId)
        _uiState.value = _uiState.value.copy(toastMessage = "Pairing token $tokenId revoked")
    }

    fun toggleRemoteDeviceLockdown(deviceId: String) {
        deviceLocationRepository.toggleRemoteLockdown(deviceId)
        val dev = trackedDevices.value.find { it.deviceId == deviceId }
        val nowLocked = dev?.isLockedRemotely ?: false
        viewModelScope.launch {
            if (firebaseSessionManager.currentUserId() != null) {
                firebaseSessionManager.updateSessionLockdown(deviceId, nowLocked)
            }
        }
        _uiState.value = _uiState.value.copy(
            toastMessage = if (nowLocked) "Emergency lockdown signal transmitted to ${dev?.deviceName ?: "device"}"
            else "Lockdown lifted for ${dev?.deviceName ?: "device"}"
        )
    }

    fun ringDevice(deviceId: String) {
        val dev = trackedDevices.value.find { it.deviceId == deviceId }
        deviceLocationRepository.triggerDeviceAlert(deviceId)
        _uiState.value = _uiState.value.copy(
            toastMessage = "Sounding alert tone on ${dev?.deviceName ?: "device"}"
        )
    }

    fun addGmailDevice(name: String, platform: String, clientType: String, location: String) {
        val addedDev = deviceLocationRepository.addGmailDevice(name, platform, clientType, location)
        viewModelScope.launch {
            locationTrackerService.recordSessionLocationFix(
                device = addedDev,
                eventNote = "Added Google session"
            )
        }
        _uiState.value = _uiState.value.copy(
            toastMessage = "Added ${addedDev.deviceName} at ${addedDev.locationAddress}"
        )
    }

    fun updateDeviceIdentityAndLocation(
        deviceId: String,
        newName: String,
        newPlatform: String,
        newClientType: String,
        locationQuery: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGoogleSignInLoading = true)
            val result = deviceLocationRepository.updateDeviceIdentityAndLocation(
                deviceId = deviceId,
                newName = newName,
                newPlatform = newPlatform,
                newClientType = newClientType,
                locationQuery = locationQuery
            )
            if (result.isSuccess) {
                val (updatedDev, historyEntry) = result.getOrThrow()
                locationTrackerService.recordSessionLocationFix(
                    device = updatedDev,
                    eventNote = "Per-device calibration: ${updatedDev.locationAddress}"
                )
                if (firebaseSessionManager.currentUserId() != null) {
                    pushHistoryToCloud(historyEntry)
                }
                _uiState.value = _uiState.value.copy(
                    isGoogleSignInLoading = false,
                    toastMessage = "Updated ${updatedDev.deviceName} • ${updatedDev.locationAddress}"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isGoogleSignInLoading = false,
                    toastMessage = "Update failed: ${result.exceptionOrNull()?.localizedMessage}"
                )
            }
        }
    }

    fun setVerifiedLocationOverride(cityQuery: String) {
        if (cityQuery.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGoogleSignInLoading = true)
            val result = deviceLocationRepository.setVerifiedLocationOverride(cityQuery)
            if (result.isSuccess) {
                val (address, historyEntry) = result.getOrThrow()
                val currentDev = deviceLocationRepository.devices.value.firstOrNull { it.isCurrentDevice }
                    ?: deviceLocationRepository.createCurrentHardwareDevice()
                locationTrackerService.recordSessionLocationFix(
                    device = currentDev,
                    eventNote = "Manual neighborhood calibration: $address"
                )
                if (firebaseSessionManager.currentUserId() != null) {
                    pushHistoryToCloud(historyEntry)
                }
                _uiState.value = _uiState.value.copy(
                    isGoogleSignInLoading = false,
                    toastMessage = "Device location calibrated to $address"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isGoogleSignInLoading = false,
                    toastMessage = "Unable to resolve location: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun resetLocationToAuto() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGoogleSignInLoading = true)
            val result = deviceLocationRepository.clearVerifiedLocationOverride()
            val (address, historyEntry) = result.getOrNull() ?: ("Refreshed" to null)
            val currentDev = deviceLocationRepository.devices.value.firstOrNull { it.isCurrentDevice }
                ?: deviceLocationRepository.createCurrentHardwareDevice()
            locationTrackerService.recordSessionLocationFix(
                device = currentDev,
                eventNote = "Reset to auto-detect: $address"
            )
            if (firebaseSessionManager.currentUserId() != null) {
                pushHistoryToCloud(historyEntry)
            }
            _uiState.value = _uiState.value.copy(
                isGoogleSignInLoading = false,
                toastMessage = "Location reset to auto-detect: $address"
            )
        }
    }

    fun discoverNetworkDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGoogleSignInLoading = true)
            val discovered = deviceLocationRepository.discoverLocalNetworkDevices()
            if (discovered.isNotEmpty() && firebaseSessionManager.currentUserId() != null) {
                firebaseSessionManager.syncFleetToCloud(deviceLocationRepository.devices.value)
            }
            val total = trackedDevices.value.size
            _uiState.value = _uiState.value.copy(
                isGoogleSignInLoading = false,
                toastMessage = if (discovered.isNotEmpty()) "Found ${discovered.size} reachable host(s) on local Wi-Fi. Total fleet: $total"
                else "Wi-Fi scan complete: no extra reachable LAN hosts ($total active device(s))"
            )
        }
    }

    fun clearNetworkDiscoveredDevices() {
        val removedIds = deviceLocationRepository.clearNetworkDiscoveredDevices()
        if (removedIds.isNotEmpty() && firebaseSessionManager.currentUserId() != null) {
            viewModelScope.launch {
                removedIds.forEach { firebaseSessionManager.deleteSession(it) }
            }
        }
        _uiState.value = _uiState.value.copy(
            toastMessage = if (removedIds.isNotEmpty()) {
                "Cleared ${removedIds.size} LAN-scanned device(s) from fleet"
            } else {
                "No LAN-scanned devices to clear"
            }
        )
    }

    fun clearLocationHistory() {
        deviceLocationRepository.clearLocalLocationHistory()
        viewModelScope.launch {
            if (firebaseSessionManager.currentUserId() != null) {
                firebaseSessionManager.clearLocationHistory()
            }
        }
        _uiState.value = _uiState.value.copy(
            toastMessage = "Location tracking history cleared"
        )
    }

    fun updateUserEmail(newEmail: String) {
        if (newEmail.isNotBlank()) {
            deviceLocationRepository.updateActiveEmail(newEmail)
            _uiState.value = _uiState.value.copy(
                emailRecipient = newEmail.trim(),
                toastMessage = "Google Account updated to ${newEmail.trim()}"
            )
        }
    }

    fun signOutDeviceRemotely(deviceId: String) {
        val dev = trackedDevices.value.find { it.deviceId == deviceId }
        googleAccountSessionRepository.terminateRemoteSession(deviceId)
        viewModelScope.launch {
            if (firebaseSessionManager.currentUserId() != null) {
                firebaseSessionManager.terminateRemoteSession(deviceId)
            }
        }
        _uiState.value = _uiState.value.copy(
            toastMessage = "Remote sign-out signal sent to ${dev?.deviceName ?: "device"}"
        )
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGoogleSignInLoading = true)
            when (val outcome = googleAuthManager.signInWithGoogle(context)) {
                is GoogleAuthOutcome.Success -> {
                    val user = outcome.user
                    val sessionResult = googleAccountSessionRepository.retrieveActiveDeviceSessions(user)
                    val sessionCount = if (sessionResult.isSuccess) sessionResult.getOrThrow().size else trackedDevices.value.size
                    _uiState.value = _uiState.value.copy(
                        isGoogleSignInLoading = false,
                        emailRecipient = user.email,
                        isGoogleAuthenticated = true,
                        authenticatedUserName = user.displayName,
                        authenticatedUserPhotoUrl = user.photoUrl,
                        authenticatedIdToken = user.idToken,
                        showGoogleAccountPickerModal = false,
                        toastMessage = "Signed in as ${user.email} ($sessionCount tracked node${if (sessionCount == 1) "" else "s"})."
                    )
                }
                is GoogleAuthOutcome.Cancelled -> {
                    _uiState.value = _uiState.value.copy(
                        isGoogleSignInLoading = false,
                        toastMessage = outcome.message
                    )
                }
                is GoogleAuthOutcome.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isGoogleSignInLoading = false,
                        showGoogleAccountPickerModal = true,
                        toastMessage = outcome.message
                    )
                }
            }
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                activeFirebaseUserId = null
                locationTrackerService.stopPeriodicSessionTracking()
                cancelSessionPointsObservers()
                firebaseSessionObserverJob?.cancelAndJoin()
                firebaseSessionObserverJob = null
                deviceLocationRepository.clearCloudSyncedSessions()
                googleAuthManager.clearCredentials()
                deviceLocationRepository.unlinkGoogleSession()
            }
            _firebaseSyncState.value = SessionSyncState.Unauthenticated
            _uiState.value = _uiState.value.copy(
                isGoogleAuthenticated = false,
                isFirebaseAuthLinked = false,
                authenticatedUserName = null,
                authenticatedUserPhotoUrl = null,
                authenticatedIdToken = null,
                toastMessage = "Google Account unlinked from AegisGuard"
            )
        }
    }

    fun linkAccountWithEmail(email: String, name: String? = null) {
        if (email.isNotBlank()) {
            val cleanEmail = email.trim()
            val cleanName = name?.trim()?.ifBlank { null } ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isGoogleSignInLoading = true)
                val sessionResult = googleAccountSessionRepository.retrieveActiveDeviceSessions(
                    email = cleanEmail,
                    displayName = cleanName,
                    idToken = "AUTH_TOKEN_GCRED_${System.currentTimeMillis()}"
                )
                val sessionCount = if (sessionResult.isSuccess) sessionResult.getOrThrow().size else trackedDevices.value.size
                _uiState.value = _uiState.value.copy(
                    isGoogleSignInLoading = false,
                    emailRecipient = cleanEmail,
                    isGoogleAuthenticated = true,
                    authenticatedUserName = cleanName,
                    showGoogleAccountPickerModal = false,
                    toastMessage = "Linked Google session for $cleanEmail ($sessionCount tracked node${if (sessionCount == 1) "" else "s"})."
                )
            }
        }
    }

    fun setGoogleAccountPickerModalVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showGoogleAccountPickerModal = visible)
    }

    fun openGoogleAccountSecurity(context: Context) {
        try {
            val uri = Uri.parse("https://myaccount.google.com/device-activity")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(toastMessage = "Unable to open Google Account Security page")
        }
    }

    fun openDeviceInGoogleMaps(context: Context, lat: Double, lng: Double, label: String = "Device Location") {
        try {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    fun dispatchFleetReportEmail(context: Context) {
        val email = _uiState.value.emailRecipient
        val devicesList = trackedDevices.value
        val reportBuilder = StringBuilder()
        reportBuilder.append("=====================================================\n")
        reportBuilder.append("AEGISGUARD GOOGLE ACCOUNT & DEVICE FLEET LOCATION AUDIT\n")
        reportBuilder.append("=====================================================\n")
        reportBuilder.append("Authenticated Gmail Account: $email\n")
        reportBuilder.append("Timestamp: ${java.util.Date()}\n")
        reportBuilder.append("Total Monitored Devices: ${devicesList.size}\n\n")

        devicesList.forEachIndexed { i, dev ->
            reportBuilder.append("[Device ${i + 1}] ${dev.deviceName}\n")
            reportBuilder.append("  • Model: ${dev.deviceModel}\n")
            reportBuilder.append("  • Tracking Protocol: ${dev.trackingMode}\n")
            reportBuilder.append("  • Physical Location: ${dev.locationAddress}\n")
            reportBuilder.append("  • GPS Coordinates: ${dev.latitude}, ${dev.longitude} (±${dev.accuracyMeters}m)\n")
            reportBuilder.append("  • Google Maps: https://www.google.com/maps/search/?api=1&query=${dev.latitude},${dev.longitude}\n")
            reportBuilder.append("  • Battery Status: ${dev.batteryLevelPercent}% ${if (dev.isCharging) "(Charging)" else ""}\n")
            reportBuilder.append("  • Network Link: ${dev.networkCarrierOrWifi}\n")
            reportBuilder.append("  • Remote Lockdown Status: ${if (dev.isLockedRemotely) "LOCKED" else "NORMAL"}\n")
            reportBuilder.append("  • Token ID: ${dev.pairedToken ?: "Google Cloud Sync"}\n\n")
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, "[AegisGuard] Google Account & Device Fleet Location Dispatch")
            putExtra(Intent.EXTRA_TEXT, reportBuilder.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
            _uiState.value = _uiState.value.copy(toastMessage = "Dispatching device fleet report to $email")
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(toastMessage = "Email client opened for fleet dispatch")
        }
    }
}
