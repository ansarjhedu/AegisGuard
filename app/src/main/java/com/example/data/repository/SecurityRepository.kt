package com.example.data.repository

import android.content.Context
import com.example.crypto.CryptoManager
import com.example.data.db.AuditLogEntity
import com.example.data.db.QuarantineEntity
import com.example.data.db.SecurityDao
import com.example.data.model.AppSecurityInfo
import com.example.data.model.NetworkSnapshot
import com.example.data.model.ThreatSeverity
import com.example.data.model.ZeroTrustPosture
import com.example.network.EmailDispatcher
import com.example.network.NetworkMonitor
import com.example.scanner.MalwareScanner
import com.example.scanner.ZeroTrustPostureEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class DecryptedAuditLog(
    val id: Long,
    val timestamp: Long,
    val eventType: String,
    val severity: ThreatSeverity,
    val description: String,
    val metadata: String,
    val isAlert: Boolean,
    val isEncryptedAtRest: Boolean = true
)

class SecurityRepository(
    private val context: Context,
    private val securityDao: SecurityDao,
    private val cryptoManager: CryptoManager,
    private val malwareScanner: MalwareScanner,
    private val networkMonitor: NetworkMonitor,
    private val postureEvaluator: ZeroTrustPostureEvaluator,
    private val emailDispatcher: EmailDispatcher
) {

    private val prefs = context.getSharedPreferences("aegis_whitelist_prefs", Context.MODE_PRIVATE)

    val auditLogs: Flow<List<DecryptedAuditLog>> = securityDao.getAllAuditLogs().map { list ->
        list.map { entity ->
            val desc = cryptoManager.decrypt(entity.encryptedIv, entity.encryptedDescription)
            val meta = cryptoManager.decrypt(entity.metadataIv, entity.encryptedMetadata)
            val severity = try {
                ThreatSeverity.valueOf(entity.severity)
            } catch (e: Exception) {
                ThreatSeverity.INFO
            }
            DecryptedAuditLog(
                id = entity.id,
                timestamp = entity.timestamp,
                eventType = entity.eventType,
                severity = severity,
                description = desc,
                metadata = meta,
                isAlert = entity.isAlert
            )
        }
    }

    val quarantinedApps: Flow<List<QuarantineEntity>> = securityDao.getAllQuarantined()

    fun isAutoWhitelistSystem(): Boolean = prefs.getBoolean("auto_whitelist_system", true)
    fun setAutoWhitelistSystem(enabled: Boolean) = prefs.edit().putBoolean("auto_whitelist_system", enabled).apply()

    fun isAutoWhitelistPlayStore(): Boolean = prefs.getBoolean("auto_whitelist_play_store", true)
    fun setAutoWhitelistPlayStore(enabled: Boolean) = prefs.edit().putBoolean("auto_whitelist_play_store", enabled).apply()

    fun getUserWhitelistedPackages(): Set<String> = prefs.getStringSet("user_whitelisted_packages", emptySet()) ?: emptySet()

    fun toggleUserWhitelist(packageName: String): Boolean {
        val current = getUserWhitelistedPackages().toMutableSet()
        val willBeWhitelisted = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        prefs.edit().putStringSet("user_whitelisted_packages", current).apply()
        return willBeWhitelisted
    }

    suspend fun recordAuditLog(
        eventType: String,
        severity: ThreatSeverity,
        description: String,
        metadata: String = "",
        isAlert: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val encryptedDesc = cryptoManager.encrypt(description)
        val encryptedMeta = cryptoManager.encrypt(metadata)

        val entity = AuditLogEntity(
            eventType = eventType,
            severity = severity.name,
            encryptedDescription = encryptedDesc.ciphertext,
            encryptedIv = encryptedDesc.iv,
            encryptedMetadata = encryptedMeta.ciphertext,
            metadataIv = encryptedMeta.iv,
            isAlert = isAlert
        )
        securityDao.insertAuditLog(entity)
    }

    suspend fun quarantinePackage(app: AppSecurityInfo, reason: String) = withContext(Dispatchers.IO) {
        val encryptedDetails = cryptoManager.encrypt(
            "Package: ${app.packageName} | Flags: ${app.suspiciousFlags.joinToString("; ")} | Installer: ${app.installerPackage}"
        )
        val item = QuarantineEntity(
            packageName = app.packageName,
            appName = app.appName,
            threatCategory = "MALWARE_SPYWARE",
            riskScore = app.riskScore,
            quarantineReason = reason,
            encryptedDetails = encryptedDetails.ciphertext,
            encryptedIv = encryptedDetails.iv,
            isIsolated = true
        )
        securityDao.insertQuarantine(item)

        recordAuditLog(
            eventType = "QUARANTINE_ACTION",
            severity = ThreatSeverity.HIGH,
            description = "Quarantined threat: ${app.appName} (${app.packageName}). Permissions isolated.",
            metadata = reason,
            isAlert = true
        )
    }

    suspend fun releaseFromQuarantine(packageName: String) = withContext(Dispatchers.IO) {
        securityDao.removeQuarantine(packageName)
        recordAuditLog(
            eventType = "QUARANTINE_RELEASED",
            severity = ThreatSeverity.LOW,
            description = "Released package from quarantine: $packageName",
            isAlert = false
        )
    }

    suspend fun scanInstalledApps(
        quarantinedSet: Set<String>,
        userWhitelisted: Set<String> = getUserWhitelistedPackages(),
        autoWhitelistSystem: Boolean = isAutoWhitelistSystem(),
        autoWhitelistPlayStore: Boolean = isAutoWhitelistPlayStore()
    ): List<AppSecurityInfo> = withContext(Dispatchers.IO) {
        val scanned = malwareScanner.scanAllInstalledApplications(
            quarantinedPackages = quarantinedSet,
            userWhitelistedPackages = userWhitelisted,
            autoWhitelistSystem = autoWhitelistSystem,
            autoWhitelistPlayStore = autoWhitelistPlayStore
        )

        // Ignore whitelisted apps so system and Play Store apps do NOT produce false-positive threat alert audit logs!
        val activeThreats = scanned.filter { !it.isWhitelisted && (it.riskLevel == ThreatSeverity.HIGH || it.riskLevel == ThreatSeverity.CRITICAL) }
        val whitelistedCount = scanned.count { it.isWhitelisted }

        if (activeThreats.isNotEmpty()) {
            activeThreats.forEach { threat ->
                if (!quarantinedSet.contains(threat.packageName)) {
                    recordAuditLog(
                        eventType = if (threat.isHiddenApp) "HIDDEN_SPYWARE_DETECTED" else "MALWARE_RISK_DETECTED",
                        severity = threat.riskLevel,
                        description = "Identified ${threat.appName} [Score ${threat.riskScore}%] - ${threat.suspiciousFlags.firstOrNull() ?: "Excessive permissions"}",
                        metadata = "Package: ${threat.packageName}",
                        isAlert = true
                    )
                }
            }
        } else {
            recordAuditLog(
                eventType = "SCAN_AUDIT_VERIFIED",
                severity = ThreatSeverity.INFO,
                description = "Zero-Trust Scan: ${scanned.size} apps analyzed ($whitelistedCount auto-whitelisted: System & Play Store). 0 active threats.",
                isAlert = false
            )
        }
        scanned
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        securityDao.clearAllAuditLogs()
    }

    fun getDevicePosture(): ZeroTrustPosture {
        return postureEvaluator.evaluatePosture()
    }

    fun prepareEmailReport(recipient: String, snapshot: NetworkSnapshot, trigger: String?, threatCount: Int): String {
        return emailDispatcher.buildSecurityReport(recipient, snapshot, trigger, threatCount)
    }

    fun createEmailIntent(recipient: String, subject: String, body: String) =
        emailDispatcher.createEmailIntent(recipient, subject, body)
}
