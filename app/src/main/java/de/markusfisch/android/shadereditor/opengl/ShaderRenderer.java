package de.markusfisch.android.shadereditor.opengl;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.fragment.AbstractSamplerPropertiesFragment;
import de.markusfisch.android.shadereditor.hardware.AccelerometerListener;
import de.markusfisch.android.shadereditor.hardware.CameraListener;
import de.markusfisch.android.shadereditor.hardware.GravityListener;
import de.markusfisch.android.shadereditor.hardware.GyroscopeListener;
import de.markusfisch.android.shadereditor.hardware.MagneticFieldListener;
import de.markusfisch.android.shadereditor.hardware.LightListener;
import de.markusfisch.android.shadereditor.hardware.LinearAccelerationListener;
import de.markusfisch.android.shadereditor.hardware.PressureListener;
import de.markusfisch.android.shadereditor.hardware.ProximityListener;
import de.markusfisch.android.shadereditor.hardware.RotationVectorListener;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.BatteryManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.lang.IllegalArgumentException;
import java.lang.InterruptedException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ShaderRenderer implements GLSurfaceView.Renderer {
	public interface OnRendererListener {
		void onInfoLog(String error);
		void onFramesPerSecond(int fps);
	}

	public static final String UNIFORM_BACKBUFFER = "backbuffer";
	public static final String UNIFORM_BATTERY = "battery";
	public static final String UNIFORM_CAMERA_ADDENT = "cameraAddent";
	public static final String UNIFORM_CAMERA_BACK = "cameraBack";
	public static final String UNIFORM_CAMERA_FRONT = "cameraFront";
	public static final String UNIFORM_CAMERA_ORIENTATION = "cameraOrientation";
	public static final String UNIFORM_DATE = "date";
	public static final String UNIFORM_FRAME_NUMBER = "frame";
	public static final String UNIFORM_FTIME = "ftime";
	public static final String UNIFORM_GRAVITY = "gravity";
	public static final String UNIFORM_GYROSCOPE = "gyroscope";
	public static final String UNIFORM_LIGHT = "light";
	public static final String UNIFORM_LINEAR = "linear";
	public static final String UNIFORM_MAGNETIC = "magnetic";
	public static final String UNIFORM_MOUSE = "mouse";
	public static final String UNIFORM_OFFSET = "offset";
	public static final String UNIFORM_ORIENTATION = "orientation";
	public static final String UNIFORM_INCLINATION = "inclination";
	public static final String UNIFORM_INCLINATION_MATRIX = "inclinationMatrix";
	public static final String UNIFORM_POINTERS = "pointers";
	public static final String UNIFORM_POINTER_COUNT = "pointerCount";
	public static final String UNIFORM_POSITION = "position";
	public static final String UNIFORM_PRESSURE = "pressure";
	public static final String UNIFORM_PROXIMITY = "proximity";
	public static final String UNIFORM_RESOLUTION = "resolution";
	public static final String UNIFORM_ROTATION_MATRIX = "rotationMatrix";
	public static final String UNIFORM_ROTATION_VECTOR = "rotationVector";
	public static final String UNIFORM_SECOND = "second";
	public static final String UNIFORM_START_RANDOM = "startRandom";
	public static final String UNIFORM_SUB_SECOND = "subsecond";
	public static final String UNIFORM_TIME = "time";
	public static final String UNIFORM_TOUCH = "touch";

	private static final int TEXTURE_UNITS[] = {
			GLES20.GL_TEXTURE0,
			GLES20.GL_TEXTURE1,
			GLES20.GL_TEXTURE2,
			GLES20.GL_TEXTURE3,
			GLES20.GL_TEXTURE4,
			GLES20.GL_TEXTURE5,
			GLES20.GL_TEXTURE6,
			GLES20.GL_TEXTURE7,
			GLES20.GL_TEXTURE8,
			GLES20.GL_TEXTURE9,
			GLES20.GL_TEXTURE10,
			GLES20.GL_TEXTURE11,
			GLES20.GL_TEXTURE12,
			GLES20.GL_TEXTURE13,
			GLES20.GL_TEXTURE14,
			GLES20.GL_TEXTURE15,
			GLES20.GL_TEXTURE16,
			GLES20.GL_TEXTURE17,
			GLES20.GL_TEXTURE18,
			GLES20.GL_TEXTURE19,
			GLES20.GL_TEXTURE20,
			GLES20.GL_TEXTURE21,
			GLES20.GL_TEXTURE22,
			GLES20.GL_TEXTURE23,
			GLES20.GL_TEXTURE24,
			GLES20.GL_TEXTURE25,
			GLES20.GL_TEXTURE26,
			GLES20.GL_TEXTURE27,
			GLES20.GL_TEXTURE28,
			GLES20.GL_TEXTURE29,
			GLES20.GL_TEXTURE30,
			GLES20.GL_TEXTURE31};
	private static final int CUBE_MAP_TARGETS[] = {
			// all sides of a cube are stored in a single
			// rectangular source image for compactness:
			//
			//      +----+         +----+----+
			//     /| -Z |         | -X | -Z |
			// -X + +----+      >  +----+----+      +----+----+
			//    |/ -Y /               | -Y |      | -X | -Z |
			//    +----+                +----+      +----+----+
			//                                   >  | +Y | -Y |
			//       +----+        +----+           +----+----+
			//      / +Y /|        | +Y |           | +Z | +X |
			//     +----+ + +X  >  +----+----+      +----+----+
			//     | +Z |/         | +Z | +X |
			//     +----+          +----+----+
			//
			// so, from left to right, top to bottom:
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_NEGATIVE_X,
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z,
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_POSITIVE_Z,
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_POSITIVE_X};
	private static final float NS_PER_SECOND = 1000000000f;
	private static final long FPS_UPDATE_FREQUENCY_NS = 200000000L;
	private static final long BATTERY_UPDATE_INTERVAL = 10000000000L;
	private static final long DATE_UPDATE_INTERVAL = 1000000000L;
	private static final String SAMPLER_2D = "2D";
	private static final String SAMPLER_CUBE = "Cube";
	private static final String SAMPLER_EXTERNAL_OES = "ExternalOES";
	private static final Pattern PATTERN_SAMPLER = Pattern.compile(
			String.format(
					"uniform[ \t]+sampler(" +
							SAMPLER_2D + "|" +
							SAMPLER_CUBE + "|" +
							SAMPLER_EXTERNAL_OES +
							")+[ \t]+(%s);[ \t]*(.*)",
					AbstractSamplerPropertiesFragment.TEXTURE_NAME_PATTERN));
	private static final Pattern PATTERN_FTIME = Pattern.compile(
			"^#define[ \\t]+FTIME_PERIOD[ \\t]+([0-9\\.]+)[ \\t]*$",
			Pattern.MULTILINE);
	private static final String OES_EXTERNAL =
			"#extension GL_OES_EGL_image_external : require\n";
	private static final String VERTEX_SHADER =
			"attribute vec2 position;" +
					"void main() {" +
					"gl_Position = vec4(position, 0., 1.);" +
					"}";
	private static final String VERTEX_SHADER_300 =
			"#version 300 es\n" +
					"in vec2 position;" +
					"void main() {" +
					"gl_Position = vec4(position, 0., 1.);" +
					"}";
	private static final String FRAGMENT_SHADER =
			"#ifdef GL_FRAGMENT_PRECISION_HIGH\n" +
					"precision highp float;\n" +
					"#else\n" +
					"precision mediump float;\n" +
					"#endif\n" +
					"uniform vec2 resolution;" +
					"uniform sampler2D frame;" +
					"void main(void) {" +
					"gl_FragColor = texture2D(frame," +
					"gl_FragCoord.xy / resolution.xy).rgba;" +
					"}";

	private final TextureBinder textureBinder = new TextureBinder();
	private final ArrayList<String> textureNames = new ArrayList<>();
	private final ArrayList<TextureParameters> textureParameters =
			new ArrayList<>();
	private final BackBufferParameters backBufferTextureParams =
			new BackBufferParameters();
	private final int fb[] = new int[]{0, 0};
	private final int tx[] = new int[]{0, 0};
	private final int textureLocs[] = new int[32];
	private final int textureTargets[] = new int[32];
	private final int textureIds[] = new int[32];
	private final float surfaceResolution[] = new float[]{0, 0};
	private final float resolution[] = new float[]{0, 0};
	private final float touch[] = new float[]{0, 0};
	private final float mouse[] = new float[]{0, 0};
	private final float pointers[] = new float[30];
	private final float offset[] = new float[]{0, 0};
	private final float dateTime[] = new float[]{0, 0, 0, 0};
	private final float rotationMatrix[] = new float[9];
	private final float inclinationMatrix[] = new float[9];
	private final float orientation[] = new float[]{0, 0, 0};
	private final Context context;
	private final ByteBuffer vertexBuffer;

	private AccelerometerListener accelerometerListener;
	private CameraListener cameraListener;
	private GravityListener gravityListener;
	private GyroscopeListener gyroscopeListener;
	private MagneticFieldListener magneticFieldListener;
	private LightListener lightListener;
	private LinearAccelerationListener linearAccelerationListener;
	private PressureListener pressureListener;
	private ProximityListener proximityListener;
	private RotationVectorListener rotationVectorListener;
	private OnRendererListener onRendererListener;
	private String fragmentShader;
	private int version = 2;
	private int deviceRotation;
	private int surfaceProgram = 0;
	private int surfacePositionLoc;
	private int surfaceResolutionLoc;
	private int surfaceFrameLoc;
	private int program = 0;
	private int positionLoc;
	private int timeLoc;
	private int secondLoc;
	private int subSecondLoc;
	private int frameNumLoc;
	private int fTimeLoc;
	private int resolutionLoc;
	private int touchLoc;
	private int mouseLoc;
	private int pointerCountLoc;
	private int pointersLoc;
	private int gravityLoc;
	private int linearLoc;
	private int gyroscopeLoc;
	private int magneticLoc;
	private int rotationMatrixLoc;
	private int rotationVectorLoc;
	private int orientationLoc;
	private int inclinationMatrixLoc;
	private int inclinationLoc;
	private int lightLoc;
	private int pressureLoc;
	private int proximityLoc;
	private int offsetLoc;
	private int batteryLoc;
	private int dateTimeLoc;
	private int startRandomLoc;
	private int backBufferLoc;
	private int cameraOrientationLoc;
	private int cameraAddentLoc;
	private int numberOfTextures;
	private int pointerCount;
	private int frontTarget;
	private int backTarget = 1;
	private int frameNum;
	private long startTime;
	private long lastRender;
	private long lastBatteryUpdate;
	private long lastDateUpdate;
	private float batteryLevel;
	private float quality = 1f;
	private float startRandom;
	private float fTimeMax;
	private float gravityValues[];
	private float linearValues[];

	private volatile byte thumbnail[] = new byte[1];
	private volatile long nextFpsUpdate = 0;
	private volatile float sum;
	private volatile float samples;
	private volatile int lastFps;

	public ShaderRenderer(Context context) {
		this.context = context;

		vertexBuffer = ByteBuffer.allocateDirect(8);
		vertexBuffer.put(new byte[]{
				-1, 1,
				-1, -1,
				1, 1,
				1, -1}).position(0);
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public void setFragmentShader(String source, float quality) {
		setQuality(quality);
		setFragmentShader(source);
	}

	private void setFragmentShader(String source) {
		fTimeMax = parseFTime(source);
		resetFps();
		fragmentShader = indexTextureNames(source);
	}

	public void setQuality(float quality) {
		this.quality = quality;
	}

	public void setOnRendererListener(OnRendererListener listener) {
		onRendererListener = listener;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glClearColor(0f, 0f, 0f, 1f);

		if (surfaceProgram != 0) {
			// Don't glDeleteProgram(surfaceProgram) because
			// GLSurfaceView::onPause() destroys the GL context
			// what also deletes all programs.
			// With glDeleteProgram():
			// <core_glDeleteProgram:594>: GL_INVALID_VALUE
			surfaceProgram = 0;
		}

		if (program != 0) {
			// Don't glDeleteProgram(program);
			// same as above
			program = 0;
			deleteTargets();
		}

		if (fragmentShader != null && fragmentShader.length() > 0) {
			resetFps();
			createTextures();
			loadPrograms();
			indexLocations();
			enableAttribArrays();
			registerListeners();
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		startTime = lastRender = System.nanoTime();
		startRandom = (float) Math.random();
		frameNum = 0;

		surfaceResolution[0] = width;
		surfaceResolution[1] = height;
		deviceRotation = getDeviceRotation(context);

		float w = Math.round(width * quality);
		float h = Math.round(height * quality);

		if (w != resolution[0] || h != resolution[1]) {
			deleteTargets();
		}

		resolution[0] = w;
		resolution[1] = h;

		resetFps();
		openCameraListener();
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		if (surfaceProgram == 0 || program == 0) {
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT |
					GLES20.GL_DEPTH_BUFFER_BIT);

			return;
		}

		final long now = System.nanoTime();

		GLES20.glUseProgram(program);
		GLES20.glVertexAttribPointer(
				positionLoc,
				2,
				GLES20.GL_BYTE,
				false,
				0,
				vertexBuffer);

		float delta = (now - startTime) / NS_PER_SECOND;

		if (timeLoc > -1) {
			GLES20.glUniform1f(
					timeLoc,
					delta);
		}

		if (secondLoc > -1) {
			GLES20.glUniform1i(
					secondLoc,
					(int) delta);
		}

		if (subSecondLoc > -1) {
			GLES20.glUniform1f(
					subSecondLoc,
					delta - (int) delta);
		}

		if (frameNumLoc > -1) {
			GLES20.glUniform1i(
					frameNumLoc,
					frameNum);
		}

		if (fTimeLoc > -1) {
			GLES20.glUniform1f(
					fTimeLoc,
					((delta % fTimeMax) / fTimeMax * 2f - 1f));
		}

		if (resolutionLoc > -1) {
			GLES20.glUniform2fv(
					resolutionLoc,
					1,
					resolution,
					0);
		}

		if (touchLoc > -1) {
			GLES20.glUniform2fv(
					touchLoc,
					1,
					touch,
					0);
		}

		if (mouseLoc > -1) {
			GLES20.glUniform2fv(
					mouseLoc,
					1,
					mouse,
					0);
		}

		if (pointerCountLoc > -1) {
			GLES20.glUniform1i(
					pointerCountLoc,
					pointerCount);
		}

		if (pointersLoc > -1) {
			GLES20.glUniform3fv(
					pointersLoc,
					pointerCount,
					pointers,
					0);
		}

		if (gravityLoc > -1 && gravityValues != null) {
			GLES20.glUniform3fv(
					gravityLoc,
					1,
					gravityValues,
					0);
		}

		if (linearLoc > -1 && linearValues != null) {
			GLES20.glUniform3fv(
					linearLoc,
					1,
					linearValues,
					0);
		}

		if (gyroscopeLoc > -1 && gyroscopeListener != null) {
			GLES20.glUniform3fv(
					gyroscopeLoc,
					1,
					gyroscopeListener.rotation,
					0);
		}

		if (magneticLoc > -1 && magneticFieldListener != null) {
			GLES20.glUniform3fv(
					magneticLoc,
					1,
					magneticFieldListener.values,
					0);
		}

		if ((rotationMatrixLoc > -1 || orientationLoc > -1 ||
				inclinationMatrixLoc > -1 || inclinationLoc > -1) &&
				gravityValues != null) {
			setRotationMatrix();
		}

		if (lightLoc > -1 && lightListener != null) {
			GLES20.glUniform1f(
					lightLoc,
					lightListener.getAmbient());
		}

		if (pressureLoc > -1 && pressureListener != null) {
			GLES20.glUniform1f(
					pressureLoc,
					pressureListener.getPressure());
		}

		if (proximityLoc > -1 && proximityListener != null) {
			GLES20.glUniform1f(
					proximityLoc,
					proximityListener.getCentimeters());
		}

		if (rotationVectorLoc > -1 && rotationVectorListener != null) {
			GLES20.glUniform3fv(
					rotationVectorLoc,
					1,
					rotationVectorListener.values,
					0);
		}

		if (offsetLoc > -1) {
			GLES20.glUniform2fv(
					offsetLoc,
					1,
					offset,
					0);
		}

		if (batteryLoc > -1) {
			if (now - lastBatteryUpdate > BATTERY_UPDATE_INTERVAL) {
				// profiled getBatteryLevel() on slow/old devices
				// and it can take up to 6ms, so better do that
				// not for every frame but only once in a while
				batteryLevel = getBatteryLevel();
				lastBatteryUpdate = now;
			}

			GLES20.glUniform1f(
					batteryLoc,
					batteryLevel);
		}

		if (dateTimeLoc > -1) {
			if (now - lastDateUpdate > DATE_UPDATE_INTERVAL) {
				Calendar calendar = Calendar.getInstance();
				dateTime[0] = calendar.get(Calendar.YEAR);
				dateTime[1] = calendar.get(Calendar.MONTH);
				dateTime[2] = calendar.get(Calendar.DAY_OF_MONTH);
				dateTime[3] = calendar.get(Calendar.HOUR_OF_DAY) * 3600f +
						calendar.get(Calendar.MINUTE) * 60f +
						calendar.get(Calendar.SECOND);

				lastDateUpdate = now;
			}

			GLES20.glUniform4fv(
					dateTimeLoc,
					1,
					dateTime,
					0);
		}

		if (startRandomLoc > -1) {
			GLES20.glUniform1f(
					startRandomLoc,
					startRandom);
		}

		if (fb[0] == 0) {
			createTargets(
					(int) resolution[0],
					(int) resolution[1]);
		}

		// first draw custom shader in framebuffer
		GLES20.glViewport(
				0,
				0,
				(int) resolution[0],
				(int) resolution[1]);

		textureBinder.reset();

		if (backBufferLoc > -1) {
			textureBinder.bind(
					backBufferLoc,
					GLES20.GL_TEXTURE_2D,
					tx[backTarget]);
		}

		if (cameraListener != null) {
			if (cameraOrientationLoc > -1) {
				GLES20.glUniformMatrix2fv(
						cameraOrientationLoc,
						1,
						false,
						cameraListener.getOrientationMatrix());
			}

			if (cameraAddentLoc > -1) {
				GLES20.glUniform2fv(
						cameraAddentLoc,
						1,
						cameraListener.addent,
						0);
			}

			cameraListener.update();
		}

		for (int i = 0; i < numberOfTextures; ++i) {
			textureBinder.bind(
					textureLocs[i],
					textureTargets[i],
					textureIds[i]);
		}

		GLES20.glBindFramebuffer(
				GLES20.GL_FRAMEBUFFER,
				fb[frontTarget]);

		GLES20.glDrawArrays(
				GLES20.GL_TRIANGLE_STRIP,
				0,
				4);

		// then draw framebuffer on screen
		GLES20.glBindFramebuffer(
				GLES20.GL_FRAMEBUFFER,
				0);

		GLES20.glViewport(
				0,
				0,
				(int) surfaceResolution[0],
				(int) surfaceResolution[1]);

		GLES20.glUseProgram(surfaceProgram);

		GLES20.glVertexAttribPointer(
				surfacePositionLoc,
				2,
				GLES20.GL_BYTE,
				false,
				0,
				vertexBuffer);

		GLES20.glUniform2fv(
				surfaceResolutionLoc,
				1,
				surfaceResolution,
				0);

		GLES20.glUniform1i(surfaceFrameLoc, 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(
				GLES20.GL_TEXTURE_2D,
				tx[frontTarget]);

		GLES20.glClear(
				GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glDrawArrays(
				GLES20.GL_TRIANGLE_STRIP,
				0,
				4);

		// swap buffers so the next image will be rendered
		// over the current backbuffer and the current image
		// will be the backbuffer for the next image
		int t = frontTarget;
		frontTarget = backTarget;
		backTarget = t;

		if (thumbnail == null) {
			thumbnail = saveThumbnail();
		}

		if (onRendererListener != null) {
			updateFps(now);
		}

		++frameNum;
	}

	public void unregisterListeners() {
		if (accelerometerListener != null) {
			accelerometerListener.unregister();
			gravityValues = linearValues = null;
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

		unregisterCameraListener();
	}

	public void touchAt(MotionEvent e) {
		float x = e.getX() * quality;
		float y = e.getY() * quality;

		touch[0] = x;
		touch[1] = resolution[1] - y;

		// to be compatible with http://glslsandbox.com/
		mouse[0] = x / resolution[0];
		mouse[1] = 1 - y / resolution[1];

		switch (e.getActionMasked()) {
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				pointerCount = 0;
				return;
		}

		pointerCount = Math.min(
				e.getPointerCount(),
				pointers.length / 3);

		for (int i = 0, offset = 0; i < pointerCount; ++i) {
			pointers[offset++] = e.getX(i) * quality;
			pointers[offset++] = resolution[1] - e.getY(i) * quality;
			pointers[offset++] = e.getTouchMajor(i);
		}
	}

	public void setOffset(float x, float y) {
		offset[0] = x;
		offset[1] = y;
	}

	public byte[] getThumbnail() {
		// settings thumbnail to null triggers
		// the capture on the OpenGL thread in
		// onDrawFrame()
		thumbnail = null;

		try {
			for (int trys = 10;
					trys-- > 0 && program > 0 && thumbnail == null; ) {
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// don't clone() because the data doesn't need to be
		// protected from modification what means copying would
		// only mean using more memory than necessary
		return thumbnail;
	}

	private void resetFps() {
		sum = samples = 0;
		lastFps = 0;
		nextFpsUpdate = 0;
	}

	private void loadPrograms() {
		String vertexShader = fragmentShader.contains("#version 300") &&
				version == 3 ? VERTEX_SHADER_300 : VERTEX_SHADER;

		if (((surfaceProgram = Program.loadProgram(
						VERTEX_SHADER,
						FRAGMENT_SHADER)) == 0 ||
				(program = Program.loadProgram(
						vertexShader,
						fragmentShader)) == 0) &&
				onRendererListener != null) {
			onRendererListener.onInfoLog(Program.getInfoLog());
		}
	}

	private void indexLocations() {
		surfacePositionLoc = GLES20.glGetAttribLocation(
				surfaceProgram, "position");
		surfaceResolutionLoc = GLES20.glGetUniformLocation(
				surfaceProgram, "resolution");
		surfaceFrameLoc = GLES20.glGetUniformLocation(
				surfaceProgram, "frame");

		positionLoc = GLES20.glGetAttribLocation(
				program, UNIFORM_POSITION);
		timeLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_TIME);
		secondLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_SECOND);
		subSecondLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_SUB_SECOND);
		frameNumLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_FRAME_NUMBER);
		fTimeLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_FTIME);
		resolutionLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_RESOLUTION);
		touchLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_TOUCH);
		mouseLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_MOUSE);
		pointerCountLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_POINTER_COUNT);
		pointersLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_POINTERS);
		gravityLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_GRAVITY);
		gyroscopeLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_GYROSCOPE);
		linearLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_LINEAR);
		magneticLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_MAGNETIC);
		rotationMatrixLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_ROTATION_MATRIX);
		rotationVectorLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_ROTATION_VECTOR);
		orientationLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_ORIENTATION);
		inclinationMatrixLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_INCLINATION_MATRIX);
		inclinationLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_INCLINATION);
		lightLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_LIGHT);
		pressureLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_PRESSURE);
		proximityLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_PROXIMITY);
		offsetLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_OFFSET);
		batteryLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_BATTERY);
		dateTimeLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_DATE);
		startRandomLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_START_RANDOM);
		backBufferLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_BACKBUFFER);
		cameraOrientationLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_CAMERA_ORIENTATION);
		cameraAddentLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_CAMERA_ADDENT);

		for (int i = numberOfTextures; i-- > 0; ) {
			textureLocs[i] = GLES20.glGetUniformLocation(
					program,
					textureNames.get(i));
		}
	}

	private void enableAttribArrays() {
		GLES20.glEnableVertexAttribArray(surfacePositionLoc);
		GLES20.glEnableVertexAttribArray(positionLoc);
	}

	private void registerListeners() {
		if (gravityLoc > -1 || rotationMatrixLoc > -1 ||
				orientationLoc > -1 || inclinationMatrixLoc > -1 ||
				inclinationLoc > -1) {
			if (gravityListener == null) {
				gravityListener = new GravityListener(context);
			}
			if (!gravityListener.register()) {
				gravityListener = null;
				AccelerometerListener l = getAccelerometerListener();
				gravityValues = l != null ? l.gravity : null;
			} else {
				gravityValues = gravityListener.values;
			}
		}

		if (linearLoc > -1) {
			if (linearAccelerationListener == null) {
				linearAccelerationListener =
						new LinearAccelerationListener(context);
			}
			if (!linearAccelerationListener.register()) {
				linearAccelerationListener = null;
				AccelerometerListener l = getAccelerometerListener();
				linearValues = l != null ? l.linear : null;
			} else {
				linearValues = linearAccelerationListener.values;
			}
		}

		if (gyroscopeLoc > -1) {
			if (gyroscopeListener == null) {
				gyroscopeListener = new GyroscopeListener(context);
			}
			if (!gyroscopeListener.register()) {
				gyroscopeListener = null;
			}
		}

		if (magneticLoc > -1 || rotationMatrixLoc > -1 ||
				orientationLoc > -1 || inclinationMatrixLoc > -1 ||
				inclinationLoc > -1) {
			if (magneticFieldListener == null) {
				magneticFieldListener = new MagneticFieldListener(context);
			}
			if (!magneticFieldListener.register()) {
				magneticFieldListener = null;
			}
		}

		if (lightLoc > -1) {
			if (lightListener == null) {
				lightListener = new LightListener(context);
			}
			if (!lightListener.register()) {
				lightListener = null;
			}
		}

		if (pressureLoc > -1) {
			if (pressureListener == null) {
				pressureListener = new PressureListener(context);
			}
			if (!pressureListener.register()) {
				pressureListener = null;
			}
		}

		if (proximityLoc > -1) {
			if (proximityListener == null) {
				proximityListener = new ProximityListener(context);
			}
			if (!proximityListener.register()) {
				proximityListener = null;
			}
		}

		if (rotationVectorLoc > -1 || (magneticFieldListener == null &&
				(orientationLoc > -1 || rotationMatrixLoc > -1))) {
			if (rotationVectorListener == null) {
				rotationVectorListener = new RotationVectorListener(context);
			}
			if (!rotationVectorListener.register()) {
				rotationVectorListener = null;
			}
		}
	}

	private AccelerometerListener getAccelerometerListener() {
		if (accelerometerListener == null) {
			accelerometerListener = new AccelerometerListener(context);
		}
		if (!accelerometerListener.register()) {
			return null;
		}
		return accelerometerListener;
	}

	private byte[] saveThumbnail() {
		final int min = (int) Math.min(
				surfaceResolution[0],
				surfaceResolution[1]);
		final int pixels = min * min;
		final int rgba[] = new int[pixels];
		final int bgra[] = new int[pixels];
		final IntBuffer colorBuffer = IntBuffer.wrap(rgba);

		GLES20.glReadPixels(
				0,
				0,
				min,
				min,
				GLES20.GL_RGBA,
				GLES20.GL_UNSIGNED_BYTE,
				colorBuffer);

		for (int i = 0, e = pixels; i < pixels; ) {
			e -= min;

			for (int x = min, b = e; x-- > 0; ++i, ++b) {
				final int c = rgba[i];

				bgra[b] = ((c >> 16) & 0xff) |
						((c << 16) & 0xff0000) |
						(c & 0xff00ff00);
			}
		}

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Bitmap.createScaledBitmap(
					Bitmap.createBitmap(
							bgra,
							min,
							min,
							Bitmap.Config.ARGB_8888),
					144,
					144,
					true).compress(
					Bitmap.CompressFormat.PNG,
					100,
					out);

			return out.toByteArray();
		} catch (IllegalArgumentException e) {
			// will never happen because neither
			// width nor height <= 0
			return null;
		}
	}

	private void setRotationMatrix() {
		boolean haveInclination = false;
		if (gravityListener != null && magneticFieldListener != null &&
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
		if (deviceRotation != 0) {
			int x = SensorManager.AXIS_Y;
			int y = SensorManager.AXIS_MINUS_X;
			switch (deviceRotation) {
				default:
					break;
				case 270:
					x = SensorManager.AXIS_MINUS_Y;
					y = SensorManager.AXIS_X;
					break;
			}
			SensorManager.remapCoordinateSystem(
					rotationMatrix,
					x,
					y,
					rotationMatrix);
		}
		if (rotationMatrixLoc > -1) {
			GLES20.glUniformMatrix3fv(
					rotationMatrixLoc,
					1,
					true,
					rotationMatrix,
					0);
		}
		if (orientationLoc > -1) {
			SensorManager.getOrientation(rotationMatrix, orientation);
			GLES20.glUniform3fv(
					orientationLoc,
					1,
					orientation,
					0);
		}
		if (inclinationMatrixLoc > -1 && haveInclination) {
			GLES20.glUniformMatrix3fv(
					inclinationMatrixLoc,
					1,
					true,
					inclinationMatrix,
					0);
		}
		if (inclinationLoc > -1 && haveInclination) {
			GLES20.glUniform1f(
					inclinationLoc,
					SensorManager.getInclination(inclinationMatrix));
		}
	}

	private void updateFps(long now) {
		long delta = now - lastRender;

		// because sum and samples are volatile
		synchronized (this) {
			sum += Math.min(NS_PER_SECOND / delta, 60f);

			if (++samples > 0xffff) {
				sum = sum / samples;
				samples = 1;
			}
		}

		if (now > nextFpsUpdate) {
			int fps = Math.round(sum / samples);

			if (fps != lastFps) {
				onRendererListener.onFramesPerSecond(fps);
				lastFps = fps;
			}

			nextFpsUpdate = now + FPS_UPDATE_FREQUENCY_NS;
		}

		lastRender = now;
	}

	private void deleteTargets() {
		if (fb[0] == 0) {
			return;
		}

		GLES20.glDeleteFramebuffers(2, fb, 0);
		GLES20.glDeleteTextures(2, tx, 0);

		fb[0] = 0;
	}

	private void createTargets(int width, int height) {
		deleteTargets();

		GLES20.glGenFramebuffers(2, fb, 0);
		GLES20.glGenTextures(2, tx, 0);

		createTarget(frontTarget, width, height, backBufferTextureParams);
		createTarget(backTarget, width, height, backBufferTextureParams);

		// unbind textures that were bound in createTarget()
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	}

	private void createTarget(
			int idx,
			int width,
			int height,
			BackBufferParameters tp) {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tx[idx]);

		boolean useBitmap = tp.setPresetBitmap(width, height);
		if (!useBitmap) {
			GLES20.glTexImage2D(
					GLES20.GL_TEXTURE_2D,
					0,
					GLES20.GL_RGBA,
					width,
					height,
					0,
					GLES20.GL_RGBA,
					GLES20.GL_UNSIGNED_BYTE,
					null);
		}

		tp.setParameters(GLES20.GL_TEXTURE_2D);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

		GLES20.glBindFramebuffer(
				GLES20.GL_FRAMEBUFFER,
				fb[idx]);
		GLES20.glFramebufferTexture2D(
				GLES20.GL_FRAMEBUFFER,
				GLES20.GL_COLOR_ATTACHMENT0,
				GLES20.GL_TEXTURE_2D,
				tx[idx],
				0);

		if (!useBitmap) {
			// clear texture because some drivers
			// don't initialize texture memory
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT |
					GLES20.GL_DEPTH_BUFFER_BIT);
		}
	}

	private void deleteTextures() {
		if (textureIds[0] == 1 || numberOfTextures < 1) {
			return;
		}
		GLES20.glDeleteTextures(numberOfTextures, textureIds, 0);
	}

	private void createTextures() {
		deleteTextures();
		GLES20.glGenTextures(numberOfTextures, textureIds, 0);

		for (int i = 0; i < numberOfTextures; ++i) {
			String name = textureNames.get(i);

			if (UNIFORM_CAMERA_BACK.equals(name) ||
					UNIFORM_CAMERA_FRONT.equals(name)) {
				// handled in onSurfaceChanged() because we need
				// the dimensions of the surface to pick a preview
				// resolution
				continue;
			}

			Bitmap bitmap = ShaderEditorApp.db.getTextureBitmap(name);

			if (bitmap == null) {
				continue;
			}

			switch (textureTargets[i]) {
				default:
					continue;
				case GLES20.GL_TEXTURE_2D:
					createTexture(textureIds[i], bitmap,
							textureParameters.get(i));
					break;
				case GLES20.GL_TEXTURE_CUBE_MAP:
					createCubeTexture(textureIds[i], bitmap,
							textureParameters.get(i));
					break;
			}

			bitmap.recycle();
		}
	}

	private void createTexture(
			int id,
			Bitmap bitmap,
			TextureParameters tp) {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
		tp.setParameters(GLES20.GL_TEXTURE_2D);
		tp.setBitmap(bitmap);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
	}

	private void createCubeTexture(
			int id,
			Bitmap bitmap,
			TextureParameters tp) {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, id);
		tp.setParameters(GLES20.GL_TEXTURE_CUBE_MAP);

		int bitmapWidth = bitmap.getWidth();
		int bitmapHeight = bitmap.getHeight();
		int sideWidth = (int) Math.ceil(bitmapWidth / 2f);
		int sideHeight = Math.round(bitmapHeight / 3f);
		int sideLength = Math.min(sideWidth, sideHeight);
		int x = 0;
		int y = 0;

		for (int target : CUBE_MAP_TARGETS) {
			Bitmap side = Bitmap.createBitmap(
					bitmap,
					x,
					y,
					// cube textures need to be quadratic
					sideLength,
					sideLength,
					// don't flip cube textures
					null,
					true);

			GLUtils.texImage2D(
					target,
					0,
					GLES20.GL_RGBA,
					side,
					GLES20.GL_UNSIGNED_BYTE,
					0);

			side.recycle();

			if ((x += sideWidth) >= bitmapWidth) {
				x = 0;
				y += sideHeight;
			}
		}

		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_CUBE_MAP);
	}

	private void unregisterCameraListener() {
		if (cameraListener != null) {
			cameraListener.unregister();
			cameraListener = null;
		}
	}

	private void openCameraListener() {
		unregisterCameraListener();

		for (int i = 0; i < numberOfTextures; ++i) {
			String name = textureNames.get(i);

			if (UNIFORM_CAMERA_BACK.equals(name) ||
					UNIFORM_CAMERA_FRONT.equals(name)) {
				openCameraListener(name, textureIds[i],
						textureParameters.get(i));

				// only one camera can be opened at a time
				break;
			}
		}
	}

	private void openCameraListener(
			String name,
			int id,
			TextureParameters tp) {
		if (Build.VERSION.SDK_INT <
				Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			return;
		}

		int cameraId = CameraListener.findCameraId(
				UNIFORM_CAMERA_BACK.equals(name) ?
						Camera.CameraInfo.CAMERA_FACING_BACK :
						Camera.CameraInfo.CAMERA_FACING_FRONT);

		if (cameraId < 0) {
			return;
		}

		if (cameraListener == null ||
				cameraListener.cameraId != cameraId) {
			unregisterCameraListener();
			requestCameraPermission();
			setCameraTextureProperties(id, tp);
			cameraListener = new CameraListener(
					id,
					cameraId,
					(int) resolution[0],
					(int) resolution[1],
					deviceRotation);
		}

		cameraListener.register();
	}

	private void requestCameraPermission() {
		String permission = android.Manifest.permission.CAMERA;
		if (ContextCompat.checkSelfPermission(context, permission) !=
				PackageManager.PERMISSION_GRANTED) {
			Activity activity;
			try {
				activity = (Activity) context;
			} catch (ClassCastException e) {
				return;
			}
			ActivityCompat.requestPermissions(
					activity,
					new String[]{permission},
					1);
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
	private static void setCameraTextureProperties(
			int id,
			TextureParameters tp) {
		if (Build.VERSION.SDK_INT <
				Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			return;
		}

		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, id);
		tp.setParameters(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
	}

	private static float parseFTime(String source) {
		if (source != null) {
			Matcher m = PATTERN_FTIME.matcher(source);
			if (m.find() && m.groupCount() > 0) {
				return Float.parseFloat(m.group(1));
			}
		}
		return 3f;
	}

	private String indexTextureNames(String source) {
		if (source == null) {
			return null;
		}

		textureNames.clear();
		textureParameters.clear();
		numberOfTextures = 0;
		backBufferTextureParams.reset();

		final int maxTextures = textureIds.length;

		for (Matcher m = PATTERN_SAMPLER.matcher(source);
				m.find() && numberOfTextures < maxTextures; ) {
			String type = m.group(1);
			String name = m.group(2);
			String params = m.group(3);

			if (type == null || name == null) {
				continue;
			}

			if (UNIFORM_BACKBUFFER.equals(name)) {
				backBufferTextureParams.parse(params);
				continue;
			}

			int target;

			switch (type) {
				case SAMPLER_2D:
					target = GLES20.GL_TEXTURE_2D;
					break;
				case SAMPLER_CUBE:
					target = GLES20.GL_TEXTURE_CUBE_MAP;
					break;
				case SAMPLER_EXTERNAL_OES:
					if (Build.VERSION.SDK_INT >
							Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
						// needs to be done here or lint won't recognize
						// we're checking SDK version
						target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
					} else {
						// ignore that uniform on lower SDKs
						continue;
					}
					if (!source.contains(OES_EXTERNAL)) {
						source = addPreprocessorDirective(source,
								OES_EXTERNAL);
					}
					break;
				default:
					continue;
			}

			textureTargets[numberOfTextures++] = target;
			textureNames.add(name);
			textureParameters.add(new TextureParameters(params));
		}

		return source;
	}

	private static String addPreprocessorDirective(String source,
			String directive) {
		// #version must always be the very first directive
		if (source.trim().startsWith("#version")) {
			int lf = source.indexOf("\n");
			if (lf < 0) {
				// no line break?
				return source;
			}
			++lf;
			return source.substring(0, lf) +
					directive +
					source.substring(lf);
		}
		return directive + source;
	}

	private float getBatteryLevel() {
		Intent batteryStatus = context.registerReceiver(
				null,
				new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		if (batteryStatus == null) {
			return 0;
		}

		int level = batteryStatus.getIntExtra(
				BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(
				BatteryManager.EXTRA_SCALE, -1);

		return (float) level / scale;
	}

	private static int getDeviceRotation(Context context) {
		switch (((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay()
				.getRotation()) {
			default:
			case Surface.ROTATION_0:
				return 0;
			case Surface.ROTATION_90:
				return 90;
			case Surface.ROTATION_180:
				return 180;
			case Surface.ROTATION_270:
				return 270;
		}
	}

	private static class TextureBinder {
		private int index;

		private void reset() {
			index = 0;
		}

		private void bind(int loc, int target, int textureId) {
			if (loc < 0 || index >= TEXTURE_UNITS.length) {
				return;
			}

			GLES20.glUniform1i(loc, index);
			GLES20.glActiveTexture(TEXTURE_UNITS[index]);
			GLES20.glBindTexture(target, textureId);

			++index;
		}
	}
}
