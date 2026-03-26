package de.markusfisch.android.shadereditor.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ShaderRenderer implements GLSurfaceView.Renderer {
	public interface OnRendererListener {
		void onInfoLog(@NonNull List<ShaderError> error);

		void onFramesPerSecond(int fps);
	}

	public static final String UNIFORM_BACKBUFFER = "backbuffer";
	public static final String UNIFORM_BATTERY = "battery";
	public static final String UNIFORM_CAMERA_ADDENT = "cameraAddent";
	public static final String UNIFORM_CAMERA_BACK = "cameraBack";
	public static final String UNIFORM_CAMERA_FRONT = "cameraFront";
	public static final String UNIFORM_CAMERA_ORIENTATION = "cameraOrientation";
	public static final String UNIFORM_DATE = "date";
	public static final String UNIFORM_DAYTIME = "daytime";
	public static final String UNIFORM_FRAME_NUMBER = "frame";
	public static final String UNIFORM_FTIME = "ftime";
	public static final String UNIFORM_GRAVITY = "gravity";
	public static final String UNIFORM_GYROSCOPE = "gyroscope";
	public static final String UNIFORM_LAST_NOTIFICATION_TIME = "lastNotificationTime";
	public static final String UNIFORM_LIGHT = "light";
	public static final String UNIFORM_LINEAR = "linear";
	public static final String UNIFORM_MAGNETIC = "magnetic";
	public static final String UNIFORM_MOUSE = "mouse";
	public static final String UNIFORM_NIGHT_MODE = "nightMode";
	public static final String UNIFORM_NOTIFICATION_COUNT = "notificationCount";
	public static final String UNIFORM_OFFSET = "offset";
	public static final String UNIFORM_ORIENTATION = "orientation";
	public static final String UNIFORM_INCLINATION = "inclination";
	public static final String UNIFORM_INCLINATION_MATRIX = "inclinationMatrix";
	public static final String UNIFORM_POINTERS = "pointers";
	public static final String UNIFORM_POINTER_COUNT = "pointerCount";
	public static final String UNIFORM_POSITION = "position";
	public static final String UNIFORM_POWER_CONNECTED = "powerConnected";
	public static final String UNIFORM_PRESSURE = "pressure";
	public static final String UNIFORM_PROXIMITY = "proximity";
	public static final String UNIFORM_RESOLUTION = "resolution";
	public static final String UNIFORM_ROTATION_MATRIX = "rotationMatrix";
	public static final String UNIFORM_ROTATION_VECTOR = "rotationVector";
	public static final String UNIFORM_SECOND = "second";
	public static final String UNIFORM_START_RANDOM = "startRandom";
	public static final String UNIFORM_SUB_SECOND = "subsecond";
	public static final String UNIFORM_TIME = "time";
	public static final String UNIFORM_MEDIA_VOLUME = "mediaVolume";
	public static final String UNIFORM_MIC_AMPLITUDE = "micAmplitude";
	public static final String UNIFORM_TOUCH = "touch";
	public static final String UNIFORM_TOUCH_START = "touchStart";

	private static final int MAX_TEXTURES = 32;
	private static final long FPS_UPDATE_FREQUENCY_NS = 200000000L;
	private static final float NS_PER_SECOND = 1000000000f;

	private final GlDevice device = new GlDevice(MAX_TEXTURES);
	private final RendererProgramManager programManager =
			new RendererProgramManager(MAX_TEXTURES);
	private final ShaderRenderPipeline renderPipeline =
			new ShaderRenderPipeline(device, Mesh.fullScreenQuad());
	private final BuiltinUniforms builtinUniforms;
	private final Context context;
	private final Object thumbnailLock = new Object();

	@Nullable
	private OnRendererListener onRendererListener;
	@NonNull
	private BuiltinUniforms.SurfaceState surfaceState =
			BuiltinUniforms.SurfaceState.empty();
	private long lastRender;
	private byte[] thumbnail = new byte[1];
	private boolean captureThumbnail;
	private volatile long nextFpsUpdate;
	private volatile float sum;
	private volatile float samples;
	private volatile int lastFps;

	public ShaderRenderer(Context context) {
		this.context = context;
		builtinUniforms = new BuiltinUniforms(context);
	}

	public void setVersion(int version) {
		programManager.setVersion(version);
	}

	public void setFragmentShader(String source, float quality) {
		setQuality(quality);
		programManager.setFragmentShader(source);
		resetFps();
	}

	public void setQuality(float quality) {
		builtinUniforms.setQuality(quality);
	}

	public void setOnRendererListener(OnRendererListener listener) {
		onRendererListener = listener;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		device.resetState();
		device.disable(GLES20.GL_CULL_FACE);
		device.disable(GLES20.GL_BLEND);
		device.disable(GLES20.GL_DEPTH_TEST);
		device.setClearColor(0f, 0f, 0f, 1f);

		discardContextResources();
		var thumbnailErrors = renderPipeline.createContextResources();
		if (!thumbnailErrors.isEmpty()) {
			submitErrors(thumbnailErrors);
		}

		if (programManager.hasPreparedShader()) {
			resetFps();
			var reloadResult = programManager.reload(context, device);
			submitErrors(reloadResult.textureErrors());
			if (reloadResult.succeeded()) {
				submitErrors(Collections.emptyList());
				builtinUniforms.configure(
						device,
						programManager.getMainProgram(),
						programManager.getFTimeMax(),
						programManager.getTextureResources());
			} else if (!reloadResult.programErrors().isEmpty()) {
				submitErrors(reloadResult.programErrors());
			}
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		long now = System.nanoTime();
		lastRender = now;
		surfaceState = builtinUniforms.updateSurface(width, height, now);
		if (surfaceState.renderTargetsChanged()) {
			renderPipeline.releaseTargets();
		}

		resetFps();
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		var surfaceProgram = programManager.getSurfaceProgram();
		var mainProgram = programManager.getMainProgram();
		var surfaceBindings = programManager.getSurfaceBindings();
		if (surfaceProgram == null ||
				mainProgram == null ||
				surfaceBindings == null) {
			device.clear(GLES20.GL_COLOR_BUFFER_BIT |
					GLES20.GL_DEPTH_BUFFER_BIT);
			cancelCaptureThumbnail();
			return;
		}

		if (!renderPipeline.hasTargets()) {
			var targetErrors = renderPipeline.ensureTargets(
					context,
					surfaceState.renderWidth(),
					surfaceState.renderHeight(),
					programManager.getBackBufferParameters());
			if (!targetErrors.isEmpty()) {
				submitErrors(targetErrors);
			}
			if (!renderPipeline.hasTargets()) {
				cancelCaptureThumbnail();
				return;
			}
		}

		var frame = builtinUniforms.beginFrame(renderPipeline.getBackTexture());
		if (frame == null) {
			device.clear(GLES20.GL_COLOR_BUFFER_BIT |
					GLES20.GL_DEPTH_BUFFER_BIT);
			cancelCaptureThumbnail();
			return;
		}

		renderPipeline.renderMainPass(frame.bindings(), mainProgram);
		renderPipeline.renderSurfacePass(
				surfaceBindings,
				surfaceProgram,
				frame.surfaceWidth(),
				frame.surfaceHeight());
		renderPipeline.swapTargets();
		captureThumbnail();

		if (onRendererListener != null) {
			updateFps(frame.now());
		}

		builtinUniforms.endFrame();
	}

	public void unregisterListeners() {
		builtinUniforms.release();
	}

	public void touchAt(MotionEvent e) {
		builtinUniforms.updateTouch(e);
	}

	public void setOffset(float x, float y) {
		builtinUniforms.updateOffset(x, y);
	}

	public byte[] getThumbnail() {
		synchronized (thumbnailLock) {
			captureThumbnail = true;
			try {
				thumbnailLock.wait(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				captureThumbnail = false;
				return null;
			}
			return thumbnail;
		}
	}

	private void resetFps() {
		sum = 0;
		samples = 0;
		lastFps = 0;
		nextFpsUpdate = 0;
	}

	private void submitErrors(@NonNull List<ShaderError> errors) {
		if (onRendererListener != null) {
			onRendererListener.onInfoLog(errors);
		}
	}

	private void discardContextResources() {
		builtinUniforms.clearConfiguration();
		renderPipeline.discardContextResources();
		programManager.discardContextResources();
	}

	private void captureThumbnail() {
		synchronized (thumbnailLock) {
			var surfaceBindings = programManager.getSurfaceBindings();
			var surfaceProgram = programManager.getSurfaceProgram();
			if (captureThumbnail &&
					surfaceBindings != null &&
					surfaceProgram != null) {
				thumbnail = renderPipeline.captureThumbnail(
						surfaceBindings,
						surfaceProgram);
				captureThumbnail = false;
				thumbnailLock.notifyAll();
			}
		}
	}

	private void cancelCaptureThumbnail() {
		synchronized (thumbnailLock) {
			if (captureThumbnail) {
				captureThumbnail = false;
				thumbnailLock.notifyAll();
			}
		}
	}

	private void updateFps(long now) {
		long delta = now - lastRender;

		synchronized (this) {
			sum += Math.min(NS_PER_SECOND / delta, 60f);

			if (++samples > 0xffff) {
				sum = sum / samples;
				samples = 1;
			}
		}

		if (now > nextFpsUpdate) {
			int fps = Math.round(sum / samples);
			if (fps != lastFps && onRendererListener != null) {
				onRendererListener.onFramesPerSecond(fps);
				lastFps = fps;
			}
			nextFpsUpdate = now + FPS_UPDATE_FREQUENCY_NS;
		}

		lastRender = now;
	}
}
