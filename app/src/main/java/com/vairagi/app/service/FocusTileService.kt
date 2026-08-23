package com.vairagi.app.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class FocusTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val isInactive = tile.state == Tile.STATE_INACTIVE

        val intent = Intent(this, TrackingService::class.java).apply {
            action = if (isInactive) TrackingService.ACTION_START_TRACKING else TrackingService.ACTION_PAUSE_TRACKING
        }
        startService(intent)

        tile.state = if (isInactive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isInactive) "Vairagi Active" else "Vairagi Paused"
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = "Vairagi Active"
        tile.updateTile()
    }
}
