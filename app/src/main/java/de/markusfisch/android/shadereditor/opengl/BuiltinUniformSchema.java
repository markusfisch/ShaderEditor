package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;

record BuiltinUniformSchema(
		boolean gravity,
		boolean linear,
		boolean gyroscope,
		boolean magnetic,
		boolean nightMode,
		boolean notificationCount,
		boolean lastNotificationTime,
		boolean light,
		boolean pressure,
		boolean proximity,
		boolean rotationVector,
		boolean rotationMatrix,
		boolean orientation,
		boolean inclinationMatrix,
		boolean inclination,
		boolean battery,
		boolean powerConnected,
		boolean date,
		boolean daytime,
		boolean mediaVolume,
		boolean micAmplitude,
		boolean cameraOrientation,
		boolean cameraAddent) {
	@NonNull
	static BuiltinUniformSchema create(
			@NonNull GlDevice device,
			@NonNull GlProgram program) {
		return new BuiltinUniformSchema(
				device.hasUniform(program, ShaderRenderer.UNIFORM_GRAVITY),
				device.hasUniform(program, ShaderRenderer.UNIFORM_LINEAR),
				device.hasUniform(program, ShaderRenderer.UNIFORM_GYROSCOPE),
				device.hasUniform(program, ShaderRenderer.UNIFORM_MAGNETIC),
				device.hasUniform(program, ShaderRenderer.UNIFORM_NIGHT_MODE),
				device.hasUniform(program, ShaderRenderer.UNIFORM_NOTIFICATION_COUNT),
				device.hasUniform(program, ShaderRenderer.UNIFORM_LAST_NOTIFICATION_TIME),
				device.hasUniform(program, ShaderRenderer.UNIFORM_LIGHT),
				device.hasUniform(program, ShaderRenderer.UNIFORM_PRESSURE),
				device.hasUniform(program, ShaderRenderer.UNIFORM_PROXIMITY),
				device.hasUniform(program, ShaderRenderer.UNIFORM_ROTATION_VECTOR),
				device.hasUniform(program, ShaderRenderer.UNIFORM_ROTATION_MATRIX),
				device.hasUniform(program, ShaderRenderer.UNIFORM_ORIENTATION),
				device.hasUniform(program, ShaderRenderer.UNIFORM_INCLINATION_MATRIX),
				device.hasUniform(program, ShaderRenderer.UNIFORM_INCLINATION),
				device.hasUniform(program, ShaderRenderer.UNIFORM_BATTERY),
				device.hasUniform(program, ShaderRenderer.UNIFORM_POWER_CONNECTED),
				device.hasUniform(program, ShaderRenderer.UNIFORM_DATE),
				device.hasUniform(program, ShaderRenderer.UNIFORM_DAYTIME),
				device.hasUniform(program, ShaderRenderer.UNIFORM_MEDIA_VOLUME),
				device.hasUniform(program, ShaderRenderer.UNIFORM_MIC_AMPLITUDE),
				device.hasUniform(program, ShaderRenderer.UNIFORM_CAMERA_ORIENTATION),
				device.hasUniform(program, ShaderRenderer.UNIFORM_CAMERA_ADDENT));
	}

	boolean needsGravitySource() {
		return gravity ||
				rotationMatrix ||
				orientation ||
				inclinationMatrix ||
				inclination;
	}

	boolean needsMagneticSource() {
		return magnetic ||
				rotationMatrix ||
				orientation ||
				inclinationMatrix ||
				inclination;
	}

	boolean needsRotationUniforms() {
		return rotationMatrix ||
				orientation ||
				inclinationMatrix ||
				inclination;
	}

	boolean needsNotifications() {
		return notificationCount || lastNotificationTime;
	}

	boolean needsDateData() {
		return date || daytime;
	}
}