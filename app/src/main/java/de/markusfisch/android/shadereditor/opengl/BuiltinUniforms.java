package de.markusfisch.android.shadereditor.opengl;

import android.content.Context;
import android.view.MotionEvent;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

	private static final float NS_PER_SECOND = 1000000000f;

	private final float[] surfaceResolution = new float[]{0, 0};
	private final float[] resolution = new float[]{0, 0};
	private final float[] touch = new float[]{0, 0};
	private final float[] touchStart = new float[]{0, 0};
	private final float[] mouse = new float[]{0, 0};
	private final float[] pointers = new float[30];
	private final float[] offset = new float[]{0, 0};
	@NonNull
	private final Context context;
	@NonNull
	private final BuiltinSensorUniforms sensorUniforms;
	@NonNull
	private final BuiltinSystemUniforms systemUniforms;
	@NonNull
	private final BuiltinCameraUniforms cameraUniforms;

	@Nullable
	private BuiltinUniformAccess uniformAccess;
	@Nullable
	private ProgramBindings programBindings;
	@NonNull
	private ShaderTextureResources textureResources =
			ShaderTextureResources.empty();
	private int pointerCount;
	private int frameNum;
	private long startTime;
	private float fTimeMax = 3f;
	private float quality = 1f;
	private float startRandom;

	BuiltinUniforms(@NonNull Context context) {
		this.context = context;
		sensorUniforms = new BuiltinSensorUniforms(context);
		systemUniforms = new BuiltinSystemUniforms(context);
		cameraUniforms = new BuiltinCameraUniforms(context);
	}

	void setQuality(float quality) {
		this.quality = quality;
	}

	void configure(
			@NonNull GlDevice device,
			@NonNull GlProgram program,
			float fTimeMax,
			@NonNull ShaderTextureResources textureResources) {
		releaseModules();
		this.fTimeMax = fTimeMax;
		this.textureResources = textureResources;
		uniformAccess = new BuiltinUniformAccess(device, program);
		programBindings = new ProgramBindings(program);

		var access = uniformAccess;
		if (access == null) {
			return;
		}

		sensorUniforms.configure(access);
		systemUniforms.configure(access);
		cameraUniforms.configure(textureResources);
	}

	@NonNull
	SurfaceState updateSurface(int width, int height, long now) {
		startTime = now;
		startRandom = (float) Math.random();
		frameNum = 0;

		surfaceResolution[0] = width;
		surfaceResolution[1] = height;
		int deviceRotation = getDeviceRotation(context);
		sensorUniforms.setDeviceRotation(deviceRotation);

		float w = Math.round(width * quality);
		float h = Math.round(height * quality);
		boolean resolutionChanged = w != resolution[0] || h != resolution[1];
		resolution[0] = w;
		resolution[1] = h;

		cameraUniforms.updateSurface(
				(int) resolution[0],
				(int) resolution[1],
				deviceRotation);
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
		var access = uniformAccess;
		var bindings = programBindings;
		if (access == null || bindings == null) {
			return null;
		}

		long now = System.nanoTime();
		float delta = (now - startTime) / NS_PER_SECOND;

		bindings.clear();
		bindFrameUniforms(bindings, delta);
		systemUniforms.apply(access, bindings, now);
		sensorUniforms.apply(access, bindings);
		if (backBufferTexture != null) {
			bindings.setTexture(ShaderRenderer.UNIFORM_BACKBUFFER, backBufferTexture);
		}
		cameraUniforms.apply(access, bindings);
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
		uniformAccess = null;
		programBindings = null;
	}

	void release() {
		releaseModules();
		clearConfiguration();
	}

	private void bindFrameUniforms(
			@NonNull ProgramBindings bindings,
			float delta) {
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
	}

	private void releaseModules() {
		sensorUniforms.release();
		systemUniforms.release();
		cameraUniforms.release();
	}

	private static int getDeviceRotation(@NonNull Context context) {
		WindowManager windowManager = (WindowManager) context.getSystemService(
				Context.WINDOW_SERVICE);
		if (windowManager == null) {
			return 0;
		}

		return windowManager.getDefaultDisplay().getRotation();
	}
}
