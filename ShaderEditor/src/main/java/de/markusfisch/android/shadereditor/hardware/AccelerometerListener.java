package de.markusfisch.android.shadereditor.hardware;

import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class AccelerometerListener implements SensorEventListener
{
	private ShaderRenderer renderer;
	private long last = 0;

	public AccelerometerListener( ShaderRenderer r )
	{
		renderer = r;
	}

	public void reset()
	{
		last = 0;
	}

	@Override
	public final void onAccuracyChanged( Sensor sensor, int accuracy )
	{
	}

	@Override
	public final void onSensorChanged( SensorEvent event )
	{
		if( last > 0 )
		{
			final float a = .8f;
			final float b = 1f-a;

			renderer.gravity[0] =
				a*renderer.gravity[0]+
				b*event.values[0];

			renderer.gravity[1] =
				a*renderer.gravity[1]+
				b*event.values[1];

			renderer.gravity[2] =
				a*renderer.gravity[2]+
				b*event.values[2];

			renderer.linear[0] =
				event.values[0]-
				renderer.gravity[0];

			renderer.linear[1] =
				event.values[1]-
				renderer.gravity[1];

			renderer.linear[2] =
				event.values[2]-
				renderer.gravity[2];
		}

		last = event.timestamp;
	}
}
