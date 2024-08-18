package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

import androidx.annotation.NonNull;

public class LinearAccelerationListener extends AbstractListener {
	public final float[] values = new float[]{0, 0, 0};

	public LinearAccelerationListener(@NonNull Context context) {
		super(context);
	}

	public boolean register() {
		return register(Sensor.TYPE_LINEAR_ACCELERATION);
	}

	@Override
	public void onSensorChanged(@NonNull SensorEvent event) {
		values[0] = event.values[0];
		values[1] = event.values[1];
		values[2] = event.values[2];
	}
}
