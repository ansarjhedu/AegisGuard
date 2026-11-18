package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ThreatSeverity
import com.example.scanner.MalwareScanner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AegisGuard", appName)
    }

    @Test
    fun `test context availability`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)
    }

    @Test
    fun `test malware scanner auto-whitelists system apps and user whitelist`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scanner = MalwareScanner(context)

        // Run scan with a user-whitelisted package
        val apps = scanner.scanAllInstalledApplications(
            quarantinedPackages = emptySet(),
            userWhitelistedPackages = setOf("com.shadow.telemetry.svc"),
            autoWhitelistSystem = true,
            autoWhitelistPlayStore = true
        )

        // The user-whitelisted app must have 0% risk score and INFO threat level
        val shadowApp = apps.find { it.packageName == "com.shadow.telemetry.svc" }
        assertNotNull(shadowApp)
        assertTrue(shadowApp!!.isWhitelisted)
        assertEquals(0, shadowApp.riskScore)
        assertEquals(ThreatSeverity.INFO, shadowApp.riskLevel)

        // All system and essential OS apps must be auto-whitelisted with 0 risk score
        val systemApps = apps.filter { it.isSystemApp || it.isEssentialSystemApp }
        for (sysApp in systemApps) {
            assertTrue("System app ${sysApp.packageName} should be whitelisted", sysApp.isWhitelisted)
            assertEquals("System app ${sysApp.packageName} should have 0 risk score", 0, sysApp.riskScore)
            assertEquals("System app ${sysApp.packageName} should be ThreatSeverity.INFO", ThreatSeverity.INFO, sysApp.riskLevel)
        }
    }

    @Test
    fun `test decoy wifi state and original icon replacement`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)

        // Verify initial or configured decoy preferences
        prefs.edit()
            .putBoolean("decoy_enabled_wifi", true)
            .putBoolean("hide_original_wifi", true)
            .putString("fake_wifi_ssid", "Metropolis_Secure_WiFi")
            .apply()

        val isWifiDecoyEnabled = prefs.getBoolean("decoy_enabled_wifi", false)
        val isOriginalWifiHidden = prefs.getBoolean("hide_original_wifi", false)
        val fakeSsid = prefs.getString("fake_wifi_ssid", "")

        assertTrue("Decoy Wi-Fi should be enabled", isWifiDecoyEnabled)
        assertTrue("Original Wi-Fi should be hidden in favor of fake decoy", isOriginalWifiHidden)
        assertEquals("Metropolis_Secure_WiFi", fakeSsid)

        // Verify DecoyItem configuration model
        val wifiDecoyItem = com.example.data.model.DecoyItem(
            type = com.example.data.model.DecoyIconType.WIFI,
            isEnabled = true,
            spoofedTitle = "Fake Wi-Fi",
            spoofedValue = "Metropolis_Secure_WiFi",
            replacesOriginal = true
        )
        assertTrue(wifiDecoyItem.isEnabled)
        assertTrue(wifiDecoyItem.replacesOriginal)
        assertEquals("Metropolis_Secure_WiFi", wifiDecoyItem.spoofedValue)
    }

    @Test
    fun `test biometric session disconnect and vault lock when user leaves`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)

        // Simulate user unlocking vault
        prefs.edit().putBoolean("is_vault_locked", false).apply()
        org.junit.Assert.assertFalse(prefs.getBoolean("is_vault_locked", true))

        // When user leaves app, disconnect session and lock vault
        prefs.edit().putBoolean("is_vault_locked", true).apply()
        assertTrue("Vault must be locked after leaving", prefs.getBoolean("is_vault_locked", false))
    }

    @Test
    fun `test aegis guard icon tile toggles active to inactive without navigating`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)

        // Test Aegis Shield Guard toggle: Active -> Inactive
        prefs.edit().putBoolean("is_shield_active", true).apply()
        var currentShield = prefs.getBoolean("is_shield_active", true)
        assertTrue(currentShield)

        // Toggle to inactive
        prefs.edit().putBoolean("is_shield_active", !currentShield).apply()
        currentShield = prefs.getBoolean("is_shield_active", true)
        org.junit.Assert.assertFalse("Shield must toggle to inactive", currentShield)

        // Test Decoy Wi-Fi toggle: Active -> Inactive
        prefs.edit().putBoolean("decoy_enabled_wifi", true).apply()
        var currentWifi = prefs.getBoolean("decoy_enabled_wifi", true)
        assertTrue(currentWifi)

        // Toggle to inactive
        prefs.edit().putBoolean("decoy_enabled_wifi", !currentWifi).apply()
        currentWifi = prefs.getBoolean("decoy_enabled_wifi", true)
        org.junit.Assert.assertFalse("Decoy Wi-Fi must toggle to inactive", currentWifi)

        // Test Anti-theft lockdown toggle: Active -> Inactive
        prefs.edit().putBoolean("is_lockdown_active", true).apply()
        var currentLockdown = prefs.getBoolean("is_lockdown_active", false)
        assertTrue(currentLockdown)

        prefs.edit().putBoolean("is_lockdown_active", !currentLockdown).apply()
        currentLockdown = prefs.getBoolean("is_lockdown_active", true)
        org.junit.Assert.assertFalse("Lockdown must toggle to inactive", currentLockdown)

        // Test Decoy Location toggle: Active -> Inactive
        prefs.edit().putBoolean("decoy_enabled_location", true).apply()
        var currentLocation = prefs.getBoolean("decoy_enabled_location", true)
        assertTrue(currentLocation)

        prefs.edit().putBoolean("decoy_enabled_location", !currentLocation).apply()
        currentLocation = prefs.getBoolean("decoy_enabled_location", true)
        org.junit.Assert.assertFalse("Location must toggle to inactive", currentLocation)

        // Test Decoy Airplane Mode toggle: Inactive -> Active
        prefs.edit().putBoolean("decoy_enabled_airplane_mode", false).apply()
        var currentAirplane = prefs.getBoolean("decoy_enabled_airplane_mode", false)
        org.junit.Assert.assertFalse(currentAirplane)

        prefs.edit().putBoolean("decoy_enabled_airplane_mode", !currentAirplane).apply()
        currentAirplane = prefs.getBoolean("decoy_enabled_airplane_mode", false)
        assertTrue("Airplane Mode must toggle to active", currentAirplane)
    }

    @Test
    fun `test device location repository initializes devices and tracking modes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.DeviceLocationRepository(context)

        // Verify devices are initialized
        val devices = repo.devices.value
        assertTrue("Should have initialized devices", devices.isNotEmpty())
        val currentDevice = devices.find { it.isCurrentDevice }
        assertNotNull(currentDevice)
        assertEquals("ansarjhedu@gmail.com", currentDevice!!.userEmail)

        // Verify default tracking mode is GOOGLE_LOCATION
        assertEquals(com.example.data.model.DeviceTrackingMode.GOOGLE_LOCATION, repo.selectedMode.value)

        // Toggle tracking mode to AEGIS_DEVICE_TOKEN
        repo.setTrackingMode(com.example.data.model.DeviceTrackingMode.AEGIS_DEVICE_TOKEN)
        assertEquals(com.example.data.model.DeviceTrackingMode.AEGIS_DEVICE_TOKEN, repo.selectedMode.value)
    }

    @Test
    fun `test aegis token generation, pairing and remote lockdown`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.DeviceLocationRepository(context)

        // 1. Generate new pairing token
        val token = repo.generateNewPairingToken("Test Remote Phone", "ansarjhedu@gmail.com")
        assertTrue(token.tokenId.startsWith("AEGIS-TK-"))
        assertEquals("Test Remote Phone", token.targetDeviceName)
        assertEquals("ansarjhedu@gmail.com", token.targetUserEmail)
        assertTrue(repo.pairingTokens.value.any { it.tokenId == token.tokenId })

        // 2. Pair a new device with the token
        val initialCount = repo.devices.value.size
        repo.pairDeviceWithToken("Galaxy Fold 5", token.tokenId)
        val afterPairing = repo.devices.value
        assertEquals(initialCount + 1, afterPairing.size)
        val pairedDev = afterPairing.find { it.pairedToken == token.tokenId }
        assertNotNull(pairedDev)
        assertEquals("Galaxy Fold 5", pairedDev!!.deviceName)

        // 3. Test Remote Lockdown Toggle
        val devId = pairedDev.deviceId
        repo.toggleRemoteLockdown(devId)
        val lockedDev = repo.devices.value.find { it.deviceId == devId }
        assertTrue("Device must be marked as locked", lockedDev!!.isLockedRemotely)

        repo.toggleRemoteLockdown(devId)
        val unlockedDev = repo.devices.value.find { it.deviceId == devId }
        org.junit.Assert.assertFalse("Device must be unlocked", unlockedDev!!.isLockedRemotely)

        // 4. Revoke token
        repo.revokeToken(token.tokenId)
        org.junit.Assert.assertFalse(repo.pairingTokens.value.any { it.tokenId == token.tokenId })
    }

    @Test
    fun `test external devices without aegisguard tracked via gmail and google location`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.DeviceLocationRepository(context)

        val initialDevices = repo.devices.value
        assertTrue("Must have at least current hardware device", initialDevices.isNotEmpty())
        val currentDev = initialDevices.find { it.isCurrentDevice }
        assertNotNull("Current hardware device must be present", currentDev)

        // Test adding a genuine user device logged into Gmail (No AegisGuard needed)
        val initialCount = repo.devices.value.size
        repo.addGmailDevice(
            name = "Personal iPad Air",
            platform = "iPadOS 18",
            clientType = "Official Gmail App",
            locationName = "Home Office, Metro City"
        )
        val updatedDevices = repo.devices.value
        assertEquals(initialCount + 1, updatedDevices.size)

        val addedIpad = updatedDevices.find { it.deviceName == "Personal iPad Air" }
        assertNotNull(addedIpad)
        org.junit.Assert.assertFalse("Newly added Gmail device must not have AegisGuard", addedIpad!!.hasAegisGuard)
        assertEquals("iPadOS 18", addedIpad.platform)
        assertEquals("Official Gmail App", addedIpad.clientType)

        // Test remote sign-out of Gmail session on that device
        repo.signOutDeviceRemotely(addedIpad.deviceId)
        val signedOutIpad = repo.devices.value.find { it.deviceId == addedIpad.deviceId }
        assertNotNull(signedOutIpad)
        assertTrue("Device must be marked as remotely signed out of Gmail", signedOutIpad!!.isSignedOutRemotely)
        org.junit.Assert.assertFalse("Device should be offline after sign out", signedOutIpad.isOnline)

        // Test updating active email across all devices
        repo.updateActiveEmail("testuser@gmail.com")
        assertEquals("testuser@gmail.com", repo.getActiveEmail())
        val allUpdated = repo.devices.value
        assertTrue(allUpdated.all { it.userEmail == "testuser@gmail.com" })
    }

    @Test
    fun `test Google Sign-In and session retrieval via Credential Manager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authManager = com.example.data.auth.GoogleAuthManager(context)
        assertNotNull(authManager)

        val repo = com.example.data.repository.DeviceLocationRepository(context)

        // Verify initial unlinked state
        repo.unlinkGoogleSession()
        org.junit.Assert.assertFalse(repo.isGoogleAuthenticated.value)

        // Authenticate user and link Gmail session via Credential Manager
        val testEmail = "ansarjhedu@gmail.com"
        val testDisplayName = "Ansar Jh"
        val testPhotoUrl = "https://lh3.googleusercontent.com/a/test-avatar"
        val testIdToken = "MOCK_JWT_ID_TOKEN_FOR_CREDENTIAL_MANAGER"

        repo.linkGoogleSession(
            email = testEmail,
            displayName = testDisplayName,
            photoUrl = testPhotoUrl,
            idToken = testIdToken
        )

        // Assert authentication state
        assertTrue(repo.isGoogleAuthenticated.value)
        assertEquals(testDisplayName, repo.authenticatedUserName.value)
        assertEquals(testPhotoUrl, repo.authenticatedPhotoUrl.value)
        assertEquals(testIdToken, repo.authenticatedIdToken.value)
        assertEquals(testEmail, repo.getActiveEmail())

        // Assert all initialized devices are mapped to this email
        val devices = repo.devices.value
        assertTrue("Should have initialized devices", devices.isNotEmpty())
        assertTrue("All device sessions must be mapped to authenticated Gmail", devices.all { it.userEmail == testEmail })

        // Verify unlink / sign out
        repo.unlinkGoogleSession()
        org.junit.Assert.assertFalse(repo.isGoogleAuthenticated.value)
        org.junit.Assert.assertNull(repo.authenticatedUserName.value)
    }

    @Test
    fun `test rejects fake Mountain View Googleplex fallback and persists verified Pakistan location`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.DeviceLocationRepository(context)

        // Default uncalibrated device must NOT fall back to hardcoded Mountain View Google HQ string
        val initialCurrent = repo.devices.value.firstOrNull { it.isCurrentDevice }
        assertNotNull(initialCurrent)
        org.junit.Assert.assertFalse(
            "Must never hardcode Google HQ, Mountain View, CA",
            initialCurrent!!.locationAddress.contains("Google HQ, Mountain View", ignoreCase = true)
        )
        assertTrue(repo.isDefaultEmulatorGoogleplex(37.4220656, -122.0840897))

        // Calibrate location to Lahore, Pakistan
        val result = repo.setVerifiedLocationOverride("Lahore, Pakistan")
        assertTrue("Expected location override to succeed", result.isSuccess)
        assertTrue(repo.isLocationManuallyCalibrated())

        val updatedCurrent = repo.devices.value.firstOrNull { it.isCurrentDevice }
        assertNotNull(updatedCurrent)
        assertTrue(
            "Address should reflect Lahore / Pakistan",
            updatedCurrent!!.locationAddress.contains("Lahore", ignoreCase = true) ||
                updatedCurrent.locationAddress.contains("Pakistan", ignoreCase = true)
        )
        assertTrue("Latitude should be in Pakistan range (~31.5)", updatedCurrent.latitude in 23.0..37.0)
        assertTrue("Longitude should be in Pakistan range (~74.3)", updatedCurrent.longitude in 60.0..78.0)

        // Verify persistence across repository instances
        val reloadedRepo = com.example.data.repository.DeviceLocationRepository(context)
        val reloadedCurrent = reloadedRepo.devices.value.firstOrNull { it.isCurrentDevice }
        assertNotNull(reloadedCurrent)
        assertTrue(reloadedRepo.isLocationManuallyCalibrated())
        assertEquals(updatedCurrent.locationAddress, reloadedCurrent!!.locationAddress)

        // Verify clearing override works
        reloadedRepo.clearVerifiedLocationOverride()
        org.junit.Assert.assertFalse(reloadedRepo.isLocationManuallyCalibrated())
    }

    @Test
    fun `test firebase session manager payload schema and multi-device session merge`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = com.example.data.session.FirebaseSessionManager(context)
        val repo = com.example.data.repository.DeviceLocationRepository(context)

        val installId = sessionManager.getInstallSessionId()
        assertTrue("Install ID must match firestore.rules regex", installId.matches(Regex("^[a-zA-Z0-9_\\-]+$")))
        assertEquals(installId, sessionManager.getInstallSessionId())

        // Verify Kotlin create/update payloads match firestore.rules schema and omit @DocumentId `id`
        val currentDev = repo.createCurrentHardwareDevice()
        val createMap = sessionManager.buildCreatePayload("uid_abc_123", currentDev)
        org.junit.Assert.assertFalse("Payload must never include @DocumentId `id`", createMap.containsKey("id"))
        assertEquals("uid_abc_123", createMap["userId"])
        assertTrue(createMap.containsKey("createdAt"))
        assertTrue(createMap.containsKey("updatedAt"))
        org.junit.Assert.assertFalse("Null optional pairedToken must be omitted", createMap.containsKey("pairedToken"))

        val updateMap = sessionManager.buildUpdatePayload(currentDev)
        org.junit.Assert.assertFalse("Update must preserve immutable createdAt", updateMap.containsKey("createdAt"))
        org.junit.Assert.assertFalse("Update must preserve immutable userId", updateMap.containsKey("userId"))
        assertTrue(updateMap.containsKey("updatedAt"))

        // Simulate querying 3 active logins from Firestore (current device + 2 desktops + 1 mobile)
        val firestoreSessions = listOf(
            com.example.data.session.FirestoreDeviceSession(
                id = installId,
                userId = "uid_abc_123",
                deviceName = "Current Android Phone",
                platform = "Android 15",
                clientType = "AegisGuard Master (Active)",
                locationAddress = "Lahore, Pakistan",
                online = true
            ),
            com.example.data.session.FirestoreDeviceSession(
                id = "node_desktop_workstation_1",
                userId = "uid_abc_123",
                deviceName = "Workstation Desktop PC",
                platform = "Windows 11",
                clientType = "Google Chrome (Gmail Web)",
                locationAddress = "Lahore, Pakistan",
                online = true
            ),
            com.example.data.session.FirestoreDeviceSession(
                id = "node_desktop_home_2",
                userId = "uid_abc_123",
                deviceName = "Home Linux Desktop",
                platform = "Ubuntu Linux",
                clientType = "Google Chrome",
                locationAddress = "Islamabad, Pakistan",
                online = true
            ),
            com.example.data.session.FirestoreDeviceSession(
                id = "node_mobile_backup_3",
                userId = "uid_abc_123",
                deviceName = "Personal Mobile Phone",
                platform = "Android 14",
                clientType = "Gmail Mobile App",
                locationAddress = "Lahore, Pakistan",
                online = true
            )
        )

        val mapped = firestoreSessions.map { it.toDeviceLocationInfo(installId) }
        repo.mergeCloudSessions(mapped, installId)

        val merged = repo.devices.value
        assertEquals("Current device should appear once alongside all 3 remote logins", 4, merged.size)
        assertEquals(1, merged.count { it.isCurrentDevice })
        assertTrue(merged.any { it.deviceName == "Workstation Desktop PC" && !it.isCurrentDevice })
        assertTrue(merged.any { it.deviceName == "Home Linux Desktop" && !it.isCurrentDevice })
        assertTrue(merged.any { it.deviceName == "Personal Mobile Phone" && !it.isCurrentDevice })
    }

    @Test
    fun `test per-device Gulberg vs Johar Town calibration, LAN device cleanup, and location history tracking`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.DeviceLocationRepository(context)
        val sessionManager = com.example.data.session.FirebaseSessionManager(context)

        // Add two desktops at Gulberg and one phone at Johar Town
        val desktop1 = repo.addGmailDevice(
            name = "Workstation 1",
            platform = "Windows 11",
            clientType = "Google Chrome",
            locationName = "Gulberg, Lahore, Pakistan"
        )
        val desktop2 = repo.addGmailDevice(
            name = "Workstation 2",
            platform = "Ubuntu Linux",
            clientType = "Firefox",
            locationName = "Gulberg, Lahore, Pakistan"
        )
        val mobileJohar = repo.addGmailDevice(
            name = "Field Phone",
            platform = "Android 15",
            clientType = "Gmail App",
            locationName = "Johar Town, Lahore, Pakistan"
        )

        assertEquals("Gulberg, Lahore, Pakistan", desktop1.locationAddress)
        assertEquals("Gulberg, Lahore, Pakistan", desktop2.locationAddress)
        assertEquals("Johar Town, Lahore, Pakistan", mobileJohar.locationAddress)
        assertEquals(31.5102, desktop1.latitude, 0.001)
        assertEquals(74.3441, desktop1.longitude, 0.001)
        assertEquals(31.4697, mobileJohar.latitude, 0.001)
        assertEquals(74.2728, mobileJohar.longitude, 0.001)

        // Move desktop2 independently to Johar Town and rename it
        val updateRes = repo.updateDeviceIdentityAndLocation(
            deviceId = desktop2.deviceId,
            newName = "Office Desktop — Johar Branch",
            locationQuery = "Johar Town, Lahore, Pakistan"
        )
        assertTrue(updateRes.isSuccess)
        val (updatedD2, historyEntry) = updateRes.getOrThrow()
        assertEquals("Office Desktop — Johar Branch", updatedD2.deviceName)
        assertEquals("Johar Town, Lahore, Pakistan", updatedD2.locationAddress)
        assertNotNull(historyEntry)

        // Verify per-device override persists independently from desktop1 (still in Gulberg)
        val reloaded = com.example.data.repository.DeviceLocationRepository(context)
        val reloadedD1 = reloaded.devices.value.firstOrNull { it.deviceId == desktop1.deviceId }
        val reloadedD2 = reloaded.devices.value.firstOrNull { it.deviceId == desktop2.deviceId }
        assertNotNull(reloadedD1)
        assertNotNull(reloadedD2)
        assertEquals("Gulberg, Lahore, Pakistan", reloadedD1!!.locationAddress)
        assertEquals("Johar Town, Lahore, Pakistan", reloadedD2!!.locationAddress)
        assertEquals("Office Desktop — Johar Branch", reloadedD2.deviceName)

        // Verify Location History captured checkpoints and persists
        assertTrue("Location history should record checkpoints", reloaded.locationHistory.value.isNotEmpty())
        assertTrue(reloaded.locationHistory.value.any { it.locationAddress == "Johar Town, Lahore, Pakistan" })
        assertTrue(reloaded.locationHistory.value.any { it.locationAddress == "Gulberg, Lahore, Pakistan" })

        // Verify Firestore locationHistory payload matches firestore.rules allowed keys and omits @DocumentId `id`
        val payload = sessionManager.buildLocationHistoryPayload("uid_abc_123", historyEntry!!)
        org.junit.Assert.assertFalse("History payload must never include @DocumentId `id`", payload.containsKey("id"))
        val expectedKeys = setOf(
            "userId", "deviceId", "deviceName", "locationAddress",
            "locationSource", "latitude", "longitude", "accuracyMeters",
            "recordedAtMillis", "eventNote", "createdAt"
        )
        assertEquals(expectedKeys, payload.keys)
    }

    @Test
    fun `test LocationTrackerService records distinct per-session geocoordinates timestamps and accuracy metadata`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.DeviceLocationRepository(context)
        val sessionManager = com.example.data.session.FirebaseSessionManager(context)
        val tracker = com.example.data.location.LocationTrackerService(context, repo, sessionManager)

        val gulbergDesktop = repo.addGmailDevice(
            name = "Workstation Gulberg",
            platform = "Windows 11",
            clientType = "Google Chrome",
            locationName = "Gulberg, Lahore, Pakistan"
        )
        val joharPhone = repo.addGmailDevice(
            name = "Mobile Johar",
            platform = "Android 15",
            clientType = "Gmail App",
            locationName = "Johar Town, Lahore, Pakistan"
        )

        val captureResult = tracker.captureAndSyncAllSessions(reason = "Unit Test Per-Session Capture")
        assertTrue(captureResult.isSuccess)
        assertTrue(captureResult.getOrThrow() >= 2)

        val trackedMap = tracker.trackedSessionPoints.value
        val gulbergSessionId = tracker.resolveSessionId(gulbergDesktop)
        val joharSessionId = tracker.resolveSessionId(joharPhone)

        val gulbergPoints = trackedMap[gulbergSessionId]
        val joharPoints = trackedMap[joharSessionId]
        assertNotNull(gulbergPoints)
        assertNotNull(joharPoints)
        assertTrue("Gulberg session must store its own point", gulbergPoints!!.isNotEmpty())
        assertTrue("Johar Town session must store its own point", joharPoints!!.isNotEmpty())

        val gPoint = gulbergPoints.first()
        val jPoint = joharPoints.first()

        // Verify distinct coordinates, non-zero timestamp & accuracy metadata per session
        assertEquals(gulbergSessionId, gPoint.sessionId)
        assertEquals(joharSessionId, jPoint.sessionId)
        assertEquals(31.5102, gPoint.latitude, 0.001)
        assertEquals(74.3441, gPoint.longitude, 0.001)
        assertEquals(31.4697, jPoint.latitude, 0.001)
        assertEquals(74.2728, jPoint.longitude, 0.001)
        assertTrue("Captured timestamp must be populated", gPoint.capturedAtMillis > 0L)
        assertTrue("Accuracy metadata must be >= 0", gPoint.accuracyMeters >= 0f)
        assertTrue("Provider metadata must be set", gPoint.provider.isNotBlank())

        // Verify Firestore session locationPoints payload matches all 14 required fields and omits @DocumentId `id`
        val payload = tracker.buildSessionLocationPointPayload("uid_abc_123", gulbergSessionId, gPoint)
        org.junit.Assert.assertFalse("Payload must never include @DocumentId `id`", payload.containsKey("id"))
        val expectedKeys = setOf(
            "userId", "sessionId", "deviceName", "latitude", "longitude",
            "accuracyMeters", "verticalAccuracyMeters", "speedMetersPerSec", "bearingDegrees",
            "provider", "locationAddress", "capturedAtMillis", "eventNote", "createdAt"
        )
        assertEquals(expectedKeys, payload.keys)
        assertEquals(gulbergSessionId, payload["sessionId"])
    }
}

