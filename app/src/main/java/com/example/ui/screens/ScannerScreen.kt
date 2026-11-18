package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSecurityInfo
import com.example.data.model.ThreatSeverity
import com.example.ui.AegisUiState
import com.example.ui.AppFilter
import com.example.ui.components.AppDetailDialog
import com.example.ui.components.AppItemCard
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberWarning

@Composable
fun ScannerScreen(
    uiState: AegisUiState,
    onFilterSelected: (AppFilter) -> Unit,
    onSearchChanged: (String) -> Unit,
    onRunScan: () -> Unit,
    onQuarantineToggle: (AppSecurityInfo) -> Unit,
    onSelectApp: (AppSecurityInfo?) -> Unit,
    onWhitelistToggle: (AppSecurityInfo) -> Unit = {},
    onAutoWhitelistSystemToggle: (Boolean) -> Unit = {},
    onAutoWhitelistPlayStoreToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Third-party applications strictly evaluated; system apps are NEVER considered threats
    val thirdPartyApps = uiState.installedApps.filter { !it.isSystemApp && !it.isEssentialSystemApp }
    val thirdPartyThreatsCount = thirdPartyApps.count {
        !it.isWhitelisted && (it.riskLevel == ThreatSeverity.CRITICAL || it.riskLevel == ThreatSeverity.HIGH)
    }

    val filteredApps = uiState.installedApps.filter { app ->
        val matchesSearch = uiState.searchQuery.isEmpty() ||
                app.appName.contains(uiState.searchQuery, ignoreCase = true) ||
                app.packageName.contains(uiState.searchQuery, ignoreCase = true)

        val matchesFilter = when (uiState.activeFilter) {
            AppFilter.ALL -> true
            AppFilter.USER_APPS -> !app.isSystemApp && !app.isEssentialSystemApp
            AppFilter.HIGH_RISK -> !app.isWhitelisted && (app.riskLevel == ThreatSeverity.CRITICAL || app.riskLevel == ThreatSeverity.HIGH)
            AppFilter.WHITELISTED -> app.isWhitelisted
            AppFilter.QUARANTINED -> app.isQuarantined
            AppFilter.GOOGLE_PLAY -> app.isGooglePlayVerified
            AppFilter.SYSTEM_PROTECTED -> app.isEssentialSystemApp || app.isSystemApp
            AppFilter.HIDDEN_APPS -> app.isHiddenApp && !app.isWhitelisted
        }

        matchesSearch && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search & Refresh Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChanged,
                placeholder = {
                    Text(
                        text = "Search third-party app or package...",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CyberPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("app_search_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberPrimary,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onRunScan,
                modifier = Modifier
                    .height(52.dp)
                    .testTag("scan_trigger_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberPrimary,
                    contentColor = Color(0xFF00363D)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isScanning
            ) {
                if (uiState.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF00363D),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan Apps",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Scanning progress bar if active
        if (uiState.isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CyberPrimary,
                trackColor = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Clean, Minimalist Status Banner (Based strictly on third-party apps)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = if (thirdPartyThreatsCount > 0) Color(0xFF450A0A).copy(alpha = 0.4f) else Color(0xFF064E3B).copy(alpha = 0.35f),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (thirdPartyThreatsCount > 0) CyberCritical else Color(0xFF059669)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (thirdPartyThreatsCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (thirdPartyThreatsCount > 0) CyberCritical else Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (thirdPartyThreatsCount > 0) "$thirdPartyThreatsCount Third-Party Threat(s) Found" else "All Third-Party Apps Safe",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (thirdPartyThreatsCount > 0) {
                            "Review flagged permissions or quarantine suspicious apps below"
                        } else {
                            "${thirdPartyApps.size} third-party apps analyzed. System apps are verified safe OS components."
                        },
                        color = if (thirdPartyThreatsCount > 0) Color(0xFFFECACA) else Color(0xFFA7F3D0),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Minimalist Filter chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AppFilterChip(
                    text = "Third-Party Apps (${thirdPartyApps.size})",
                    selected = uiState.activeFilter == AppFilter.USER_APPS,
                    onClick = { onFilterSelected(AppFilter.USER_APPS) },
                    accentColor = CyberPrimary
                )
            }
            item {
                AppFilterChip(
                    text = "High Risk ($thirdPartyThreatsCount)",
                    selected = uiState.activeFilter == AppFilter.HIGH_RISK,
                    onClick = { onFilterSelected(AppFilter.HIGH_RISK) },
                    accentColor = CyberCritical
                )
            }
            item {
                AppFilterChip(
                    text = "All Apps (${uiState.installedApps.size})",
                    selected = uiState.activeFilter == AppFilter.ALL,
                    onClick = { onFilterSelected(AppFilter.ALL) }
                )
            }
            item {
                AppFilterChip(
                    text = "Quarantined (${uiState.installedApps.count { it.isQuarantined }})",
                    selected = uiState.activeFilter == AppFilter.QUARANTINED,
                    onClick = { onFilterSelected(AppFilter.QUARANTINED) },
                    accentColor = Color(0xFF94A3B8)
                )
            }
            item {
                AppFilterChip(
                    text = "Whitelisted (${uiState.installedApps.count { it.isWhitelisted }})",
                    selected = uiState.activeFilter == AppFilter.WHITELISTED,
                    onClick = { onFilterSelected(AppFilter.WHITELISTED) },
                    accentColor = Color(0xFF10B981)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Applications List
        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No apps match current filter",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppItemCard(
                        app = app,
                        onQuarantineClick = { onQuarantineToggle(app) },
                        onUninstallClick = {
                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.parse("package:${app.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        onCardClick = { onSelectApp(app) },
                        onWhitelistToggle = { onWhitelistToggle(app) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // App Detail Bottom Sheet / Dialog
    uiState.selectedAppForDetail?.let { app ->
        AppDetailDialog(
            app = app,
            onDismiss = { onSelectApp(null) },
            onToggleQuarantine = {
                onQuarantineToggle(app)
                onSelectApp(null)
            },
            onUninstall = {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:${app.packageName}")
                }
                context.startActivity(intent)
                onSelectApp(null)
            },
            onToggleWhitelist = {
                onWhitelistToggle(app)
                onSelectApp(null)
            }
        )
    }
}

@Composable
private fun AppFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = CyberPrimary
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor.copy(alpha = 0.2f),
            selectedLabelColor = accentColor,
            containerColor = Color(0xFF1E293B),
            labelColor = Color(0xFF94A3B8)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color(0xFF334155),
            selectedBorderColor = accentColor,
            borderWidth = 1.dp
        ),
        shape = RoundedCornerShape(8.dp)
    )
}
