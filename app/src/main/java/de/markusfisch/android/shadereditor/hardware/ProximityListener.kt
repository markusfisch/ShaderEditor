package de.markusfisch.android.shadereditor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent

class ProximityListener(context: Context) : AbstractListener(context) {
    var centimeters: Float = 0f
        private set

    fun register(): Boolean = register(Sensor.TYPE_PROXIMITY)

    override fun onSensorChanged(event: SensorEvent) {
        centimeters = event.values[0]
    }
}




