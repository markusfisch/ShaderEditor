package de.markusfisch.android.shadereditor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent

class PressureListener(context: Context) : AbstractListener(context) {
    var pressure: Float = 0f
        private set

    fun register(): Boolean = register(Sensor.TYPE_PRESSURE)

    override fun onSensorChanged(event: SensorEvent) {
        pressure = event.values[0]
    }
}



