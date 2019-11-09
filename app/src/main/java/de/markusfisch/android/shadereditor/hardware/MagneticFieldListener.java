package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class MagneticFieldListener extends AbstractListener {
	public final float[] values = new float[]{0, 0, 0};
	public final float[] filtered = new float[]{0, 0, 0};

	public MagneticFieldListener(Context context) {
		super(context);
	}

	public boolean register() {
		return register(Sensor.TYPE_MAGNETIC_FIELD);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		final float a = .8f;
		final float b = 1f - a;

		filtered[0] = a * filtered[0] + b * event.values[0];
		filtered[1] = a * filtered[1] + b * event.values[1];
		filtered[2] = a * filtered[2] + b * event.values[2];

		values[0] = event.values[0];
		values[1] = event.values[1];
		values[2] = event.values[2];
	}
}

