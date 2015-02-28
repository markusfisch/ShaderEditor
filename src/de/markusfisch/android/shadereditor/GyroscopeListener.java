package de.markusfisch.android.shadereditor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class GyroscopeListener
	implements SensorEventListener
{
	private static final float EPSILON = 1.0f;

	private ShaderRenderer renderer;
	private static final float NS2S = 1.0f/1000000000.0f;
	private final float[] deltaRotationVector = new float[4];
	private long last = 0;

	public GyroscopeListener( ShaderRenderer r )
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
		if( last != 0 )
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
		}

		last = event.timestamp;

		float[] deltaRotationMatrix = new float[9];
		SensorManager.getRotationMatrixFromVector(
			deltaRotationMatrix,
			deltaRotationVector );

		renderer.rotation[0] += deltaRotationMatrix[0];
		renderer.rotation[1] += deltaRotationMatrix[1];
		renderer.rotation[2] += deltaRotationMatrix[2];
	}
}
