package com.example.data.model

import java.util.UUID

enum class DeviceTrackingMode {
    GOOGLE_LOCATION,     // Live Google Location Services & Account Session Sync (No AegisGuard needed on remote device)
    AEGIS_DEVICE_TOKEN   // P2P cryptographic handshake token between AegisGuard instances
}

data class DeviceLocationInfo(
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val isCurrentDevice: Boolean = false,
    val hasAegisGuard: Boolean = false,            // True if AegisGuard is installed on this device, false if tracked purely via Gmail / Google Location
    val platform: String = "Windows 11",           // e.g., Windows, iOS, macOS, Android, Linux
    val clientType: String = "Google Chrome (Gmail Web)", // e.g., Chrome Browser, Official Gmail iOS, Play Services
    val ipAddress: String = "198.51.100.22",
    val locationSource: String = "Google Location & IP Telemetry",
    val userEmail: String = "ansarjhedu@gmail.com",
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 4.2f,
    val locationAddress: String,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val batteryLevelPercent: Int = 88,
    val isCharging: Boolean = false,
    val isOnline: Boolean = true,
    val trackingMode: DeviceTrackingMode = DeviceTrackingMode.GOOGLE_LOCATION,
    val pairedToken: String? = null,
    val isLockedRemotely: Boolean = false,
    val isSignedOutRemotely: Boolean = false,
    val networkCarrierOrWifi: String = "Secure Wi-Fi"
)

data class AegisPairingToken(
    val tokenId: String = "AEGIS-TK-" + UUID.randomUUID().toString().take(8).uppercase(),
    val targetDeviceName: String,
    val targetUserEmail: String = "ansarjhedu@gmail.com",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L * 7, // 7 days validity
    val isVerified: Boolean = true,
    val cryptographicFingerprint: String = "SHA256:" + UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
)

data class DeviceLocationHistoryEntry(
    val entryId: String,
    val deviceId: String,
    val deviceName: String,
    val locationAddress: String,
    val locationSource: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 15.0f,
    val recordedAtMillis: Long = System.currentTimeMillis(),
    val eventNote: String = "Location Check-In"
)

data class SessionLocationPoint(
    val pointId: String,
    val sessionId: String,
    val deviceName: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val verticalAccuracyMeters: Float = 0f,
    val speedMetersPerSec: Float = 0f,
    val bearingDegrees: Float = 0f,
    val provider: String,
    val locationAddress: String,
    val capturedAtMillis: Long = System.currentTimeMillis(),
    val eventNote: String = "Session Geolocation Fix"
)
