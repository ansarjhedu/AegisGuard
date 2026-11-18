package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AegisPairingToken
import com.example.data.model.DeviceLocationHistoryEntry
import com.example.data.model.DeviceLocationInfo
import com.example.data.model.DeviceTrackingMode
import com.example.data.model.SessionLocationPoint
import com.example.data.session.SessionSyncState
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

enum class DeviceFilterCategory {
    ALL,
    THIS_DEVICE_ONLY,
    GOOGLE_SESSIONS_ONLY,
    TOKEN_PAIRED_ONLY
}

@Composable
fun DeviceLocationScreen(
    userEmail: String,
    trackingMode: DeviceTrackingMode,
    devices: List<DeviceLocationInfo>,
    pairingTokens: List<AegisPairingToken>,
    onSelectMode: (DeviceTrackingMode) -> Unit,
    onRefreshLocations: () -> Unit,
    onAddGmailDevice: (String, String, String, String) -> Unit,
    onUpdateUserEmail: (String) -> Unit,
    onSignOutDeviceRemotely: (String) -> Unit,
    onOpenGoogleAccountSecurity: (Context) -> Unit,
    onGenerateToken: (String) -> Unit,
    onPairWithToken: (String, String) -> Unit,
    onRemoveDevice: (String) -> Unit,
    onRevokeToken: (String) -> Unit,
    onToggleLockdown: (String) -> Unit,
    onRingDevice: (String) -> Unit,
    onOpenMaps: (Context, Double, Double, String) -> Unit,
    onDispatchFleetReport: (Context) -> Unit,
    locationHistory: List<DeviceLocationHistoryEntry> = emptyList(),
    onUpdateDeviceIdentityAndLocation: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onClearNetworkDevices: () -> Unit = {},
    onClearLocationHistory: () -> Unit = {},
    isGoogleAuthenticated: Boolean = false,
    authenticatedUserName: String? = null,
    authenticatedPhotoUrl: String? = null,
    isGoogleSignInLoading: Boolean = false,
    firebaseSyncState: SessionSyncState = SessionSyncState.Unauthenticated,
    onSignInWithGoogle: (Context) -> Unit = {},
    onSignOutGoogle: () -> Unit = {},
    onLinkManualAccount: (String, String?) -> Unit = { _, _ -> },
    onDiscoverNetworkDevices: () -> Unit = {},
    onSetLocationOverride: (String) -> Unit = {},
    onResetLocationToAuto: () -> Unit = {},
    sessionLocationPoints: Map<String, List<SessionLocationPoint>> = emptyMap(),
    currentInstallSessionId: String? = null,
    isTrackerActive: Boolean = false,
    lastTrackerFixSummary: String? = null,
    onCaptureSessionFixes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPairDialog by remember { mutableStateOf(false) }
    var showGenerateTokenDialog by remember { mutableStateOf(false) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showGoogleAccountLinkDialog by remember { mutableStateOf(false) }
    var showSetLocationDialog by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<DeviceLocationInfo?>(null) }
    var selectedFilter by remember { mutableStateOf(DeviceFilterCategory.ALL) }
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }

    val filteredDevices = remember(devices, selectedFilter, trackingMode) {
        when {
            trackingMode == DeviceTrackingMode.AEGIS_DEVICE_TOKEN -> {
                devices.filter { it.pairedToken != null || it.isCurrentDevice }
            }
            selectedFilter == DeviceFilterCategory.THIS_DEVICE_ONLY -> {
                devices.filter { it.isCurrentDevice }
            }
            selectedFilter == DeviceFilterCategory.GOOGLE_SESSIONS_ONLY -> {
                devices.filter { !it.hasAegisGuard || it.isCurrentDevice }
            }
            selectedFilter == DeviceFilterCategory.TOKEN_PAIRED_ONLY -> {
                devices.filter { it.pairedToken != null }
            }
            else -> devices
        }
    }

    val currentDevice = devices.firstOrNull { it.isCurrentDevice }
    val externalDeviceCount = devices.count { !it.isCurrentDevice }
    val lanScannedCount = devices.count { !it.isCurrentDevice && it.deviceId.startsWith("dev-net-") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 1. Unified Fleet Command & Google Identity Header Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                    .testTag("location_command_header_card"),
                color = Color(0xFF0F172A)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Row 1: Title, Active Node Count, and Refresh
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberPrimary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = "Fleet Telemetry",
                                    tint = CyberPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Device Telemetry & Fleet",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val firebaseLabel = when (firebaseSyncState) {
                                    is SessionSyncState.Synced -> " • Firebase Cloud (${firebaseSyncState.remoteSessionCount})"
                                    is SessionSyncState.Syncing -> " • Firebase Syncing..."
                                    is SessionSyncState.Error -> " • Cloud Sync Error"
                                    is SessionSyncState.Unauthenticated -> " • Local Telemetry"
                                }
                                Text(
                                    text = "${devices.size} Active Node${if (devices.size == 1) "" else "s"}$firebaseLabel",
                                    color = if (firebaseSyncState is SessionSyncState.Error) Color(0xFFFCA5A5) else Color(0xFFA7F3D0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onRefreshLocations,
                            modifier = Modifier.testTag("refresh_locations_btn")
                        ) {
                            if (isGoogleSignInLoading) {
                                CircularProgressIndicator(
                                    color = CyberPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh GPS Telemetry",
                                    tint = CyberPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Google Account Identity & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isGoogleAuthenticated) Color(0xFF10B981).copy(alpha = 0.2f)
                                    else Color(0xFF3B82F6).copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isGoogleAuthenticated) Icons.Default.CheckCircle else Icons.Default.AccountCircle,
                                contentDescription = "Google Account Status",
                                tint = if (isGoogleAuthenticated) Color(0xFF10B981) else Color(0xFF60A5FA),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isGoogleAuthenticated) (authenticatedUserName ?: "Verified Google Account") else "Google Account",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isGoogleAuthenticated) Color(0xFF10B981).copy(alpha = 0.2f)
                                            else Color(0xFF3B82F6).copy(alpha = 0.2f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isGoogleAuthenticated) "VERIFIED" else "CREDENTIAL MANAGER",
                                        color = if (isGoogleAuthenticated) Color(0xFF10B981) else Color(0xFF93C5FD),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showChangeEmailDialog = true }
                            ) {
                                Text(
                                    text = userEmail,
                                    color = CyberPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Monitored Gmail",
                                    tint = CyberPrimary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    // Authentication Action (if not authenticated)
                    if (!isGoogleAuthenticated) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onSignInWithGoogle(context) },
                                enabled = !isGoogleSignInLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3B82F6),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .heightIn(min = 40.dp)
                                    .testTag("google_sign_in_button")
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign In with Google", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { showGoogleAccountLinkDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF93C5FD)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A8A)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .testTag("link_manual_account_btn")
                            ) {
                                Text("Link Session", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 3: Action Quick-Bar (Google Device Activity, Scan Wi-Fi, Add Device)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onOpenGoogleAccountSecurity(context) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 38.dp)
                                .testTag("open_google_security_btn")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google Devices", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }

                        Button(
                            onClick = onDiscoverNetworkDevices,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberPrimary.copy(alpha = 0.16f),
                                contentColor = CyberPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 38.dp)
                                .testTag("discover_network_devices_btn")
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Wi-Fi", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }

                        Button(
                            onClick = { showAddDeviceDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFFE2E8F0)
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 38.dp)
                                .testTag("add_custom_session_optional_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Device", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // 2. Compact Live Radar Telemetry HUD
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CyberPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .testTag("location_radar_map_surface"),
                color = Color(0xFF07131F)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(CyberPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FLEET RADAR HUD",
                                color = CyberPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                            if (isTrackerActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.18f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "TRACKER ON",
                                        color = Color(0xFF10B981),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${devices.size} ONLINE",
                            color = Color(0xFFA7F3D0),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Radar Canvas Component (compact 140dp)
                    DeviceFleetRadarMap(
                        devices = devices,
                        selectedDeviceId = selectedDeviceId,
                        onDeviceSelected = { selectedDeviceId = it.deviceId },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Coordinates & Calibrate Location Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                            .clickable { showSetLocationDialog = true }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val focusedDevice = devices.firstOrNull { it.deviceId == selectedDeviceId } ?: currentDevice
                        val lat = focusedDevice?.latitude ?: 0.0
                        val lng = focusedDevice?.longitude ?: 0.0
                        val distinctAreas = devices.map { it.locationAddress.substringBefore(" • LAN").trim() }.distinct().size
                        val coordLabel = if (lat == 0.0 && lng == 0.0) {
                            "Uncalibrated Node"
                        } else {
                            val latStr = if (lat >= 0) "%.4f° N".format(Locale.US, lat) else "%.4f° S".format(Locale.US, -lat)
                            val lngStr = if (lng >= 0) "%.4f° E".format(Locale.US, lng) else "%.4f° W".format(Locale.US, -lng)
                            "$latStr, $lngStr"
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = focusedDevice?.locationAddress ?: "Resolving location...",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$coordLabel • $distinctAreas zone${if (distinctAreas == 1) "" else "s"} • ${focusedDevice?.locationSource ?: "IP / GPS"}",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.18f))
                                    .clickable { onCaptureSessionFixes() }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("capture_session_gps_fix_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = "Capture Per-Session GPS Fix",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "GPS Fix",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberPrimary.copy(alpha = 0.16f))
                                    .clickable { showSetLocationDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("set_location_override_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Set Location",
                                        tint = CyberPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Set City",
                                        color = CyberPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    if (!lastTrackerFixSummary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Location Tracker: $lastTrackerFixSummary",
                            color = Color(0xFFA7F3D0),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("location_tracker_summary_text")
                        )
                    }
                }
            }
        }

        // 3. Segmented Mode & Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == DeviceFilterCategory.ALL,
                    onClick = { selectedFilter = DeviceFilterCategory.ALL },
                    label = { Text("All (${devices.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberPrimary,
                        selectedLabelColor = Color(0xFF00363D)
                    )
                )

                FilterChip(
                    selected = selectedFilter == DeviceFilterCategory.THIS_DEVICE_ONLY,
                    onClick = { selectedFilter = DeviceFilterCategory.THIS_DEVICE_ONLY },
                    label = { Text("This Device", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberPrimary,
                        selectedLabelColor = Color(0xFF00363D)
                    )
                )

                FilterChip(
                    selected = selectedFilter == DeviceFilterCategory.GOOGLE_SESSIONS_ONLY,
                    onClick = { selectedFilter = DeviceFilterCategory.GOOGLE_SESSIONS_ONLY },
                    label = { Text("Gmail Sessions (${devices.count { !it.hasAegisGuard || it.isCurrentDevice }})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981),
                        selectedLabelColor = Color(0xFF022C22)
                    )
                )

                FilterChip(
                    selected = selectedFilter == DeviceFilterCategory.TOKEN_PAIRED_ONLY,
                    onClick = { selectedFilter = DeviceFilterCategory.TOKEN_PAIRED_ONLY },
                    label = { Text("Tokens (${pairingTokens.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberSecondary,
                        selectedLabelColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Mode switch pill
                val isGoogleMode = trackingMode == DeviceTrackingMode.GOOGLE_LOCATION
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.clickable {
                        onSelectMode(if (isGoogleMode) DeviceTrackingMode.AEGIS_DEVICE_TOKEN else DeviceTrackingMode.GOOGLE_LOCATION)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGoogleMode) Icons.Default.GpsFixed else Icons.Default.Key,
                            contentDescription = null,
                            tint = CyberPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGoogleMode) "Mode: Google" else "Mode: Token",
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Token Management Panel (if Token mode is active)
        if (trackingMode == DeviceTrackingMode.AEGIS_DEVICE_TOKEN) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, CyberSecondary.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    color = Color(0xFF0F172A)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AEGIS PAIRING TOKENS",
                                color = CyberSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { showPairDialog = true },
                                    modifier = Modifier.heightIn(min = 34.dp).testTag("pair_with_token_action_btn"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Enter Token", fontSize = 10.sp, color = CyberSecondary, maxLines = 1, softWrap = false)
                                }

                                Button(
                                    onClick = { showGenerateTokenDialog = true },
                                    modifier = Modifier.heightIn(min = 34.dp).testTag("generate_new_token_action_btn"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("New Token", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        pairingTokens.forEach { token ->
                            TokenCard(
                                token = token,
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("AegisGuard Token", token.tokenId))
                                    Toast.makeText(context, "Copied ${token.tokenId}", Toast.LENGTH_SHORT).show()
                                },
                                onRevoke = { onRevokeToken(token.tokenId) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // 5. Active Devices Section Header + LAN Scan Identification Helper
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ACTIVE DEVICES & SESSIONS (${filteredDevices.size})",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = onRefreshLocations,
                        modifier = Modifier.testTag("resync_gmail_devices_btn")
                    ) {
                        Text("Auto-Sync", color = CyberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }

                if (lanScannedCount > 0) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            .testTag("lan_scan_notice_banner"),
                        color = Color(0xFF0D1B2A)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$lanScannedCount LAN-Scanned Node(s) Detected",
                                    color = Color(0xFFFDE68A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Added by Wi-Fi subnet scan. Tap any device's location box to name it & set Gulberg / Johar Town, or clear scan noise.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = onClearNetworkDevices,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCritical.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .testTag("clear_lan_scan_devices_btn")
                            ) {
                                Text(
                                    text = "Clear LAN ($lanScannedCount)",
                                    color = CyberCritical,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Original Google Account Sessions Guidance Card (when only This Device is present)
        if (externalDeviceCount == 0) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                        .testTag("google_original_sessions_guide_card"),
                    color = Color(0xFF0D1B2A)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Original Google Account Sessions",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Google restricts third-party apps from reading remote desktop/mobile browser sessions automatically. View all active sessions in Google Device Activity or register your desktops & mobile below:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onOpenGoogleAccountSecurity(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E3A8A),
                                contentColor = Color(0xFF93C5FD)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 38.dp)
                                .testTag("open_google_device_activity_guide_btn")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Google Device Activity (myaccount.google.com)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val currentCity = currentDevice?.locationAddress ?: "Gulberg, Lahore, Pakistan"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onAddGmailDevice("Desktop PC", "Windows 11", "Google Chrome (Gmail Web)", currentCity)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 36.dp)
                            ) {
                                Text("+ Desktop PC", fontSize = 10.sp, color = CyberPrimary, maxLines = 1, softWrap = false)
                            }

                            OutlinedButton(
                                onClick = {
                                    onAddGmailDevice("Mobile Phone", "Android / iOS", "Gmail Mobile App", currentCity)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 36.dp)
                            ) {
                                Text("+ Mobile Phone", fontSize = 10.sp, color = CyberPrimary, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }

        // 7. Device Items List
        items(filteredDevices, key = { it.deviceId }) { dev ->
            val sanitizedId = dev.deviceId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val pointsForSession = if (dev.isCurrentDevice && !currentInstallSessionId.isNullOrBlank()) {
                sessionLocationPoints[currentInstallSessionId]
                    ?: sessionLocationPoints[sanitizedId]
                    ?: sessionLocationPoints[dev.deviceId]
                    ?: emptyList()
            } else {
                sessionLocationPoints[sanitizedId]
                    ?: sessionLocationPoints[dev.deviceId]
                    ?: emptyList()
            }
            DeviceItemCard(
                device = dev,
                sessionPoints = pointsForSession,
                onRing = { onRingDevice(dev.deviceId) },
                onToggleLock = { onToggleLockdown(dev.deviceId) },
                onSignOutRemotely = { onSignOutDeviceRemotely(dev.deviceId) },
                onOpenMap = { onOpenMaps(context, dev.latitude, dev.longitude, dev.deviceName) },
                onOpenGoogleSecurity = { onOpenGoogleAccountSecurity(context) },
                onRemove = { onRemoveDevice(dev.deviceId) },
                onEditDevice = { editingDevice = dev }
            )
        }

        // 8. Location History Timeline & How Tracking Works
        item {
            LocationHistorySection(
                devices = devices,
                historyEntries = locationHistory,
                isCloudSynced = firebaseSyncState is SessionSyncState.Synced,
                onClearHistory = onClearLocationHistory
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal: Add Device Dialog with Quick Platform Presets
    if (showAddDeviceDialog) {
        val defaultCity = currentDevice?.locationAddress ?: "Pakistan"
        var deviceNameInput by remember { mutableStateOf("") }
        var platformInput by remember { mutableStateOf("Windows 11") }
        var clientTypeInput by remember { mutableStateOf("Google Chrome (Gmail Web)") }
        var locationInput by remember { mutableStateOf(defaultCity) }

        AlertDialog(
            onDismissRequest = { showAddDeviceDialog = false },
            title = {
                Text("Add Google Account Device", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Add a personal computer, laptop, or phone where $userEmail is logged in:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("My Laptop", "Windows 11", "Google Chrome"),
                            Triple("MacBook Pro", "macOS", "Safari (Gmail Web)"),
                            Triple("iPad Pro", "Apple iPadOS", "Gmail iOS App"),
                            Triple("Desktop PC", "Linux", "Firefox (Gmail Web)")
                        ).forEach { (presetName, presetPlat, presetClient) ->
                            FilterChip(
                                selected = platformInput == presetPlat,
                                onClick = {
                                    deviceNameInput = presetName
                                    platformInput = presetPlat
                                    clientTypeInput = presetClient
                                },
                                label = { Text(presetName, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = deviceNameInput,
                        onValueChange = { deviceNameInput = it },
                        label = { Text("Device Name (e.g. My Laptop)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_device_name_input")
                    )

                    OutlinedTextField(
                        value = platformInput,
                        onValueChange = { platformInput = it },
                        label = { Text("Platform (Windows, Mac, iOS, Android, Linux)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_device_platform_input")
                    )

                    OutlinedTextField(
                        value = clientTypeInput,
                        onValueChange = { clientTypeInput = it },
                        label = { Text("Client App (Chrome, Safari, Gmail App)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_device_client_input")
                    )

                    OutlinedTextField(
                        value = locationInput,
                        onValueChange = { locationInput = it },
                        label = { Text("Approximate Location") },
                        modifier = Modifier.fillMaxWidth().testTag("add_device_location_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deviceNameInput.isNotBlank()) {
                            onAddGmailDevice(deviceNameInput.trim(), platformInput.trim(), clientTypeInput.trim(), locationInput.trim())
                            showAddDeviceDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    modifier = Modifier.testTag("confirm_add_gmail_device_btn")
                ) {
                    Text("Add Device", fontWeight = FontWeight.Bold, color = Color(0xFF00363D))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDeviceDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Modal: Change Email Dialog
    if (showChangeEmailDialog) {
        var emailInput by remember { mutableStateOf(userEmail) }
        AlertDialog(
            onDismissRequest = { showChangeEmailDialog = false },
            title = {
                Text("Change Monitored Gmail", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column {
                    Text("Enter the Google account email to monitor.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Gmail Address") },
                        modifier = Modifier.fillMaxWidth().testTag("change_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            onUpdateUserEmail(emailInput.trim())
                            showChangeEmailDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    modifier = Modifier.testTag("confirm_change_email_btn")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color(0xFF00363D))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeEmailDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Modal: Link Google Account
    if (showGoogleAccountLinkDialog) {
        var emailInput by remember { mutableStateOf(userEmail) }
        var nameInput by remember { mutableStateOf(authenticatedUserName ?: "") }

        AlertDialog(
            onDismissRequest = { showGoogleAccountLinkDialog = false },
            title = {
                Text("Link Google Account Session", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Link your Google credentials with AegisGuard to monitor active sessions.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Gmail Address") },
                        modifier = Modifier.fillMaxWidth().testTag("google_link_email_input")
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name (Optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("google_link_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            onLinkManualAccount(emailInput.trim(), nameInput.trim().ifBlank { null })
                            showGoogleAccountLinkDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    modifier = Modifier.testTag("confirm_link_google_btn")
                ) {
                    Text("Link Session", fontWeight = FontWeight.Bold, color = Color(0xFF00363D))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleAccountLinkDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Modal: Generate Token
    if (showGenerateTokenDialog) {
        var targetDeviceNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGenerateTokenDialog = false },
            title = { Text("Generate Aegis Token", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = targetDeviceNameInput,
                    onValueChange = { targetDeviceNameInput = it },
                    label = { Text("Device Name") },
                    modifier = Modifier.fillMaxWidth().testTag("token_device_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onGenerateToken(targetDeviceNameInput.ifBlank { "Remote Device" })
                        showGenerateTokenDialog = false
                    },
                    modifier = Modifier.testTag("confirm_generate_token_btn")
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateTokenDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: Pair Token
    if (showPairDialog) {
        var devName by remember { mutableStateOf("") }
        var tokenCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPairDialog = false },
            title = { Text("Pair Device via Token", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = devName,
                        onValueChange = { devName = it },
                        label = { Text("Device Name") },
                        modifier = Modifier.fillMaxWidth().testTag("pair_device_name_input")
                    )
                    OutlinedTextField(
                        value = tokenCode,
                        onValueChange = { tokenCode = it.uppercase() },
                        label = { Text("Token Code") },
                        modifier = Modifier.fillMaxWidth().testTag("pair_token_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tokenCode.isNotBlank()) {
                            onPairWithToken(devName.ifBlank { "Paired Device" }, tokenCode)
                            showPairDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_pair_device_btn")
                ) {
                    Text("Pair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPairDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: Calibrate / Set Current Physical City & Neighborhood
    if (showSetLocationDialog) {
        var cityInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSetLocationDialog = false },
            title = {
                Text("Set Verified Device Location", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Calibrate this hardware's neighborhood or city (tap any device card's location box to calibrate other devices individually):",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PAKISTAN_NEIGHBORHOOD_PRESETS.forEach { presetCity ->
                            FilterChip(
                                selected = cityInput == presetCity,
                                onClick = { cityInput = presetCity },
                                label = { Text(presetCity.substringBefore(", Pakistan"), fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text("Area, City or Country (e.g. Gulberg, Lahore, Pakistan)") },
                        modifier = Modifier.fillMaxWidth().testTag("set_location_city_input")
                    )
                    TextButton(
                        onClick = {
                            onResetLocationToAuto()
                            showSetLocationDialog = false
                        }
                    ) {
                        Text("Reset to Auto-Detect (GPS / Live IP)", color = Color(0xFF93C5FD), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cityInput.isNotBlank()) {
                            onSetLocationOverride(cityInput.trim())
                            showSetLocationDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    modifier = Modifier.testTag("confirm_set_location_btn")
                ) {
                    Text("Save Location", fontWeight = FontWeight.Bold, color = Color(0xFF00363D))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetLocationDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Modal: Per-Device Identity & Independent Location Editor (Any Card)
    editingDevice?.let { devToEdit ->
        EditDeviceIdentityAndLocationDialog(
            device = devToEdit,
            onDismiss = { editingDevice = null },
            onSave = { newName, newPlat, newClient, locQuery ->
                onUpdateDeviceIdentityAndLocation(
                    devToEdit.deviceId,
                    newName,
                    newPlat,
                    newClient,
                    locQuery
                )
            },
            onResetAuto = { onResetLocationToAuto() }
        )
    }
}

/**
 * Compact Radar HUD Canvas with concentric distance rings, crosshairs, animated rotating sweep,
 * and interactive device blips.
 */
@Composable
private fun DeviceFleetRadarMap(
    devices: List<DeviceLocationInfo>,
    selectedDeviceId: String?,
    onDeviceSelected: (DeviceLocationInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF040D14))
            .border(1.dp, Color(0xFF0F2B3C), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = minOf(centerX, centerY) * 0.85f

            // Concentric Radar Distance Rings
            val ringCount = 3
            for (i in 1..ringCount) {
                val r = (maxRadius / ringCount) * i
                drawCircle(
                    color = Color(0xFF0F3B4C).copy(alpha = 0.5f),
                    radius = r,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                )
            }

            // Crosshairs
            drawLine(
                color = Color(0xFF0F3B4C).copy(alpha = 0.4f),
                start = Offset(centerX, centerY - maxRadius),
                end = Offset(centerX, centerY + maxRadius),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0xFF0F3B4C).copy(alpha = 0.4f),
                start = Offset(centerX - maxRadius, centerY),
                end = Offset(centerX + maxRadius, centerY),
                strokeWidth = 1.dp.toPx()
            )

            // Animated Sweeping Beam
            val angleRad = Math.toRadians(sweepAngle.toDouble())
            val sweepX = (centerX + maxRadius * cos(angleRad)).toFloat()
            val sweepY = (centerY + maxRadius * sin(angleRad)).toFloat()

            drawLine(
                brush = Brush.radialGradient(
                    colors = listOf(CyberPrimary.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = maxRadius
                ),
                start = Offset(centerX, centerY),
                end = Offset(sweepX, sweepY),
                strokeWidth = 2.dp.toPx()
            )

            // Central Beacon: This Device
            drawCircle(
                color = CyberPrimary.copy(alpha = 0.25f),
                radius = 12.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = CyberPrimary,
                radius = 5.dp.toPx(),
                center = Offset(centerX, centerY)
            )

            // Plot Other Devices (use geographic bearing & scaled distance when coordinates differ; ring spread when stacked)
            val current = devices.firstOrNull { it.isCurrentDevice }
            val currentLat = current?.latitude ?: 0.0
            val currentLng = current?.longitude ?: 0.0

            val otherDevices = devices.filter { !it.isCurrentDevice }
            otherDevices.forEachIndexed { index, dev ->
                val deltaLat = (dev.latitude - currentLat)
                val deltaLng = (dev.longitude - currentLng)
                val geoDist = kotlin.math.hypot(deltaLat, deltaLng)

                val angle = if (geoDist > 0.0005) {
                    Math.atan2(deltaLat, deltaLng)
                } else {
                    Math.toRadians((360.0 / otherDevices.size.coerceAtLeast(1)) * index + 25.0)
                }
                val normDist = if (geoDist > 0.0005) {
                    ((geoDist / 0.12).coerceIn(0.35, 0.85) * maxRadius).toFloat()
                } else {
                    maxRadius * 0.45f
                }

                val devX = (centerX + normDist * cos(angle)).toFloat()
                val devY = (centerY - normDist * sin(angle)).toFloat()

                val isSelected = selectedDeviceId == dev.deviceId
                val pinColor = when {
                    dev.deviceId.startsWith("dev-net-") -> Color(0xFFF59E0B)
                    dev.hasAegisGuard -> CyberSecondary
                    else -> Color(0xFF10B981)
                }

                // Halo ring
                drawCircle(
                    color = pinColor.copy(alpha = if (isSelected) 0.6f else 0.25f),
                    radius = if (isSelected) 10.dp.toPx() else 7.dp.toPx(),
                    center = Offset(devX, devY)
                )
                // Core dot
                drawCircle(
                    color = pinColor,
                    radius = if (isSelected) 4.5.dp.toPx() else 3.5.dp.toPx(),
                    center = Offset(devX, devY)
                )
            }
        }

        // Cardinal compass directions
        Text(
            text = "N",
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp)
        )
        Text(
            text = "S",
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
        )
        Text(
            text = "W",
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
        )
        Text(
            text = "E",
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
        )
    }
}

/**
 * Responsive Device Item Card with clean button sizing, non-overlapping telemetry,
 * and high-contrast status tags.
 */
@Composable
private fun DeviceItemCard(
    device: DeviceLocationInfo,
    sessionPoints: List<SessionLocationPoint> = emptyList(),
    onRing: () -> Unit,
    onToggleLock: () -> Unit,
    onSignOutRemotely: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenGoogleSecurity: () -> Unit,
    onRemove: () -> Unit,
    onEditDevice: () -> Unit = {}
) {
    val icon = when {
        device.platform.contains("Windows", ignoreCase = true) -> Icons.Default.Laptop
        device.platform.contains("macOS", ignoreCase = true) || device.platform.contains("Mac", ignoreCase = true) -> Icons.Default.Laptop
        device.platform.contains("iOS", ignoreCase = true) || device.platform.contains("iPhone", ignoreCase = true) -> Icons.Default.PhoneIphone
        device.platform.contains("iPad", ignoreCase = true) -> Icons.Default.Tablet
        device.platform.contains("Linux", ignoreCase = true) -> Icons.Default.Computer
        device.platform.contains("Router", ignoreCase = true) || device.platform.contains("Gateway", ignoreCase = true) -> Icons.Default.Wifi
        else -> Icons.Default.PhoneAndroid
    }
    val isLanScan = !device.isCurrentDevice && device.deviceId.startsWith("dev-net-")
    val isCloudNode = !device.isCurrentDevice && device.deviceId.startsWith("node_")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                when {
                    device.isCurrentDevice -> CyberPrimary.copy(alpha = 0.5f)
                    isLanScan -> Color(0xFFF59E0B).copy(alpha = 0.35f)
                    else -> Color(0xFF1E293B)
                },
                RoundedCornerShape(16.dp)
            )
            .testTag("device_card_${device.deviceId}"),
        color = Color(0xFF0F172A)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Device Icon, Name, Hardware/Node ID, and Origin Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (device.isCurrentDevice) CyberPrimary.copy(alpha = 0.16f)
                            else if (isLanScan) Color(0xFFF59E0B).copy(alpha = 0.15f)
                            else Color(0xFF1E293B)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = when {
                            device.isCurrentDevice -> CyberPrimary
                            isLanScan -> Color(0xFFFBBF24)
                            else -> Color(0xFF94A3B8)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.deviceName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${device.platform} • ${device.clientType}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Model: ${device.deviceModel} • Node #${device.deviceId.takeLast(6).uppercase()}",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                device.isSignedOutRemotely -> CyberCritical.copy(alpha = 0.2f)
                                device.isLockedRemotely -> CyberCritical.copy(alpha = 0.2f)
                                device.isCurrentDevice -> CyberPrimary.copy(alpha = 0.2f)
                                isLanScan -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                isCloudNode -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                !device.hasAegisGuard -> Color(0xFF10B981).copy(alpha = 0.2f)
                                else -> Color(0xFF64748B).copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when {
                            device.isSignedOutRemotely -> "SIGNED OUT"
                            device.isLockedRemotely -> "LOCKED"
                            device.isCurrentDevice -> "THIS HARDWARE"
                            isLanScan -> "LAN SCAN"
                            isCloudNode -> "CLOUD SESSION"
                            !device.hasAegisGuard -> "GMAIL SESSION"
                            else -> "TOKEN PAIRED"
                        },
                        color = when {
                            device.isSignedOutRemotely -> CyberCritical
                            device.isLockedRemotely -> CyberCritical
                            device.isCurrentDevice -> CyberPrimary
                            isLanScan -> Color(0xFFFBBF24)
                            isCloudNode -> Color(0xFF93C5FD)
                            !device.hasAegisGuard -> Color(0xFF10B981)
                            else -> Color(0xFF94A3B8)
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Per-device Location Box (Clickable on EVERY card to name device & set neighborhood/city)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.55f))
                    .clickable { onEditDevice() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("edit_device_location_box_${device.deviceId}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (device.isCurrentDevice) CyberPrimary else Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.locationAddress,
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (device.latitude == 0.0 && device.longitude == 0.0) {
                            "${device.locationSource} • Tap to name & set area"
                        } else {
                            "%.4f, %.4f (±%.1fm) • ${device.locationSource}".format(Locale.US, device.latitude, device.longitude, device.accuracyMeters)
                        },
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val latestFix = sessionPoints.firstOrNull()
                    if (latestFix != null) {
                        val fixFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
                        Text(
                            text = "Tracker: ${sessionPoints.size} fix${if (sessionPoints.size == 1) "" else "es"} • ${latestFix.provider} • ${fixFmt.format(Date(latestFix.capturedAtMillis))}",
                            color = Color(0xFF6EE7B7),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberPrimary.copy(alpha = 0.14f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename or Set Area",
                        tint = CyberPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Edit",
                        color = CyberPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry: Battery, IP, and Last Seen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (device.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = if (device.batteryLevelPercent > 20) Color(0xFF10B981) else CyberCritical,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${device.batteryLevelPercent}%",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "IP: ${device.ipAddress}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val format = SimpleDateFormat("HH:mm", Locale.US)
                Text(
                    text = if (device.isCurrentDevice) "Live GPS" else "Active ${format.format(Date(device.lastSeenTimestamp))}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Balanced widths, proper padding, never clipped
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Map + Sound Alarm (Ring)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenMap,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .testTag("open_map_btn_${device.deviceId}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberPrimary.copy(alpha = 0.16f),
                            contentColor = CyberPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Map", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = onRing,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .testTag("ring_device_btn_${device.deviceId}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberSecondary.copy(alpha = 0.16f),
                            contentColor = CyberSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sound Ring", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }

                // Row 2: Sign Out / Lock / Unlink (only if not current device)
                if (!device.isCurrentDevice) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!device.hasAegisGuard) {
                            Button(
                                onClick = onSignOutRemotely,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .testTag("signout_btn_${device.deviceId}"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (device.isSignedOutRemotely) Color(0xFF334155) else CyberCritical.copy(alpha = 0.2f),
                                    contentColor = if (device.isSignedOutRemotely) Color(0xFF94A3B8) else CyberCritical
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !device.isSignedOutRemotely
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (device.isSignedOutRemotely) "Signed Out" else "Remote Sign Out",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Button(
                                onClick = onToggleLock,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .testTag("lock_toggle_btn_${device.deviceId}"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (device.isLockedRemotely) CyberCritical else Color(0xFF1E293B),
                                    contentColor = if (device.isLockedRemotely) Color.White else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (device.isLockedRemotely) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (device.isLockedRemotely) "Unlock Node" else "Lock Node",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                                .testTag("remove_device_btn_${device.deviceId}"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Unlink", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unlink", fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenCard(
    token: AegisPairingToken,
    onCopy: () -> Unit,
    onRevoke: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = Color(0xFF1E293B)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = token.tokenId,
                    color = CyberPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Node: ${token.targetDeviceName}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }

            Row {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberPrimary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onRevoke, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = CyberCritical, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
