package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ZeroTrustPosture
import com.example.data.model.ZeroTrustStatus
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberWarning

@Composable
fun ThreatRadarCard(
    posture: ZeroTrustPosture?,
    isShieldActive: Boolean,
    onToggleShield: () -> Unit,
    onRunScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val score = posture?.score ?: 95

    val statusColor = when (posture?.status) {
        ZeroTrustStatus.SECURE_VERIFIED -> CyberSecondary
        ZeroTrustStatus.WARNING_DEGRADED -> CyberWarning
        ZeroTrustStatus.HIGH_RISK_COMPROMISED -> CyberCritical
        null -> CyberPrimary
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(statusColor.copy(alpha = 0.5f), CyberPrimary.copy(alpha = 0.2f))
                ),
                RoundedCornerShape(20.dp)
            )
            .testTag("threat_radar_card"),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Status & Realtime Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isShieldActive) CyberSecondary else CyberWarning)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isShieldActive) "REAL-TIME SHIELD ON" else "SHIELD PAUSED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isShieldActive) CyberSecondary else CyberWarning
                    )
                }

                Switch(
                    checked = isShieldActive,
                    onCheckedChange = { onToggleShield() },
                    modifier = Modifier.testTag("realtime_shield_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyberSecondary,
                        uncheckedTrackColor = Color(0xFF334155)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Radar Circle + Score Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radar Graphic
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        // Background concentric circles
                        drawCircle(
                            color = statusColor.copy(alpha = 0.15f),
                            radius = (size.width / 2) * pulseScale,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFF1E293B),
                            radius = size.width / 2.3f,
                            center = center,
                            style = Stroke(width = 6.dp.toPx())
                        )
                        // Active Arc
                        drawArc(
                            color = statusColor,
                            startAngle = -90f,
                            sweepAngle = (score * 3.6f),
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx())
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "SECURITY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Posture Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zero-Trust Architecture",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (posture?.status) {
                            ZeroTrustStatus.SECURE_VERIFIED -> "All microservices verified. AES-256 vault intact, no root intrusions."
                            ZeroTrustStatus.WARNING_DEGRADED -> "Developer mode or insecure route identified."
                            ZeroTrustStatus.HIGH_RISK_COMPROMISED -> "High risk! Potential tampering or root elevation."
                            null -> "Zero-trust policy active."
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecurityBadge(text = "AES-256", icon = Icons.Default.Lock)
                        SecurityBadge(text = "TLS 1.3", icon = Icons.Default.Shield)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = { onRunScan() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("radar_quick_scan_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberPrimary,
                    contentColor = Color(0xFF00363D)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RUN DEEP MALWARE SCAN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SecurityBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyberPrimary,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = Color(0xFFE2E8F0),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
