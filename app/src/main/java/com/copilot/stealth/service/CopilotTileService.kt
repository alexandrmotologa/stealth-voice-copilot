package com.copilot.stealth.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

/**
 * Quick Settings tile enabling rapid activation and deactivation
 * directly from the lockscreen shade without unlocking the phone.
 */
@RequiresApi(Build.VERSION_CODES.N)
class CopilotTileService : TileService() {

    private var isListening = false

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        isListening = !isListening

        val serviceIntent = Intent(this, CopilotForegroundService::class.java).apply {
            action = if (isListening) {
                CopilotForegroundService.ACTION_START_LISTENING
            } else {
                CopilotForegroundService.ACTION_STOP_LISTENING
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = if (isListening) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isListening) "Copilot Active" else "Copilot Standby"
        tile.updateTile()
    }
}
