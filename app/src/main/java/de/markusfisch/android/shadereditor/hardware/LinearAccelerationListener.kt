package de.markusfisch.android.shadereditor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent

class LinearAccelerationListener(context: Context) : AbstractListener(context) {
    @JvmField
    val values: FloatArray = FloatArray(3) { 0f }

    fun register(): Boolean = register(Sensor.TYPE_LINEAR_ACCELERATION)

    override fun onSensorChanged(event: SensorEvent) {
        event.values.copyInto(values)
    }
}
