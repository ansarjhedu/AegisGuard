package com.example.service

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.R

class DecoyLocationTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val prefs = getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        val current = prefs.getBoolean("decoy_enabled_location", true)
        val newState = !current
        prefs.edit().putBoolean("decoy_enabled_location", newState).apply()

        updateTileState()

        val city = prefs.getString("fake_location_city", "Tokyo, Japan") ?: "Tokyo, Japan"
        val msg = if (newState) {
            "Decoy Location (GPS): ACTIVE ($city)"
        } else {
            "Decoy Location (GPS): INACTIVE"
        }
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("decoy_enabled_location", true)
        val city = prefs.getString("fake_location_city", "Tokyo, Japan") ?: "Tokyo, Japan"

        tile.label = getString(R.string.tile_decoy_location_name)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_location)

        if (isActive) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = city
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
