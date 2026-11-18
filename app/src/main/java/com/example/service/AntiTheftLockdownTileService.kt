package com.example.service

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.R

class AntiTheftLockdownTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val prefs = getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        val current = prefs.getBoolean("is_lockdown_active", false)
        val newState = !current

        prefs.edit()
            .putBoolean("is_lockdown_active", newState)
            .putBoolean("is_vault_locked", newState)
            .putLong("lockdown_triggered_timestamp", System.currentTimeMillis())
            .apply()

        updateTileState()

        val msg = if (newState) {
            "Anti-Theft Lock: ACTIVE (Vault Secured)"
        } else {
            "Anti-Theft Lock: INACTIVE"
        }
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("aegis_shield_prefs", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("is_lockdown_active", false)

        tile.label = getString(R.string.tile_lockdown_name)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_lockdown)

        if (isActive) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Active"
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
