package com.example.data.model

data class AppSecurityInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val isHiddenApp: Boolean,
    val riskScore: Int, // 0 to 100
    val riskLevel: ThreatSeverity,
    val declaredPermissions: List<String>,
    val dangerousPermissions: List<String>,
    val suspiciousFlags: List<String>,
    val installerPackage: String?,
    val isQuarantined: Boolean = false,
    val firstInstallTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val isGooglePlayVerified: Boolean = false,
    val isEssentialSystemApp: Boolean = false,
    val installerLabel: String = "Unknown",
    val canBeQuarantined: Boolean = true,
    val isWhitelisted: Boolean = false,
    val whitelistReason: String? = null
)
