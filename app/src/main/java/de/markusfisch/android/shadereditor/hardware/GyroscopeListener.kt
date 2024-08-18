package de.markusfisch.android.shadereditor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GyroscopeListener(context: Context) : AbstractListener(context) {
    val rotation: FloatArray = FloatArray(3) { 1f }

    private val deltaRotationVector = FloatArray(4)
    private val deltaRotationMatrix = FloatArray(9)

    fun register(): Boolean = rotation.fill(1f).run { register(Sensor.TYPE_GYROSCOPE) }

    override fun onSensorChanged(event: SensorEvent) {
        if (last > 0) {
            val dT = (event.timestamp - last) * NS2S

            // Calculate the angular speed of the sample.
            val omegaMagnitude = sqrt(event.values.reduce { acc, value -> acc + value * value })
            // Axis of the rotation sample, not normalized yet.
            var (axisX, axisY, axisZ) = event.values

            // Normalize the rotation vector.
            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude
                axisY /= omegaMagnitude
                axisZ /= omegaMagnitude
            }

            // Integrate around this axis with the angular speed by the
            // time step in order to get a delta rotation from this sample
            // over the time step. Then convert this axis-angle representation
            // of the delta rotation into a quaternion before turning it
            // into the rotation matrix.
            val thetaOverTwo = omegaMagnitude * dT / 2.0f
            val sinThetaOverTwo = sin(thetaOverTwo.toDouble()).toFloat()
            val cosThetaOverTwo = cos(thetaOverTwo.toDouble()).toFloat()
            deltaRotationVector[0] = sinThetaOverTwo * axisX
            deltaRotationVector[1] = sinThetaOverTwo * axisY
            deltaRotationVector[2] = sinThetaOverTwo * axisZ
            deltaRotationVector[3] = cosThetaOverTwo

            SensorManager.getRotationMatrixFromVector(
                deltaRotationMatrix, deltaRotationVector
            )

            val (r0, r1, r2) = rotation
            rotation[0] =
                r0 * deltaRotationMatrix[0] + r1 * deltaRotationMatrix[1] + r2 * deltaRotationMatrix[2]
            rotation[1] =
                r0 * deltaRotationMatrix[3] + r1 * deltaRotationMatrix[4] + r2 * deltaRotationMatrix[5]
            rotation[2] =
                r0 * deltaRotationMatrix[6] + r1 * deltaRotationMatrix[7] + r2 * deltaRotationMatrix[8]
        }

        last = event.timestamp
    }

    companion object {
        private const val NS2S = 1f / 1_000_000_000f
        private const val EPSILON = 1f
    }
}
