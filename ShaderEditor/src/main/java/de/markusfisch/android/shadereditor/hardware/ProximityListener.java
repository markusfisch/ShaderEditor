package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class ProximityListener extends AbstractListener
{
	public float centimeters = 0f;

	public ProximityListener( Context context )
	{
		super( context );
	}

	public boolean register()
	{
		return register( Sensor.TYPE_PROXIMITY );
	}

	@Override
	public void onSensorChanged( SensorEvent event )
	{
		centimeters = event.values[0];
	}
}




