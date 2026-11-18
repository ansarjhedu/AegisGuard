package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThreatSeverity
import com.example.data.repository.DecryptedAuditLog
import com.example.ui.AegisUiState
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogFilter {
    ALL,
    ALERTS_ONLY,
    CRITICAL
}

@Composable
fun AuditLogsScreen(
    uiState: AegisUiState,
    auditLogs: List<DecryptedAuditLog>,
    onDispatchEmail: () -> Unit,
    onClearLogs: () -> Unit,
    onLockVault: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(LogFilter.ALL) }
    var showRawCryptoDetails by remember { mutableStateOf(false) }

    val filteredLogs = auditLogs.filter { log ->
        when (activeFilter) {
            LogFilter.ALL -> true
            LogFilter.ALERTS_ONLY -> log.isAlert
            LogFilter.CRITICAL -> log.severity == ThreatSeverity.CRITICAL
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Security Cryptography Header Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CyberPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
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
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CyberSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AES-256 SECURE AUDIT LEDGER",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "BIOMETRIC",
                                    color = CyberPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberSecondary.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "GCM 128-TAG",
                                    color = CyberSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Every entry is symmetrically encrypted at rest with a 256-bit AES master key. Access is gated behind AndroidX Biometric authentication.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDispatchEmail,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("email_report_from_logs_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberPrimary,
                                contentColor = Color(0xFF00363D)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Email Report",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onLockVault,
                            modifier = Modifier.height(38.dp).testTag("lock_logs_vault_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CyberSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lock",
                                fontSize = 11.sp,
                                color = CyberSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = { showRawCryptoDetails = !showRawCryptoDetails },
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (showRawCryptoDetails) "Hide Crypto" else "Inspect Crypto",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        IconButton(
                            onClick = onClearLogs,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Logs",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeFilter == LogFilter.ALL,
                    onClick = { activeFilter = LogFilter.ALL },
                    label = { Text("All Logs (${auditLogs.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CyberPrimary,
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color(0xFF94A3B8)
                    )
                )
                FilterChip(
                    selected = activeFilter == LogFilter.ALERTS_ONLY,
                    onClick = { activeFilter = LogFilter.ALERTS_ONLY },
                    label = { Text("Alerts (${auditLogs.count { it.isAlert }})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberWarning.copy(alpha = 0.2f),
                        selectedLabelColor = CyberWarning,
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color(0xFF94A3B8)
                    )
                )
                FilterChip(
                    selected = activeFilter == LogFilter.CRITICAL,
                    onClick = { activeFilter = LogFilter.CRITICAL },
                    label = { Text("Critical (${auditLogs.count { it.severity == ThreatSeverity.CRITICAL }})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCritical.copy(alpha = 0.2f),
                        selectedLabelColor = CyberCritical,
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color(0xFF94A3B8)
                    )
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No log records found for current filter",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                AuditLogItemCard(
                    log = log,
                    showRawCrypto = showRawCryptoDetails
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AuditLogItemCard(
    log: DecryptedAuditLog,
    showRawCrypto: Boolean
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(log.timestamp))

    val severityColor = when (log.severity) {
        ThreatSeverity.CRITICAL -> CyberCritical
        ThreatSeverity.HIGH -> CyberWarning
        ThreatSeverity.MEDIUM -> Color(0xFF38BDF8)
        ThreatSeverity.LOW -> CyberSecondary
        ThreatSeverity.INFO -> Color(0xFF64748B)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(severityColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.severity.name,
                            color = severityColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.eventType,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = formattedTime,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = log.description,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (log.metadata.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.metadata,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (showRawCrypto) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF070B14),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = "AES-256 Vault Status: Decrypted verified via GCM-128 tag",
                            color = CyberSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Storage: aegis_guard_encrypted.db (Record #${log.id})",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
