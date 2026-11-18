package com.example.scanner

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.data.model.ZeroTrustPosture
import com.example.data.model.ZeroTrustStatus
import com.example.network.TlsClientProvider
import java.io.File

class ZeroTrustPostureEvaluator(private val context: Context) {

    fun evaluatePosture(): ZeroTrustPosture {
        val isRooted = checkRootMethod()
        val isUsbDebugging = checkUsbDebugging()
        val isMockLocation = checkMockLocation()
        val isEncrypted = checkDeviceEncryption()
        val areUnknownSources = checkUnknownSources()
        val isSelinuxEnforcing = checkSelinuxEnforcing()

        var score = 100
        if (isRooted) score -= 40
        if (isUsbDebugging) score -= 15
        if (isMockLocation) score -= 15
        if (!isEncrypted) score -= 15
        if (areUnknownSources) score -= 10
        if (!isSelinuxEnforcing) score -= 20

        score = score.coerceIn(10, 100)

        val status = when {
            score >= 80 -> ZeroTrustStatus.SECURE_VERIFIED
            score >= 50 -> ZeroTrustStatus.WARNING_DEGRADED
            else -> ZeroTrustStatus.HIGH_RISK_COMPROMISED
        }

        val microservices = TlsClientProvider.getActiveMicroservices()

        return ZeroTrustPosture(
            score = score,
            status = status,
            isRootDetected = isRooted,
            isUsbDebuggingEnabled = isUsbDebugging,
            isMockLocationActive = isMockLocation,
            isDeviceEncrypted = isEncrypted,
            isSelinuxEnforcing = isSelinuxEnforcing,
            areUnknownSourcesAllowed = areUnknownSources,
            activeMicroservices = microservices
        )
    }

    private fun checkRootMethod(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkUsbDebugging(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }

    private fun checkMockLocation(): Boolean {
        return try {
            @Suppress("DEPRECATION")
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ALLOW_MOCK_LOCATION
            ) == "1"
        } catch (e: Exception) {
            false
        }
    }

    private fun checkDeviceEncryption(): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val status = dpm?.storageEncryptionStatus
            status == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
                    status == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY ||
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q // Android 10+ enforces encryption
        } catch (e: Exception) {
            true
        }
    }

    private fun checkUnknownSources(): Boolean {
        return try {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.INSTALL_NON_MARKET_APPS, 0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }

    private fun checkSelinuxEnforcing(): Boolean {
        return try {
            val c = Class.forName("android.os.SELinux")
            val m = c.getMethod("isSELinuxEnforced")
            m.invoke(null) as Boolean
        } catch (e: Exception) {
            true
        }
    }
}
