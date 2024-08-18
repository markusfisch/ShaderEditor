package de.markusfisch.android.shadereditor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent

class AccelerometerListener(context: Context) : AbstractListener(context) {

    val gravity = FloatArray(3) { 0f }
    val linear = FloatArray(3) { 0f }
    val values = FloatArray(3) { 0f }

    fun register(): Boolean = register(Sensor.TYPE_ACCELEROMETER)

    override fun onSensorChanged(event: SensorEvent) {
        if (last > 0) {
            val a = 0.8f
            val b = 1f - a

            event.values.copyInto(values)
            event.values.forEachIndexed { index, _ ->
                gravity[index] = a * gravity[index] + b * values[index]
            }

            event.values.copyInto(linear)
                .forEachIndexed { index, _ -> linear[index] = values[index] - gravity[index] }

        }

        last = event.timestamp
    }
}