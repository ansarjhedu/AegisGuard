package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.DecoyIconType
import com.example.data.model.DecoyItem
import com.example.data.model.DecoyState
import com.example.service.DecoyBluetoothTileService
import com.example.service.DecoyHotspotTileService
import com.example.service.DecoyMobileDataTileService
import com.example.service.DecoyWifiTileService
import com.example.service.requestAddQuickSettingsTile
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DecoyStatusBarPreview(
    decoyState: DecoyState,
    onBarClick: () -> Unit,
    onToggleIcon: ((DecoyIconType) -> Unit)? = null,
    onToggleReplaceOriginal: ((DecoyIconType) -> Unit)? = null,
    onUpdateWifi: ((String) -> Unit)? = null,
    onTriggerDecoyTrap: ((DecoyIconType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val currentTime = timeFormat.format(Date())

    val wifiItem = decoyState.icons.find { it.type == DecoyIconType.WIFI }
    val isWifiDecoyActive = wifiItem?.isEnabled == true
    val isWifiReplacingOriginal = wifiItem?.replacesOriginal == true

    var selectedDecoyForDetail by remember { mutableStateOf<DecoyItem?>(null) }

    if (selectedDecoyForDetail != null) {
        val currentItem = decoyState.icons.find { it.type == selectedDecoyForDetail?.type } ?: selectedDecoyForDetail!!
        DecoyIconDetailDialog(
            item = currentItem,
            decoyState = decoyState,
            onDismiss = { selectedDecoyForDetail = null },
            onToggleEnabled = {
                onToggleIcon?.invoke(currentItem.type)
            },
            onToggleReplaceOriginal = {
                onToggleReplaceOriginal?.invoke(currentItem.type)
            },
            onUpdateWifiSsid = { newSsid ->
                onUpdateWifi?.invoke(newSsid)
            },
            onTriggerTrap = {
                selectedDecoyForDetail = null
                onTriggerDecoyTrap?.invoke(currentItem.type)
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isWifiDecoyActive && isWifiReplacingOriginal) CyberPrimary else if (decoyState.isCamouflageServiceActive) CyberSecondary else Color(0xFF1E293B),
                RoundedCornerShape(14.dp)
            )
            .testTag("decoy_top_status_bar"),
        color = Color(0xFF090D16)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Realistic Android Status Bar simulation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Clock & Active Decoy Tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentTime,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isWifiDecoyActive && isWifiReplacingOriginal) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CyberPrimary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "WI-FI DECOY (ORIGINAL HIDDEN)",
                                    color = CyberPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    } else if (decoyState.isCamouflageServiceActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberSecondary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DECOY ACTIVE",
                                color = CyberSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TOP BAR PREVIEW",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Right side: Active Status Icons with interactive touch targets
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // VPN Decoy Slot
                    val vpnItem = decoyState.icons.find { it.type == DecoyIconType.VPN_SHIELD }
                    if (vpnItem?.isEnabled == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onToggleIcon?.invoke(DecoyIconType.VPN_SHIELD) }
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "Decoy VPN",
                                tint = CyberSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Location / GPS Decoy Slot
                    val locItem = decoyState.icons.find { it.type == DecoyIconType.LOCATION }
                    if (locItem?.isEnabled == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onToggleIcon?.invoke(DecoyIconType.LOCATION) }
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Decoy GPS",
                                tint = CyberPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Hotspot Decoy Slot
                    val hotspotItem = decoyState.icons.find { it.type == DecoyIconType.HOTSPOT }
                    if (hotspotItem?.isEnabled == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onToggleIcon?.invoke(DecoyIconType.HOTSPOT) }
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = "Decoy Hotspot",
                                tint = Color(0xFFFFB703),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Airplane Mode Decoy Slot
                    val airplaneItem = decoyState.icons.find { it.type == DecoyIconType.AIRPLANE_MODE }
                    if (airplaneItem?.isEnabled == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onToggleIcon?.invoke(DecoyIconType.AIRPLANE_MODE) }
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AirplanemodeActive,
                                contentDescription = "Decoy Airplane Mode",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Wi-Fi Slot (Decoy replacing original or regular Wi-Fi)
                    if (wifiItem != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onToggleIcon?.invoke(DecoyIconType.WIFI) }
                                .background(
                                    if (wifiItem.isEnabled && wifiItem.replacesOriginal)
                                        CyberPrimary.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("top_bar_wifi_icon")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = if (wifiItem.isEnabled) "Decoy Wi-Fi Trap" else "System Wi-Fi",
                                    tint = if (wifiItem.isEnabled) {
                                        if (wifiItem.replacesOriginal) CyberPrimary else CyberSecondary
                                    } else Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                                if (wifiItem.isEnabled && wifiItem.replacesOriginal) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Fake Wi-Fi Active",
                                        tint = CyberPrimary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Mobile Data (5G) Slot
                    val mobileItem = decoyState.icons.find { it.type == DecoyIconType.MOBILE_DATA }
                    if (mobileItem?.isEnabled == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onToggleIcon?.invoke(DecoyIconType.MOBILE_DATA) }
                                .padding(2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "5G",
                                    color = CyberPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = "Decoy 5G",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Bluetooth Slot
                    val btItem = decoyState.icons.find { it.type == DecoyIconType.BLUETOOTH }
                    if (btItem?.isEnabled == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onToggleIcon?.invoke(DecoyIconType.BLUETOOTH) }
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = "Decoy Bluetooth",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Battery Slot
                    val batteryItem = decoyState.icons.find { it.type == DecoyIconType.BATTERY }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                onToggleIcon?.invoke(DecoyIconType.BATTERY)
                            }
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = "Battery Status",
                            tint = if (batteryItem?.isEnabled == true) CyberSecondary else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Subtitle information strip
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        isWifiDecoyActive && isWifiReplacingOriginal ->
                            "Fake Wi-Fi ('${decoyState.fakeWifiSsid}') replaces real icon • Real Wi-Fi hidden & locked"
                        isWifiDecoyActive ->
                            "Decoy Wi-Fi active ('${decoyState.fakeWifiSsid}') • Tap icon to hide original"
                        decoyState.isCamouflageServiceActive ->
                            "Spoofed: ${decoyState.fakeWifiSsid} • ${decoyState.fakeLocationCity}"
                        else ->
                            "Tap Wi-Fi or any icon to replace real icon with Anti-Theft Decoy"
                    },
                    fontSize = 11.sp,
                    color = if (isWifiDecoyActive && isWifiReplacingOriginal) CyberPrimary else if (decoyState.isCamouflageServiceActive) CyberSecondary else Color(0xFF94A3B8),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isWifiReplacingOriginal) "🛡️ Trap On" else "${decoyState.icons.count { it.isEnabled }} At Top",
                    fontSize = 11.sp,
                    color = if (isWifiReplacingOriginal) CyberPrimary else Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DecoyIconDetailDialog(
    item: DecoyItem,
    decoyState: DecoyState,
    onDismiss: () -> Unit,
    onToggleEnabled: () -> Unit,
    onToggleReplaceOriginal: () -> Unit,
    onUpdateWifiSsid: (String) -> Unit,
    onTriggerTrap: () -> Unit
) {
    val context = LocalContext.current
    var wifiInput by remember { mutableStateOf(decoyState.fakeWifiSsid) }

    val iconVector = when (item.type) {
        DecoyIconType.WIFI -> Icons.Default.Wifi
        DecoyIconType.HOTSPOT -> Icons.Default.WifiTethering
        DecoyIconType.LOCATION -> Icons.Default.GpsFixed
        DecoyIconType.AIRPLANE_MODE -> Icons.Default.AirplanemodeActive
        DecoyIconType.MOBILE_DATA -> Icons.Default.NetworkCheck
        DecoyIconType.VPN_SHIELD -> Icons.Default.VpnKey
        DecoyIconType.BLUETOOTH -> Icons.Default.Bluetooth
        DecoyIconType.BATTERY -> Icons.Default.BatteryFull
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = item.type.displayName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (item.isEnabled && item.replacesOriginal)
                                    "🛡️ REPLACES ORIGINAL ICON"
                                else if (item.isEnabled)
                                    "DISPLAYED AT TOP"
                                else "DECOY DISABLED",
                                color = if (item.isEnabled && item.replacesOriginal) CyberPrimary else if (item.isEnabled) CyberSecondary else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security Status Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (item.replacesOriginal && item.isEnabled) CyberPrimary.copy(alpha = 0.4f) else Color(0xFF1E293B),
                            RoundedCornerShape(12.dp)
                        ),
                    color = Color(0xFF090D16)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (item.replacesOriginal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (item.replacesOriginal) CyberPrimary else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (item.replacesOriginal)
                                    "Original System Icon: HIDDEN & PROTECTED"
                                else "Original System Icon: VISIBLE",
                                color = if (item.replacesOriginal) CyberPrimary else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (item.type == DecoyIconType.WIFI) {
                                if (item.replacesOriginal)
                                    "Anti-theft protection active. If a thief attempts to disconnect Wi-Fi or turn off internet, the fake decoy blocks access. Real Wi-Fi stays hidden and active in stealth mode for remote GPS tracking!"
                                else
                                    "Original Wi-Fi is currently visible. Enable 'Hide Original Icon' to replace it with the fake decoy trap."
                            } else {
                                "Disguises device status to thwart forensic snooping and unauthorized hardware inspection."
                            },
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle 1: Enable / Display at Top
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Display Decoy Icon at Top",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Shows this fake decoy in the top status bar",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = item.isEnabled,
                        onCheckedChange = { onToggleEnabled() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyberPrimary,
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle 2: Hide Original & Replace with Decoy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hide Original & Replace with Decoy",
                            color = if (item.isEnabled) Color.White else Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hides real icon from top shade to prevent unauthorized tampering or shutoff",
                            color = if (item.isEnabled) Color(0xFF94A3B8) else Color(0xFF475569),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = item.replacesOriginal,
                        onCheckedChange = { onToggleReplaceOriginal() },
                        enabled = item.isEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyberPrimary,
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }

                // If Wi-Fi: Custom SSID input
                if (item.type == DecoyIconType.WIFI) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Disguised Fake Wi-Fi Name (SSID)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = wifiInput,
                            onValueChange = { wifiInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberPrimary,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onUpdateWifiSsid(wifiInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberPrimary,
                                contentColor = Color(0xFF090D16)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button: Add Quick Settings Tile to Top Shade
                val tileServiceClass = when (item.type) {
                    DecoyIconType.WIFI -> DecoyWifiTileService::class.java
                    DecoyIconType.MOBILE_DATA -> DecoyMobileDataTileService::class.java
                    DecoyIconType.HOTSPOT -> DecoyHotspotTileService::class.java
                    DecoyIconType.BLUETOOTH -> DecoyBluetoothTileService::class.java
                    else -> null
                }

                val titleRes = when (item.type) {
                    DecoyIconType.WIFI -> R.string.tile_decoy_wifi_name
                    DecoyIconType.MOBILE_DATA -> R.string.tile_decoy_mobile_data_name
                    DecoyIconType.HOTSPOT -> R.string.tile_decoy_hotspot_name
                    DecoyIconType.BLUETOOTH -> R.string.tile_decoy_bluetooth_name
                    else -> R.string.tile_shield_name
                }

                val iconRes = when (item.type) {
                    DecoyIconType.WIFI -> R.drawable.ic_qs_wifi
                    DecoyIconType.MOBILE_DATA -> R.drawable.ic_qs_mobile_data
                    DecoyIconType.HOTSPOT -> R.drawable.ic_qs_hotspot
                    DecoyIconType.BLUETOOTH -> R.drawable.ic_qs_bluetooth
                    else -> R.drawable.ic_qs_shield
                }

                if (tileServiceClass != null) {
                    Button(
                        onClick = {
                            requestAddQuickSettingsTile(
                                context = context,
                                serviceClass = tileServiceClass,
                                titleRes = titleRes,
                                iconRes = iconRes,
                                tileName = item.type.displayName
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = CyberPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Lookalike Tile to Android Top Shade",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action Button: Test Intruder Trap
                OutlinedButton(
                    onClick = onTriggerTrap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyberCritical
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CyberCritical,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simulate Intruder Trap (Lock Device)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
