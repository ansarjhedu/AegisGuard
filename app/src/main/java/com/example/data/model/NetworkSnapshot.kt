package com.example.data.model

data class NetworkSnapshot(
    val transportType: String, // "Wi-Fi", "Cellular (5G/LTE)", "Ethernet", "Disconnected"
    val isConnected: Boolean,
    val ssid: String,
    val bssid: String,
    val ipAddress: String,
    val gateway: String,
    val dnsServers: List<String>,
    val isVpnActive: Boolean,
    val isProxyConfigured: Boolean,
    val proxyDetails: String,
    val linkSpeedMbps: Int,
    val isTampered: Boolean = false,
    val tamperReason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
