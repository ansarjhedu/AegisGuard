package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppSecurityInfo
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberWarning

@Composable
fun AppDetailDialog(
    app: AppSecurityInfo,
    onDismiss: () -> Unit,
    onToggleQuarantine: () -> Unit,
    onUninstall: () -> Unit,
    onToggleWhitelist: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("app_detail_dialog"),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = app.packageName,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Whitelist Status Card
                if (app.isWhitelisted) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF064E3B).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Whitelisted from Threat Report",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = app.whitelistReason ?: "Ignored in risk scoring to prevent false positives.",
                                    color = Color(0xFFA7F3D0),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailMetric(
                        label = "Risk Score",
                        value = if (app.isWhitelisted) "0% (Ignored)" else "${app.riskScore}%",
                        color = if (app.isWhitelisted) Color(0xFF10B981) else if (app.riskScore > 50) CyberCritical else CyberPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    DetailMetric(
                        label = "Category",
                        value = if (app.isEssentialSystemApp) "OS Protected" else if (app.isGooglePlayVerified) "Google Play" else if (app.isHiddenApp) "Hidden App" else "User App",
                        color = if (app.isEssentialSystemApp) CyberPrimary else if (app.isGooglePlayVerified) Color(0xFF10B981) else if (app.isHiddenApp) CyberCritical else Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    DetailMetric(
                        label = "Status",
                        value = if (app.isQuarantined) "Quarantined" else if (app.isWhitelisted) "Whitelisted" else if (app.isEssentialSystemApp) "Protected" else "Active",
                        color = if (app.isQuarantined) CyberWarning else Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Source Info Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (app.isEssentialSystemApp) Icons.Default.Shield else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (app.isEssentialSystemApp) CyberPrimary else Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Origin: ${app.installerLabel}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Suspicious Behavior Flags
                if (app.suspiciousFlags.isNotEmpty()) {
                    Text(
                        text = if (app.isWhitelisted) "SECURITY AUDIT NOTES" else "SECURITY THREAT FLAGS",
                        color = if (app.isWhitelisted) Color(0xFF38BDF8) else CyberCritical,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    for (flag in app.suspiciousFlags) {
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (app.isWhitelisted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (app.isWhitelisted) Color(0xFF10B981) else CyberCritical,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = flag,
                                color = if (app.isWhitelisted) Color(0xFFCBD5E1) else Color(0xFFFFD1D9),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Dangerous Permissions
                Text(
                    text = "SENSITIVE PERMISSIONS (${app.dangerousPermissions.size})",
                    color = CyberWarning,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (app.dangerousPermissions.isEmpty()) {
                    Text(
                        text = "No high-risk permissions detected",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                } else {
                    for (perm in app.dangerousPermissions) {
                        Text(
                            text = "• ${perm.substringAfterLast('.')}",
                            color = Color(0xFFFDE68A),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Whitelist Control Button
                OutlinedButton(
                    onClick = onToggleWhitelist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("dialog_whitelist_toggle_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (app.isWhitelisted) Color(0xFF10B981) else Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (app.isWhitelisted) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (app.isWhitelisted) Color(0xFF10B981) else CyberPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (app.isWhitelisted) "Remove from Whitelist" else "Add to Whitelist (Ignore Risk)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons
                if (!app.canBeQuarantined) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "This application is an essential Android operating system component. Removal or quarantine is disabled to protect device integrity.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onToggleQuarantine,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("dialog_quarantine_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (app.isQuarantined) Color(0xFF475569) else Color(0xFFB91C1C),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (app.isQuarantined) "Release App" else "Quarantine Threat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onUninstall,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("dialog_uninstall_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Uninstall App",
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
