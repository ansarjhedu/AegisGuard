package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AegisViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.BiometricLockScreen
import com.example.ui.components.EmailDispatchDialog
import com.example.ui.screens.CamouflageFullscreenScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceLocationScreen
import com.example.ui.screens.GoogleSignInProfileScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.CyberDarkBackground
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberLightBackground
import androidx.core.app.ActivityCompat
import com.example.ui.theme.CyberLightSurface
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private val viewModel: AegisViewModel by viewModels()

    private var hasRequestedStartupPermissions = false
    private var isAwaitingPermissionResult = false
    private var isBiometricPromptActive = false
    private var hasAutoPromptedBiometricForLockCycle = false
    private var isStartupFlowScheduled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle Quick Settings Tile emergency actions (e.g. Anti-Theft Lock, Decoy Camouflage)
        handleIncomingIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
            val context = LocalContext.current

            // Toast feedback
            LaunchedEffect(uiState.toastMessage) {
                uiState.toastMessage?.let { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearToast()
                }
            }

            MyApplicationTheme(darkTheme = uiState.isDarkMode) {
                if (uiState.showCamouflageFullscreen) {
                    CamouflageFullscreenScreen(
                        uiState = uiState,
                        onExit = { viewModel.setCamouflageFullscreen(false) }
                    )
                } else if (uiState.isBiometricEnforced && !uiState.isSessionUnlocked) {
                    BiometricLockScreen(
                        biometricAvailability = uiState.biometricAvailability,
                        authErrorMessage = uiState.authErrorMessage,
                        onAuthenticateClick = { launchBiometricPrompt(manualTrigger = true) },
                        onFallbackPasskeyClick = { viewModel.unlockVault() }
                    )
                } else {
                    AegisMainScaffold(
                        viewModel = viewModel,
                        uiState = uiState,
                        onLockVault = { viewModel.lockVault() }
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            scheduleDeferredStartupFlow()
        }
    }

    private fun scheduleDeferredStartupFlow() {
        if (isStartupFlowScheduled || isFinishing || isDestroyed || isAwaitingPermissionResult) return
        isStartupFlowScheduled = true
        window.decorView.post {
            isStartupFlowScheduled = false
            if (isFinishing || isDestroyed || !hasWindowFocus() || isAwaitingPermissionResult) return@post

            if (!hasRequestedStartupPermissions && ungrantedRuntimePermissions().isNotEmpty()) {
                hasRequestedStartupPermissions = true
                requestAppPermissions()
                return@post
            }

            maybeAutoLaunchBiometricPrompt()
        }
    }

    private fun ungrantedRuntimePermissions(): List<String> {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestAppPermissions() {
        try {
            val ungranted = ungrantedRuntimePermissions()
            if (ungranted.isNotEmpty()) {
                isAwaitingPermissionResult = true
                // Use standard lower 16-bit requestCode (1001) which is valid for FragmentActivity
                ActivityCompat.requestPermissions(this, ungranted.toTypedArray(), 1001)
            }
        } catch (e: Exception) {
            isAwaitingPermissionResult = false
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        isAwaitingPermissionResult = false
        scheduleDeferredStartupFlow()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val intruderAlert = intent.getStringExtra("EXTRA_INTRUDER_ALERT")
        if (intruderAlert != null) {
            viewModel.setAuthErrorMessage(intruderAlert)
        }
        if (intent.getBooleanExtra("EXTRA_FORCE_LOCK", false)) {
            viewModel.lockVault()
        }
        if (intent.getBooleanExtra("EXTRA_CAMOUFLAGE", false)) {
            viewModel.setCamouflageFullscreen(true)
        }
    }

    override fun onStop() {
        super.onStop()
        // Avoid locking/resetting during configuration changes, system permission sheets, or device credential prompts
        if (!isChangingConfigurations && !isAwaitingPermissionResult && !isBiometricPromptActive) {
            hasAutoPromptedBiometricForLockCycle = false
            viewModel.disconnectSessionAndLock()
        }
    }

    private fun maybeAutoLaunchBiometricPrompt() {
        if (hasAutoPromptedBiometricForLockCycle || isBiometricPromptActive) return
        val state = viewModel.uiState.value
        if (state.isBiometricEnforced &&
            !state.isSessionUnlocked &&
            state.biometricAvailability == com.example.auth.BiometricAvailability.AVAILABLE
        ) {
            hasAutoPromptedBiometricForLockCycle = true
            launchBiometricPrompt(manualTrigger = false)
        }
    }

    private fun launchBiometricPrompt(manualTrigger: Boolean = false) {
        if (isFinishing || isDestroyed) return
        if (isBiometricPromptActive && !manualTrigger) return
        isBiometricPromptActive = true
        viewModel.biometricAuthManager.promptBiometric(
            activity = this,
            title = "AegisGuard Zero-Trust Authentication",
            subtitle = "Verify identity to access secure dashboard",
            description = "Scan fingerprint or enter device PIN/Pattern",
            onSuccess = {
                isBiometricPromptActive = false
                viewModel.unlockVault()
            },
            onError = { errorMsg ->
                isBiometricPromptActive = false
                viewModel.setAuthErrorMessage(errorMsg)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AegisMainScaffold(
    viewModel: AegisViewModel,
    uiState: com.example.ui.AegisUiState,
    onLockVault: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (uiState.isDarkMode) CyberDarkBackground else CyberLightBackground,
        topBar = {
            Surface(
                color = if (uiState.isDarkMode) CyberDarkSurface else CyberLightSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo & App Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("app_branding_header")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Aegis",
                                    color = if (uiState.isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Guard",
                                    color = CyberPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = "ZERO-TRUST MOBILE DEFENSE",
                                color = if (uiState.isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    // Actions: Lock Vault, Zero-Trust score pill & Dark Mode Switch
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onLockVault,
                            modifier = Modifier.testTag("lock_vault_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Security Vault",
                                tint = CyberSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(CyberPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
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
                                    text = "Protected",
                                    color = CyberPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("dark_mode_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark Mode",
                                tint = if (uiState.isDarkMode) Color(0xFFFFB703) else Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = if (uiState.isDarkMode) CyberDarkSurface else CyberLightSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == ScreenTab.DASHBOARD,
                    onClick = { viewModel.selectTab(ScreenTab.DASHBOARD) },
                    icon = {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = "Guard Icons")
                    },
                    label = { Text("Guard Icons", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimary,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary.copy(alpha = 0.2f),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == ScreenTab.SCANNER,
                    onClick = { viewModel.selectTab(ScreenTab.SCANNER) },
                    icon = {
                        Icon(imageVector = Icons.Default.Security, contentDescription = "App Security")
                    },
                    label = { Text("App Security", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimary,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary.copy(alpha = 0.2f),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_scanner")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == ScreenTab.DEVICES_LOCATION,
                    onClick = { viewModel.selectTab(ScreenTab.DEVICES_LOCATION) },
                    icon = {
                        Icon(imageVector = Icons.Default.GpsFixed, contentDescription = "Device Locations")
                    },
                    label = { Text("Locations", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimary,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary.copy(alpha = 0.2f),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_devices_location")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == ScreenTab.GOOGLE_AUTH,
                    onClick = { viewModel.selectTab(ScreenTab.GOOGLE_AUTH) },
                    icon = {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Google Identity")
                    },
                    label = { Text("Google Auth", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimary,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary.copy(alpha = 0.2f),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_google_auth")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                ScreenTab.DASHBOARD -> {
                    DashboardScreen(
                        uiState = uiState,
                        onNavigate = { viewModel.selectTab(it) },
                        onToggleShield = { viewModel.toggleRealtimeShield() },
                        onRunScan = {
                            viewModel.selectTab(ScreenTab.SCANNER)
                            viewModel.runAppScan()
                        },
                        onDispatchEmailClicked = { viewModel.showEmailDialog(true) },
                        onDismissTamper = { viewModel.dismissTamperAlert() },
                        onToggleBiometric = { viewModel.toggleBiometricEnforcement() },
                        onLockNow = { viewModel.lockVault() },
                        onToggleDecoyIcon = { viewModel.toggleDecoyIcon(it) },
                        onToggleDecoyReplaceOriginal = { viewModel.toggleDecoyReplaceOriginal(it) },
                        onUpdateDecoyWifi = { viewModel.updateDecoyWifi(it) },
                        onTriggerDecoyTrap = { type ->
                            viewModel.lockVault()
                            viewModel.setAuthErrorMessage("ANTI-THEFT TRAP: Unauthorized attempt to manipulate ${type.displayName} blocked! Real system network remains connected in stealth mode. Device locked with biometric security.")
                        }
                    )
                }

                ScreenTab.SCANNER -> {
                    ScannerScreen(
                        uiState = uiState,
                        onFilterSelected = { viewModel.setAppFilter(it) },
                        onSearchChanged = { viewModel.setSearchQuery(it) },
                        onRunScan = { viewModel.runAppScan() },
                        onQuarantineToggle = { app ->
                            if (app.isQuarantined) viewModel.releaseApp(app.packageName)
                            else viewModel.quarantineApp(app)
                        },
                        onSelectApp = { viewModel.selectAppForDetail(it) },
                        onWhitelistToggle = { viewModel.toggleAppWhitelist(it) },
                        onAutoWhitelistSystemToggle = { viewModel.toggleAutoWhitelistSystem(it) },
                        onAutoWhitelistPlayStoreToggle = { viewModel.toggleAutoWhitelistPlayStore(it) }
                    )
                }

                ScreenTab.DEVICES_LOCATION -> {
                    val trackedDevices by viewModel.trackedDevices.collectAsStateWithLifecycle()
                    val locationHistory by viewModel.locationHistory.collectAsStateWithLifecycle()
                    val pairingTokens by viewModel.pairingTokens.collectAsStateWithLifecycle()
                    val trackingMode by viewModel.trackingMode.collectAsStateWithLifecycle()
                    val firebaseSyncState by viewModel.firebaseSyncState.collectAsStateWithLifecycle()
                    val sessionLocationPoints by viewModel.sessionLocationPoints.collectAsStateWithLifecycle()
                    val isTrackerActive by viewModel.isLocationTrackerActive.collectAsStateWithLifecycle()
                    val lastTrackerFixSummary by viewModel.lastTrackerFixSummary.collectAsStateWithLifecycle()

                    DeviceLocationScreen(
                        userEmail = uiState.emailRecipient,
                        trackingMode = trackingMode,
                        devices = trackedDevices,
                        locationHistory = locationHistory,
                        pairingTokens = pairingTokens,
                        onSelectMode = { viewModel.setTrackingMode(it) },
                        onRefreshLocations = { viewModel.refreshDeviceLocations() },
                        onAddGmailDevice = { name, plat, client, loc -> viewModel.addGmailDevice(name, plat, client, loc) },
                        onUpdateDeviceIdentityAndLocation = { id, name, plat, client, loc ->
                            viewModel.updateDeviceIdentityAndLocation(id, name, plat, client, loc)
                        },
                        onClearNetworkDevices = { viewModel.clearNetworkDiscoveredDevices() },
                        onClearLocationHistory = { viewModel.clearLocationHistory() },
                        onUpdateUserEmail = { newEmail -> viewModel.updateUserEmail(newEmail) },
                        onSignOutDeviceRemotely = { devId -> viewModel.signOutDeviceRemotely(devId) },
                        onOpenGoogleAccountSecurity = { ctx -> viewModel.openGoogleAccountSecurity(ctx) },
                        onGenerateToken = { viewModel.generatePairingToken(it) },
                        onPairWithToken = { name, token -> viewModel.pairDeviceWithToken(name, token) },
                        onRemoveDevice = { viewModel.removeTrackedDevice(it) },
                        onRevokeToken = { viewModel.revokePairingToken(it) },
                        onToggleLockdown = { viewModel.toggleRemoteDeviceLockdown(it) },
                        onRingDevice = { viewModel.ringDevice(it) },
                        onOpenMaps = { ctx, lat, lng, lbl -> viewModel.openDeviceInGoogleMaps(ctx, lat, lng, lbl) },
                        onDispatchFleetReport = { ctx -> viewModel.dispatchFleetReportEmail(ctx) },
                        isGoogleAuthenticated = uiState.isGoogleAuthenticated,
                        authenticatedUserName = uiState.authenticatedUserName,
                        authenticatedPhotoUrl = uiState.authenticatedUserPhotoUrl,
                        isGoogleSignInLoading = uiState.isGoogleSignInLoading,
                        firebaseSyncState = firebaseSyncState,
                        onSignInWithGoogle = { ctx -> viewModel.signInWithGoogle(ctx) },
                        onSignOutGoogle = { viewModel.signOutGoogle() },
                        onLinkManualAccount = { email, name -> viewModel.linkAccountWithEmail(email, name) },
                        onDiscoverNetworkDevices = { viewModel.discoverNetworkDevices() },
                        onSetLocationOverride = { city -> viewModel.setVerifiedLocationOverride(city) },
                        onResetLocationToAuto = { viewModel.resetLocationToAuto() },
                        sessionLocationPoints = sessionLocationPoints,
                        currentInstallSessionId = viewModel.firebaseSessionManager.getInstallSessionId(),
                        isTrackerActive = isTrackerActive,
                        lastTrackerFixSummary = lastTrackerFixSummary,
                        onCaptureSessionFixes = { viewModel.captureSessionLocationFixes() }
                    )
                }

                ScreenTab.GOOGLE_AUTH -> {
                    val trackedDevices by viewModel.trackedDevices.collectAsStateWithLifecycle()
                    val firebaseSyncState by viewModel.firebaseSyncState.collectAsStateWithLifecycle()

                    GoogleSignInProfileScreen(
                        uiState = uiState,
                        peopleProfile = uiState.googlePeopleProfile,
                        deviceSessions = trackedDevices,
                        firebaseSyncState = firebaseSyncState,
                        onSignInWithGoogle = { ctx -> viewModel.signInWithGoogle(ctx) },
                        onSignOutGoogle = { viewModel.signOutGoogle() },
                        onLinkManualAccount = { email, name -> viewModel.linkAccountWithEmail(email, name) },
                        onRefreshProfileAndSessions = { viewModel.refreshDeviceLocations() },
                        onNavigateToTab = { tab -> viewModel.selectTab(tab) },
                        onRemoteSignOutDevice = { devId -> viewModel.signOutDeviceRemotely(devId) },
                        onAddDeviceSession = { name, plat, client, loc -> viewModel.addGmailDevice(name, plat, client, loc) }
                    )
                }
            }
        }
    }

    // Email Emergency Alert Dialog
    if (uiState.showEmailDispatchDialog) {
        EmailDispatchDialog(
            currentEmail = uiState.emailRecipient,
            onEmailChanged = { viewModel.setEmailRecipient(it) },
            onDispatch = { email ->
                viewModel.setEmailRecipient(email)
                viewModel.dispatchLocationAlertEmail(context, uiState.activeTamperAlert?.tamperReason)
            },
            onDismiss = { viewModel.showEmailDialog(false) }
        )
    }

    // Google Account Sign-In Fallback / Account Setup Dialog
    if (uiState.showGoogleAccountPickerModal) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.setGoogleAccountPickerModalVisible(false) },
            title = {
                Text(
                    text = "Google Account Sign-In",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "No Google account credential was returned by Android Credential Manager. You can add a Google account in device settings, retry Google Sign-In, or link your Gmail session directly.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            viewModel.setGoogleAccountPickerModalVisible(false)
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
                                    putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Open Android Settings > Accounts to add Google", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Google Account in Settings", fontSize = 12.sp, color = CyberPrimary)
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            viewModel.linkAccountWithEmail(uiState.emailRecipient, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Link ${uiState.emailRecipient} Directly", fontSize = 12.sp, color = Color(0xFF93C5FD))
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.setGoogleAccountPickerModalVisible(false)
                        viewModel.signInWithGoogle(context)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                ) {
                    Text("Retry Google Sign-In", fontWeight = FontWeight.Bold, color = Color(0xFF00363D))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.setGoogleAccountPickerModalVisible(false) }
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
