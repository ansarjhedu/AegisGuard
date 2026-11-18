package com.example.auth

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    HW_UNAVAILABLE,
    UNSUPPORTED
}

class BiometricAuthManager(private val context: Context) {

    private val biometricManager: BiometricManager? by lazy {
        try {
            BiometricManager.from(context)
        } catch (e: Exception) {
            null
        }
    }

    fun checkBiometricAvailability(): BiometricAvailability {
        val manager = biometricManager ?: return BiometricAvailability.NO_HARDWARE
        return try {
            val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            }

            when (manager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HW_UNAVAILABLE
                else -> BiometricAvailability.UNSUPPORTED
            }
        } catch (e: Exception) {
            BiometricAvailability.UNSUPPORTED
        }
    }

    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "AegisGuard Zero-Trust Authentication",
        subtitle: String = "Verify identity to access secure dashboard and audit logs",
        description: String = "Scan your biometric fingerprint or use device screen lock PIN/Pattern",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        try {
            if (activity.supportFragmentManager.isStateSaved) {
                onError("System is busy preparing screen. Please tap to unlock.")
                return
            }

            val executor = ContextCompat.getMainExecutor(activity)

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Biometric authentication failed. Please try again.")
                }
            }

            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)

            var useDeviceCredentials = false
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    promptInfoBuilder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    useDeviceCredentials = true
                } else {
                    promptInfoBuilder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    useDeviceCredentials = true
                }
            } catch (e: Exception) {
                useDeviceCredentials = false
            }

            if (!useDeviceCredentials) {
                promptInfoBuilder.setNegativeButtonText("Cancel")
            }

            val promptInfo = promptInfoBuilder.build()
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Throwable) {
            onError(e.localizedMessage ?: "Biometric prompt error. Please try alternate unlock.")
        }
    }
}
