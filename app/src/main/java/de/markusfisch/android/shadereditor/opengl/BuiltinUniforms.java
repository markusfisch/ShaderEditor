package de.markusfisch.android.shadereditor.opengl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import java.util.Calendar;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.hardware.AccelerometerListener;
import de.markusfisch.android.shadereditor.hardware.CameraListener;
import de.markusfisch.android.shadereditor.hardware.GravityListener;
import de.markusfisch.android.shadereditor.hardware.GyroscopeListener;
import de.markusfisch.android.shadereditor.hardware.LightListener;
import de.markusfisch.android.shadereditor.hardware.LinearAccelerationListener;
import de.markusfisch.android.shadereditor.hardware.MagneticFieldListener;
import de.markusfisch.android.shadereditor.hardware.MicInputListener;
import de.markusfisch.android.shadereditor.hardware.PressureListener;
import de.markusfisch.android.shadereditor.hardware.ProximityListener;
import de.markusfisch.android.shadereditor.hardware.RotationVectorListener;
import de.markusfisch.android.shadereditor.service.NotificationService;

final class BuiltinUniforms {
	record SurfaceState(int renderWidth, int renderHeight, boolean renderTargetsChanged) {
		@NonNull
		static SurfaceState empty() {
			return new SurfaceState(0, 0, false);
		}
	}

	record PreparedFrame(
			@NonNull ProgramBindings bindings,
			int surfaceWidth,
			int surfaceHeight,
			long now) {
	}

