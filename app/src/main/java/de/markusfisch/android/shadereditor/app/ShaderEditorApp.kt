package de.markusfisch.android.shadereditor.app

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.StrictMode
import de.markusfisch.android.shadereditor.BuildConfig
import de.markusfisch.android.shadereditor.database.Database
import de.markusfisch.android.shadereditor.preference.Preferences
import de.markusfisch.android.shadereditor.receiver.BatteryLevelReceiver
import de.markusfisch.android.shadereditor.view.UndoRedo

class ShaderEditorApp : Application() {

    companion object {
        @JvmField
        val preferences = Preferences()

        @JvmField
        val db = Database()

        @JvmField
        val editHistory = UndoRedo.EditHistory()

        private val batteryLevelReceiver = BatteryLevelReceiver()
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().penaltyLog()
                    .penaltyDeath().build()
            )
        }

        preferences.init(this)
        db.open(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerBatteryReceiver()
        }
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(batteryLevelReceiver, filter)
        // Note it's not required to unregister the receiver because it
        // needs to be there as long as this application is running.
    }
}
