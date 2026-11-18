package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import kotlin.coroutines.resume

data class GoogleSignInResult(
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val idToken: String?,
    val givenName: String? = null,
    val familyName: String? = null
)

sealed interface GoogleAuthOutcome {
    data class Success(val user: GoogleSignInResult) : GoogleAuthOutcome
    data class Cancelled(val message: String = "Google Sign-In was cancelled") : GoogleAuthOutcome
    data class Error(val message: String, val throwable: Throwable? = null) : GoogleAuthOutcome
}

class GoogleAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleAuthManager"
    }

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun getWebClientId(): String? {
        return try {
            context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "default_web_client_id missing from generated resources: ${e.message}")
            null
        }
    }

    private fun generateSecureRandomNonce(byteLength: Int = 32): String {
        val randomBytes = ByteArray(byteLength)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    /**
     * Interactive Sign in with Google via Jetpack Credential Manager.
     * Uses a single GetSignInWithGoogleOption per GetCredentialRequest for API 36 responsiveness.
     */
    suspend fun signInWithGoogle(activityContext: Context): GoogleAuthOutcome {
        val serverClientId = getWebClientId()
            ?: return GoogleAuthOutcome.Error(
                "Google Sign-In configuration missing: default_web_client_id not found. Check google-services.json."
            )

        val resolvedActivity = activityContext.findActivity()
        val credentialContext: Context = if (resolvedActivity != null) {
            MutableContextWrapper(resolvedActivity)
        } else {
            activityContext
        }

        val nonce = generateSecureRandomNonce()
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId)
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return try {
            val response = credentialManager.getCredential(
                context = credentialContext,
                request = request
            )
            handleCredentialResponse(response.credential)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            GoogleAuthOutcome.Cancelled(
                "Google Sign-In cancelled (${e.message ?: "dismissed by user or Play Services"})."
            )
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts available via Credential Manager: ${e.message}", e)
            GoogleAuthOutcome.Error(
                "No Google account is signed in on this device. Add a Google account in Android Settings or link your Gmail session below.",
                e
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error during Google Sign-In: ${e.type} - ${e.message}", e)
            GoogleAuthOutcome.Error("Google Sign-In failed: ${e.message ?: e.type}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
            GoogleAuthOutcome.Error("Authentication error: ${e.message}", e)
        }
    }

    /**
     * Silent auto sign-in check using GetGoogleIdOption in an isolated GetCredentialRequest.
     */
    suspend fun attemptSilentSignIn(activityContext: Context): GoogleAuthOutcome {
        val serverClientId = getWebClientId()
            ?: return GoogleAuthOutcome.Error("default_web_client_id missing")

        val resolvedActivity = activityContext.findActivity() ?: return GoogleAuthOutcome.Cancelled()
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .setNonce(generateSecureRandomNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(
                context = MutableContextWrapper(resolvedActivity),
                request = request
            )
            handleCredentialResponse(response.credential)
        } catch (e: Exception) {
            Log.d(TAG, "Silent auto sign-in unavailable: ${e.message}")
            GoogleAuthOutcome.Error(e.message ?: "No authorized account", e)
        }
    }

    private suspend fun handleCredentialResponse(credential: androidx.credentials.Credential): GoogleAuthOutcome {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                val idToken = googleIdTokenCredential.idToken
                val givenName = googleIdTokenCredential.givenName
                val familyName = googleIdTokenCredential.familyName

                // Also authenticate Firebase Auth with the Google ID token when Firebase is initialized
                authenticateFirebaseWithIdToken(idToken)

                Log.d(TAG, "Google Sign-In successful for $email ($displayName)")
                GoogleAuthOutcome.Success(
                    GoogleSignInResult(
                        email = email,
                        displayName = displayName ?: email.substringBefore("@"),
                        photoUrl = photoUrl,
                        idToken = idToken,
                        givenName = givenName,
                        familyName = familyName
                    )
                )
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Failed to parse Google ID token credential", e)
                GoogleAuthOutcome.Error("Invalid Google credential format: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "Received unexpected credential type: ${credential.type}")
            return GoogleAuthOutcome.Error("Unsupported credential type: ${credential.type}")
        }
    }

    private suspend fun authenticateFirebaseWithIdToken(idToken: String) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) return
            val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
            suspendCancellableCoroutine<Unit> { continuation ->
                Firebase.auth.signInWithCredential(firebaseCred)
                    .addOnSuccessListener {
                        Log.d(TAG, "Firebase Auth linked with Google ID token")
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    .addOnFailureListener { ex ->
                        Log.w(TAG, "Firebase Auth credential link notice: ${ex.message}")
                        if (continuation.isActive) continuation.resume(Unit)
                    }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth sync skipped: ${e.message}")
        }
    }

    suspend fun clearCredentials() {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                Log.d(TAG, "Cleared credential manager state")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credentials: ${e.message}")
            } finally {
                try {
                    if (FirebaseApp.getApps(context).isNotEmpty()) {
                        Firebase.auth.signOut()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase signOut notice: ${e.message}")
                }
            }
        }
    }
}
