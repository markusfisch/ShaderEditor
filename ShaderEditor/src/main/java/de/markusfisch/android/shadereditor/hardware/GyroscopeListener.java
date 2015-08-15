package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

public class GyroscopeListener extends AbstractListener
{
	public final float rotation[] = new float[]{ 0, 0, 0 };

	private static final float NS2S = 1f/1000000000f;
	private static final float EPSILON = 1f;

	private final float deltaRotationVector[] = new float[4];
	private final float deltaRotationMatrix[] = new float[9];

	public GyroscopeListener( Context context )
	{
		super( context );
	}

	public boolean register()
	{
		return register( Sensor.TYPE_GYROSCOPE );
	}

	@Override
	public void onSensorChanged( SensorEvent event )
	{
		if( last > 0 )
		{
			final float dT = (event.timestamp-last)*NS2S;

			float axisX = event.values[0];
			float axisY = event.values[1];
			float axisZ = event.values[2];

			float omegaMagnitude = (float)Math.sqrt(
				axisX*axisX +
				axisY*axisY +
				axisZ*axisZ );

			if( omegaMagnitude > EPSILON )
			{
				axisX /= omegaMagnitude;
				axisY /= omegaMagnitude;
				axisZ /= omegaMagnitude;
			}

			float thetaOverTwo = omegaMagnitude*dT/2.0f;
			float sinThetaOverTwo = (float)Math.sin( thetaOverTwo );
			float cosThetaOverTwo = (float)Math.cos( thetaOverTwo );

			deltaRotationVector[0] = sinThetaOverTwo*axisX;
			deltaRotationVector[1] = sinThetaOverTwo*axisY;
			deltaRotationVector[2] = sinThetaOverTwo*axisZ;
			deltaRotationVector[3] = cosThetaOverTwo;

			SensorManager.getRotationMatrixFromVector(
				deltaRotationMatrix,
				deltaRotationVector );

			rotation[0] += deltaRotationMatrix[0];
			rotation[1] += deltaRotationMatrix[1];
			rotation[2] += deltaRotationMatrix[2];
		}

		last = event.timestamp;
	}
}
