package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

import androidx.annotation.NonNull;

public class RotationVectorListener extends AbstractListener {
	public final float[] values = new float[]{0, 0, 0};

	public RotationVectorListener(@NonNull Context context) {
		super(context);
	}

	public boolean register() {
		// Prefer TYPE_GAME_ROTATION_VECTOR if possible because it doesn't
		// depend from a geomagnetic sensor.
		return register(Sensor.TYPE_GAME_ROTATION_VECTOR) ||
				register(Sensor.TYPE_ROTATION_VECTOR);
	}

	@Override
	public void onSensorChanged(@NonNull SensorEvent event) {
		System.arraycopy(event.values, 0, values, 0, 3);
	}
}