	private record ShaderUniformSchema(
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
		static ShaderUniformSchema create(
				@NonNull GlDevice device,
				@NonNull GlProgram program) {
			return new ShaderUniformSchema(
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

	private static final float NS_PER_SECOND = 1000000000f;
	private static final long BATTERY_UPDATE_INTERVAL = 10000000000L;
	private static final long DATE_UPDATE_INTERVAL = 1000000000L;

	private final float[] surfaceResolution = new float[]{0, 0};
	private final float[] resolution = new float[]{0, 0};
	private final float[] touch = new float[]{0, 0};
	private final float[] touchStart = new float[]{0, 0};
	private final float[] mouse = new float[]{0, 0};
	private final float[] pointers = new float[30];
	private final float[] offset = new float[]{0, 0};
	private final float[] daytime = new float[]{0, 0, 0};
	private final float[] dateTime = new float[]{0, 0, 0, 0};
	private final float[] rotationMatrix = new float[9];
	private final float[] inclinationMatrix = new float[9];
	private final float[] orientation = new float[]{0, 0, 0};
	private final float[] cameraOrientation = new float[4];
	@NonNull
	private final Context context;

	@Nullable
	private AccelerometerListener accelerometerListener;
	@Nullable
	private CameraListener cameraListener;
	@Nullable
	private GravityListener gravityListener;
	@Nullable
	private GyroscopeListener gyroscopeListener;
	@Nullable
	private MagneticFieldListener magneticFieldListener;
	@Nullable
	private MicInputListener micInputListener;
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
	private ShaderTextureResources.LoadedTextureBinding cameraBinding;
	@Nullable
	private ShaderUniformSchema shaderUniformSchema;
	@Nullable
	private ProgramBindings programBindings;
	@NonNull
	private ShaderTextureResources textureResources =
			ShaderTextureResources.empty();
	@Nullable
	private float[] gravityValues;
	@Nullable
	private float[] linearValues;
	private int deviceRotation;
	private int nightMode;
	private int pointerCount;
	private int frameNum;
	private long startTime;
	private long lastBatteryUpdate;
	private long lastDateUpdate;
	private float batteryLevel;
	private float fTimeMax = 3f;
	private float quality = 1f;
	private float startRandom;

	BuiltinUniforms(@NonNull Context context) {
		this.context = context;
	}

	void setQuality(float quality) {
		this.quality = quality;
	}

	void configure(
			@NonNull GlDevice device,
			@NonNull GlProgram program,
			float fTimeMax,
			@NonNull ShaderTextureResources textureResources) {
		releaseListeners();
		this.fTimeMax = fTimeMax;
		this.textureResources = textureResources;
		cameraBinding = textureResources.getCameraBinding();
		shaderUniformSchema = ShaderUniformSchema.create(device, program);
		programBindings = new ProgramBindings(program);
		registerInputs();
		if (resolution[0] > 0 && resolution[1] > 0) {
			openCameraIfNeeded();
		}
	}

	@NonNull
	SurfaceState updateSurface(int width, int height, long now) {
		startTime = now;
		startRandom = (float) Math.random();
		frameNum = 0;

		surfaceResolution[0] = width;
		surfaceResolution[1] = height;
		deviceRotation = getDeviceRotation(context);

		float w = Math.round(width * quality);
		float h = Math.round(height * quality);
		boolean resolutionChanged = w != resolution[0] || h != resolution[1];
		resolution[0] = w;
		resolution[1] = h;

		openCameraIfNeeded();
		return new SurfaceState(
				(int) resolution[0],
				(int) resolution[1],
				resolutionChanged);
	}

	void updateTouch(@NonNull MotionEvent e) {
		float x = e.getX() * quality;
		float y = e.getY() * quality;

		touch[0] = x;
		touch[1] = resolution[1] - y;

		mouse[0] = x / resolution[0];
		mouse[1] = 1 - y / resolution[1];

		switch (e.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				touchStart[0] = touch[0];
				touchStart[1] = touch[1];
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				pointerCount = 0;
				return;
			default:
				break;
		}

		pointerCount = Math.min(e.getPointerCount(), pointers.length / 3);
		for (int i = 0, pointerOffset = 0; i < pointerCount; ++i) {
			pointers[pointerOffset++] = e.getX(i) * quality;
			pointers[pointerOffset++] = resolution[1] - e.getY(i) * quality;
			pointers[pointerOffset++] = e.getTouchMajor(i);
		}
	}

	void updateOffset(float x, float y) {
		offset[0] = x;
		offset[1] = y;
	}

	@Nullable
	PreparedFrame beginFrame(@Nullable GlTexture2D backBufferTexture) {
		var schema = shaderUniformSchema;
		var bindings = programBindings;
		if (schema == null || bindings == null) {
			return null;
		}

		var now = System.nanoTime();
		float delta = (now - startTime) / NS_PER_SECOND;

		bindings.clear();
		bindings.setFloat(ShaderRenderer.UNIFORM_TIME, delta);
		bindings.setInt(ShaderRenderer.UNIFORM_SECOND, (int) delta);
		bindings.setFloat(
				ShaderRenderer.UNIFORM_SUB_SECOND,
				delta - (int) delta);
		bindings.setInt(ShaderRenderer.UNIFORM_FRAME_NUMBER, frameNum);
		bindings.setFloat(
				ShaderRenderer.UNIFORM_FTIME,
				((delta % fTimeMax) / fTimeMax * 2f - 1f));
		bindings.setFloat2(ShaderRenderer.UNIFORM_RESOLUTION, resolution);
		bindings.setFloat2(ShaderRenderer.UNIFORM_TOUCH, touch);
		bindings.setFloat2(ShaderRenderer.UNIFORM_TOUCH_START, touchStart);
		bindings.setFloat2(ShaderRenderer.UNIFORM_MOUSE, mouse);
		bindings.setInt(ShaderRenderer.UNIFORM_POINTER_COUNT, pointerCount);
		if (pointerCount > 0) {
			bindings.setFloat3(
					ShaderRenderer.UNIFORM_POINTERS,
					pointerCount,
					pointers);
		}
		bindings.setFloat2(ShaderRenderer.UNIFORM_OFFSET, offset);
		bindings.setFloat(ShaderRenderer.UNIFORM_START_RANDOM, startRandom);

		if (schema.nightMode()) {
			bindings.setInt(ShaderRenderer.UNIFORM_NIGHT_MODE, nightMode);
		}
		if (schema.notificationCount()) {
			bindings.setInt(
					ShaderRenderer.UNIFORM_NOTIFICATION_COUNT,
					NotificationService.getCount());
		}
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
		if (schema.needsRotationUniforms() && gravityValues != null) {
			bindRotationUniforms(schema, bindings);
		}
		if (schema.lastNotificationTime()) {
			Long lastTime = NotificationService.getLastNotificationTime();
			if (lastTime == null) {
				bindings.setFloat(
						ShaderRenderer.UNIFORM_LAST_NOTIFICATION_TIME,
						Float.NaN);
			} else {
				float millisPerSecond = 1f / 1000f;
				bindings.setFloat(
						ShaderRenderer.UNIFORM_LAST_NOTIFICATION_TIME,
						(System.currentTimeMillis() - lastTime) * millisPerSecond);
			}
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
		if (schema.battery()) {
			if (now - lastBatteryUpdate > BATTERY_UPDATE_INTERVAL) {
				batteryLevel = getBatteryLevel();
				lastBatteryUpdate = now;
			}
			bindings.setFloat(ShaderRenderer.UNIFORM_BATTERY, batteryLevel);
		}
		if (schema.powerConnected()) {
			bindings.setInt(
					ShaderRenderer.UNIFORM_POWER_CONNECTED,
					ShaderEditorApp.preferences.isPowerConnected() ? 1 : 0);
		}
		if (schema.needsDateData()) {
			if (now - lastDateUpdate > DATE_UPDATE_INTERVAL) {
				Calendar calendar = Calendar.getInstance();
				if (schema.date()) {
					dateTime[0] = calendar.get(Calendar.YEAR);
					dateTime[1] = calendar.get(Calendar.MONTH);
					dateTime[2] = calendar.get(Calendar.DAY_OF_MONTH);
					dateTime[3] = calendar.get(Calendar.HOUR_OF_DAY) * 3600f +
							calendar.get(Calendar.MINUTE) * 60f +
							calendar.get(Calendar.SECOND);
				}
				if (schema.daytime()) {
					daytime[0] = calendar.get(Calendar.HOUR_OF_DAY);
					daytime[1] = calendar.get(Calendar.MINUTE);
					daytime[2] = calendar.get(Calendar.SECOND);
				}
				lastDateUpdate = now;
			}
			if (schema.date()) {
				bindings.setFloat4(ShaderRenderer.UNIFORM_DATE, dateTime);
			}
			if (schema.daytime()) {
				bindings.setFloat3(ShaderRenderer.UNIFORM_DAYTIME, daytime);
			}
		}
		if (schema.mediaVolume()) {
			bindings.setFloat(
					ShaderRenderer.UNIFORM_MEDIA_VOLUME,
					getMediaVolumeLevel(context));
		}
		if (schema.micAmplitude() && micInputListener != null) {
			bindings.setFloat(
					ShaderRenderer.UNIFORM_MIC_AMPLITUDE,
					micInputListener.getAmplitude());
		}
		if (backBufferTexture != null) {
			bindings.setTexture(ShaderRenderer.UNIFORM_BACKBUFFER, backBufferTexture);
		}
		if (cameraListener != null) {
			if (schema.cameraOrientation()) {
				cameraListener.getOrientationMatrix().rewind();
				cameraListener.getOrientationMatrix().get(cameraOrientation);
				cameraListener.getOrientationMatrix().rewind();
				bindings.setMatrix2(
						ShaderRenderer.UNIFORM_CAMERA_ORIENTATION,
						false,
						cameraOrientation);
			}
			if (schema.cameraAddent()) {
				bindings.setFloat2(
						ShaderRenderer.UNIFORM_CAMERA_ADDENT,
						cameraListener.addent);
			}
			cameraListener.update();
		}

		textureResources.applyTo(bindings);

		return new PreparedFrame(
				bindings,
				(int) surfaceResolution[0],
				(int) surfaceResolution[1],
				now);
	}

	void endFrame() {
		++frameNum;
	}

	void clearConfiguration() {
		fTimeMax = 3f;
		textureResources = ShaderTextureResources.empty();
		cameraBinding = null;
		shaderUniformSchema = null;
		programBindings = null;
	}

	void release() {
		releaseListeners();
		clearConfiguration();
	}

	private void registerInputs() {
		var schema = shaderUniformSchema;
		if (schema == null) {
			return;
		}

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

		if (schema.nightMode()) {
			nightMode = (context.getResources().getConfiguration().uiMode &
					Configuration.UI_MODE_NIGHT_MASK) ==
					Configuration.UI_MODE_NIGHT_YES ? 1 : 0;
		}

		if (schema.light()) {
			if (lightListener == null) {
				lightListener = new LightListener(context);
			}
			if (!lightListener.register()) {
				lightListener = null;
			}
		}

		if (schema.needsNotifications()) {
			NotificationService.requirePermissions(context);
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

		if (schema.micAmplitude()) {
			if (micInputListener == null) {
				micInputListener = new MicInputListener(context);
			}
			if (!micInputListener.register()) {
				micInputListener = null;
				requestPermission(android.Manifest.permission.RECORD_AUDIO);
			}
		}
	}

	private void bindRotationUniforms(
			@NonNull ShaderUniformSchema schema,
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

	private void openCameraIfNeeded() {
		unregisterCamera();

		if (cameraBinding == null ||
				!(cameraBinding.texture() instanceof GlExternalTexture texture) ||
				!(context instanceof LifecycleOwner lifecycleOwner)) {
			return;
		}

		int lensFacing = ShaderRenderer.UNIFORM_CAMERA_BACK.equals(
				cameraBinding.sampler().name())
				? CameraSelector.LENS_FACING_BACK
				: CameraSelector.LENS_FACING_FRONT;
		requestPermission(android.Manifest.permission.CAMERA);
		cameraListener = new CameraListener(
				texture.getId(),
				lensFacing,
				(int) resolution[0],
				(int) resolution[1],
				deviceRotation,
				context);
		cameraListener.register(lifecycleOwner);
	}

	private void releaseListeners() {
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

		unregisterCamera();
		if (micInputListener != null) {
			micInputListener.unregister();
			micInputListener = null;
		}
	}

	private void unregisterCamera() {
		if (cameraListener != null) {
			cameraListener.unregister();
			cameraListener = null;
		}
	}

	private void requestPermission(@NonNull String permission) {
		if (ContextCompat.checkSelfPermission(context, permission) ==
				PackageManager.PERMISSION_GRANTED) {
			return;
		}
		if (!(context instanceof Activity activity)) {
			return;
		}
		ActivityCompat.requestPermissions(
				activity,
				new String[]{permission},
				1);
	}

	private float getBatteryLevel() {
		Intent batteryStatus = context.registerReceiver(
				null,
				new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		if (batteryStatus == null) {
			return 0;
		}

		int level = batteryStatus.getIntExtra(
				BatteryManager.EXTRA_LEVEL,
				-1);
		int scale = batteryStatus.getIntExtra(
				BatteryManager.EXTRA_SCALE,
				-1);

		return (float) level / scale;
	}

	private static int getDeviceRotation(@NonNull Context context) {
		WindowManager windowManager = (WindowManager) context.getSystemService(
				Context.WINDOW_SERVICE);
		if (windowManager == null) {
			return 0;
		}

		return windowManager.getDefaultDisplay().getRotation();
	}

	private static float getMediaVolumeLevel(@NonNull Context context) {
		AudioManager audioManager = (AudioManager) context.getSystemService(
				Context.AUDIO_SERVICE);
		if (audioManager == null) {
			return 0;
		}
		float maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		if (maxVolume <= 0 || currentVolume < 0) {
			return 0;
		}
		return currentVolume / maxVolume;
	}
}
