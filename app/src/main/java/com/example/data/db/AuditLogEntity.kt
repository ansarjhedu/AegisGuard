package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val severity: String,
    val encryptedDescription: String,
    val encryptedIv: String,
    val encryptedMetadata: String,
    val metadataIv: String,
    val isAlert: Boolean = false
)

@Entity(tableName = "quarantined_apps")
data class QuarantineEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val detectionDate: Long = System.currentTimeMillis(),
    val threatCategory: String,
    val riskScore: Int,
    val quarantineReason: String,
    val encryptedDetails: String,
    val encryptedIv: String,
    val isIsolated: Boolean = true
)
