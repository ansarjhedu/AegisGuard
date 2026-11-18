package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AuditLogEntity::class, QuarantineEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AegisDatabase : RoomDatabase() {

    abstract fun securityDao(): SecurityDao

    companion object {
        @Volatile
        private var INSTANCE: AegisDatabase? = null

        fun getDatabase(context: Context): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AegisDatabase::class.java,
                    "aegis_guard_encrypted.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
