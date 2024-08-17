package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

import androidx.annotation.NonNull;

public class AccelerometerListener extends AbstractListener {
	public final float[] gravity = new float[]{0, 0, 0};
	public final float[] linear = new float[]{0, 0, 0};
	public final float[] values = new float[]{0, 0, 0};

	public AccelerometerListener(@NonNull Context context) {
		super(context);
	}

	public boolean register() {
		return register(Sensor.TYPE_ACCELEROMETER);
	}

	@Override
	public void onSensorChanged(@NonNull SensorEvent event) {
		if (last > 0) {
			final float a = .8f;
			final float b = 1f - a;

			gravity[0] = a * gravity[0] + b * event.values[0];
			gravity[1] = a * gravity[1] + b * event.values[1];
			gravity[2] = a * gravity[2] + b * event.values[2];

			linear[0] = event.values[0] - gravity[0];
			linear[1] = event.values[1] - gravity[1];
			linear[2] = event.values[2] - gravity[2];

			values[0] = event.values[0];
			values[1] = event.values[1];
			values[2] = event.values[2];
		}

		last = event.timestamp;
	}
}
