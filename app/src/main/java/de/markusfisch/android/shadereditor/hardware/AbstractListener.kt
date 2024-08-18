package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

public abstract class AbstractListener implements SensorEventListener {
	long last = 0;

	private final SensorManager sensorManager;

	private boolean listening = false;
	@Nullable
	private Sensor sensor;

	AbstractListener(@NonNull Context context) {
		sensorManager = (SensorManager) context.getSystemService(
				Context.SENSOR_SERVICE);
	}

	public void unregister() {
		if (sensor == null || !listening) {
			return;
		}

		sensorManager.unregisterListener(this);
		listening = false;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(@NonNull SensorEvent event) {
		last = event.timestamp;
	}

	boolean register(int type) {
		if (listening || sensorManager == null || (sensor == null &&
				(sensor = sensorManager.getDefaultSensor(type)) == null)) {
			return false;
		}

		last = 0;
		listening = sensorManager.registerListener(this, sensor,
				ShaderEditorApp.preferences.getSensorDelay());

		return listening;
	}
}
