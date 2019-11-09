package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

public class GyroscopeListener extends AbstractListener {
	public final float[] rotation = new float[]{1f, 1f, 1f};

	private static final float NS2S = 1f / 1000000000f;
	private static final float EPSILON = 1f;

	private final float[] deltaRotationVector = new float[4];
	private final float[] deltaRotationMatrix = new float[9];

	public GyroscopeListener(Context context) {
		super(context);
	}

	public boolean register() {
		rotation[0] = rotation[1] = rotation[2] = 1f;
		return register(Sensor.TYPE_GYROSCOPE);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (last > 0) {
			final float dT = (event.timestamp - last) * NS2S;

			// axis of the rotation sample, not normalized yet
			float axisX = event.values[0];
			float axisY = event.values[1];
			float axisZ = event.values[2];

			// calculate the angular speed of the sample
			float omegaMagnitude = (float) Math.sqrt(
					axisX * axisX + axisY * axisY + axisZ * axisZ);

			// normalize the rotation vector
			if (omegaMagnitude > EPSILON) {
				axisX /= omegaMagnitude;
				axisY /= omegaMagnitude;
				axisZ /= omegaMagnitude;
			}

			// integrate around this axis with the angular speed by the
			// timestep in order to get a delta rotation from this sample
			// over the timestep; then convert this axis-angle representation
			// of the delta rotation into a quaternion before turning it
			// into the rotation matrix
			float thetaOverTwo = omegaMagnitude * dT / 2.0f;
			float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
			float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
			deltaRotationVector[0] = sinThetaOverTwo * axisX;
			deltaRotationVector[1] = sinThetaOverTwo * axisY;
			deltaRotationVector[2] = sinThetaOverTwo * axisZ;
			deltaRotationVector[3] = cosThetaOverTwo;

			SensorManager.getRotationMatrixFromVector(
					deltaRotationMatrix,
					deltaRotationVector);

			float r0 = rotation[0];
			float r1 = rotation[1];
			float r2 = rotation[2];
			rotation[0] = r0 * deltaRotationMatrix[0] +
					r1 * deltaRotationMatrix[1] +
					r2 * deltaRotationMatrix[2];
			rotation[1] = r0 * deltaRotationMatrix[3] +
					r1 * deltaRotationMatrix[4] +
					r2 * deltaRotationMatrix[5];
			rotation[2] = r0 * deltaRotationMatrix[6] +
					r1 * deltaRotationMatrix[7] +
					r2 * deltaRotationMatrix[8];
		}

		last = event.timestamp;
	}
}
