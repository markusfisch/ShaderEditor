package de.markusfisch.android.shadereditor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent

class GravityListener(context: Context) : AbstractListener(context) {
    @JvmField
    val values: FloatArray = FloatArray(3) { 0f }

    fun register(): Boolean = register(Sensor.TYPE_GRAVITY)

    override fun onSensorChanged(event: SensorEvent) {
        event.values.copyInto(values)
    }
}
