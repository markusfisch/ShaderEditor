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
	private boolean hasGravity;
	private boolean hasLinear;
	private boolean hasGyroscope;
	private boolean hasMagnetic;
	private boolean hasLight;
	private boolean hasPressure;
	private boolean hasProximity;
	private boolean hasRotationVector;
	private boolean hasRotationMatrix;
	private boolean hasOrientation;
	private boolean hasInclinationMatrix;
	private boolean hasInclination;
	private int deviceRotation;

	BuiltinSensorUniforms(@NonNull Context context) {
		this.context = context;
	}

	void configure(
			@NonNull GlDevice device,
			@NonNull GlProgram program) {
		hasGravity = device.hasUniform(program, ShaderRenderer.UNIFORM_GRAVITY);
		hasLinear = device.hasUniform(program, ShaderRenderer.UNIFORM_LINEAR);
		hasGyroscope = device.hasUniform(program, ShaderRenderer.UNIFORM_GYROSCOPE);
		hasMagnetic = device.hasUniform(program, ShaderRenderer.UNIFORM_MAGNETIC);
		hasLight = device.hasUniform(program, ShaderRenderer.UNIFORM_LIGHT);
		hasPressure = device.hasUniform(program, ShaderRenderer.UNIFORM_PRESSURE);
		hasProximity = device.hasUniform(program, ShaderRenderer.UNIFORM_PROXIMITY);
		hasRotationVector = device.hasUniform(
				program,
				ShaderRenderer.UNIFORM_ROTATION_VECTOR);
		hasRotationMatrix = device.hasUniform(
				program,
				ShaderRenderer.UNIFORM_ROTATION_MATRIX);
		hasOrientation = device.hasUniform(program, ShaderRenderer.UNIFORM_ORIENTATION);
		hasInclinationMatrix = device.hasUniform(
				program,
				ShaderRenderer.UNIFORM_INCLINATION_MATRIX);
		hasInclination = device.hasUniform(
				program,
				ShaderRenderer.UNIFORM_INCLINATION);

		if (needsGravitySource()) {
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

		if (hasLinear) {
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

		if (hasGyroscope) {
			if (gyroscopeListener == null) {
				gyroscopeListener = new GyroscopeListener(context);
			}
			if (!gyroscopeListener.register()) {
				gyroscopeListener = null;
			}
		}

		if (needsMagneticSource()) {
			if (magneticFieldListener == null) {
				magneticFieldListener = new MagneticFieldListener(context);
			}
			if (!magneticFieldListener.register()) {
				magneticFieldListener = null;
			}
		}

		if (hasLight) {
			if (lightListener == null) {
				lightListener = new LightListener(context);
			}
			if (!lightListener.register()) {
				lightListener = null;
			}
		}

		if (hasPressure) {
			if (pressureListener == null) {
				pressureListener = new PressureListener(context);
			}
			if (!pressureListener.register()) {
				pressureListener = null;
			}
		}

		if (hasProximity) {
			if (proximityListener == null) {
				proximityListener = new ProximityListener(context);
			}
			if (!proximityListener.register()) {
				proximityListener = null;
			}
		}

		if (hasRotationVector ||
				(magneticFieldListener == null && usesRotationUniforms())) {
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

	void apply(@NonNull ProgramBindings bindings) {
		if (hasGravity && gravityValues != null) {
			bindings.setFloat3(ShaderRenderer.UNIFORM_GRAVITY, gravityValues);
		}
		if (hasLinear && linearValues != null) {
			bindings.setFloat3(ShaderRenderer.UNIFORM_LINEAR, linearValues);
		}
		if (hasGyroscope && gyroscopeListener != null) {
			bindings.setFloat3(
					ShaderRenderer.UNIFORM_GYROSCOPE,
					gyroscopeListener.rotation);
		}
		if (hasMagnetic && magneticFieldListener != null) {
			bindings.setFloat3(
					ShaderRenderer.UNIFORM_MAGNETIC,
					magneticFieldListener.values);
		}
		if (hasLight && lightListener != null) {
			bindings.setFloat(
					ShaderRenderer.UNIFORM_LIGHT,
					lightListener.getAmbient());
		}
		if (hasPressure && pressureListener != null) {
			bindings.setFloat(
					ShaderRenderer.UNIFORM_PRESSURE,
					pressureListener.getPressure());
		}
		if (hasProximity && proximityListener != null) {
			bindings.setFloat(
					ShaderRenderer.UNIFORM_PROXIMITY,
					proximityListener.getCentimeters());
		}
		if (hasRotationVector && rotationVectorListener != null) {
			bindings.setFloat3(
					ShaderRenderer.UNIFORM_ROTATION_VECTOR,
					rotationVectorListener.values);
		}
		if (usesRotationUniforms()) {
			bindRotationUniforms(bindings);
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

	private boolean needsGravitySource() {
		return hasGravity || usesRotationUniforms();
	}

	private boolean needsMagneticSource() {
		return hasMagnetic || usesRotationUniforms();
	}

	private boolean usesRotationUniforms() {
		return hasRotationMatrix ||
				hasOrientation ||
				hasInclinationMatrix ||
				hasInclination;
	}

	private void bindRotationUniforms(@NonNull ProgramBindings bindings) {
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

		if (hasRotationMatrix) {
			bindings.setMatrix3(
					ShaderRenderer.UNIFORM_ROTATION_MATRIX,
					true,
					rotationMatrix);
		}
		if (hasOrientation) {
			SensorManager.getOrientation(rotationMatrix, orientation);
			bindings.setFloat3(ShaderRenderer.UNIFORM_ORIENTATION, orientation);
		}
		if (hasInclinationMatrix && haveInclination) {
			bindings.setMatrix3(
					ShaderRenderer.UNIFORM_INCLINATION_MATRIX,
					true,
					inclinationMatrix);
		}
		if (hasInclination && haveInclination) {
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