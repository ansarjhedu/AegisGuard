package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.AegisApplication

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val app = context.applicationContext as? AegisApplication
            app?.networkMonitor?.startMonitoring()
        }
    }
}
