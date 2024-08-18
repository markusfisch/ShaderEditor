package de.markusfisch.android.shadereditor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent

class LightListener(context: Context) : AbstractListener(context) {
    var ambient: Float = 0f
        private set

    fun register(): Boolean = register(Sensor.TYPE_LIGHT)

    override fun onSensorChanged(event: SensorEvent) {
        ambient = event.values[0]
    }
}


