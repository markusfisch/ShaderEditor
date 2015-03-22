package de.markusfisch.android.shadereditor;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.BatteryManager;
import android.os.SystemClock;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ShaderRenderer implements GLSurfaceView.Renderer
{
	public interface ErrorListener
	{
		public void onShaderError( String error );
	}

	public interface FpsListener
	{
		public void onShaderFramesPerSecond( int fps );
	}

	public volatile ErrorListener errorListener = null;
	public volatile FpsListener fpsListener = null;
	public volatile String fragmentShader = null;
	public volatile ShaderView view = null;
	public volatile float gravity[] = new float[]{ 0, 0, 0 };
	public volatile float linear[] = new float[]{ 0, 0, 0 };
	public volatile float rotation[] = new float[]{ 0, 0, 0 };
	public volatile float offset[] = new float[]{ 0, 0 };
	public volatile boolean showFpsGauge = false;

	private static final int FPS_UPDATE_FREQUENCY = 200;
	private static final String vertexShader =
		"attribute vec2 position;"+
		"void main()"+
		"{"+
			"gl_Position = vec4( position, 0., 1. );"+
		"}";
	private ByteBuffer vertexBuffer;
	private int program = 0;
	private int timeLoc;
	private int resolutionLoc;
	private int mouseLoc;
	private int touchLoc;
	private int gravityLoc;
	private int linearLoc;
	private int rotationLoc;
	private int offsetLoc;
	private int positionLoc;
	private int batteryLoc;
	private int backBufferLoc = -1;
	private int frontTarget = 0;
	private int backTarget = 1;
	private final int fb[] = new int[]{ 0, 0 };
	private final int tx[] = new int[]{ 0, 0 };
	private final float resolution[] = new float[]{ 0, 0 };
	private volatile float mouse[] = new float[]{ 0, 0 };
	private volatile float touch[] = new float[]{ 0, 0 };
	private long startTime;
	private long lastRender;
	private volatile long lastFpsUpdate = 0;
	private volatile byte thumbnail[] = new byte[1];
	private FpsGauge fpsGauge;
	private volatile float sum;
	private volatile float samples;
	private volatile int lastFps;
	private Intent batteryStatus = null;

	@Override
	public void onSurfaceCreated( GL10 gl, EGLConfig config )
	{
		startTime = lastRender = SystemClock.elapsedRealtime();

		final byte screenCoords[] = {
			-1, 1,
			-1, -1,
			1, 1,
			1, -1 };
		vertexBuffer = ByteBuffer.allocateDirect( 8 );
		vertexBuffer.put( screenCoords ).position( 0 );

		fpsGauge = new FpsGauge();

		GLES20.glDisable( GLES20.GL_CULL_FACE );
		GLES20.glDisable( GLES20.GL_BLEND );
		GLES20.glDisable( GLES20.GL_DEPTH_TEST );

		GLES20.glClearColor( 0f, 0f, 0f, 1f );

		if( fragmentShader != null )
		{
			resetFps();
			loadProgram();
		}
	}

	@Override
	public void onDrawFrame( GL10 gl )
	{
		if( program == 0 )
		{
			GLES20.glClear(
				GLES20.GL_COLOR_BUFFER_BIT |
				GLES20.GL_DEPTH_BUFFER_BIT );

			return;
		}

		final long now = SystemClock.elapsedRealtime();

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
				(now-startTime)/1000f );

		if( resolutionLoc > -1 )
			GLES20.glUniform2fv(
				resolutionLoc,
				1,
				resolution,
				0 );

		if( mouseLoc > -1 )
			GLES20.glUniform2fv(
				mouseLoc,
				1,
				mouse,
				0 );

		if( touchLoc > -1 )
			GLES20.glUniform2fv(
				touchLoc,
				1,
				touch,
				0 );

		if( gravityLoc > -1 )
			GLES20.glUniform3fv(
				gravityLoc,
				1,
				gravity,
				0 );

		if( linearLoc > -1 )
			GLES20.glUniform3fv(
				linearLoc,
				1,
				linear,
				0 );

		if( rotationLoc > -1 )
			GLES20.glUniform3fv(
				rotationLoc,
				1,
				rotation,
				0 );

		if( offsetLoc > -1 )
			GLES20.glUniform2fv(
				offsetLoc,
				1,
				offset,
				0 );

		if( batteryLoc > -1 )
		{
			int level = batteryStatus.getIntExtra(
				BatteryManager.EXTRA_LEVEL, -1 );
			int scale = batteryStatus.getIntExtra(
				BatteryManager.EXTRA_SCALE, -1 );
			float battery = level/(float)scale;

			GLES20.glUniform1f(
				batteryLoc,
				battery );
		}

		if( backBufferLoc > -1 )
		{
			if( fb[0] == 0 )
				createTargets(
					(int)resolution[0],
					(int)resolution[1] );

			GLES20.glUniform1i(
				backBufferLoc,
				0 );

			GLES20.glBindTexture(
				GLES20.GL_TEXTURE_2D,
				tx[backTarget] );
		}

		GLES20.glClear(
			GLES20.GL_COLOR_BUFFER_BIT );
		GLES20.glDrawArrays(
			GLES20.GL_TRIANGLE_STRIP,
			0,
			4 );

		if( backBufferLoc > -1 )
		{
			// for some drivers it's important to bind
			// the texture again after glDrawArrays()
			GLES20.glBindTexture(
				GLES20.GL_TEXTURE_2D,
				tx[backTarget] );

			GLES20.glBindFramebuffer(
				GLES20.GL_FRAMEBUFFER,
				fb[frontTarget] );

			GLES20.glClear(
				GLES20.GL_COLOR_BUFFER_BIT );
			GLES20.glDrawArrays(
				GLES20.GL_TRIANGLE_STRIP,
				0,
				4 );

			GLES20.glBindFramebuffer(
				GLES20.GL_FRAMEBUFFER,
				0 );
			GLES20.glBindTexture(
				GLES20.GL_TEXTURE_2D,
				0 );

			// swap buffers so the next image will be rendered
			// over the current backbuffer and the current image
			// will be the backbuffer for the next image
			int t = frontTarget;
			frontTarget = backTarget;
			backTarget = t;
		}

		if( thumbnail == null )
			thumbnail = saveThumbnail();

		if( fpsListener != null )
			updateFps( now );
	}

	@Override
	public void onSurfaceChanged( GL10 gl, int width, int height )
	{
		GLES20.glViewport( 0, 0, width, height );

		if( width != resolution[0] ||
			height != resolution[1] )
			deleteTargets();

		resolution[0] = width;
		resolution[1] = height;

		fpsGauge.resetHeight( height );

		resetFps();
	}

	public void onTouch( float x, float y )
	{
		touch[0] = x;
		touch[1] = resolution[1]-y;

		// to be compatible with glsl.heroku.com
		mouse[0] = x/resolution[0];
		mouse[1] = 1-y/resolution[1];
	}

	public byte[] getThumbnail()
	{
		thumbnail = null;

		try
		{
			while( program > 0 && thumbnail == null )
				Thread.sleep( 100 );
		}
		catch( Exception e )
		{
		}

		return thumbnail;
	}

	public void resetFps()
	{
		sum = samples = 0;
		lastFps = 0;
		lastFpsUpdate = 0;
	}

	private void loadProgram()
	{
		if( program != 0 )
			GLES20.glDeleteProgram( program );

		deleteTargets();

		if( (program = Shader.loadProgram(
			vertexShader,
			fragmentShader )) == 0 )
		{
			if( errorListener != null )
				errorListener.onShaderError( Shader.lastError );

			return;
		}

		positionLoc = GLES20.glGetAttribLocation(
			program, "position" );
		GLES20.glEnableVertexAttribArray( positionLoc );

		timeLoc = GLES20.glGetUniformLocation(
			program, "time" );
		resolutionLoc = GLES20.glGetUniformLocation(
			program, "resolution" );
		mouseLoc = GLES20.glGetUniformLocation(
			program, "mouse" );
		touchLoc = GLES20.glGetUniformLocation(
			program, "touch" );
		gravityLoc = GLES20.glGetUniformLocation(
			program, "gravity" );
		linearLoc = GLES20.glGetUniformLocation(
			program, "linear" );
		rotationLoc = GLES20.glGetUniformLocation(
			program, "rotation" );
		offsetLoc = GLES20.glGetUniformLocation(
			program, "offset" );
		batteryLoc = GLES20.glGetUniformLocation(
			program, "battery" );
		backBufferLoc = GLES20.glGetUniformLocation(
			program, "backbuffer" );

		if( view != null )
		{
			if( gravityLoc > -1 ||
				linearLoc > -1 )
				view.registerAccelerometerListener();

			if( rotationLoc > -1 )
				view.registerGyroscopeListener();

			if( batteryLoc > -1 &&
				batteryStatus == null )
				batteryStatus = view.getContext().registerReceiver(
					null,
					new IntentFilter( Intent.ACTION_BATTERY_CHANGED ) );
		}
	}

	private byte[] saveThumbnail()
	{
		final int min = (int)Math.min( resolution[0], resolution[1] );
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
		catch( Exception e )
		{
			return null;
		}
	}

	private void updateFps( long now )
	{
		int fps = (int)(1000f/(now-lastRender));

		if( fps < 0 )
			fps = 0;
		else if( fps > 59 )
			fps = 59;

		sum += fps;
		++samples;

		if( samples > 0xffff )
		{
			sum = sum/samples;
			samples = 1;
		}

		fps = (int)(sum/samples);

		if( now-lastFpsUpdate > FPS_UPDATE_FREQUENCY )
		{
			if( fps != lastFps )
			{
				fpsListener.onShaderFramesPerSecond( fps );
				lastFps = fps;
			}

			lastFpsUpdate = now;
		}

		if( showFpsGauge )
			fpsGauge.draw( fps );

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

		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, 0 );
		GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER, 0 );
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
}
