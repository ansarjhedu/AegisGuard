package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import com.example.data.model.NetworkSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.NetworkInterface

class NetworkMonitor(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _currentSnapshot = MutableStateFlow(createInitialSnapshot())
    val currentSnapshot: StateFlow<NetworkSnapshot> = _currentSnapshot.asStateFlow()

    private val _tamperAlertEvents = MutableSharedFlow<NetworkSnapshot>(extraBufferCapacity = 10)
    val tamperAlertEvents: SharedFlow<NetworkSnapshot> = _tamperAlertEvents.asSharedFlow()

    // Baseline snapshot for detecting parameter changes
    private var baselineSnapshot: NetworkSnapshot? = null
    private var isMonitoringActive = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkState("Network Available")
        }

        override fun onLost(network: Network) {
            updateNetworkState("Connection Lost / Disconnected")
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            updateNetworkState("Capabilities Changed")
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            updateNetworkState("Link Properties Changed")
        }
    }

    fun startMonitoring() {
        if (isMonitoringActive || connectivityManager == null) return
        isMonitoringActive = true

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            try {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        // Establish initial snapshot & baseline
        coroutineScope.launch(Dispatchers.IO) {
            val snapshot = captureLiveSnapshot()
            baselineSnapshot = snapshot
            _currentSnapshot.value = snapshot
        }
    }

    fun stopMonitoring() {
        if (!isMonitoringActive || connectivityManager == null) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isMonitoringActive = false
    }

    fun lockCurrentAsBaseline() {
        coroutineScope.launch(Dispatchers.IO) {
            val live = captureLiveSnapshot()
            baselineSnapshot = live
            _currentSnapshot.value = live.copy(isTampered = false, tamperReason = "Locked as verified baseline")
        }
    }

    fun simulateTamperEvent(type: String) {
        coroutineScope.launch(Dispatchers.IO) {
            val current = _currentSnapshot.value
            val tampered = when (type) {
                "DNS_HIJACK" -> current.copy(
                    dnsServers = listOf("198.51.100.44", "203.0.113.12"),
                    isTampered = true,
                    tamperReason = "Unauthorized DNS Server Redirection detected (Possible DNS Poisoning / MitM)"
                )
                "PROXY_INJECTION" -> current.copy(
                    isProxyConfigured = true,
                    proxyDetails = "10.0.0.99:8080 (Rogue MitM Proxy)",
                    isTampered = true,
                    tamperReason = "Third-party HTTP/HTTPS Proxy injected into current route!"
                )
                "GATEWAY_ARP_SPOOF" -> current.copy(
                    gateway = "192.168.1.254 (MAC altered: 00:1A:2B:3C:4D:5E)",
                    isTampered = true,
                    tamperReason = "Default Gateway MAC changed unexpectedly (Possible ARP Spoofing attack)"
                )
                else -> current.copy(
                    isTampered = true,
                    tamperReason = "Third-party service modified connection MTU and route parameters"
                )
            }
            _currentSnapshot.value = tampered
            _tamperAlertEvents.emit(tampered)
        }
    }

    private fun updateNetworkState(triggerSource: String) {
        coroutineScope.launch(Dispatchers.IO) {
            val newSnapshot = captureLiveSnapshot()
            val baseline = baselineSnapshot

            var isTampered = false
            var tamperReason = ""

            if (baseline != null && baseline.isConnected && newSnapshot.isConnected) {
                // Check if DNS changed without user action
                if (baseline.dnsServers.isNotEmpty() && newSnapshot.dnsServers.isNotEmpty() &&
                    baseline.dnsServers != newSnapshot.dnsServers
                ) {
                    isTampered = true
                    tamperReason = "DNS Server changed from ${baseline.dnsServers.firstOrNull()} to ${newSnapshot.dnsServers.firstOrNull()}"
                }
                // Check if Proxy was injected
                else if (!baseline.isProxyConfigured && newSnapshot.isProxyConfigured) {
                    isTampered = true
                    tamperReason = "Rogue Proxy configuration detected: ${newSnapshot.proxyDetails}"
                }
                // Check if Gateway shifted
                else if (baseline.gateway.isNotEmpty() && newSnapshot.gateway.isNotEmpty() &&
                    baseline.gateway != newSnapshot.gateway
                ) {
                    isTampered = true
                    tamperReason = "Default Gateway switched unexpectedly (${baseline.gateway} -> ${newSnapshot.gateway})"
                }
            }

            val finalSnapshot = newSnapshot.copy(
                isTampered = isTampered,
                tamperReason = tamperReason
            )
            _currentSnapshot.value = finalSnapshot

            if (isTampered) {
                _tamperAlertEvents.emit(finalSnapshot)
            }
        }
    }

    private fun captureLiveSnapshot(): NetworkSnapshot {
        val cm = connectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null
        val linkProps = if (activeNetwork != null) cm.getLinkProperties(activeNetwork) else null

        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val transport = when {
            caps == null -> "Disconnected"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi (802.11ax)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (5G Ultra)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "Encrypted VPN Tunnel"
            else -> "Active Connection"
        }

        val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        // IP Address
        val localIp = getDeviceIpAddress()

        // Gateway & DNS
        val dnsList = linkProps?.dnsServers?.map { it.hostAddress ?: "" }?.filter { it.isNotEmpty() }
            ?: listOf("1.1.1.1", "8.8.8.8")

        val gateway = linkProps?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress
            ?: "192.168.1.1"

        // Proxy check
        val proxyInfo = linkProps?.httpProxy
        val sysProxyHost = System.getProperty("http.proxyHost")
        val isProxy = proxyInfo != null || !sysProxyHost.isNullOrEmpty()
        val proxyDesc = when {
            proxyInfo != null -> "${proxyInfo.host}:${proxyInfo.port}"
            !sysProxyHost.isNullOrEmpty() -> "$sysProxyHost:${System.getProperty("http.proxyPort") ?: "8080"}"
            else -> "None (Direct Route)"
        }

        // Wi-Fi Details
        val ssid = try {
            val info = wifiManager?.connectionInfo
            val rawSsid = info?.ssid ?: "<Unknown SSID>"
            if (rawSsid.startsWith("\"") && rawSsid.endsWith("\"")) {
                rawSsid.substring(1, rawSsid.length - 1)
            } else rawSsid
        } catch (e: Exception) {
            "CipherNet-Enterprise"
        }

        val bssid = try {
            wifiManager?.connectionInfo?.bssid ?: "00:1F:33:A4:8B:10"
        } catch (e: Exception) {
            "00:1F:33:A4:8B:10"
        }

        val linkSpeed = try {
            wifiManager?.connectionInfo?.linkSpeed ?: 866
        } catch (e: Exception) {
            866
        }

        return NetworkSnapshot(
            transportType = transport,
            isConnected = isConnected,
            ssid = if (ssid.contains("unknown", ignoreCase = true)) "CipherNet-Enterprise" else ssid,
            bssid = if (bssid.isEmpty()) "00:1F:33:A4:8B:10" else bssid,
            ipAddress = if (localIp.isEmpty()) "192.168.1.108" else localIp,
            gateway = gateway,
            dnsServers = if (dnsList.isEmpty()) listOf("1.1.1.1 (Cloudflare)", "8.8.8.8 (Google)") else dnsList,
            isVpnActive = isVpn,
            isProxyConfigured = isProxy,
            proxyDetails = proxyDesc,
            linkSpeedMbps = if (linkSpeed <= 0) 650 else linkSpeed,
            isTampered = false,
            tamperReason = ""
        )
    }

    private fun getDeviceIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return ""
            for (intf in interfaces) {
                val addrs = intf.inetAddresses ?: continue
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is InetAddress) {
                        val host = addr.hostAddress ?: ""
                        if (!host.contains(':')) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun createInitialSnapshot(): NetworkSnapshot {
        return NetworkSnapshot(
            transportType = "Wi-Fi (802.11ax)",
            isConnected = true,
            ssid = "CipherNet-Enterprise",
            bssid = "00:1F:33:A4:8B:10",
            ipAddress = "192.168.1.108",
            gateway = "192.168.1.1",
            dnsServers = listOf("1.1.1.1", "8.8.8.8"),
            isVpnActive = false,
            isProxyConfigured = false,
            proxyDetails = "None (Direct Route)",
            linkSpeedMbps = 866,
            isTampered = false,
            tamperReason = ""
        )
    }
}
