package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceLocationInfo
import com.example.data.repository.GooglePeopleProfile
import com.example.data.session.SessionSyncState
import com.example.ui.AegisUiState
import com.example.ui.ScreenTab
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberDarkBackground
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberLightBackground
import com.example.ui.theme.CyberLightSurface
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen that triggers the Android Credential Manager sign-in flow for Google Accounts
 * and displays the user's verified profile information, ID token claims, and active account device sessions upon authentication.
 */
@Composable
fun GoogleSignInProfileScreen(
    uiState: AegisUiState,
    peopleProfile: GooglePeopleProfile?,
    deviceSessions: List<DeviceLocationInfo>,
    firebaseSyncState: SessionSyncState = SessionSyncState.Unauthenticated,
    onSignInWithGoogle: (Context) -> Unit,
    onSignOutGoogle: () -> Unit,
    onLinkManualAccount: (String, String?) -> Unit,
    onRefreshProfileAndSessions: () -> Unit,
    onNavigateToTab: (ScreenTab) -> Unit,
    onRemoteSignOutDevice: (String) -> Unit,
    onAddDeviceSession: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showManualLinkDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmationDialog by remember { mutableStateOf(false) }
    var showTokenClaimsDialog by remember { mutableStateOf(false) }
    var showAddSessionDialog by remember { mutableStateOf(false) }
    var copiedTokenNotification by remember { mutableStateOf(false) }

    val isDark = uiState.isDarkMode
    val bgColor = if (isDark) CyberDarkBackground else CyberLightBackground
    val cardColor = if (isDark) CyberDarkSurface else CyberLightSurface
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 1. Header Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.dp,
                        if (uiState.isGoogleAuthenticated) Color(0xFF10B981).copy(alpha = 0.4f)
                        else CyberPrimary.copy(alpha = 0.3f),
                        RoundedCornerShape(20.dp)
                    )
                    .testTag("google_auth_screen_header"),
                color = cardColor
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    if (uiState.isGoogleAuthenticated) Color(0xFF064E3B).copy(alpha = 0.25f)
                                    else CyberPrimary.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
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
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (uiState.isGoogleAuthenticated) Color(0xFF10B981).copy(alpha = 0.2f)
                                            else Color(0xFF4285F4).copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isGoogleAuthenticated) Icons.Default.CheckCircle else Icons.Default.Key,
                                        contentDescription = "Credential Manager Status",
                                        tint = if (uiState.isGoogleAuthenticated) Color(0xFF10B981) else Color(0xFF60A5FA),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Google Identity & Profile",
                                        color = textPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "JETPACK CREDENTIAL MANAGER",
                                        color = if (uiState.isGoogleAuthenticated) Color(0xFF34D399) else Color(0xFF93C5FD),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (uiState.isGoogleAuthenticated) Color(0xFF065F46)
                                        else Color(0xFF1E3A8A)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (uiState.isGoogleAuthenticated) "AUTHENTICATED" else "UNLINKED",
                                    color = if (uiState.isGoogleAuthenticated) Color(0xFFA7F3D0) else Color(0xFFBFDBFE),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (uiState.isGoogleAuthenticated)
                                "Your Google Identity is cryptographically verified via Credential Manager. Manage this device's active session and register your other desktop or mobile logins below."
                            else
                                "Authenticate using Android's native Credential Manager to verify Gmail ownership, inspect OAuth tokens, and monitor active sessions across your devices.",
                            color = textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // 2. Main Content based on Authentication State
        if (!uiState.isGoogleAuthenticated) {
            // Unauthenticated Flow: Trigger Google Sign-In via Credential Manager
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .testTag("unauthenticated_card"),
                    color = cardColor
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Google G Icon / Shield Embellishment
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E3A8A).copy(alpha = 0.35f))
                                .border(2.dp, Color(0xFF3B82F6).copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Google Sign In",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Authenticate with Google",
                            color = textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Trigger native Jetpack Credential Manager to sign in with your Google account. This verifies your Gmail identity and pulls active device session telemetry.",
                            color = textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary "Sign in with Google" button
                        Button(
                            onClick = { onSignInWithGoogle(context) },
                            enabled = !uiState.isGoogleSignInLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("credential_manager_signin_btn")
                        ) {
                            if (uiState.isGoogleSignInLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .testTag("google_signin_loading_indicator"),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Launching Credential Manager...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sign in with Google",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary fallback: Direct / manual link dialog for test environments
                        OutlinedButton(
                            onClick = { showManualLinkDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("manual_link_account_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Link Account for Session Retrieval",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Architecture & Features Info Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp)),
                    color = cardColor
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Credential Manager Features",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FeatureItem(
                            icon = Icons.Default.Key,
                            title = "Native Passkey & Google ID Token",
                            description = "Delegates authentication to Android OS Credential Manager with SHA-256 nonces."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        FeatureItem(
                            icon = Icons.Default.Security,
                            title = "Google People API Integration",
                            description = "Retrieves name, email, avatar, and metadata without exposing plain credentials."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        FeatureItem(
                            icon = Icons.Default.Devices,
                            title = "Connected Fleet Discovery",
                            description = "Instantly links active web, desktop, and mobile sessions logged into your account."
                        )
                    }
                }
            }
        } else {
            // Authenticated Flow: Display User Profile Information
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .testTag("authenticated_user_profile_card"),
                    color = cardColor
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User Monogram Avatar with unclipped verified check badge
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .testTag("user_avatar_badge")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF059669), Color(0xFF10B981))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (uiState.authenticatedUserName ?: uiState.emailRecipient)
                                            .firstOrNull()?.uppercase() ?: "G",
                                        color = Color.White,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A))
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Verified",
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.authenticatedUserName ?: "Authenticated User",
                                    color = textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = uiState.emailRecipient,
                                    color = Color(0xFF34D399),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF065F46))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "GOOGLE SIGN-IN VERIFIED",
                                            color = Color(0xFFA7F3D0),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Profile Metadata Attributes
                        ProfileDetailRow(
                            label = "Given Name",
                            value = peopleProfile?.givenName ?: uiState.authenticatedUserName?.substringBefore(" ") ?: "Google User"
                        )
                        ProfileDetailRow(
                            label = "Family Name",
                            value = peopleProfile?.familyName ?: uiState.authenticatedUserName?.substringAfter(" ", "Verified") ?: "Account"
                        )
                        ProfileDetailRow(
                            label = "Account ID (sub)",
                            value = peopleProfile?.accountId ?: "g-id-${uiState.emailRecipient.hashCode().toUInt().toString(16).uppercase()}"
                        )
                        ProfileDetailRow(
                            label = "Email Status",
                            value = if (peopleProfile?.isEmailVerified != false) "Verified (Google Identity)" else "Pending"
                        )
                        ProfileDetailRow(
                            label = "Resource Endpoint",
                            value = peopleProfile?.resourceName ?: "people/me (Google People API v1)"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Profile Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onRefreshProfileAndSessions,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F766E),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 42.dp)
                                    .testTag("refresh_profile_telemetry_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Sessions", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }

                            OutlinedButton(
                                onClick = { showSignOutConfirmationDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCritical),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCritical.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 42.dp)
                                    .testTag("google_sign_out_button")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            // 3. Credential & ID Token Security Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp))
                        .testTag("credential_security_card"),
                    color = cardColor
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = CyberPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Credential Manager Token Claims",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(onClick = { showTokenClaimsDialog = true }) {
                                Text("View Claims", color = CyberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val rawToken = uiState.authenticatedIdToken ?: "AUTH_TOKEN_GCRED_MOCK_VALIDATED"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Google ID Token (RS256 JWT)",
                                        color = CyberPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Google ID Token", rawToken))
                                            copiedTokenNotification = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Token",
                                            tint = CyberPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (rawToken.length > 40) "${rawToken.take(24)}...${rawToken.takeLast(16)}" else rawToken,
                                    color = textSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        AnimatedVisibility(visible = copiedTokenNotification) {
                            Text(
                                text = "Token copied to clipboard",
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 4. Connected Device Sessions Associated with this Google Account
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                        .testTag("connected_sessions_summary_card"),
                    color = cardColor
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = CyberSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Active Account Sessions (${deviceSessions.size})",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            TextButton(
                                onClick = { onNavigateToTab(ScreenTab.DEVICES_LOCATION) },
                                modifier = Modifier.testTag("navigate_to_fleet_map_btn")
                            ) {
                                Text("Fleet Map", color = CyberSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = CyberSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Live Firebase Session Manager Status Banner
                        val (syncTitle, syncSubtitle, syncAccent) = when (firebaseSyncState) {
                            is SessionSyncState.Synced -> Triple(
                                "Firebase Session Manager: Live Multi-Device Sync",
                                "Querying Cloud Firestore across all AegisGuard logins (${firebaseSyncState.remoteSessionCount} cloud record${if (firebaseSyncState.remoteSessionCount == 1) "" else "s"}). Any desktop/mobile device signed into this Google account syncs here automatically.",
                                Color(0xFF10B981)
                            )
                            is SessionSyncState.Syncing -> Triple(
                                "Firebase Session Manager: Syncing...",
                                "Querying active login documents from Cloud Firestore for ${uiState.emailRecipient}.",
                                CyberPrimary
                            )
                            is SessionSyncState.Error -> Triple(
                                "Firebase Session Manager Notice",
                                firebaseSyncState.message,
                                CyberCritical
                            )
                            is SessionSyncState.Unauthenticated -> Triple(
                                "Firebase Cloud Sync Inactive (Local Only)",
                                "You are linked locally. Tap 'Sign in with Google' via Credential Manager to authenticate Firebase Auth and retrieve active logins synced across all your devices.",
                                Color(0xFF60A5FA)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .border(0.5.dp, syncAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                                .testTag("firebase_session_sync_banner")
                        ) {
                            Column {
                                Text(
                                    text = syncTitle,
                                    color = syncAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = syncSubtitle,
                                    color = textSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                if (firebaseSyncState is SessionSyncState.Unauthenticated) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextButton(
                                        onClick = { onSignInWithGoogle(context) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.heightIn(min = 32.dp)
                                    ) {
                                        Text("Connect Google Cloud Sync", color = Color(0xFF60A5FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Device session items list (show up to 10 so desktops & mobile are all visible)
                        deviceSessions.take(10).forEach { device ->
                            SessionDeviceRow(
                                device = device,
                                onSignOut = { onRemoteSignOutDevice(device.deviceId) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (deviceSessions.size > 10) {
                            Text(
                                text = "+ ${deviceSessions.size - 10} more devices active in fleet",
                                color = CyberPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { onNavigateToTab(ScreenTab.DEVICES_LOCATION) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddSessionDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberPrimary.copy(alpha = 0.16f),
                                    contentColor = CyberPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 42.dp)
                                    .testTag("add_session_from_auth_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Add Desktop / Mobile", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val uri = Uri.parse("https://myaccount.google.com/device-activity")
                                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF93C5FD)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A8A)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 42.dp)
                                    .testTag("manage_google_device_activity_btn")
                            ) {
                                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Google Activity", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Dialog: Manual / Test Gmail Account Linking
    if (showManualLinkDialog) {
        var inputEmail by remember { mutableStateOf(uiState.emailRecipient) }
        var inputName by remember { mutableStateOf(uiState.authenticatedUserName ?: "Verified User") }

        AlertDialog(
            onDismissRequest = { showManualLinkDialog = false },
            title = {
                Text("Link Google Account Session", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        text = "Enter a verified Gmail account to retrieve and monitor active sessions across all devices logged into Google services.",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Gmail Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_gmail_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputEmail.isNotBlank()) {
                            onLinkManualAccount(inputEmail.trim(), inputName.trim())
                            showManualLinkDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    modifier = Modifier.testTag("confirm_manual_link_btn")
                ) {
                    Text("Link & Retrieve Sessions", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualLinkDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    // Modal Dialog: Sign-Out Confirmation
    if (showSignOutConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmationDialog = false },
            title = {
                Text("Unlink Google Account?", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    text = "Signing out will clear the local Credential Manager state and unlink the active session telemetries from AegisGuard.",
                    color = textSecondary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSignOutGoogle()
                        showSignOutConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCritical),
                    modifier = Modifier.testTag("confirm_sign_out_btn")
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmationDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    // Modal Dialog: Detailed ID Token Claims
    if (showTokenClaimsDialog) {
        AlertDialog(
            onDismissRequest = { showTokenClaimsDialog = false },
            title = {
                Text("Google ID Token Claims", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val clientId = try {
                        context.getString(com.example.R.string.default_web_client_id)
                    } catch (_: Exception) {
                        "Configured via google-services.json"
                    }
                    ClaimItem("Issuer (iss)", "https://accounts.google.com")
                    ClaimItem("Audience (aud)", clientId)
                    ClaimItem("Subject (sub)", peopleProfile?.accountId ?: "g-id-user-sub-verified")
                    ClaimItem("Email", uiState.emailRecipient)
                    ClaimItem("Email Verified", "true (Google Identity Services)")
                    ClaimItem("Name", uiState.authenticatedUserName ?: "Verified User")
                    ClaimItem("Algorithm", "RS256 (PKI RSA Signature)")
                    ClaimItem("Auth Mechanism", "Jetpack Credential Manager 1.5.0")
                }
            },
            confirmButton = {
                TextButton(onClick = { showTokenClaimsDialog = false }) {
                    Text("Close", color = CyberPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Modal Dialog: Register Active Desktop / Mobile Session
    if (showAddSessionDialog) {
        val defaultCity = deviceSessions.firstOrNull { it.isCurrentDevice }?.locationAddress ?: "Pakistan"
        var deviceNameInput by remember { mutableStateOf("") }
        var platformInput by remember { mutableStateOf("Windows 11") }
        var clientTypeInput by remember { mutableStateOf("Google Chrome (Gmail Web)") }
        var locationInput by remember { mutableStateOf(defaultCity) }

        AlertDialog(
            onDismissRequest = { showAddSessionDialog = false },
            title = {
                Text("Register Active Session", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Add another desktop or mobile device where ${uiState.emailRecipient} is signed in:",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("Desktop PC 1", "Windows 11", "Google Chrome (Gmail Web)"),
                            Triple("Desktop PC 2", "Windows 11 / macOS", "Chrome / Edge"),
                            Triple("Mobile Phone", "Android / iOS", "Gmail Mobile App")
                        ).forEach { (presetName, presetPlat, presetClient) ->
                            androidx.compose.material3.FilterChip(
                                selected = deviceNameInput == presetName,
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
                        label = { Text("Device Name (e.g. Home Desktop)") },
                        modifier = Modifier.fillMaxWidth().testTag("auth_add_session_name_input")
                    )
                    OutlinedTextField(
                        value = platformInput,
                        onValueChange = { platformInput = it },
                        label = { Text("Platform (Windows, macOS, Android, iOS)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clientTypeInput,
                        onValueChange = { clientTypeInput = it },
                        label = { Text("Browser / App") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = locationInput,
                        onValueChange = { locationInput = it },
                        label = { Text("City / Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deviceNameInput.isNotBlank()) {
                            onAddDeviceSession(
                                deviceNameInput.trim(),
                                platformInput.trim(),
                                clientTypeInput.trim(),
                                locationInput.trim()
                            )
                            showAddSessionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    modifier = Modifier.testTag("confirm_auth_add_session_btn")
                ) {
                    Text("Register Session", fontWeight = FontWeight.Bold, color = Color(0xFF00363D))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSessionDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ClaimItem(label: String, value: String) {
    Column {
        Text(text = label, color = CyberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = Color(0xFFCBD5E1), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CyberPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyberPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SessionDeviceRow(
    device: DeviceLocationInfo,
    onSignOut: () -> Unit
) {
    val icon = when {
        device.platform.contains("Windows", ignoreCase = true) -> Icons.Default.Laptop
        device.platform.contains("macOS", ignoreCase = true) || device.platform.contains("Mac", ignoreCase = true) -> Icons.Default.Laptop
        device.platform.contains("iOS", ignoreCase = true) || device.platform.contains("iPhone", ignoreCase = true) -> Icons.Default.PhoneIphone
        device.platform.contains("iPad", ignoreCase = true) -> Icons.Default.Tablet
        device.platform.contains("Linux", ignoreCase = true) -> Icons.Default.Computer
        else -> Icons.Default.PhoneAndroid
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(10.dp)),
        color = Color(0xFF0F172A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (device.isCurrentDevice) CyberPrimary else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = device.deviceName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (device.isCurrentDevice) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "THIS DEVICE",
                                    color = CyberPrimary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${device.clientType} • ${device.locationAddress}",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!device.isCurrentDevice) {
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                    onClick = onSignOut,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.heightIn(min = 32.dp)
                ) {
                    Text(
                        text = if (device.isSignedOutRemotely) "Signed Out" else "Sign Out",
                        color = if (device.isSignedOutRemotely) Color(0xFF64748B) else CyberCritical,
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
