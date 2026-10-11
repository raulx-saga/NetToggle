package com.example.nettoggle

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

// A TileService = the logic behind one Quick Settings tile.
class ToggleTileService : TileService() {

    // Called when the panel is opened and our tile becomes visible.
    override fun onStartListening() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE   // greyed look; ACTIVE would be highlighted
        tile.updateTile()                  // apply the change
    }

    // Called when the user taps the tile.
    override fun onClick() {
        // An Intent = "please open this screen". This one is the mobile network settings.
        val intent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Android 14+ wants a PendingIntent when launching from a tile; older versions take the Intent directly.
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
