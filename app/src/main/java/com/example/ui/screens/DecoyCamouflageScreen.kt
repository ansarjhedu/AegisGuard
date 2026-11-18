package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DecoyIconType
import com.example.data.model.DecoyItem
import com.example.ui.AegisUiState
import com.example.ui.components.DecoyStatusBarPreview
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberWarning

@Composable
fun DecoyCamouflageScreen(
    uiState: AegisUiState,
    onToggleService: () -> Unit,
    onToggleIcon: (DecoyIconType) -> Unit,
    onUpdateWifi: (String) -> Unit,
    onUpdateLocation: (String, String) -> Unit,
    onUpdateCarrier: (String) -> Unit,
    onOpenFullscreen: () -> Unit,
    onToggleReplaceOriginal: (DecoyIconType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val decoyState = uiState.decoyState

    var showLocationEditor by remember { mutableStateOf(false) }
    var showWifiEditor by remember { mutableStateOf(false) }
    var customWifiInput by remember { mutableStateOf(decoyState.fakeWifiSsid) }
    var customLocationCity by remember { mutableStateOf(decoyState.fakeLocationCity) }
    var customLocationCoords by remember { mutableStateOf(decoyState.fakeLocationCoordinates) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Live Status Bar Simulator
            Text(
                text = "STATUS BAR CAMOUFLAGE PREVIEW",
                color = CyberPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            DecoyStatusBarPreview(
                decoyState = decoyState,
                onBarClick = { /* already here */ },
                onToggleIcon = onToggleIcon,
                onToggleReplaceOriginal = onToggleReplaceOriginal,
                onUpdateWifi = onUpdateWifi
            )
        }

        // Service Master Toggle Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        1.dp,
                        if (decoyState.isCamouflageServiceActive) CyberSecondary else Color(0xFF1E293B),
                        RoundedCornerShape(18.dp)
                    ),
                color = Color(0xFF0F172A)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Persistent System Bar Camouflage",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (decoyState.isCamouflageServiceActive)
                                "Foreground Service active. Ongoing notification displays disguised status icons to trackers."
                            else "Activate to show disguised Wi-Fi, 5G, GPS, and Hotspot icons in the system bar shelf.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = decoyState.isCamouflageServiceActive,
                        onCheckedChange = { onToggleService() },
                        modifier = Modifier.testTag("decoy_service_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyberSecondary,
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }
            }
        }

        // Location Spoofing Shortcut Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                color = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Decoy Location Preset",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { showLocationEditor = !showLocationEditor },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = CyberPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (showLocationEditor) "Done" else "Change",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current Spoof: ${decoyState.fakeLocationCity} (${decoyState.fakeLocationCoordinates})",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (showLocationEditor) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Location presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PresetChip("Tokyo", "35.6762° N, 139.6503° E") {
                                onUpdateLocation("Tokyo, Japan", "35.6762° N, 139.6503° E")
                            }
                            PresetChip("London", "51.5074° N, 0.1278° W") {
                                onUpdateLocation("London, UK", "51.5074° N, 0.1278° W")
                            }
                            PresetChip("Zurich", "47.3769° N, 8.5417° E") {
                                onUpdateLocation("Zurich, Switzerland", "47.3769° N, 8.5417° E")
                            }
                            PresetChip("NYC", "40.7128° N, 74.0060° W") {
                                onUpdateLocation("New York, USA", "40.7128° N, 74.0060° W")
                            }
                        }
                    }
                }
            }
        }

        // List of Decoy Top Bar Icons with customizable toggles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CUSTOMIZE STATUS BAR ICONS",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${decoyState.icons.count { it.isEnabled }} Enabled",
                    color = CyberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(decoyState.icons, key = { it.type.name }) { item ->
            DecoyIconConfigRow(
                item = item,
                onToggle = { onToggleIcon(item.type) }
            )
        }

        // Full-Screen Camouflage Launch Button
        item {
            Button(
                onClick = onOpenFullscreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("fullscreen_camouflage_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = CyberPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PREVIEW FULL-SCREEN CAMOUFLAGE MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DecoyIconConfigRow(
    item: DecoyItem,
    onToggle: () -> Unit
) {
    val iconVector = when (item.type) {
        DecoyIconType.HOTSPOT -> Icons.Default.WifiTethering
        DecoyIconType.WIFI -> Icons.Default.Wifi
        DecoyIconType.LOCATION -> Icons.Default.GpsFixed
        DecoyIconType.AIRPLANE_MODE -> Icons.Default.AirplanemodeActive
        DecoyIconType.MOBILE_DATA -> Icons.Default.NetworkCheck
        DecoyIconType.VPN_SHIELD -> Icons.Default.VpnKey
        DecoyIconType.BLUETOOTH -> Icons.Default.Bluetooth
        DecoyIconType.BATTERY -> Icons.Default.BatteryFull
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (item.isEnabled) CyberPrimary.copy(alpha = 0.3f) else Color(0xFF1E293B),
                RoundedCornerShape(14.dp)
            ),
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
                    .background(if (item.isEnabled) CyberPrimary.copy(alpha = 0.15f) else Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = if (item.isEnabled) CyberPrimary else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.type.displayName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Camouflage: ${item.spoofedValue}",
                    color = if (item.isEnabled) CyberSecondary else Color(0xFF64748B),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Switch(
                checked = item.isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("toggle_icon_${item.type.name}"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CyberPrimary,
                    uncheckedTrackColor = Color(0xFF334155)
                )
            )
        }
    }
}

@Composable
private fun PresetChip(
    city: String,
    coords: String,
    onSelect: () -> Unit
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onSelect() }
    ) {
        Text(
            text = city,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
