package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

import androidx.annotation.NonNull;

public class GravityListener extends AbstractListener {
	public final float[] values = new float[]{0, 0, 0};

	public GravityListener(@NonNull Context context) {
		super(context);
	}

	public boolean register() {
		return register(Sensor.TYPE_GRAVITY);
	}

	@Override
	public void onSensorChanged(@NonNull SensorEvent event) {
		values[0] = event.values[0];
		values[1] = event.values[1];
		values[2] = event.values[2];
	}
}
