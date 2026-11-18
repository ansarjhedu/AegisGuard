package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NetworkSnapshot
import com.example.ui.AegisUiState
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberWarning

@Composable
fun NetworkGuardScreen(
    uiState: AegisUiState,
    onLockBaseline: () -> Unit,
    onSimulateTamper: (String) -> Unit,
    onDispatchEmail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = uiState.networkSnapshot

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Network Guard Header Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        1.dp,
                        if (snapshot?.isTampered == true) CyberCritical else CyberSecondary.copy(alpha = 0.5f),
                        RoundedCornerShape(18.dp)
                    ),
                color = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (snapshot?.isTampered == true) Icons.Default.Warning else Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = if (snapshot?.isTampered == true) CyberCritical else CyberSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (snapshot?.isTampered == true) "CONNECTION COMPROMISED" else "NETWORK INTEGRITY SECURE",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (snapshot?.isTampered == true) CyberCritical.copy(alpha = 0.2f) else CyberSecondary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (snapshot?.isTampered == true) "TAMPERED" else "MONITORED",
                                color = if (snapshot?.isTampered == true) CyberCritical else CyberSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (snapshot?.isTampered == true)
                            "Warning: ${snapshot.tamperReason}"
                        else "Real-time daemon active. Monitoring DNS resolvers, route gateways, and proxy injections for third-party parameter manipulation.",
                        color = if (snapshot?.isTampered == true) Color(0xFFFFD1D9) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onLockBaseline,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("lock_baseline_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberSecondary,
                                contentColor = Color(0xFF003919)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lock As Baseline",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onDispatchEmail,
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("network_email_dispatch_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberPrimary,
                                contentColor = Color(0xFF00363D)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Email Report",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Live Parameters Inspector
        item {
            Text(
                text = "ACTIVE CONNECTION TELEMETRY",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NetworkParamRow(
                    label = "Transport Protocol",
                    value = snapshot?.transportType ?: "Wi-Fi",
                    icon = Icons.Default.Wifi
                )
                NetworkParamRow(
                    label = "Local IP Address",
                    value = snapshot?.ipAddress ?: "192.168.1.108",
                    icon = Icons.Default.Router
                )
                NetworkParamRow(
                    label = "Default Gateway",
                    value = snapshot?.gateway ?: "192.168.1.1",
                    icon = Icons.Default.Router
                )
                NetworkParamRow(
                    label = "DNS Resolvers",
                    value = snapshot?.dnsServers?.joinToString(", ") ?: "1.1.1.1, 8.8.8.8",
                    icon = Icons.Default.Dns,
                    isHighlight = snapshot?.isTampered == true && snapshot.tamperReason.contains("DNS")
                )
                NetworkParamRow(
                    label = "Wi-Fi SSID / BSSID",
                    value = "${snapshot?.ssid} (${snapshot?.bssid})",
                    icon = Icons.Default.Wifi
                )
                NetworkParamRow(
                    label = "Proxy Injection Status",
                    value = snapshot?.proxyDetails ?: "None (Direct Route)",
                    icon = Icons.Default.Security,
                    isHighlight = snapshot?.isProxyConfigured == true
                )
                NetworkParamRow(
                    label = "VPN Tunnel",
                    value = if (snapshot?.isVpnActive == true) "Active (Encrypted)" else "Inactive (Direct Carrier)",
                    icon = Icons.Default.VpnKey
                )
            }
        }

        // Tampering Simulation Laboratory
        item {
            Text(
                text = "TAMPER DETECTION SIMULATOR",
                color = CyberWarning,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Test real-time alert dispatch when third-party services manipulate device routing parameters:",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSimulateTamper("DNS_HIJACK") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_dns_hijack_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "DNS Poisoning",
                            fontSize = 11.sp,
                            color = Color(0xFFFFB703),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onSimulateTamper("PROXY_INJECTION") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_proxy_injection_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Proxy Injection",
                            fontSize = 11.sp,
                            color = CyberCritical,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSimulateTamper("GATEWAY_ARP_SPOOF") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_arp_spoof_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ARP Gateway Spoof",
                            fontSize = 11.sp,
                            color = Color(0xFFFF7043),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onLockBaseline,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reset_clean_baseline_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = CyberSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset Clean",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NetworkParamRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isHighlight) CyberCritical else CyberPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
            Text(
                text = value,
                color = if (isHighlight) CyberCritical else Color.White,
                fontSize = 13.sp,
                fontWeight = if (isHighlight) FontWeight.ExtraBold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                maxLines = 2
            )
        }
    }
}
