package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DecoyIconType
import com.example.data.model.DecoyItem
import com.example.data.model.ThreatSeverity
import com.example.ui.AegisUiState
import com.example.ui.ScreenTab
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary

data class GuardIconDisplayMeta(
    val type: DecoyIconType,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val guardIconCatalog = listOf(
    GuardIconDisplayMeta(
        type = DecoyIconType.LOCATION,
        title = "Location (GPS)",
        description = "Camouflages active GPS telemetry",
        icon = Icons.Default.GpsFixed
    ),
    GuardIconDisplayMeta(
        type = DecoyIconType.AIRPLANE_MODE,
        title = "Airplane Mode",
        description = "Blocks unauthorized radio disconnect attempts",
        icon = Icons.Default.AirplanemodeActive
    ),
    GuardIconDisplayMeta(
        type = DecoyIconType.WIFI,
        title = "Wi-Fi Guard",
        description = "Displays secure enterprise Wi-Fi link",
        icon = Icons.Default.Wifi
    ),
    GuardIconDisplayMeta(
        type = DecoyIconType.MOBILE_DATA,
        title = "Mobile Data (5G)",
        description = "Simulates encrypted 5G carrier connection",
        icon = Icons.Default.NetworkCheck
    ),
    GuardIconDisplayMeta(
        type = DecoyIconType.HOTSPOT,
        title = "Personal Hotspot",
        description = "Broadcasts disguised hotspot indicator",
        icon = Icons.Default.WifiTethering
    ),
    GuardIconDisplayMeta(
        type = DecoyIconType.VPN_SHIELD,
        title = "Stealth VPN",
        description = "Displays active encrypted tunnel status",
        icon = Icons.Default.Shield
    ),
    GuardIconDisplayMeta(
        type = DecoyIconType.BLUETOOTH,
        title = "Bluetooth Guard",
        description = "Simulates encrypted hardware tethering",
        icon = Icons.Default.Bluetooth
    ),
    GuardIconDisplayMeta(
        type = DecoyIconType.BATTERY,
        title = "Battery Status",
        description = "Hides real power telemetry from profiling apps",
        icon = Icons.Default.BatteryFull
    )
)

@Composable
fun DashboardScreen(
    uiState: AegisUiState,
    onNavigate: (ScreenTab) -> Unit,
    onToggleShield: () -> Unit,
    onRunScan: () -> Unit,
    onDispatchEmailClicked: () -> Unit,
    onDismissTamper: () -> Unit,
    onToggleBiometric: () -> Unit = {},
    onLockNow: () -> Unit = {},
    onToggleDecoyIcon: (DecoyIconType) -> Unit = {},
    onToggleDecoyReplaceOriginal: (DecoyIconType) -> Unit = {},
    onUpdateDecoyWifi: (String) -> Unit = {},
    onTriggerDecoyTrap: (DecoyIconType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Strictly evaluate third-party applications - system apps are NEVER considered threats
    val thirdPartyApps = uiState.installedApps.filter { !it.isSystemApp && !it.isEssentialSystemApp }
    val thirdPartyThreatsCount = thirdPartyApps.count {
        !it.isWhitelisted && (it.riskLevel == ThreatSeverity.CRITICAL || it.riskLevel == ThreatSeverity.HIGH)
    }

    val activeDecoyCount = uiState.decoyState.icons.count { it.isEnabled }
    val totalIconsCount = guardIconCatalog.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        // 1. Minimalist Master Protection Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                    .testTag("minimalist_master_card"),
                color = Color(0xFF0F172A)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberPrimary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aegis Guard",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (uiState.isRealtimeShieldEnabled) "Real-time protection active" else "Protection paused",
                                color = if (uiState.isRealtimeShieldEnabled) CyberPrimary else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Master Shield Switch
                        Switch(
                            checked = uiState.isRealtimeShieldEnabled,
                            onCheckedChange = { onToggleShield() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyberPrimary,
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.testTag("master_shield_toggle")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Biometric Lock Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = CyberSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric Lock",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Locks when leaving the app",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = uiState.isBiometricEnforced,
                            onCheckedChange = { onToggleBiometric() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyberSecondary,
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.testTag("dashboard_biometric_toggle")
                        )
                    }

                    if (uiState.isBiometricEnforced) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onLockNow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("lock_session_now_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CyberSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lock Session Now",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 2. Third-Party App Security Summary Card (Minimalist)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (thirdPartyThreatsCount > 0) CyberCritical.copy(alpha = 0.5f) else Color(0xFF1E293B),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onNavigate(ScreenTab.SCANNER) }
                    .testTag("third_party_security_summary_card"),
                color = Color(0xFF0F172A)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (thirdPartyThreatsCount > 0) CyberCritical.copy(alpha = 0.18f)
                                else Color(0xFF10B981).copy(alpha = 0.18f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (thirdPartyThreatsCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (thirdPartyThreatsCount > 0) CyberCritical else Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Third-Party App Security",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (thirdPartyThreatsCount > 0) {
                                "$thirdPartyThreatsCount threats detected in third-party apps"
                            } else {
                                "${thirdPartyApps.size} third-party apps scanned • All safe"
                            },
                            color = if (thirdPartyThreatsCount > 0) CyberCritical else Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onRunScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberPrimary,
                            contentColor = Color(0xFF00363D)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dashboard_scan_now_button")
                    ) {
                        Text(
                            text = "Scan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2b. Device Fleet Location Card (Minimalist)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .clickable { onNavigate(ScreenTab.DEVICES_LOCATION) }
                    .testTag("dashboard_location_card"),
                color = Color(0xFF0F172A)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberPrimary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = CyberPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Device Fleet Location",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.isGoogleAuthenticated) "Google Account Linked (Verified)" else "6 Devices Auto-Synced via Google Account",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${if (uiState.isGoogleAuthenticated) "${uiState.authenticatedUserName ?: "Verified"} • " else ""}${uiState.emailRecipient}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onNavigate(ScreenTab.GOOGLE_AUTH) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isGoogleAuthenticated) Color(0xFF0F766E) else Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("dashboard_google_auth_btn")
                        ) {
                            Text(
                                text = if (uiState.isGoogleAuthenticated) "Profile" else "Auth",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { onNavigate(ScreenTab.DEVICES_LOCATION) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color(0xFF022C22)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("dashboard_track_devices_button")
                        ) {
                            Text(
                                text = "Fleet",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Section Title: Icons & Toggles
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GUARD & DECOY ICONS",
                    color = CyberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$activeDecoyCount of $totalIconsCount Active",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 4. Clean List of all 8 Icons and Toggle Buttons
        items(guardIconCatalog, key = { it.type.name }) { meta ->
            val iconItem = uiState.decoyState.icons.find { it.type == meta.type }
            val isEnabled = iconItem?.isEnabled ?: false

            MinimalistIconToggleCard(
                meta = meta,
                isEnabled = isEnabled,
                onToggle = { onToggleDecoyIcon(meta.type) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MinimalistIconToggleCard(
    meta: GuardIconDisplayMeta,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    val containerBorderColor by animateColorAsState(
        targetValue = if (isEnabled) CyberPrimary.copy(alpha = 0.45f) else Color(0xFF1E293B),
        label = "borderColor"
    )

    val iconBgColor by animateColorAsState(
        targetValue = if (isEnabled) CyberPrimary.copy(alpha = 0.16f) else Color(0xFF1E293B).copy(alpha = 0.7f),
        label = "iconBgColor"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isEnabled) CyberPrimary else Color(0xFF64748B),
        label = "iconTint"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, containerBorderColor, RoundedCornerShape(14.dp))
            .clickable { onToggle() }
            .testTag("guard_icon_card_${meta.type.name.lowercase()}"),
        color = Color(0xFF0F172A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = meta.icon,
                    contentDescription = meta.title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Description
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meta.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isEnabled) CyberPrimary.copy(alpha = 0.15f)
                                else Color(0xFF334155).copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isEnabled) "ACTIVE" else "OFF",
                            color = if (isEnabled) CyberPrimary else Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = meta.description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Direct Toggle Switch
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CyberPrimary,
                    uncheckedThumbColor = Color(0xFF64748B),
                    uncheckedTrackColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.testTag("toggle_icon_${meta.type.name.lowercase()}")
            )
        }
    }
}
