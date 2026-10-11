package com.example.nettoggle

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class ToggleTileService : TileService() {

    override fun onStartListening() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        val robot = ToggleAccessibilityService.instance

        if (robot == null) {
            // Robot not switched on yet: send the user to the Accessibility screen instead.
            Toast.makeText(this, "Turn on NetToggle under Accessibility first", Toast.LENGTH_LONG).show()
            launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        robot.arm()                                                   // 1. tell the robot to start watching
        launch(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))     // 2. open the settings screen
    }

    private fun launch(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
