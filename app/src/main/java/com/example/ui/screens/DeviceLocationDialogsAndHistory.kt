package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceLocationHistoryEntry
import com.example.data.model.DeviceLocationInfo
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val PAKISTAN_NEIGHBORHOOD_PRESETS = listOf(
    "Gulberg, Lahore, Pakistan",
    "Johar Town, Lahore, Pakistan",
    "DHA, Lahore, Pakistan",
    "Model Town, Lahore, Pakistan",
    "Bahria Town, Lahore, Pakistan",
    "Lahore, Pakistan",
    "Islamabad, Pakistan",
    "Karachi, Pakistan"
)

@Composable
internal fun EditDeviceIdentityAndLocationDialog(
    device: DeviceLocationInfo,
    onDismiss: () -> Unit,
    onSave: (newName: String, newPlatform: String, newClientType: String, locationQuery: String) -> Unit,
    onResetAuto: () -> Unit
) {
    var nameInput by remember(device.deviceId) { mutableStateOf(device.deviceName) }
    var platformInput by remember(device.deviceId) { mutableStateOf(device.platform) }
    var clientInput by remember(device.deviceId) { mutableStateOf(device.clientType) }
    var locationInput by remember(device.deviceId) {
        mutableStateOf(
            if (device.locationAddress.contains("uncalibrated", ignoreCase = true)) ""
            else device.locationAddress.substringBefore(" • LAN").trim()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Identify & Calibrate Device",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Node #${device.deviceId.takeLast(6).uppercase()} • IP ${device.ipAddress}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Device Label (e.g. Office Desktop — Gulberg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_device_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = platformInput,
                        onValueChange = { platformInput = it },
                        label = { Text("OS / Type") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_device_platform_input")
                    )
                    OutlinedTextField(
                        value = clientInput,
                        onValueChange = { clientInput = it },
                        label = { Text("App / Browser") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_device_client_input")
                    )
                }

                Text(
                    text = "Assign Physical Location (independent per device):",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PAKISTAN_NEIGHBORHOOD_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = locationInput.equals(preset, ignoreCase = true),
                            onClick = { locationInput = preset },
                            label = { Text(preset.substringBefore(", Pakistan"), fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("Area, City or lat, lng") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_device_location_input")
                )

                if (device.isCurrentDevice) {
                    TextButton(
                        onClick = {
                            onResetAuto()
                            onDismiss()
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Reset this hardware to auto-detect (GPS / IP / Timezone)",
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        nameInput.trim().ifBlank { device.deviceName },
                        platformInput.trim().ifBlank { device.platform },
                        clientInput.trim().ifBlank { device.clientType },
                        locationInput.trim()
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                modifier = Modifier.testTag("confirm_edit_device_btn")
            ) {
                Text("Save Device", fontWeight = FontWeight.Bold, color = Color(0xFF00363D))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
internal fun LocationHistorySection(
    devices: List<DeviceLocationInfo>,
    historyEntries: List<DeviceLocationHistoryEntry>,
    isCloudSynced: Boolean,
    onClearHistory: () -> Unit
) {
    var showTrackingExplainer by remember { mutableStateOf(false) }
    var selectedDeviceFilterId by remember { mutableStateOf<String?>(null) }

    val filteredHistory = remember(historyEntries, selectedDeviceFilterId) {
        if (selectedDeviceFilterId == null) historyEntries
        else historyEntries.filter { it.deviceId == selectedDeviceFilterId }
    }

    val filterDevices = remember(devices, historyEntries) {
        val map = linkedMapOf<String, String>()
        devices.forEach { map[it.deviceId] = it.deviceName }
        historyEntries.forEach { entry ->
            if (!map.containsKey(entry.deviceId)) {
                map[entry.deviceId] = entry.deviceName
            }
        }
        map.entries.toList()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .testTag("location_history_section"),
        color = Color(0xFF0F172A)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSecondary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Location History",
                            tint = CyberSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Location History (${filteredHistory.size})",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCloudSynced) {
                                "Synced: Local Ledger + Firestore Trail"
                            } else {
                                "Stored: Local Rolling Audit Ledger (100 pts)"
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                if (historyEntries.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("clear_location_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear History",
                            tint = CyberCritical,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", color = CyberCritical, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Collapsible honest explanation of how location history is tracked
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                    .clickable { showTrackingExplainer = !showTrackingExplainer }
                    .padding(horizontal = 10.dp, vertical = 9.dp)
                    .testTag("toggle_tracking_explainer_btn"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How is Location History Tracked?",
                        color = Color(0xFFBAE6FD),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (showTrackingExplainer) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Tracking Details",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = showTrackingExplainer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF07131F))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TrackingMethodBullet(
                        title = "1. This Device Telemetry",
                        detail = "Reads Android LocationManager (GPS/Network) excluding emulator default California coordinates, falls back to IP geolocation (ipwho.is) and system timezone (Asia/Karachi)."
                    )
                    TrackingMethodBullet(
                        title = "2. Per-Device Calibration (Gulberg / Johar Town)",
                        detail = "Google does not expose background GPS or session street addresses of remote desktops/phones to third-party apps. Calibrate any card independently to separate devices across neighborhoods."
                    )
                    TrackingMethodBullet(
                        title = "3. Immutable Trail Checkpoints",
                        detail = "Checkpoints append whenever a device initializes, changes neighborhood/coordinates, is added, or is calibrated—saved locally (last 100) and synced to Cloud Firestore (/users/{uid}/locationHistory) when signed in."
                    )
                }
            }

            if (filterDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedDeviceFilterId == null,
                        onClick = { selectedDeviceFilterId = null },
                        label = { Text("All Devices (${historyEntries.size})", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberSecondary,
                            selectedLabelColor = Color.White
                        )
                    )
                    filterDevices.forEach { (id, name) ->
                        val count = historyEntries.count { it.deviceId == id }
                        FilterChip(
                            selected = selectedDeviceFilterId == id,
                            onClick = { selectedDeviceFilterId = id },
                            label = {
                                Text(
                                    text = "${name.take(18)} ($count)",
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberPrimary,
                                selectedLabelColor = Color(0xFF00363D)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredHistory.isEmpty()) {
                Text(
                    text = "No location checkpoints recorded yet. Tap Refresh or Set City / Area on any device to log a location checkpoint.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val timeFmt = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredHistory.take(25).forEachIndexed { idx, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.45f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (idx == 0) CyberPrimary else CyberSecondary)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.deviceName,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = timeFmt.format(Date(entry.recordedAtMillis)),
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = entry.locationAddress,
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${entry.eventNote} • %.4f, %.4f".format(Locale.US, entry.latitude, entry.longitude),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (idx < filteredHistory.take(25).lastIndex) {
                            HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingMethodBullet(
    title: String,
    detail: String
) {
    Column {
        Text(
            text = title,
            color = CyberPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = detail,
            color = Color(0xFFCBD5E1),
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}
