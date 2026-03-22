package de.markusfisch.android.shadereditor.opengl;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.markusfisch.android.shadereditor.hardware.AccelerometerListener;
import de.markusfisch.android.shadereditor.hardware.GravityListener;
import de.markusfisch.android.shadereditor.hardware.GyroscopeListener;
import de.markusfisch.android.shadereditor.hardware.LightListener;
import de.markusfisch.android.shadereditor.hardware.LinearAccelerationListener;
import de.markusfisch.android.shadereditor.hardware.MagneticFieldListener;
import de.markusfisch.android.shadereditor.hardware.PressureListener;
import de.markusfisch.android.shadereditor.hardware.ProximityListener;
import de.markusfisch.android.shadereditor.hardware.RotationVectorListener;

final class BuiltinSensorUniforms {
	private final float[] rotationMatrix = new float[9];
	private final float[] inclinationMatrix = new float[9];
	private final float[] orientation = new float[]{0, 0, 0};
	@NonNull
	private final Context context;

	@Nullable
	private AccelerometerListener accelerometerListener;
	@Nullable
	private GravityListener gravityListener;
	@Nullable
	private GyroscopeListener gyroscopeListener;
	@Nullable
	private MagneticFieldListener magneticFieldListener;
	@Nullable
	private LightListener lightListener;
	@Nullable
	private LinearAccelerationListener linearAccelerationListener;
	@Nullable
	private PressureListener pressureListener;
	@Nullable
	private ProximityListener proximityListener;
	@Nullable
	private RotationVectorListener rotationVectorListener;
	@Nullable
	private float[] gravityValues;
	@Nullable
	private float[] linearValues;
	private int deviceRotation;

	BuiltinSensorUniforms(@NonNull Context context) {
		this.context = context;
	}

	void configure(@NonNull BuiltinUniformSchema schema) {
		if (schema.needsGravitySource()) {
			if (gravityListener == null) {
				gravityListener = new GravityListener(context);
			}
			if (!gravityListener.register()) {
				gravityListener = null;
				var listener = getAccelerometerListener();
				gravityValues = listener != null ? listener.gravity : null;
			} else {
				gravityValues = gravityListener.values;
			}
		}

		if (schema.linear()) {
			if (linearAccelerationListener == null) {
				linearAccelerationListener =
						new LinearAccelerationListener(context);
			}
			if (!linearAccelerationListener.register()) {
				linearAccelerationListener = null;
				var listener = getAccelerometerListener();
				linearValues = listener != null ? listener.linear : null;
			} else {
				linearValues = linearAccelerationListener.values;
			}
		}

		if (schema.gyroscope()) {
			if (gyroscopeListener == null) {
				gyroscopeListener = new GyroscopeListener(context);
			}
			if (!gyroscopeListener.register()) {
				gyroscopeListener = null;
			}
		}

		if (schema.needsMagneticSource()) {
			if (magneticFieldListener == null) {
				magneticFieldListener = new MagneticFieldListener(context);
			}
			if (!magneticFieldListener.register()) {
				magneticFieldListener = null;
			}
		}

		if (schema.light()) {
			if (lightListener == null) {
				lightListener = new LightListener(context);
			}
			if (!lightListener.register()) {
				lightListener = null;
			}
		}

		if (schema.pressure()) {
			if (pressureListener == null) {
				pressureListener = new PressureListener(context);
			}
			if (!pressureListener.register()) {
				pressureListener = null;
			}
		}

		if (schema.proximity()) {
			if (proximityListener == null) {
				proximityListener = new ProximityListener(context);
			}
			if (!proximityListener.register()) {
				proximityListener = null;
			}
		}

		if (schema.rotationVector() ||
				(magneticFieldListener == null &&
						(schema.orientation() || schema.rotationMatrix()))) {
			if (rotationVectorListener == null) {
				rotationVectorListener = new RotationVectorListener(context);
			}
			if (!rotationVectorListener.register()) {
				rotationVectorListener = null;
			}
		}
	}

	void setDeviceRotation(int deviceRotation) {
		this.deviceRotation = deviceRotation;
	}

