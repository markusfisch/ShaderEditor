package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class MagneticFieldListener extends AbstractListener
{
	public final float values[] = new float[]{ 0, 0, 0 };

	public MagneticFieldListener( Context context )
	{
		super( context );
	}

	public boolean register()
	{
		return register( Sensor.TYPE_MAGNETIC_FIELD );
	}

	@Override
	public void onSensorChanged( SensorEvent event )
	{
		values[0] = event.values[0];
		values[1] = event.values[1];
		values[2] = event.values[2];
	}
}

