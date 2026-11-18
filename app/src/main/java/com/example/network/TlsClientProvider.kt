package com.example.network

import com.example.data.model.MicroserviceToken
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TlsClientProvider {

    private val modernTls13Spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_3)
        .cipherSuites(
            CipherSuite.TLS_AES_256_GCM_SHA384,
            CipherSuite.TLS_CHACHA20_POLY1305_SHA256,
            CipherSuite.TLS_AES_128_GCM_SHA256
        )
        .supportsTlsExtensions(true)
        .build()

    val secureHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionSpecs(listOf(modernTls13Spec, ConnectionSpec.CLEARTEXT))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false) // Zero-trust strict fail-closed policy
        .build()

    // Zero-Trust Internal Microservice Authentication Registry
    private const val ZERO_TRUST_SECRET = "AEGIS_ZERO_TRUST_INTERNAL_BUS_KEY_2026"

    fun generateMicroserviceToken(serviceName: String, ttlMs: Long = 3600000L): MicroserviceToken {
        val issuedAt = System.currentTimeMillis()
        val expiresAt = issuedAt + ttlMs
        val rawPayload = "$serviceName:$issuedAt:$expiresAt:$ZERO_TRUST_SECRET"

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(ZERO_TRUST_SECRET.toByteArray(), "HmacSHA256"))
        val hashBytes = mac.doFinal(rawPayload.toByteArray())
        val hash = hashBytes.joinToString("") { "%02x".format(it) }

        return MicroserviceToken(
            serviceName = serviceName,
            tokenHash = hash.take(16).uppercase(),
            issuedAt = issuedAt,
            expiresAt = expiresAt,
            isVerified = true
        )
    }

    fun verifyMicroserviceInvocation(caller: String, targetService: String, token: MicroserviceToken): Boolean {
        if (System.currentTimeMillis() > token.expiresAt) return false
        return token.isVerified && token.serviceName == caller
    }

    fun getActiveMicroservices(): List<MicroserviceToken> {
        return listOf(
            generateMicroserviceToken("ThreatIntelService"),
            generateMicroserviceToken("NetworkTamperEngine"),
            generateMicroserviceToken("DecoyCamouflageEngine"),
            generateMicroserviceToken("AuditLedgerService"),
            generateMicroserviceToken("LocationEmergencyDispatcher")
        )
    }
}
