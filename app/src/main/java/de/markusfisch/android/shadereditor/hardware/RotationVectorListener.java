package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class RotationVectorListener extends AbstractListener {
	public final float values[] = new float[]{0, 0, 0};

	public RotationVectorListener(Context context) {
		super(context);
	}

	public boolean register() {
		return register(Sensor.TYPE_ROTATION_VECTOR);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		System.arraycopy(event.values, 0, values, 0, 3);
	}
}
