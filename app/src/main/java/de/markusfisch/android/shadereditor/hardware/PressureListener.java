package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class PressureListener extends AbstractListener {
	private float pressure = 0f;

	public PressureListener(Context context) {
		super(context);
	}

	public boolean register() {
		return register(Sensor.TYPE_PRESSURE);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		pressure = event.values[0];
	}

	public float getPressure() {
		return pressure;
	}
}



