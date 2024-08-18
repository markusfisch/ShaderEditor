package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

import androidx.annotation.NonNull;

public class LightListener extends AbstractListener {
	private float ambient = 0f;

	public LightListener(@NonNull Context context) {
		super(context);
	}

	public boolean register() {
		return register(Sensor.TYPE_LIGHT);
	}

	@Override
	public void onSensorChanged(@NonNull SensorEvent event) {
		ambient = event.values[0];
	}

	public float getAmbient() {
		return ambient;
	}
}


