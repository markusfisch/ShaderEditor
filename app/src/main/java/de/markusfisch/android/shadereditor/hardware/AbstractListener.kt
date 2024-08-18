package de.markusfisch.android.shadereditor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import de.markusfisch.android.shadereditor.app.ShaderEditorApp

abstract class AbstractListener(context: Context) : SensorEventListener {

    var last: Long = 0
        protected set

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?

    private var listening = false

    private var sensor: Sensor? = null

    fun unregister() {
        if (sensor != null && listening) {
            sensorManager?.unregisterListener(this)
            listening = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No implementation required
    }

    override fun onSensorChanged(event: SensorEvent) {
        last = event.timestamp
    }

    fun register(type: Int): Boolean {
        if (listening) return false
        sensor = sensor ?: sensorManager?.getDefaultSensor(type) ?: return false

        last = 0
        listening = sensorManager?.registerListener(
            this, sensor, ShaderEditorApp.preferences.sensorDelay
        ) ?: false

        return listening
    }
}