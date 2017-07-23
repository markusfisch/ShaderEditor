package de.markusfisch.android.shadereditor.hardware;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public abstract class AbstractListener implements SensorEventListener {
	protected long last = 0;

	private final SensorManager sensorManager;

	private boolean listening = false;
	private Sensor sensor;

	public AbstractListener(Context context) {
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
	public void onSensorChanged(SensorEvent event) {
		last = event.timestamp;
	}

	protected boolean register(int type) {
		if (listening || sensorManager == null || (sensor == null &&
				(sensor = sensorManager.getDefaultSensor(type)) == null)) {
			return false;
		}

		last = 0;

		return listening = sensorManager.registerListener(this, sensor,
				ShaderEditorApp.preferences.getSensorDelay());
	}
}
