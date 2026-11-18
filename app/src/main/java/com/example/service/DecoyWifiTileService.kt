package com.example.service

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.R

class DecoyWifiTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val prefs = getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        val current = prefs.getBoolean("decoy_enabled_wifi", true)
        val newState = !current
        prefs.edit().putBoolean("decoy_enabled_wifi", newState).apply()

        updateTileState()

        val fakeSsid = prefs.getString("fake_wifi_ssid", "Metropolis_Secure_WiFi") ?: "Metropolis_Secure_WiFi"
        val msg = if (newState) {
            "Decoy Wi-Fi: ACTIVE ($fakeSsid)"
        } else {
            "Decoy Wi-Fi: INACTIVE"
        }
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("decoy_enabled_wifi", true)
        val fakeSsid = prefs.getString("fake_wifi_ssid", "Metropolis_Secure_WiFi") ?: "Metropolis_Secure_WiFi"

        // Appears identically to Android standard Internet / Wi-Fi tile
        tile.label = getString(R.string.tile_decoy_wifi_name)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_wifi)

        if (isActive) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = fakeSsid
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Inactive"
            }
        }
        tile.updateTile()
    }
}
