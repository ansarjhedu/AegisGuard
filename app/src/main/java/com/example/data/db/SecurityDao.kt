package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityDao {

    @Query("SELECT * FROM security_audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM security_audit_logs WHERE isAlert = 1 ORDER BY timestamp DESC")
    fun getAlertLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    @Query("DELETE FROM security_audit_logs WHERE id = :id")
    suspend fun deleteAuditLog(id: Long)

    @Query("DELETE FROM security_audit_logs")
    suspend fun clearAllAuditLogs()

    @Query("SELECT * FROM quarantined_apps ORDER BY detectionDate DESC")
    fun getAllQuarantined(): Flow<List<QuarantineEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM quarantined_apps WHERE packageName = :packageName)")
    suspend fun isAppQuarantined(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuarantine(item: QuarantineEntity)

    @Query("DELETE FROM quarantined_apps WHERE packageName = :packageName")
    suspend fun removeQuarantine(packageName: String)
}
