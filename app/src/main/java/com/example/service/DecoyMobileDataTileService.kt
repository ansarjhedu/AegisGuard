package com.example.service

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.R

class DecoyMobileDataTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val prefs = getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        val current = prefs.getBoolean("decoy_enabled_mobile_data", true)
        val newState = !current
        prefs.edit().putBoolean("decoy_enabled_mobile_data", newState).apply()

        updateTileState()

        val msg = if (newState) {
            "Decoy Mobile Data (5G): ACTIVE"
        } else {
            "Decoy Mobile Data (5G): INACTIVE"
        }
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("decoy_enabled_mobile_data", true)

        tile.label = getString(R.string.tile_decoy_mobile_data_name)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_mobile_data)

        if (isActive) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Veritas 5G+ (Active)"
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
