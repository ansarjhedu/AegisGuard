package com.example.data.model

enum class ZeroTrustStatus {
    SECURE_VERIFIED,
    WARNING_DEGRADED,
    HIGH_RISK_COMPROMISED
}

data class MicroserviceToken(
    val serviceName: String,
    val tokenHash: String,
    val issuedAt: Long,
    val expiresAt: Long,
    val isVerified: Boolean
)

data class ZeroTrustPosture(
    val score: Int, // 0 to 100
    val status: ZeroTrustStatus,
    val isRootDetected: Boolean,
    val isUsbDebuggingEnabled: Boolean,
    val isMockLocationActive: Boolean,
    val isDeviceEncrypted: Boolean,
    val isSelinuxEnforcing: Boolean,
    val areUnknownSourcesAllowed: Boolean,
    val activeMicroservices: List<MicroserviceToken>,
    val zeroTrustPolicyVersion: String = "ZT-ARCH-2026.4"
)
