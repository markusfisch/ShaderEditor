package de.markusfisch.android.shadereditor.opengl;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.fragment.SamplerPropertiesFragment;
import de.markusfisch.android.shadereditor.hardware.AccelerometerListener;
import de.markusfisch.android.shadereditor.hardware.GyroscopeListener;
import de.markusfisch.android.shadereditor.hardware.MagneticFieldListener;
import de.markusfisch.android.shadereditor.hardware.LightListener;
import de.markusfisch.android.shadereditor.hardware.PressureListener;
import de.markusfisch.android.shadereditor.hardware.ProximityListener;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.BatteryManager;
import android.view.MotionEvent;

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

public class ShaderRenderer implements GLSurfaceView.Renderer
{
	public interface OnRendererListener
	{
		void onInfoLog( String error );
		void onFramesPerSecond( int fps );
	}

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
		GLES20.GL_TEXTURE31 };
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
		GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_POSITIVE_X };
	private static final float NS_PER_SECOND = 1000000000f;
	private static final long FPS_UPDATE_FREQUENCY_NS = 200000000L;
	private static final long BATTERY_UPDATE_INTERVAL = 10000000000L;
	private static final long DATE_UPDATE_INTERVAL = 1000000000L;
	private static final Pattern SAMPLER = Pattern.compile(
		String.format(
			"uniform[ \t]+sampler(2D|Cube)+[ \t]+(%s);",
			SamplerPropertiesFragment.TEXTURE_NAME_PATTERN ) );
	private static final String VERTEX_SHADER =
		"attribute vec2 position;"+
		"void main()"+
		"{"+
			"gl_Position = vec4( position, 0., 1. );"+
		"}";
	private static final String FRAGMENT_SHADER =
		"#ifdef GL_FRAGMENT_PRECISION_HIGH\n"+
		"precision highp float;\n"+
		"#else\n"+
		"precision mediump float;\n"+
		"#endif\n"+
		"uniform vec2 resolution;"+
		"uniform sampler2D frame;"+
		"void main( void )"+
		"{"+
			"gl_FragColor = texture2D( "+
				"frame,"+
				"gl_FragCoord.xy/resolution.xy ).rgba;"+
		"}";

	private final TextureBinder textureBinder = new TextureBinder();
	private final ArrayList<String> textureNames = new ArrayList<>();
	private final Matrix flipMatrix = new Matrix();
	private final Calendar calendar = Calendar.getInstance();
	private final int fb[] = new int[]{ 0, 0 };
	private final int tx[] = new int[]{ 0, 0 };
	private final int textureLocs[] = new int[32];
	private final int textureTargets[] = new int[32];
	private final int textureIds[] = new int[32];
	private final float surfaceResolution[] = new float[]{ 0, 0 };
	private final float resolution[] = new float[]{ 0, 0 };
	private final float touch[] = new float[]{ 0, 0 };
	private final float mouse[] = new float[]{ 0, 0 };
	private final float pointers[] = new float[30];
	private final float offset[] = new float[]{ 0, 0 };
	private final float dateTime[] = new float[]{ 0, 0, 0, 0 };

	private Context context;
	private AccelerometerListener accelerometerListener;
	private GyroscopeListener gyroscopeListener;
	private MagneticFieldListener magneticFieldListener;
	private LightListener lightListener;
	private PressureListener pressureListener;
	private ProximityListener proximityListener;
	private OnRendererListener onRendererListener;
	private String fragmentShader;
	private ByteBuffer vertexBuffer;
	private int surfaceProgram = 0;
	private int surfacePositionLoc;
	private int surfaceResolutionLoc;
	private int surfaceFrameLoc;
	private int program = 0;
	private int positionLoc;
	private int timeLoc;
	private int resolutionLoc;
	private int touchLoc;
	private int mouseLoc;
	private int pointerCountLoc;
	private int pointersLoc;
	private int gravityLoc;
	private int linearLoc;
	private int rotationLoc;
	private int magneticLoc;
	private int lightLoc;
	private int pressureLoc;
	private int proximityLoc;
	private int offsetLoc;
	private int batteryLoc;
	private int dateTimeLoc;
	private int startRandomLoc;
	private int backBufferLoc;
	private int numberOfTextures = 0;
	private int pointerCount;
	private int frontTarget = 0;
	private int backTarget = 1;
	private long startTime;
	private long lastRender;
	private long lastBatteryUpdate;
	private long lastDateUpdate;
	private float batteryLevel;
	private float quality = 1f;
	private float startRandom;

	private volatile byte thumbnail[] = new byte[1];
	private volatile long nextFpsUpdate = 0;
	private volatile float sum;
	private volatile float samples;
	private volatile int lastFps;

	public ShaderRenderer( Context context )
	{
		this.context = context;

		flipMatrix.postScale( 1f, -1f );

		vertexBuffer = ByteBuffer.allocateDirect( 8 );
		vertexBuffer.put( new byte[]{
			-1, 1,
			-1, -1,
			1, 1,
			1, -1 } ).position( 0 );
	}

	public void setFragmentShader( String source, float quality )
	{
		setQuality( quality );
		setFragmentShader( source );
	}

	public void setFragmentShader( String source )
	{
		resetFps();
		fragmentShader = source;

		indexTextureNames( source );
	}

	public void setQuality( float quality )
	{
		this.quality = quality;
	}

	public void setOnRendererListener( OnRendererListener listener )
	{
		onRendererListener = listener;
	}

	@Override
	public void onSurfaceCreated( GL10 gl, EGLConfig config )
	{
		GLES20.glDisable( GLES20.GL_CULL_FACE );
		GLES20.glDisable( GLES20.GL_BLEND );
		GLES20.glDisable( GLES20.GL_DEPTH_TEST );

		GLES20.glClearColor( 0f, 0f, 0f, 1f );

		if( surfaceProgram != 0 )
		{
			// Don't glDeleteProgram( surfaceProgram ) because
			// GLSurfaceView::onPause() destroys the GL context
			// what also deletes all programs.
			// With glDeleteProgram():
			// <core_glDeleteProgram:594>: GL_INVALID_VALUE
			surfaceProgram = 0;
		}

		if( program != 0 )
		{
			// Don't glDeleteProgram( program );
			// same as above
			program = 0;

			deleteTargets();
		}

		if( fragmentShader != null &&
			fragmentShader.length() > 0 )
		{
			resetFps();
			createTextures();
			loadProgram();
		}
	}

	@Override
	public void onSurfaceChanged( GL10 gl, int width, int height )
	{
		startTime = lastRender = System.nanoTime();
		startRandom = (float)Math.random();

		surfaceResolution[0] = width;
		surfaceResolution[1] = height;

		float w = Math.round( width*quality );
		float h = Math.round( height*quality );

		if( w != resolution[0] ||
			h != resolution[1] )
			deleteTargets();

		resolution[0] = w;
		resolution[1] = h;

		resetFps();
	}

	@Override
	public void onDrawFrame( GL10 gl )
	{
		if( surfaceProgram == 0 ||
			program == 0 )
		{
			GLES20.glClear(
				GLES20.GL_COLOR_BUFFER_BIT |
				GLES20.GL_DEPTH_BUFFER_BIT );

			return;
		}

		final long now = System.nanoTime();

		GLES20.glUseProgram( program );

		GLES20.glVertexAttribPointer(
			positionLoc,
			2,
			GLES20.GL_BYTE,
			false,
			0,
			vertexBuffer );

		if( timeLoc > -1 )
			GLES20.glUniform1f(
				timeLoc,
				(now-startTime)/NS_PER_SECOND );

		if( resolutionLoc > -1 )
			GLES20.glUniform2fv(
				resolutionLoc,
				1,
				resolution,
				0 );

		if( touchLoc > -1 )
			GLES20.glUniform2fv(
				touchLoc,
				1,
				touch,
				0 );

		if( mouseLoc > -1 )
			GLES20.glUniform2fv(
				mouseLoc,
				1,
				mouse,
				0 );

		if( pointerCountLoc > -1 )
			GLES20.glUniform1i(
				pointerCountLoc,
				pointerCount );

		if( pointersLoc > -1 )
			GLES20.glUniform3fv(
				pointersLoc,
				pointerCount,
				pointers,
				0 );

		if( gravityLoc > -1 &&
			accelerometerListener != null )
			GLES20.glUniform3fv(
				gravityLoc,
				1,
				accelerometerListener.gravity,
				0 );

		if( linearLoc > -1 &&
			accelerometerListener != null )
			GLES20.glUniform3fv(
				linearLoc,
				1,
				accelerometerListener.linear,
				0 );

		if( rotationLoc > -1 &&
			gyroscopeListener != null )
			GLES20.glUniform3fv(
				rotationLoc,
				1,
				gyroscopeListener.rotation,
				0 );

		if( magneticLoc > -1 &&
			magneticFieldListener != null )
			GLES20.glUniform3fv(
				magneticLoc,
				1,
				magneticFieldListener.values,
				0 );

		if( lightLoc > -1 &&
			lightListener != null )
			GLES20.glUniform1f(
				lightLoc,
				lightListener.ambient );

		if( pressureLoc > -1 &&
			pressureListener != null )
			GLES20.glUniform1f(
				pressureLoc,
				pressureListener.pressure );

		if( proximityLoc > -1 &&
			proximityListener != null )
			GLES20.glUniform1f(
				proximityLoc,
				proximityListener.centimeters );

		if( offsetLoc > -1 )
			GLES20.glUniform2fv(
				offsetLoc,
				1,
				offset,
				0 );

		if( batteryLoc > -1 )
		{
			if( now-lastBatteryUpdate > BATTERY_UPDATE_INTERVAL )
			{
				// profiled getBatteryLevel() on slow/old devices
				// and it can take up to 6ms, so better do that
				// not for every frame but only once in a while
				batteryLevel = getBatteryLevel();
				lastBatteryUpdate = now;
			}

			GLES20.glUniform1f(
				batteryLoc,
				batteryLevel );
		}

		if( dateTimeLoc > -1 )
		{
			if( now-lastDateUpdate > DATE_UPDATE_INTERVAL )
			{
				dateTime[0] = calendar.get( Calendar.YEAR );
				dateTime[1] = calendar.get( Calendar.MONTH );
				dateTime[2] = calendar.get( Calendar.DAY_OF_MONTH );
				dateTime[3] =
					calendar.get( Calendar.HOUR_OF_DAY )*3600f+
					calendar.get( Calendar.MINUTE )*60f+
					calendar.get( Calendar.SECOND );

				lastDateUpdate = now;
			}

			GLES20.glUniform4fv(
				dateTimeLoc,
				1,
				dateTime,
				0 );
		}

		if( startRandomLoc > -1 )
			GLES20.glUniform1f(
				startRandomLoc,
				startRandom );

		if( fb[0] == 0 )
			createTargets(
				(int)resolution[0],
				(int)resolution[1] );

		// first draw custom shader in framebuffer
		GLES20.glViewport(
			0,
			0,
			(int)resolution[0],
			(int)resolution[1] );

		textureBinder.reset();

		if( backBufferLoc > -1 )
			textureBinder.bind(
				backBufferLoc,
				GLES20.GL_TEXTURE_2D,
				tx[backTarget] );

		for( int n = 0; n < numberOfTextures; ++n )
			textureBinder.bind(
				textureLocs[n],
				textureTargets[n],
				textureIds[n] );

		GLES20.glBindFramebuffer(
			GLES20.GL_FRAMEBUFFER,
			fb[frontTarget] );

		GLES20.glDrawArrays(
			GLES20.GL_TRIANGLE_STRIP,
			0,
			4 );

		// then draw framebuffer on screen
		GLES20.glBindFramebuffer(
			GLES20.GL_FRAMEBUFFER,
			0 );

		GLES20.glViewport(
			0,
			0,
			(int)surfaceResolution[0],
			(int)surfaceResolution[1] );

		GLES20.glUseProgram( surfaceProgram );

		GLES20.glVertexAttribPointer(
			surfacePositionLoc,
			2,
			GLES20.GL_BYTE,
			false,
			0,
			vertexBuffer );

		GLES20.glUniform2fv(
			surfaceResolutionLoc,
			1,
			surfaceResolution,
			0 );

		GLES20.glUniform1i( surfaceFrameLoc, 0 );
		GLES20.glActiveTexture( GLES20.GL_TEXTURE0 );
		GLES20.glBindTexture(
			GLES20.GL_TEXTURE_2D,
			tx[frontTarget] );

		GLES20.glClear(
			GLES20.GL_COLOR_BUFFER_BIT );
		GLES20.glDrawArrays(
			GLES20.GL_TRIANGLE_STRIP,
			0,
			4 );

		// swap buffers so the next image will be rendered
		// over the current backbuffer and the current image
		// will be the backbuffer for the next image
		int t = frontTarget;
		frontTarget = backTarget;
		backTarget = t;

		if( thumbnail == null )
			thumbnail = saveThumbnail();

		if( onRendererListener != null )
			updateFps( now );
	}

	public void unregisterListeners()
	{
		if( accelerometerListener != null )
			accelerometerListener.unregister();

		if( gyroscopeListener != null )
			gyroscopeListener.unregister();

		if( magneticFieldListener != null )
			magneticFieldListener.unregister();

		if( lightListener != null )
			lightListener.unregister();

		if( pressureListener != null )
			pressureListener.unregister();

		if( proximityListener != null )
			proximityListener.unregister();
	}

	public void touchAt( MotionEvent e )
	{
		float x = e.getX()*quality;
		float y = e.getY()*quality;

		touch[0] = x;
		touch[1] = resolution[1]-y;

		// to be compatible with http://glslsandbox.com/
		mouse[0] = x/resolution[0];
		mouse[1] = 1-y/resolution[1];

		switch( e.getActionMasked() )
		{
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				pointerCount = 0;
				return;
		}

		pointerCount = Math.min(
			e.getPointerCount(),
			pointers.length/3 );

		for( int n = 0, offset = 0;
			n < pointerCount;
			++n )
		{
			pointers[offset++] = e.getX( n )*quality;
			pointers[offset++] = resolution[1]-e.getY( n )*quality;
			pointers[offset++] = e.getTouchMajor( n );
		}
	}

	public void setOffset( float x, float y )
	{
		offset[0] = x;
		offset[1] = y;
	}

	public byte[] getThumbnail()
	{
		// settings thumbnail to null triggers
		// the capture on the OpenGL thread in
		// onDrawFrame()
		thumbnail = null;

		try
		{
			for( int trys = 10;
				trys-- > 0 &&
					program > 0 &&
					thumbnail == null; )
				Thread.sleep( 100 );
		}
		catch( InterruptedException e )
		{
			// thread got interrupted, ignore that
		}

		// don't clone() because the data doesn't need to be
		// protected from modification what means copying would
		// only mean using more memory than necessary
		return thumbnail;
	}

	private void resetFps()
	{
		sum = samples = 0;
		lastFps = 0;
		nextFpsUpdate = 0;
	}

	private void loadProgram()
	{
		if( (surfaceProgram = Program.loadProgram(
			VERTEX_SHADER,
			FRAGMENT_SHADER )) == 0 )
			return;

		if( (program = Program.loadProgram(
			VERTEX_SHADER,
			fragmentShader )) == 0 )
		{
			if( onRendererListener != null )
				onRendererListener.onInfoLog(
					Program.getInfoLog() );

			return;
		}

		indexLocations();

		GLES20.glEnableVertexAttribArray( surfacePositionLoc );
		GLES20.glEnableVertexAttribArray( positionLoc );

		registerListeners();
	}

	private void indexLocations()
	{
		surfacePositionLoc = GLES20.glGetAttribLocation(
			surfaceProgram, "position" );
		surfaceResolutionLoc = GLES20.glGetUniformLocation(
			surfaceProgram, "resolution" );
		surfaceFrameLoc = GLES20.glGetUniformLocation(
			surfaceProgram, "frame" );

		positionLoc = GLES20.glGetAttribLocation(
			program, "position" );
		timeLoc = GLES20.glGetUniformLocation(
			program, "time" );
		resolutionLoc = GLES20.glGetUniformLocation(
			program, "resolution" );
		touchLoc = GLES20.glGetUniformLocation(
			program, "touch" );
		mouseLoc = GLES20.glGetUniformLocation(
			program, "mouse" );
		pointerCountLoc = GLES20.glGetUniformLocation(
			program, "pointerCount" );
		pointersLoc = GLES20.glGetUniformLocation(
			program, "pointers" );
		gravityLoc = GLES20.glGetUniformLocation(
			program, "gravity" );
		linearLoc = GLES20.glGetUniformLocation(
			program, "linear" );
		rotationLoc = GLES20.glGetUniformLocation(
			program, "rotation" );
		magneticLoc = GLES20.glGetUniformLocation(
			program, "magnetic" );
		lightLoc = GLES20.glGetUniformLocation(
			program, "light" );
		pressureLoc = GLES20.glGetUniformLocation(
			program, "pressure" );
		proximityLoc = GLES20.glGetUniformLocation(
			program, "proximity" );
		offsetLoc = GLES20.glGetUniformLocation(
			program, "offset" );
		batteryLoc = GLES20.glGetUniformLocation(
			program, "battery" );
		dateTimeLoc = GLES20.glGetUniformLocation(
			program, "date" );
		startRandomLoc = GLES20.glGetUniformLocation(
			program, "startRandom" );
		backBufferLoc = GLES20.glGetUniformLocation(
			program, "backbuffer" );

		for( int n = numberOfTextures; n-- > 0; )
			textureLocs[n] = GLES20.glGetUniformLocation(
				program,
				textureNames.get( n ) );
	}

	private void registerListeners()
	{
		if( gravityLoc > -1 ||
			linearLoc > -1 )
		{
			if( accelerometerListener == null )
				accelerometerListener =
					new AccelerometerListener( context );

			accelerometerListener.register();
		}

		if( rotationLoc > -1 )
		{
			if( gyroscopeListener == null )
				gyroscopeListener =
					new GyroscopeListener( context );

			gyroscopeListener.register();
		}

		if( magneticLoc > -1 )
		{
			if( magneticFieldListener == null )
				magneticFieldListener =
					new MagneticFieldListener( context );

			magneticFieldListener.register();
		}

		if( lightLoc > -1 )
		{
			if( lightListener == null )
				lightListener =
					new LightListener( context );

			lightListener.register();
		}

		if( pressureLoc > -1 )
		{
			if( pressureListener == null )
				pressureListener =
					new PressureListener( context );

			pressureListener.register();
		}

		if( proximityLoc > -1 )
		{
			if( proximityListener == null )
				proximityListener =
					new ProximityListener( context );

			proximityListener.register();
		}
	}

	private byte[] saveThumbnail()
	{
		final int min = (int)Math.min(
			surfaceResolution[0],
			surfaceResolution[1] );
		final int pixels = min*min;
		final int rgba[] = new int[pixels];
		final int bgra[] = new int[pixels];
		final IntBuffer colorBuffer = IntBuffer.wrap( rgba );

		GLES20.glReadPixels(
			0,
			0,
			min,
			min,
			GLES20.GL_RGBA,
			GLES20.GL_UNSIGNED_BYTE,
			colorBuffer );

		for( int n = 0, e = pixels; n < pixels; )
		{
			e -= min;

			for( int x = min, b = e;
				x-- > 0;
				++n, ++b )
			{
				final int c = rgba[n];

				bgra[b] =
					((c >> 16) & 0xff) |
					((c << 16) & 0xff0000) |
					(c & 0xff00ff00);
			}
		}

		try
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Bitmap.createScaledBitmap(
				Bitmap.createBitmap(
					bgra,
					min,
					min,
					Bitmap.Config.ARGB_8888 ),
				144,
				144,
				true ).compress(
					Bitmap.CompressFormat.PNG,
					100,
					out );

			return out.toByteArray();
		}
		catch( IllegalArgumentException e )
		{
			// will never happen because neither
			// width nor height <= 0
			return null;
		}
	}

	private void updateFps( long now )
	{
		sum += Math.min( NS_PER_SECOND/(now-lastRender), 60f );

		if( ++samples > 0xffff )
		{
			sum = sum/samples;
			samples = 1;
		}

		if( now > nextFpsUpdate )
		{
			int fps = Math.round( sum/samples );

			if( fps != lastFps )
			{
				onRendererListener.onFramesPerSecond( fps );
				lastFps = fps;
			}

			nextFpsUpdate = now+FPS_UPDATE_FREQUENCY_NS;
		}

		lastRender = now;
	}

	private void deleteTargets()
	{
		if( fb[0] == 0 )
			return;

		GLES20.glDeleteFramebuffers( 2, fb, 0 );
		GLES20.glDeleteTextures( 2, tx, 0 );

		fb[0] = 0;
	}

	private void createTargets( int width, int height )
	{
		deleteTargets();

		GLES20.glGenFramebuffers( 2, fb, 0 );
		GLES20.glGenTextures( 2, tx, 0 );

		createTarget( frontTarget, width, height );
		createTarget( backTarget, width, height );

		// unbind textures that were bound in createTarget()
		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, 0 );
	}

	private void createTarget( int idx, int width, int height )
	{
		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, tx[idx] );
		GLES20.glTexImage2D(
			GLES20.GL_TEXTURE_2D,
			0,
			GLES20.GL_RGBA,
			width,
			height,
			0,
			GLES20.GL_RGBA,
			GLES20.GL_UNSIGNED_BYTE,
			null );

		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_2D,
			GLES20.GL_TEXTURE_WRAP_S,
			GLES20.GL_CLAMP_TO_EDGE );
		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_2D,
			GLES20.GL_TEXTURE_WRAP_T,
			GLES20.GL_CLAMP_TO_EDGE );

		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_2D,
			GLES20.GL_TEXTURE_MAG_FILTER,
			GLES20.GL_NEAREST );
		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_2D,
			GLES20.GL_TEXTURE_MIN_FILTER,
			GLES20.GL_NEAREST );

		GLES20.glBindFramebuffer(
			GLES20.GL_FRAMEBUFFER,
			fb[idx] );
		GLES20.glFramebufferTexture2D(
			GLES20.GL_FRAMEBUFFER,
			GLES20.GL_COLOR_ATTACHMENT0,
			GLES20.GL_TEXTURE_2D,
			tx[idx],
			0 );

		// clear texture because some drivers
		// don't initialize texture memory
		GLES20.glClear(
			GLES20.GL_COLOR_BUFFER_BIT |
			GLES20.GL_DEPTH_BUFFER_BIT );
	}

	private void deleteTextures()
	{
		if( textureIds[0] == 1 ||
			numberOfTextures < 1 )
			return;

		GLES20.glDeleteTextures(
			numberOfTextures,
			textureIds,
			0 );
	}

	private void createTextures()
	{
		deleteTextures();

		GLES20.glGenTextures(
			numberOfTextures,
			textureIds,
			0 );

		for( int n = 0; n < numberOfTextures; ++n )
		{
			Bitmap bitmap = ShaderEditorApplication
				.dataSource
				.getTextureBitmap( textureNames.get( n ) );

			if( bitmap == null )
				continue;

			switch( textureTargets[n] )
			{
				default:
					continue;
				case GLES20.GL_TEXTURE_2D:
					createTexture( textureIds[n], bitmap );
					break;
				case GLES20.GL_TEXTURE_CUBE_MAP:
					createCubeTexture( textureIds[n], bitmap );
					break;
			}

			bitmap.recycle();
		}
	}

	private void createTexture( int id, Bitmap bitmap )
	{
		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, id );

		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_2D,
			GLES20.GL_TEXTURE_WRAP_S,
			GLES20.GL_REPEAT );
		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_2D,
			GLES20.GL_TEXTURE_WRAP_T,
			GLES20.GL_REPEAT );

		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_2D,
			GLES20.GL_TEXTURE_MIN_FILTER,
			GLES20.GL_NEAREST );
		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_2D,
			GLES20.GL_TEXTURE_MAG_FILTER,
			GLES20.GL_LINEAR );

		// flip bitmap because 0/0 is bottom left in OpenGL
		Bitmap flippedBitmap = Bitmap.createBitmap(
			bitmap,
			0,
			0,
			bitmap.getWidth(),
			bitmap.getHeight(),
			flipMatrix,
			true );

		GLUtils.texImage2D(
			GLES20.GL_TEXTURE_2D,
			0,
			GLES20.GL_RGBA,
			flippedBitmap,
			GLES20.GL_UNSIGNED_BYTE,
			0 );

		flippedBitmap.recycle();

		GLES20.glGenerateMipmap(
			GLES20.GL_TEXTURE_2D );
	}

	private void createCubeTexture( int id, Bitmap bitmap )
	{
		GLES20.glBindTexture( GLES20.GL_TEXTURE_CUBE_MAP, id );

		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_CUBE_MAP,
			GLES20.GL_TEXTURE_MAG_FILTER,
			GLES20.GL_LINEAR );
		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_CUBE_MAP,
			GLES20.GL_TEXTURE_MIN_FILTER,
			GLES20.GL_LINEAR );

		int bitmapWidth = bitmap.getWidth();
		int bitmapHeight = bitmap.getHeight();
		int sideWidth = (int)Math.ceil( bitmapWidth/2f );
		int sideHeight = Math.round( bitmapHeight/3f );
		int sideLength = Math.min( sideWidth, sideHeight );
		int x = 0;
		int y = 0;

		for( int target : CUBE_MAP_TARGETS )
		{
			Bitmap side = Bitmap.createBitmap(
				bitmap,
				x,
				y,
				// cube textures need to be quadratic
				sideLength,
				sideLength,
				// don't flip cube textures
				null,
				true );

			GLUtils.texImage2D(
				target,
				0,
				GLES20.GL_RGBA,
				side,
				GLES20.GL_UNSIGNED_BYTE,
				0 );

			side.recycle();

			if( (x += sideWidth) >= bitmapWidth )
			{
				x = 0;
				y += sideHeight;
			}
		}

		GLES20.glGenerateMipmap(
			GLES20.GL_TEXTURE_CUBE_MAP );
	}

	private void indexTextureNames( String source )
	{
		if( source == null )
			return;

		textureNames.clear();
		numberOfTextures = 0;

		final int maxTextures = textureIds.length;

		for( Matcher m = SAMPLER.matcher( source );
			m.find() && numberOfTextures < maxTextures; )
		{
			String type = m.group( 1 );
			String name = m.group( 2 );

			if( type == null ||
				name == null ||
				name.equals( "backbuffer" ) )
				continue;

			int target;

			switch( type )
			{
				case "2D":
					target = GLES20.GL_TEXTURE_2D;
					break;
				case "Cube":
					target = GLES20.GL_TEXTURE_CUBE_MAP;
					break;
				default:
					continue;
			}

			textureTargets[numberOfTextures++] = target;
			textureNames.add( name );
		}
	}

	private float getBatteryLevel()
	{
		Intent batteryStatus = context.registerReceiver(
			null,
			new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED ) );

		if( batteryStatus == null )
			return 0;

		int level = batteryStatus.getIntExtra(
			BatteryManager.EXTRA_LEVEL, -1 );
		int scale = batteryStatus.getIntExtra(
			BatteryManager.EXTRA_SCALE, -1 );

		return (float)level/scale;
	}

	private static class TextureBinder
	{
		private int index;

		public void reset()
		{
			index = 0;
		}

		public void bind( int loc, int target, int textureId )
		{
			if( loc < 0 ||
				index >= TEXTURE_UNITS.length )
				return;

			GLES20.glUniform1i( loc, index );
			GLES20.glActiveTexture( TEXTURE_UNITS[index] );
			GLES20.glBindTexture( target, textureId );

			++index;
		}
	}
}
