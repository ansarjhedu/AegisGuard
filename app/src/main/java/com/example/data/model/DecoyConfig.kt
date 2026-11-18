package com.example.data.model

enum class DecoyIconType(val displayName: String, val defaultSubtitle: String) {
    HOTSPOT("Decoy Hotspot", "Broadcasts disguised Hotspot active indicator"),
    WIFI("Decoy Wi-Fi", "Fakes secure enterprise Wi-Fi link"),
    LOCATION("Decoy GPS / Location", "Spoofs active location tracking telemetry"),
    AIRPLANE_MODE("Decoy Airplane Mode", "Traps and blocks radio disconnect attempts"),
    MOBILE_DATA("Decoy Mobile Data (5G)", "Simulates high-speed encrypted 5G carrier"),
    VPN_SHIELD("Decoy Stealth VPN", "Fakes encrypted tunnel status"),
    BLUETOOTH("Decoy Bluetooth", "Simulates encrypted peripheral tethering"),
    BATTERY("Decoy Power Status", "Hides real power telemetry from profiling apps")
}

data class DecoyItem(
    val type: DecoyIconType,
    val isEnabled: Boolean,
    val spoofedTitle: String,
    val spoofedValue: String,
    val replacesOriginal: Boolean = true,
    val signalBars: Int = 4 // 1 to 4
)

data class DecoyState(
    val isCamouflageServiceActive: Boolean = false,
    val isTopBarOverlayVisible: Boolean = true,
    val fakeLocationCoordinates: String = "35.6762° N, 139.6503° E (Tokyo)",
    val fakeLocationCity: String = "Tokyo, Japan",
    val fakeWifiSsid: String = "Metropolis_Secure_WiFi",
    val fakeCarrierName: String = "Veritas 5G+",
    val fakeHotspotSsid: String = "GhostAP_Encrypted",
    val icons: List<DecoyItem> = listOf(
        DecoyItem(DecoyIconType.LOCATION, true, "Fake GPS", "35.6762° N, 139.6503° E"),
        DecoyItem(DecoyIconType.AIRPLANE_MODE, false, "Decoy Airplane Mode", "Antenna Guard Active", false),
        DecoyItem(DecoyIconType.WIFI, true, "Fake Wi-Fi", "Metropolis_Secure_WiFi"),
        DecoyItem(DecoyIconType.MOBILE_DATA, true, "Fake 5G", "Veritas 5G+"),
        DecoyItem(DecoyIconType.HOTSPOT, true, "Fake Hotspot", "GhostAP_Encrypted"),
        DecoyItem(DecoyIconType.VPN_SHIELD, true, "Fake VPN", "Zero-Trust Tunnel Active"),
        DecoyItem(DecoyIconType.BLUETOOTH, false, "Fake Bluetooth", "Hardware Guard Active"),
        DecoyItem(DecoyIconType.BATTERY, true, "Decoy Battery", "98% (Secure)")
    )
)
