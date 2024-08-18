package de.markusfisch.android.shadereditor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent

class RotationVectorListener(context: Context) : AbstractListener(context) {
    @JvmField
    val values: FloatArray = FloatArray(3) { 0f }

    // Prefer TYPE_GAME_ROTATION_VECTOR if possible because it doesn't
    // depend from a geomagnetic sensor.
    fun register(): Boolean =
        register(Sensor.TYPE_GAME_ROTATION_VECTOR) || register(Sensor.TYPE_ROTATION_VECTOR)

    override fun onSensorChanged(event: SensorEvent) {
        event.values.copyInto(values)
    }
}
