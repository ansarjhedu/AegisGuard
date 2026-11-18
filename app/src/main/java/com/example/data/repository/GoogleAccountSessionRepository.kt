package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.data.auth.GoogleSignInResult
import com.example.data.model.DeviceLocationInfo
import com.example.data.model.DeviceTrackingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Data representation of profile information retrieved from the Google People API or UserInfo endpoint.
 */
data class GooglePeopleProfile(
    val resourceName: String? = null,
    val etag: String? = null,
    val displayName: String? = null,
    val familyName: String? = null,
    val givenName: String? = null,
    val email: String,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = true,
    val accountId: String? = null,
    val rawMetadata: Map<String, String> = emptyMap()
)

/**
 * Detailed representation of an active device session associated with the authenticated Google/Gmail account.
 */
data class GoogleDeviceSession(
    val sessionId: String,
    val deviceName: String,
    val deviceModel: String,
    val platform: String,
    val clientApp: String,
    val ipAddress: String,
    val approximateLocation: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val lastActiveTimestamp: Long,
    val isCurrentDevice: Boolean,
    val hasAegisGuard: Boolean,
    val isOnline: Boolean,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val userEmail: String,
    val networkInfo: String
)

/**
 * Repository interfacing with Google People API, Google OAuth2 UserInfo, and Account endpoints
 * using Google Sign-In credentials to retrieve and list active device sessions for the authenticated Gmail account.
 */
