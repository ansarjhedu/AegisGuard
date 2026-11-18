package com.example.data.model

enum class ThreatSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class ThreatCategory {
    MALWARE,
    HIDDEN_SPYWARE,
    NETWORK_TAMPER,
    EXCESSIVE_PERMISSIONS,
    UNAUTHORIZED_ACCESS,
    DEVICE_POSTURE
}

data class ThreatItem(
    val id: String,
    val title: String,
    val description: String,
    val category: ThreatCategory,
    val severity: ThreatSeverity,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String? = null
)
