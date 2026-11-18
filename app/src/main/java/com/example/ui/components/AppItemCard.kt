package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSecurityInfo
import com.example.data.model.ThreatSeverity
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppItemCard(
    app: AppSecurityInfo,
    onQuarantineClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onCardClick: () -> Unit,
    onWhitelistToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        app.isQuarantined -> Color(0xFF64748B)
        app.isWhitelisted -> Color(0xFF059669) // Emerald green border for whitelisted
        app.riskLevel == ThreatSeverity.CRITICAL -> CyberCritical
        app.riskLevel == ThreatSeverity.HIGH -> CyberWarning
        app.isHiddenApp -> Color(0xFFFF7043)
        else -> Color(0xFF1E293B)
    }

    val riskBgColor = when {
        app.isWhitelisted -> Color(0xFF10B981)
        app.riskLevel == ThreatSeverity.CRITICAL -> CyberCritical
        app.riskLevel == ThreatSeverity.HIGH -> CyberWarning
        app.riskLevel == ThreatSeverity.MEDIUM -> Color(0xFF38BDF8)
        app.riskLevel == ThreatSeverity.LOW -> CyberSecondary
        else -> Color(0xFF64748B)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCardClick() }
                .padding(14.dp)
                .testTag("app_card_${app.packageName}")
        ) {
            // Header Row: App icon, Title, Risk Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (app.isWhitelisted) Color(0xFF064E3B)
                            else if (app.isHiddenApp) Color(0xFF3B1E1E)
                            else Color(0xFF1E293B)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (app.isWhitelisted) Icons.Default.CheckCircle
                        else if (app.isHiddenApp) Icons.Default.VisibilityOff
                        else Icons.Default.Android,
                        contentDescription = null,
                        tint = if (app.isWhitelisted) Color(0xFF10B981)
                        else if (app.isHiddenApp) CyberCritical
                        else CyberPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = app.packageName,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Risk / Whitelist Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(riskBgColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (app.isQuarantined) "QUARANTINED"
                        else if (app.isWhitelisted) "WHITELISTED"
                        else "${app.riskScore}% RISK",
                        color = if (app.isQuarantined) Color(0xFFCBD5E1) else riskBgColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Flags & Tags
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (app.isWhitelisted) {
                    TagChip(text = "WHITELISTED", color = Color(0xFF10B981))
                }
                if (app.isEssentialSystemApp) {
                    TagChip(text = "PROTECTED OS CORE", color = CyberPrimary)
                }
                if (app.isGooglePlayVerified) {
                    TagChip(text = "PLAY STORE VERIFIED", color = Color(0xFF10B981))
                }
                TagChip(text = app.installerLabel, color = Color(0xFF94A3B8))

                if (app.isHiddenApp && !app.isWhitelisted) {
                    TagChip(text = "HIDDEN APP (NO ICON)", color = CyberCritical)
                }
                if (app.dangerousPermissions.any { it.contains("SMS") }) {
                    TagChip(text = "SMS ACCESS", color = if (app.isWhitelisted) Color(0xFF64748B) else CyberWarning)
                }
                if (app.dangerousPermissions.any { it.contains("ACCESSIBILITY") }) {
                    TagChip(text = "ACCESSIBILITY", color = if (app.isWhitelisted) Color(0xFF64748B) else CyberCritical)
                }
                if (app.dangerousPermissions.any { it.contains("RECORD_AUDIO") }) {
                    TagChip(text = "MIC ACCESS", color = if (app.isWhitelisted) Color(0xFF64748B) else CyberWarning)
                }
            }

            // Suspicious Flags Summary / Whitelist Note
            if (app.isWhitelisted) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = app.whitelistReason ?: "Auto-whitelisted (Zero false positives in risk report)",
                        color = Color(0xFF6EE7B7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            } else if (app.suspiciousFlags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• " + app.suspiciousFlags.first(),
                    color = Color(0xFFFFB703),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            // Action Buttons
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (app.canBeQuarantined) {
                    Button(
                        onClick = onQuarantineClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("quarantine_btn_${app.packageName}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (app.isQuarantined) Color(0xFF334155) else Color(0xFF991B1B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (app.isQuarantined) "Release" else "Quarantine",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1E293B).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Protected OS",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Whitelist Toggle Button
                OutlinedButton(
                    onClick = onWhitelistToggle,
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("whitelist_btn_${app.packageName}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (app.isWhitelisted) Color(0xFF10B981) else Color(0xFF94A3B8)
                    )
                ) {
                    Icon(
                        imageVector = if (app.isWhitelisted) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (app.isWhitelisted) Color(0xFF10B981) else Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (app.isWhitelisted) "Whitelisted" else "Whitelist",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (app.canBeQuarantined) {
                    OutlinedButton(
                        onClick = onUninstallClick,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("uninstall_btn_${app.packageName}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Uninstall",
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