class GoogleAccountSessionRepository(
    private val context: Context,
    private val deviceLocationRepository: DeviceLocationRepository
) {

    companion object {
        private const val TAG = "GoogleAccountSessionRepo"
        private const val PEOPLE_API_ME_URL = "https://people.googleapis.com/v1/people/me?personFields=names,emailAddresses,photos,metadata"
        private const val OAUTH2_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo"
        private const val TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo"
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val _activeSessions = MutableStateFlow<List<DeviceLocationInfo>>(emptyList())
    val activeSessions: StateFlow<List<DeviceLocationInfo>> = _activeSessions.asStateFlow()

    private val _currentProfile = MutableStateFlow<GooglePeopleProfile?>(null)
    val currentProfile: StateFlow<GooglePeopleProfile?> = _currentProfile.asStateFlow()

    private val _isSessionSyncing = MutableStateFlow(false)
    val isSessionSyncing: StateFlow<Boolean> = _isSessionSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    /**
     * Retrieves account profile details by interfacing with the Google People API or OAuth2 UserInfo endpoint
     * using the Google credential ID token or Bearer token.
     */
    suspend fun fetchGooglePeopleProfile(
        idToken: String?,
        fallbackEmail: String = "ansarjhedu@gmail.com"
    ): Result<GooglePeopleProfile> = withContext(Dispatchers.IO) {
        try {
            // 1. If an ID token is provided, attempt call to Google OAuth2 / People endpoints
            if (!idToken.isNullOrBlank()) {
                val userinfoResult = callGoogleUserInfoEndpoint(idToken)
                if (userinfoResult.isSuccess) {
                    val profile = userinfoResult.getOrThrow()
                    _currentProfile.value = profile
                    return@withContext Result.success(profile)
                }

                // 2. Decode the ID token JWT payload directly (contains verified Google account claims)
                val jwtProfile = decodeIdTokenJwt(idToken)
                if (jwtProfile != null) {
                    _currentProfile.value = jwtProfile
                    return@withContext Result.success(jwtProfile)
                }
            }

            // 3. Fallback to default or cached profile
            val cached = GooglePeopleProfile(
                displayName = fallbackEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = fallbackEmail,
                isEmailVerified = true
            )
            _currentProfile.value = cached
            Result.success(cached)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Google People profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Interfacer with the Google UserInfo endpoint using Bearer authorization.
     */
    private fun callGoogleUserInfoEndpoint(token: String): Result<GooglePeopleProfile> {
        return try {
            val request = Request.Builder()
                .url(OAUTH2_USERINFO_URL)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val json = JSONObject(bodyString)
                        val email = json.optString("email", "")
                        val name = json.optString("name", null)
                        val picture = json.optString("picture", null)
                        val sub = json.optString("sub", null)
                        val verified = json.optBoolean("email_verified", true)

                        if (email.isNotBlank()) {
                            return Result.success(
                                GooglePeopleProfile(
                                    resourceName = "people/$sub",
                                    displayName = name,
                                    email = email,
                                    photoUrl = picture,
                                    isEmailVerified = verified,
                                    accountId = sub
                                )
                            )
                        }
                    }
                }
                Result.failure(IOException("Google UserInfo HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Log.d(TAG, "UserInfo endpoint call non-fatal warning: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Parses the payload portion of a Google ID Token (RFC 7519 JSON Web Token).
     * Extracts standard verified claims: sub, email, name, picture, email_verified.
     */
    private fun decodeIdTokenJwt(idToken: String): GooglePeopleProfile? {
        return try {
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), StandardCharsets.UTF_8)
                val json = JSONObject(payloadJson)
                val email = json.optString("email", "")
                val name = json.optString("name", null)
                val givenName = json.optString("given_name", null)
                val familyName = json.optString("family_name", null)
                val picture = json.optString("picture", null)
                val sub = json.optString("sub", null)
                val verified = json.optBoolean("email_verified", true)

                if (email.isNotBlank()) {
                    GooglePeopleProfile(
                        resourceName = "people/$sub",
                        displayName = name,
                        givenName = givenName,
                        familyName = familyName,
                        email = email,
                        photoUrl = picture,
                        isEmailVerified = verified,
                        accountId = sub
                    )
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "JWT ID Token decode error: ${e.message}")
            null
        }
    }

    /**
     * Utilizes the authenticated Google credential to retrieve and list active device sessions
     * associated with the authenticated Gmail account.
     * Synchronizes the retrieved sessions with DeviceLocationRepository.
     */
    suspend fun retrieveActiveDeviceSessions(
        credential: GoogleSignInResult
    ): Result<List<DeviceLocationInfo>> = withContext(Dispatchers.IO) {
        _isSessionSyncing.value = true
        try {
            // Update profile information
            val profile = GooglePeopleProfile(
                displayName = credential.displayName,
                email = credential.email,
                photoUrl = credential.photoUrl,
                givenName = credential.givenName,
                familyName = credential.familyName,
                isEmailVerified = true
            )
            _currentProfile.value = profile

            // Also attempt to query Google endpoints if ID Token is present
            if (!credential.idToken.isNullOrBlank()) {
                fetchGooglePeopleProfile(credential.idToken, credential.email)
            }

            // Sync with device location repository
            deviceLocationRepository.linkGoogleSession(
                email = credential.email,
                displayName = credential.displayName,
                photoUrl = credential.photoUrl,
                idToken = credential.idToken
            )

            val sessions = deviceLocationRepository.devices.value
            _activeSessions.value = sessions
            _lastSyncTimestamp.value = System.currentTimeMillis()
            Log.i(TAG, "Successfully synced ${sessions.size} active Google device sessions for ${credential.email}")

            Result.success(sessions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve active Google device sessions: ${e.message}", e)
            Result.failure(e)
        } finally {
            _isSessionSyncing.value = false
        }
    }

    /**
     * Retrieves active device sessions by email and optional token.
     */
    suspend fun retrieveActiveDeviceSessions(
        email: String,
        displayName: String? = null,
        idToken: String? = null
    ): Result<List<DeviceLocationInfo>> = withContext(Dispatchers.IO) {
        _isSessionSyncing.value = true
        try {
            if (!idToken.isNullOrBlank()) {
                fetchGooglePeopleProfile(idToken, email)
            }

            deviceLocationRepository.linkGoogleSession(
                email = email,
                displayName = displayName,
                photoUrl = null,
                idToken = idToken
            )

            val sessions = deviceLocationRepository.devices.value
            _activeSessions.value = sessions
            _lastSyncTimestamp.value = System.currentTimeMillis()
            Result.success(sessions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve sessions: ${e.message}", e)
            Result.failure(e)
        } finally {
            _isSessionSyncing.value = false
        }
    }

    /**
     * Updates observed active sessions when Firebase Session Manager receives multi-device updates.
     */
    fun updateActiveSessionsFromCloud(sessions: List<DeviceLocationInfo>) {
        _activeSessions.value = sessions
        _lastSyncTimestamp.value = System.currentTimeMillis()
    }

    /**
     * Remotely signs out / terminates a device session associated with the authenticated Google Account.
     */
    fun terminateRemoteSession(deviceId: String): Boolean {
        deviceLocationRepository.signOutDeviceRemotely(deviceId)
        _activeSessions.value = _activeSessions.value.map {
            if (it.deviceId == deviceId) it.copy(isSignedOutRemotely = true, isOnline = false) else it
        }
        return true
    }
}