	void apply(
			@NonNull BuiltinUniformSchema schema,
			@NonNull ProgramBindings bindings) {
		if (schema.gravity() && gravityValues != null) {
			bindings.setFloat3(ShaderRenderer.UNIFORM_GRAVITY, gravityValues);
		}
		if (schema.linear() && linearValues != null) {
			bindings.setFloat3(ShaderRenderer.UNIFORM_LINEAR, linearValues);
		}
		if (schema.gyroscope() && gyroscopeListener != null) {
			bindings.setFloat3(
					ShaderRenderer.UNIFORM_GYROSCOPE,
					gyroscopeListener.rotation);
		}
		if (schema.magnetic() && magneticFieldListener != null) {
			bindings.setFloat3(
					ShaderRenderer.UNIFORM_MAGNETIC,
					magneticFieldListener.values);
		}
		if (schema.light() && lightListener != null) {
			bindings.setFloat(
					ShaderRenderer.UNIFORM_LIGHT,
					lightListener.getAmbient());
		}
		if (schema.pressure() && pressureListener != null) {
			bindings.setFloat(
					ShaderRenderer.UNIFORM_PRESSURE,
					pressureListener.getPressure());
		}
		if (schema.proximity() && proximityListener != null) {
			bindings.setFloat(
					ShaderRenderer.UNIFORM_PROXIMITY,
					proximityListener.getCentimeters());
		}
		if (schema.rotationVector() && rotationVectorListener != null) {
			bindings.setFloat3(
					ShaderRenderer.UNIFORM_ROTATION_VECTOR,
					rotationVectorListener.values);
		}
		if (schema.needsRotationUniforms() && gravityValues != null) {
			bindRotationUniforms(schema, bindings);
		}
	}

	void release() {
		if (accelerometerListener != null) {
			accelerometerListener.unregister();
			gravityValues = null;
			linearValues = null;
		}

		if (gravityListener != null) {
			gravityListener.unregister();
			gravityValues = null;
		}

		if (linearAccelerationListener != null) {
			linearAccelerationListener.unregister();
			linearValues = null;
		}

		if (gyroscopeListener != null) {
			gyroscopeListener.unregister();
		}

		if (magneticFieldListener != null) {
			magneticFieldListener.unregister();
		}

		if (lightListener != null) {
			lightListener.unregister();
		}

		if (pressureListener != null) {
			pressureListener.unregister();
		}

		if (proximityListener != null) {
			proximityListener.unregister();
		}

		if (rotationVectorListener != null) {
			rotationVectorListener.unregister();
		}
	}

	private void bindRotationUniforms(
			@NonNull BuiltinUniformSchema schema,
			@NonNull ProgramBindings bindings) {
		boolean haveInclination = false;
		if (gravityListener != null &&
				magneticFieldListener != null &&
				SensorManager.getRotationMatrix(
						rotationMatrix,
						inclinationMatrix,
						gravityValues,
						magneticFieldListener.filtered)) {
			haveInclination = true;
		} else if (rotationVectorListener != null) {
			SensorManager.getRotationMatrixFromVector(
					rotationMatrix,
					rotationVectorListener.values);
		} else {
			return;
		}

		if (deviceRotation != Surface.ROTATION_0) {
			record AxisPair(int x, int y) {
			}
			@SuppressWarnings("SuspiciousNameCombination")
			var rotation = switch (deviceRotation) {
				case Surface.ROTATION_90 ->
						new AxisPair(SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X);
				case Surface.ROTATION_180 ->
						new AxisPair(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y);
				case Surface.ROTATION_270 ->
						new AxisPair(SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X);
				default -> new AxisPair(SensorManager.AXIS_X, SensorManager.AXIS_Y);
			};
			SensorManager.remapCoordinateSystem(
					rotationMatrix,
					rotation.x(),
					rotation.y(),
					rotationMatrix);
		}

		if (schema.rotationMatrix()) {
			bindings.setMatrix3(
					ShaderRenderer.UNIFORM_ROTATION_MATRIX,
					true,
					rotationMatrix);
		}
		if (schema.orientation()) {
			SensorManager.getOrientation(rotationMatrix, orientation);
			bindings.setFloat3(ShaderRenderer.UNIFORM_ORIENTATION, orientation);
		}
		if (schema.inclinationMatrix() && haveInclination) {
			bindings.setMatrix3(
					ShaderRenderer.UNIFORM_INCLINATION_MATRIX,
					true,
					inclinationMatrix);
		}
		if (schema.inclination() && haveInclination) {
			bindings.setFloat(
					ShaderRenderer.UNIFORM_INCLINATION,
					SensorManager.getInclination(inclinationMatrix));
		}
	}

	@Nullable
	private AccelerometerListener getAccelerometerListener() {
		if (accelerometerListener == null) {
			accelerometerListener = new AccelerometerListener(context);
		}
		if (!accelerometerListener.register()) {
			return null;
		}
		return accelerometerListener;
	}
}