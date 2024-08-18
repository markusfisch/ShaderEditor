package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

import androidx.annotation.NonNull;

public class PressureListener extends AbstractListener {
	private float pressure = 0f;

	public PressureListener(@NonNull Context context) {
		super(context);
	}

	public boolean register() {
		return register(Sensor.TYPE_PRESSURE);
	}

	@Override
	public void onSensorChanged(@NonNull SensorEvent event) {
		pressure = event.values[0];
	}

	public float getPressure() {
		return pressure;
	}
}



