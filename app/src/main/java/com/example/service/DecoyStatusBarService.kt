package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class DecoyStatusBarService : Service() {

    companion object {
        const val CHANNEL_ID = "aegis_decoy_camouflage_channel"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START = "ACTION_START_DECOY"
        const val ACTION_STOP = "ACTION_STOP_DECOY"
        const val ACTION_UPDATE = "ACTION_UPDATE_DECOY"

        const val EXTRA_STATUS_TEXT = "EXTRA_STATUS_TEXT"

        fun startService(context: Context, statusSummary: String = "Camouflage Active • Decoy Wi-Fi, 5G & GPS Shielded") {
            val intent = Intent(context, DecoyStatusBarService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_STATUS_TEXT, statusSummary)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DecoyStatusBarService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val statusText = intent?.getStringExtra(EXTRA_STATUS_TEXT)
                    ?: "Camouflage Active • Decoy Status Bar Strip Running"
                val notification = buildDecoyNotification(statusText)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AegisGuard Camouflage Status Bar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays the persistent decoy status bar icons for Wi-Fi, Hotspot, 5G, and Location camouflage"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildDecoyNotification(statusSummary: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("🛡️ Decoy Status Bar Camouflage Active")
            .setContentText(statusSummary)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    """
                    $statusSummary
                    • Spoofed Wi-Fi: Active (Metropolis_Secure_WiFi)
                    • Spoofed 5G Carrier: Veritas 5G+
                    • Spoofed GPS Telemetry: 35.6762° N, 139.6503° E
                    • Real Connection: Masked from hostile trackers
                    """.trimIndent()
                )
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }
}
