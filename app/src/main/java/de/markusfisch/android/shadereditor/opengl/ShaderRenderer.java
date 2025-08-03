package de.markusfisch.android.shadereditor.opengl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.fragment.AbstractSamplerPropertiesFragment;
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

	private static final int[] TEXTURE_UNITS = {
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
	private static final int[] CUBE_MAP_TARGETS = {
			// All sides of a cube are stored in a single
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
			// So, from left to right, top to bottom:
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
			"^#define[ \\t]+FTIME_PERIOD[ \\t]+([0-9.]+)[ \\t]*$",
			Pattern.MULTILINE);
	private static final Pattern PATTERN_GLES3_VERSION = Pattern.compile(
			"^#version 3[0-9]{2} es$", Pattern.MULTILINE);
	private static final String OES_EXTERNAL =
			"#extension GL_OES_EGL_image_external : require\n";
	private static final String OES_EXTERNAL_ESS3 =
			"#extension GL_OES_EGL_image_external_essl3 : require\n";
	private static final String SHADER_EDITOR =
			"#define SHADER_EDITOR 1\n";
	private static final String VERTEX_SHADER = """
			attribute vec2 position;
			void main() {
				gl_Position = vec4(position, 0., 1.);
			}
			""";
	private static final String VERTEX_SHADER_3 = """
			in vec2 position;
			void main() {
				gl_Position = vec4(position, 0., 1.);
			}
			""";
	private static final String FRAGMENT_SHADER = """
			#ifdef GL_FRAGMENT_PRECISION_HIGH
			precision highp float;
			#else
			precision mediump float;
			#endif
			
			uniform vec2 resolution;
			uniform sampler2D frame;
			void main(void) {
				gl_FragColor = texture2D(frame,gl_FragCoord.xy / resolution.xy).rgba;
			}
			""";

	private final TextureBinder textureBinder = new TextureBinder();
	private final ArrayList<String> textureNames = new ArrayList<>();
	private final ArrayList<TextureParameters> textureParameters =
			new ArrayList<>();
	private final BackBufferParameters backBufferTextureParams =
			new BackBufferParameters();
	private final int[] fb = new int[]{0, 0};
	private final int[] tx = new int[]{0, 0};
	private final int[] textureLocs = new int[32];
	private final int[] textureTargets = new int[32];
	private final int[] textureIds = new int[32];
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
	private final Context context;
	private final ByteBuffer vertexBuffer;

	private AccelerometerListener accelerometerListener;
	private CameraListener cameraListener;
	private GravityListener gravityListener;
	private GyroscopeListener gyroscopeListener;
	private MagneticFieldListener magneticFieldListener;
	private MicInputListener micInputListener;
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
	private int touchStartLoc;
	private int mouseLoc;
	private int nightModeLoc;
	private int notificationCountLoc;
	private int pointerCountLoc;
	private int pointersLoc;
	private int powerConnectedLoc;
	private int gravityLoc;
	private int linearLoc;
	private int gyroscopeLoc;
	private int magneticLoc;
	private int rotationMatrixLoc;
	private int rotationVectorLoc;
	private int orientationLoc;
	private int inclinationMatrixLoc;
	private int inclinationLoc;
	private int lastNotificationTimeLoc;
	private int lightLoc;
	private int pressureLoc;
	private int proximityLoc;
	private int offsetLoc;
	private int batteryLoc;
	private int dateTimeLoc;
	private int daytimeLoc;
	private int startRandomLoc;
	private int backBufferLoc;
	private int cameraOrientationLoc;
	private int cameraAddentLoc;
	private int mediaVolumeLoc;
	private int micAmplitudeLoc;
	private int nightMode;
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
	private float[] gravityValues;
	private float[] linearValues;

	private static final int THUMBNAIL_WIDTH = 144;
	private static final int THUMBNAIL_HEIGHT = 144;
	private final int[] thumbnailFb = new int[1];
	private final int[] thumbnailTx = new int[1];
	private final Object thumbnailLock = new Object();
	private boolean captureThumbnail = false;

	private byte[] thumbnail = new byte[1];
	private volatile long nextFpsUpdate = 0;
	private volatile float sum;
	private volatile float samples;
	private volatile int lastFps;

	public ShaderRenderer(Context context) {
		this.context = context;

		// -1/1   1/1
		//    +---+
		//    |  /|
		//    | 0 |
		//    |/  |
		//    +---+
		// -1/-1  1/-1
		vertexBuffer = ByteBuffer.allocateDirect(8);
		vertexBuffer.put(new byte[]{
				-1, 1, // left top
				-1, -1, // left bottom
				1, 1, // right top, first triangle from last 3 vertices
				1, -1 // right bottom, second triangle from last 3 vertices
		}).position(0);
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

		// Re-create thumbnail framebuffer on each surface creation to ensure a clean state.
		GLES20.glDeleteFramebuffers(1, thumbnailFb, 0);
		GLES20.glDeleteTextures(1, thumbnailTx, 0);
		GLES20.glGenFramebuffers(1, thumbnailFb, 0);
		GLES20.glGenTextures(1, thumbnailTx, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, thumbnailTx[0]);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
				THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, 0, GLES20.GL_RGBA,
				GLES20.GL_UNSIGNED_BYTE, null);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
				GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
				GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, thumbnailFb[0]);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
				GLES20.GL_TEXTURE_2D, thumbnailTx[0], 0);

		// Unbind to be safe.
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

		if (surfaceProgram != 0) {
			// Don't glDeleteProgram(surfaceProgram) because
			// GLSurfaceView::onPause() destroys the GL context
			// what also deletes all programs.
			// With glDeleteProgram():
			// <core_glDeleteProgram:594>: GL_INVALID_VALUE
			surfaceProgram = 0;
		}

		if (program != 0) {
			// Don't glDeleteProgram(program).
			// Same as above.
			program = 0;
			deleteTargets();
		}

		if (fragmentShader != null && !fragmentShader.isEmpty()) {
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
			captureThumbnail();
			return;
		}

		GLES20.glUseProgram(program);
		GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_BYTE,
				false, 0, vertexBuffer);

		final long now = System.nanoTime();
		float delta = (now - startTime) / NS_PER_SECOND;

		if (timeLoc > -1) {
			GLES20.glUniform1f(timeLoc, delta);
		}
		if (secondLoc > -1) {
			GLES20.glUniform1i(secondLoc, (int) delta);
		}
		if (subSecondLoc > -1) {
			GLES20.glUniform1f(subSecondLoc, delta - (int) delta);
		}
		if (frameNumLoc > -1) {
			GLES20.glUniform1i(frameNumLoc, frameNum);
		}
		if (fTimeLoc > -1) {
			GLES20.glUniform1f(fTimeLoc,
					((delta % fTimeMax) / fTimeMax * 2f - 1f));
		}
		if (resolutionLoc > -1) {
			GLES20.glUniform2fv(resolutionLoc, 1, resolution, 0);
		}
		if (touchLoc > -1) {
			GLES20.glUniform2fv(touchLoc, 1, touch, 0);
		}
		if (touchStartLoc > -1) {
			GLES20.glUniform2fv(touchStartLoc, 1, touchStart, 0);
		}
		if (mouseLoc > -1) {
			GLES20.glUniform2fv(mouseLoc, 1, mouse, 0);
		}
		if (nightModeLoc > -1) {
			GLES20.glUniform1i(nightModeLoc, nightMode);
		}
		if (notificationCountLoc > -1) {
			GLES20.glUniform1i(notificationCountLoc, NotificationService.getCount());
		}
		if (pointerCountLoc > -1) {
			GLES20.glUniform1i(pointerCountLoc, pointerCount);
		}
		if (pointersLoc > -1) {
			GLES20.glUniform3fv(pointersLoc, pointerCount, pointers, 0);
		}
		if (gravityLoc > -1 && gravityValues != null) {
			GLES20.glUniform3fv(gravityLoc, 1, gravityValues, 0);
		}
		if (linearLoc > -1 && linearValues != null) {
			GLES20.glUniform3fv(linearLoc, 1, linearValues, 0);
		}
		if (gyroscopeLoc > -1 && gyroscopeListener != null) {
			GLES20.glUniform3fv(gyroscopeLoc, 1, gyroscopeListener.rotation, 0);
		}
		if (magneticLoc > -1 && magneticFieldListener != null) {
			GLES20.glUniform3fv(magneticLoc, 1, magneticFieldListener.values, 0);
		}
		if ((rotationMatrixLoc > -1 || orientationLoc > -1 ||
				inclinationMatrixLoc > -1 || inclinationLoc > -1) &&
				gravityValues != null) {
			setRotationMatrix();
		}
		if (lastNotificationTimeLoc > -1) {
			Long lastTime = NotificationService.getLastNotificationTime();
			if (lastTime == null) {
				GLES20.glUniform1f(lastNotificationTimeLoc, Float.NaN);
			} else {
				float millisPerSecond = 1.f / 1000.f;
				GLES20.glUniform1f(lastNotificationTimeLoc,
						(System.currentTimeMillis() - lastTime) * millisPerSecond);
			}
		}
		if (lightLoc > -1 && lightListener != null) {
			GLES20.glUniform1f(lightLoc, lightListener.getAmbient());
		}
		if (pressureLoc > -1 && pressureListener != null) {
			GLES20.glUniform1f(pressureLoc, pressureListener.getPressure());
		}
		if (proximityLoc > -1 && proximityListener != null) {
			GLES20.glUniform1f(proximityLoc, proximityListener.getCentimeters());
		}
		if (rotationVectorLoc > -1 && rotationVectorListener != null) {
			GLES20.glUniform3fv(rotationVectorLoc, 1,
					rotationVectorListener.values, 0);
		}
		if (offsetLoc > -1) {
			GLES20.glUniform2fv(offsetLoc, 1, offset, 0);
		}
		if (batteryLoc > -1) {
			if (now - lastBatteryUpdate > BATTERY_UPDATE_INTERVAL) {
				// Profiled getBatteryLevel() on slow/old devices
				// and it can take up to 6ms. So better do that
				// not for every frame but only once in a while.
				batteryLevel = getBatteryLevel();
				lastBatteryUpdate = now;
			}
			GLES20.glUniform1f(batteryLoc, batteryLevel);
		}
		if (powerConnectedLoc > -1) {
			GLES20.glUniform1i(powerConnectedLoc,
					ShaderEditorApp.preferences.isPowerConnected() ? 1 : 0);
		}
		if (dateTimeLoc > -1 || daytimeLoc > -1) {
			if (now - lastDateUpdate > DATE_UPDATE_INTERVAL) {
				Calendar calendar = Calendar.getInstance();
				if (dateTimeLoc > -1) {
					dateTime[0] = calendar.get(Calendar.YEAR);
					dateTime[1] = calendar.get(Calendar.MONTH);
					dateTime[2] = calendar.get(Calendar.DAY_OF_MONTH);
					dateTime[3] = calendar.get(Calendar.HOUR_OF_DAY) * 3600f +
							calendar.get(Calendar.MINUTE) * 60f +
							calendar.get(Calendar.SECOND);
				}
				if (daytimeLoc > -1) {
					daytime[0] = calendar.get(Calendar.HOUR_OF_DAY);
					daytime[1] = calendar.get(Calendar.MINUTE);
					daytime[2] = calendar.get(Calendar.SECOND);
				}
				lastDateUpdate = now;
			}
			if (dateTimeLoc > -1) {
				GLES20.glUniform4fv(dateTimeLoc, 1, dateTime, 0);
			}
			if (daytimeLoc > -1) {
				GLES20.glUniform3fv(daytimeLoc, 1, daytime, 0);
			}
		}
		if (startRandomLoc > -1) {
			GLES20.glUniform1f(startRandomLoc, startRandom);
		}
		if (mediaVolumeLoc > -1) {
			GLES20.glUniform1f(mediaVolumeLoc, getMediaVolumeLevel(context));
		}
		if (micAmplitudeLoc > -1 && micInputListener != null) {
			GLES20.glUniform1f(micAmplitudeLoc, micInputListener.getAmplitude());
		}

		if (fb[0] == 0) {
			createTargets((int) resolution[0], (int) resolution[1]);
		}

		// First draw custom shader in framebuffer.
		GLES20.glViewport(0, 0, (int) resolution[0], (int) resolution[1]);

		textureBinder.reset();

		if (backBufferLoc > -1) {
			textureBinder.bind(backBufferLoc, GLES20.GL_TEXTURE_2D,
					tx[backTarget]);
		}
		if (cameraListener != null) {
			if (cameraOrientationLoc > -1) {
				GLES20.glUniformMatrix2fv(cameraOrientationLoc, 1, false,
						cameraListener.getOrientationMatrix());
			}
			if (cameraAddentLoc > -1) {
				GLES20.glUniform2fv(cameraAddentLoc, 1,
						cameraListener.addent, 0);
			}
			cameraListener.update();
		}

		for (int i = 0; i < numberOfTextures; ++i) {
			textureBinder.bind(
					textureLocs[i],
					textureTargets[i],
					textureIds[i]);
		}

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[frontTarget]);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		// then draw framebuffer on screen
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0,
				(int) surfaceResolution[0],
				(int) surfaceResolution[1]);
		GLES20.glUseProgram(surfaceProgram);
		GLES20.glVertexAttribPointer(surfacePositionLoc, 2, GLES20.GL_BYTE,
				false, 0, vertexBuffer);

		GLES20.glUniform2fv(surfaceResolutionLoc, 1, surfaceResolution, 0);

		GLES20.glUniform1i(surfaceFrameLoc, 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tx[frontTarget]);

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		// Swap buffers so the next image will be rendered
		// over the current backbuffer and the current image
		// will be the backbuffer for the next image.
		int t = frontTarget;
		frontTarget = backTarget;
		backTarget = t;

		captureThumbnail();

		if (onRendererListener != null) {
			updateFps(now);
		}

		++frameNum;
	}

	private void captureThumbnail() {
		// Check if a capture is requested and notify the waiting thread.
		synchronized (thumbnailLock) {
			if (captureThumbnail) {
				thumbnail = saveThumbnail();
				captureThumbnail = false;
				thumbnailLock.notifyAll();
			}
		}
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
		if (micInputListener != null) {
			micInputListener.unregister();
			micInputListener = null;
		}
	}

	public void touchAt(MotionEvent e) {
		float x = e.getX() * quality;
		float y = e.getY() * quality;

		touch[0] = x;
		touch[1] = resolution[1] - y;

		// To be compatible with http://glslsandbox.com/
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
		synchronized (thumbnailLock) {
			captureThumbnail = true;
			try {
				// Wait for the GL thread to capture and notify.
				thumbnailLock.wait(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				// Explicitly set the flag to false to avoid a stray capture later.
				captureThumbnail = false;
				return null;
			}
			return thumbnail;
		}
	}

	private void resetFps() {
		sum = samples = 0;
		lastFps = 0;
		nextFpsUpdate = 0;
	}

	private void loadPrograms() {
		// Attempt to load the surface program
		surfaceProgram = Program.loadProgram(VERTEX_SHADER, FRAGMENT_SHADER);

		// If the surface program fails to compile, submit errors and return
		if (surfaceProgram == 0) {
			submitErrors(Program.getInfoLog());
			return;
		}

		// Attempt to load the main program
		program = Program.loadProgram(getVertexShader(), fragmentShader);

		// If the main program fails to compile, submit errors and return
		if (program == 0) {
			submitErrors(Program.getInfoLog());
			return;
		}

		// If both programs compiled successfully, log an empty list of errors
		submitErrors(Collections.emptyList());
	}

	// Helper method to submit program errors
	private void submitErrors(@NonNull List<ShaderError> errors) {
		if (onRendererListener != null) {
			onRendererListener.onInfoLog(errors);
		}
	}

	private String getVertexShader() {
		String version = getGLES3Version(fragmentShader);
		return version != null
				? version + "\n" + VERTEX_SHADER_3
				: VERTEX_SHADER;
	}

	private String getGLES3Version(String source) {
		Matcher m = PATTERN_GLES3_VERSION.matcher(source);
		return version == 3 && m.find() ? m.group(0) : null;
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
		touchStartLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_TOUCH_START);
		mouseLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_MOUSE);
		nightModeLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_NIGHT_MODE);
		notificationCountLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_NOTIFICATION_COUNT);
		pointerCountLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_POINTER_COUNT);
		pointersLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_POINTERS);
		powerConnectedLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_POWER_CONNECTED);
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
		lastNotificationTimeLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_LAST_NOTIFICATION_TIME);
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
		daytimeLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_DAYTIME);
		startRandomLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_START_RANDOM);
		backBufferLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_BACKBUFFER);
		cameraOrientationLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_CAMERA_ORIENTATION);
		cameraAddentLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_CAMERA_ADDENT);
		mediaVolumeLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_MEDIA_VOLUME);
		micAmplitudeLoc = GLES20.glGetUniformLocation(
				program, UNIFORM_MIC_AMPLITUDE);

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

		if (nightModeLoc > -1) {
			nightMode = (context.getResources().getConfiguration().uiMode &
					Configuration.UI_MODE_NIGHT_MASK) ==
					Configuration.UI_MODE_NIGHT_YES ? 1 : 0;
		}

		if (lightLoc > -1) {
			if (lightListener == null) {
				lightListener = new LightListener(context);
			}
			if (!lightListener.register()) {
				lightListener = null;
			}
		}

		if (notificationCountLoc > -1 || lastNotificationTimeLoc > -1) {
			NotificationService.requirePermissions(context);
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

		if (micAmplitudeLoc > -1) {
			if (micInputListener == null) {
				micInputListener = new MicInputListener(context);
			}
			if (!micInputListener.register()) {
				micInputListener = null;
				requestRecordAudioPermission();
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

	@Nullable
	private byte[] saveThumbnail() {
		// 1. Bind the thumbnail FBO and set the viewport.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, thumbnailFb[0]);
		GLES20.glViewport(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

		// 2. Use the surface program to draw the main scene texture (downsampling it).
		GLES20.glUseProgram(surfaceProgram);
		GLES20.glVertexAttribPointer(surfacePositionLoc, 2, GLES20.GL_BYTE,
				false, 0, vertexBuffer);

		// Use a dedicated resolution vector for the thumbnail size.
		float[] thumbnailResolution = {THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT};
		GLES20.glUniform2fv(surfaceResolutionLoc, 1, thumbnailResolution, 0);

		GLES20.glUniform1i(surfaceFrameLoc, 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tx[frontTarget]);

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		// 3. Read the much smaller buffer of pixels.
		final int pixels = THUMBNAIL_WIDTH * THUMBNAIL_HEIGHT;
		final int[] rgba = new int[pixels];
		final IntBuffer buffer = IntBuffer.wrap(rgba);
		GLES20.glReadPixels(
				0, 0,
				THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT,
				GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
				buffer);

		// 4. Restore the default framebuffer to not affect main rendering.
		// The viewport will be reset in the next onDrawFrame anyway.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

		// 5. Vertically flip the image and swap R/B channels on the CPU.
		// This is much faster now because it's only a 144x144 image.
		var argb = new int[pixels];
		for (int y = 0; y < THUMBNAIL_HEIGHT; ++y) {
			for (int x = 0; x < THUMBNAIL_WIDTH; ++x) {
				int srcIdx = y * THUMBNAIL_WIDTH + x;
				int destIdx = (THUMBNAIL_HEIGHT - y - 1) * THUMBNAIL_WIDTH + x;
				int pixel = rgba[srcIdx]; // AABBGGRR from OpenGL
				// Swap R and B to get AARRGGBB for Android Bitmap
				argb[destIdx] = (pixel & 0xff00ff00)
						| ((pixel << 16) & 0x00ff0000)
						| ((pixel >> 16) & 0x000000ff);
			}
		}

		// 6. Create Bitmap from processed pixels and compress to PNG.
		try (var out = new ByteArrayOutputStream()) {
			Bitmap.createBitmap(
					argb,
					THUMBNAIL_WIDTH,
					THUMBNAIL_HEIGHT,
					Bitmap.Config.ARGB_8888).compress(
					Bitmap.CompressFormat.PNG,
					100,
					out);
			return out.toByteArray();
			// Has to catch IOException because ByteArrayOutputStream#close's
			// signature includes it.
		} catch (OutOfMemoryError | IllegalArgumentException | IOException e) {
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
		if (deviceRotation != Surface.ROTATION_0) {
			// Suppress the warning for this block, because the logic is intentional.
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
		if (rotationMatrixLoc > -1) {
			GLES20.glUniformMatrix3fv(rotationMatrixLoc, 1, true,
					rotationMatrix, 0);
		}
		if (orientationLoc > -1) {
			SensorManager.getOrientation(rotationMatrix, orientation);
			GLES20.glUniform3fv(orientationLoc, 1, orientation, 0);
		}
		if (inclinationMatrixLoc > -1 && haveInclination) {
			GLES20.glUniformMatrix3fv(inclinationMatrixLoc, 1, true,
					inclinationMatrix, 0);
		}
		if (inclinationLoc > -1 && haveInclination) {
			GLES20.glUniform1f(inclinationLoc,
					SensorManager.getInclination(inclinationMatrix));
		}
	}

	private void updateFps(long now) {
		long delta = now - lastRender;

		// Because sum and samples are volatile.
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

		Bitmap bitmap = tp.getPresetBitmap(context, width, height);
		if (bitmap != null) {
			setTexture(bitmap);
		} else {
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

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[idx]);
		GLES20.glFramebufferTexture2D(
				GLES20.GL_FRAMEBUFFER,
				GLES20.GL_COLOR_ATTACHMENT0,
				GLES20.GL_TEXTURE_2D,
				tx[idx],
				0);

		if (bitmap == null) {
			// Clear texture because some drivers
			// don't initialize texture memory.
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

		// Get the DataSource instance.
		DataSource dataSource = Database.getInstance(context).getDataSource();

		for (int i = 0; i < numberOfTextures; ++i) {
			String name = textureNames.get(i);
			if (UNIFORM_CAMERA_BACK.equals(name) ||
					UNIFORM_CAMERA_FRONT.equals(name)) {
				// Handled in onSurfaceChanged() because we need
				// the dimensions of the surface to pick a preview
				// resolution.
				continue;
			}

			Bitmap bitmap = dataSource.texture.getTextureBitmap(name);
			if (bitmap == null) {
				continue;
			}

			switch (textureTargets[i]) {
				case GLES20.GL_TEXTURE_2D:
					createTexture(textureIds[i], bitmap,
							textureParameters.get(i));
					break;
				case GLES20.GL_TEXTURE_CUBE_MAP:
					createCubeTexture(textureIds[i], bitmap,
							textureParameters.get(i));
					break;
				default:
					continue;
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
		setTexture(bitmap);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
	}

	private void setTexture(Bitmap bitmap) {
		String message = TextureParameters.setBitmap(bitmap);
		if (message != null && onRendererListener != null) {
			onRendererListener.onInfoLog(List.of(ShaderError.createGeneral(message)));
		}
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
					// Cube textures need to be quadratic.
					sideLength,
					sideLength,
					// Don't flip cube textures.
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

				// Only one camera can be opened at a time.
				break;
			}
		}
	}

	private void openCameraListener(
			String name,
			int id,
			TextureParameters tp) {
		int lensFacing = UNIFORM_CAMERA_BACK.equals(name)
				? CameraSelector.LENS_FACING_BACK
				: CameraSelector.LENS_FACING_FRONT;

		if (cameraListener == null ||
				cameraListener.facing != lensFacing) {
			unregisterCameraListener();
			requestCameraPermission();
			setCameraTextureProperties(id, tp);
			cameraListener = new CameraListener(
					id,
					lensFacing,
					(int) resolution[0],
					(int) resolution[1],
					deviceRotation,
					context);
		}

		cameraListener.register((LifecycleOwner) context);
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

	private void requestRecordAudioPermission() {
		String permission = android.Manifest.permission.RECORD_AUDIO;
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

	private static void setCameraTextureProperties(
			int id,
			TextureParameters tp) {
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, id);
		tp.setParameters(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
	}

	private static float parseFTime(String source) {
		if (source != null) {
			Matcher m = PATTERN_FTIME.matcher(source);
			String s;
			if (m.find() && m.groupCount() > 0 &&
					(s = m.group(1)) != null) {
				return Float.parseFloat(s);
			}
		}
		return 3f;
	}

	private String indexTextureNames(String source) {
		ShaderError.resetSilentlyAddedExtraLines();
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
				case SAMPLER_EXTERNAL_OES: {
					target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
					String pattern = getGLES3Version(source) != null
							? OES_EXTERNAL_ESS3
							: OES_EXTERNAL;
					if (!source.contains(pattern)) {
						source = addPreprocessorDirective(source, pattern);
					}
					break;
				}
				default:
					continue;
			}

			textureTargets[numberOfTextures++] = target;
			textureNames.add(name);
			textureParameters.add(new TextureParameters(params));
		}

		if (!source.contains(SHADER_EDITOR)) {
			source = addPreprocessorDirective(source, SHADER_EDITOR);
		}

		return source;
	}

	private static String addPreprocessorDirective(String source,
			String directive) {
		ShaderError.addSilentlyAddedExtraLine();
		// #version must always be the very first directive.
		if (source.trim().startsWith("#version")) {
			int lf = source.indexOf("\n");
			if (lf < 0) {
				// No line break?
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

	private static int getDeviceRotation(@NonNull Context context) {
		WindowManager wm = (WindowManager) context.getSystemService(
				Context.WINDOW_SERVICE);
		if (wm == null) {
			return 0;
		}

		return wm.getDefaultDisplay().getRotation();
	}

	private static float getMediaVolumeLevel(Context context) {
		AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		float maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float currVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
		if (maxVolume <= 0 || currVolume < 0) {
			return 0;
		}
		return currVolume / maxVolume;
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
