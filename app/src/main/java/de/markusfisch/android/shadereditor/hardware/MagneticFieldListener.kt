package de.markusfisch.android.shadereditor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent

class MagneticFieldListener(context: Context) : AbstractListener(context) {
    val values: FloatArray = FloatArray(3) { 0f }
    val filtered: FloatArray = FloatArray(3) { 0f }

    fun register(): Boolean =  register(Sensor.TYPE_MAGNETIC_FIELD)

    override fun onSensorChanged(event: SensorEvent) {
        val a = .8f
        val b = 1f - a

        event.values.copyInto(values)

        filtered.forEachIndexed { index, _ ->
            filtered[index] = a * filtered[index] + b * values[index]
        }
    }
}

