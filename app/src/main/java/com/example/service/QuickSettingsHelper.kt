package com.example.service

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon as AndroidIcon
import android.os.Build
import android.widget.Toast

fun requestAddQuickSettingsTile(
    context: Context,
    serviceClass: Class<*>,
    titleRes: Int,
    iconRes: Int,
    tileName: String
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        try {
            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
            val component = ComponentName(context, serviceClass)
            statusBarManager?.requestAddTileService(
                component,
                context.getString(titleRes),
                AndroidIcon.createWithResource(context, iconRes),
                context.mainExecutor
            ) { result ->
                when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                        Toast.makeText(context, "$tileName added to Quick Settings top shade!", Toast.LENGTH_SHORT).show()
                    }
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                        Toast.makeText(context, "$tileName is already in your Quick Settings top shade", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, "Tile request closed. Drag $tileName from Edit (✏️) in top shade.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Swipe down twice and tap Edit (✏️) to drag $tileName to front tiles.", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Swipe down twice and tap Edit (✏️) to drag $tileName to front tiles.", Toast.LENGTH_LONG).show()
    }
}
