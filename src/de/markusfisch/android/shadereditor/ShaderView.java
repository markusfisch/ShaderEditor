package de.markusfisch.android.shadereditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ShaderView
	extends GLSurfaceView
{
	public final ShaderRenderer renderer = new ShaderRenderer();

	private AccelerometerListener accelerometerListener = null;
	private GyroscopeListener gyroscopeListener = null;
	private SensorManager sensorManager = null;
	private Sensor accelerometerSensor = null;
	private Sensor gyroscopeSensor = null;
	private boolean listeningToAccelerometer = false;
	private boolean listeningToGyroscope = false;

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
	public void onPause()
	{
		super.onPause();

		if( accelerometerSensor != null &&
			listeningToAccelerometer )
		{
			sensorManager.unregisterListener(
				accelerometerListener );

			listeningToAccelerometer = false;
		}

		if( gyroscopeSensor != null &&
			listeningToGyroscope )
		{
			sensorManager.unregisterListener(
				gyroscopeListener );

			listeningToGyroscope = false;
		}
	}

	@Override
	public boolean onTouchEvent( MotionEvent e )
	{
		renderer.onTouch( e.getX(), e.getY() );

		return true;
	}

	public void registerAccelerometerListener()
	{
		if( !listeningToAccelerometer &&
			getSensorManager() != null &&
			(accelerometerSensor != null ||
				(accelerometerSensor = sensorManager.getDefaultSensor(
					Sensor.TYPE_ACCELEROMETER )) != null) )
		{
			if( accelerometerListener == null )
				accelerometerListener =
					new AccelerometerListener( renderer );

			accelerometerListener.reset();

			listeningToAccelerometer = sensorManager.registerListener(
				accelerometerListener,
				accelerometerSensor,
				getSensorDelay() );
		}
	}

	public void registerGyroscopeListener()
	{
		if( !listeningToGyroscope &&
			getSensorManager() != null &&
			(gyroscopeSensor != null ||
				(gyroscopeSensor = sensorManager.getDefaultSensor(
					Sensor.TYPE_GYROSCOPE )) != null) )
		{
			if( gyroscopeListener == null )
				gyroscopeListener =
					new GyroscopeListener( renderer );

			gyroscopeListener.reset();

			listeningToGyroscope = sensorManager.registerListener(
				gyroscopeListener,
				gyroscopeSensor,
				getSensorDelay() );
		}
	}

	private SensorManager getSensorManager()
	{
		if( sensorManager != null )
			return sensorManager;

		sensorManager = (SensorManager)
			getContext().getSystemService( Context.SENSOR_SERVICE );

		return sensorManager;
	}

	private void init()
	{
		renderer.view = this;

		setEGLContextClientVersion( 2 );
		setRenderer( renderer );
		setRenderMode( GLSurfaceView.RENDERMODE_CONTINUOUSLY );
	}

	private int getSensorDelay()
	{
		SharedPreferences p = getContext().getSharedPreferences(
			ShaderPreferenceActivity.SHARED_PREFERENCES_NAME,
			0 );
		String s = p.getString(
			ShaderPreferenceActivity.SENSOR_DELAY,
			"Normal" );

		if( s.equals( "Fastest" ) )
			return SensorManager.SENSOR_DELAY_FASTEST;
		else if( s.equals( "Game" ) )
			return SensorManager.SENSOR_DELAY_GAME;
		else if( s.equals( "UI" ) )
			return SensorManager.SENSOR_DELAY_UI;

		return SensorManager.SENSOR_DELAY_NORMAL;
	}
}
