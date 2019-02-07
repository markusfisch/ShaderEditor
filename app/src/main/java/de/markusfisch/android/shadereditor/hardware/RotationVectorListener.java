package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Build;

public class RotationVectorListener extends AbstractListener {
	public final float values[] = new float[]{0, 0, 0};

	public RotationVectorListener(Context context) {
		super(context);
	}

	public boolean register() {
		// prefer TYPE_GAME_ROTATION_VECTOR if possible because it doesn't
		// depend from a geomagnetic sensor
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
				register(Sensor.TYPE_GAME_ROTATION_VECTOR)) {
			return true;
		}
		return register(Sensor.TYPE_ROTATION_VECTOR);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		System.arraycopy(event.values, 0, values, 0, 3);
	}
}
