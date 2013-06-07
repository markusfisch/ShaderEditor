package de.markusfisch.android.shadereditor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ShaderView
	extends GLSurfaceView
{
	public final ShaderRenderer renderer = new ShaderRenderer();

	private final AccelerometerListener accelerometerListener =
		new AccelerometerListener();
	private SensorManager sensorManager = null;
	private Sensor accelerometerSensor = null;

	public ShaderView( Context context )
	{
		super( context );
		init();
	}

	public ShaderView( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		init();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if( (sensorManager != null ||
				(sensorManager = (SensorManager)
					getContext().getSystemService(
						Context.SENSOR_SERVICE )) != null) &&
			(accelerometerSensor != null ||
				(accelerometerSensor = sensorManager.getDefaultSensor(
					Sensor.TYPE_ACCELEROMETER )) != null) )
			sensorManager.registerListener(
				accelerometerListener,
				accelerometerSensor,
				SensorManager.SENSOR_DELAY_NORMAL );
	}

	@Override
	public void onPause()
	{
		super.onPause();

		if( accelerometerSensor != null )
			sensorManager.unregisterListener( accelerometerListener );
	}

	@Override
	public boolean onTouchEvent( MotionEvent e )
	{
		renderer.onTouch( e.getX(), e.getY() );

		return true;
	}

	private void init()
	{
		setEGLContextClientVersion( 2 );
		setRenderer( renderer );
		setRenderMode( GLSurfaceView.RENDERMODE_CONTINUOUSLY );
	}

	private class AccelerometerListener implements SensorEventListener
	{
		private long last = 0;

		@Override
		public final void onAccuracyChanged( Sensor sensor, int accuracy )
		{
		}

		@Override
		public final void onSensorChanged( SensorEvent event )
		{
			if( last > 0 )
			{
				final float t = event.timestamp;
				final float a = t/(t+(t-last));
				final float b = 1f-a;

				renderer.gravity[0] =
					a*renderer.gravity[0]+b*event.values[0];
				renderer.gravity[1] =
					a*renderer.gravity[1]+b*event.values[1];
				renderer.gravity[2] =
					a*renderer.gravity[2]+b*event.values[2];

				renderer.linear[0] =
					event.values[0]-renderer.gravity[0];
				renderer.linear[1] =
					event.values[1]-renderer.gravity[1];
				renderer.linear[2] =
					event.values[2]-renderer.gravity[2];
			}

			last = event.timestamp;
		}
	}
}
