package de.markusfisch.android.shadereditor.opengl;

import de.markusfisch.android.shadereditor.hardware.AccelerometerListener;
import de.markusfisch.android.shadereditor.hardware.GyroscopeListener;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class ShaderRenderer implements GLSurfaceView.Renderer
{
	public interface OnRendererListener
	{
		public void onInfoLog( String error );
		public void onFramesPerSecond( int fps );
	}

	private static final float NANO = 1000000000f;
	private static final long FPS_UPDATE_FREQUENCY = 200000000;
	private static final String vertexShader =
		"attribute vec2 position;"+
		"void main()"+
		"{"+
			"gl_Position = vec4( position, 0., 1. );"+
		"}";

	private final int fb[] = new int[]{ 0, 0 };
	private final int tx[] = new int[]{ 0, 0, 0 };
	private final float resolution[] = new float[]{ 0, 0 };
	private final float touch[] = new float[]{ 0, 0 };
	private final float mouse[] = new float[]{ 0, 0 };
	private final float pointers[] = new float[30];
	private final float offset[] = new float[]{ 0, 0 };

	private Context context;
	private AccelerometerListener accelerometerListener;
	private GyroscopeListener gyroscopeListener;
	private OnRendererListener onRendererListener;
	private String fragmentShader;
	private ByteBuffer vertexBuffer;
	private int program = 0;
	private int timeLoc;
	private int resolutionLoc;
	private int touchLoc;
	private int mouseLoc;
	private int pointerCountLoc;
	private int pointersLoc;
	private int gravityLoc;
	private int linearLoc;
	private int rotationLoc;
	private int offsetLoc;
	private int positionLoc;
	private int batteryLoc;
	private int backBufferLoc;
	private int textureLoc;
	private int pointerCount;
	private int frontTarget = 0;
	private int backTarget = 1;
	private int inputTexture = 2;
	private long startTime;
	private long lastRender;
	private Intent batteryStatus;

	private volatile byte thumbnail[] = new byte[1];
	private volatile long nextFpsUpdate = 0;
	private volatile float sum;
	private volatile float samples;
	private volatile int lastFps;

	public ShaderRenderer( Context context )
	{
		this.context = context;

		accelerometerListener =
			new AccelerometerListener( context );
		gyroscopeListener =
			new GyroscopeListener( context );
	}

	public void setFragmentShader( String source )
	{
		resetFps();
		fragmentShader = source;
	}

	public void setOnRendererListener( OnRendererListener listener )
	{
		onRendererListener = listener;
	}

	@Override
	public void onSurfaceCreated( GL10 gl, EGLConfig config )
	{
		startTime = lastRender = System.nanoTime();

		final byte screenCoords[] = {
			-1, 1,
			-1, -1,
			1, 1,
			1, -1 };
		vertexBuffer = ByteBuffer.allocateDirect( 8 );
		vertexBuffer.put( screenCoords ).position( 0 );

		GLES20.glDisable( GLES20.GL_CULL_FACE );
		GLES20.glDisable( GLES20.GL_BLEND );
		GLES20.glDisable( GLES20.GL_DEPTH_TEST );

		GLES20.glClearColor( 0f, 0f, 0f, 1f );

		if( program != 0 )
		{
			GLES20.glDeleteProgram( program );
			program = 0;

			deleteTargets();
		}

		if( fragmentShader != null &&
			fragmentShader.length() > 0 )
		{
			resetFps();
			loadProgram();
		}
	}

	@Override
	public void onDrawFrame( GL10 gl )
	{
		GLES20.glClear(
			GLES20.GL_COLOR_BUFFER_BIT |
			GLES20.GL_DEPTH_BUFFER_BIT );

		if( program == 0 )
			return;

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
				// wrap time early to avoid rounding
				// errors when the value becomes too
				// big for a mere mediump float
				(now-startTime)/NANO % 60000f );

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

		if( gravityLoc > -1 )
			GLES20.glUniform3fv(
				gravityLoc,
				1,
				accelerometerListener.gravity,
				0 );

		if( linearLoc > -1 )
			GLES20.glUniform3fv(
				linearLoc,
				1,
				accelerometerListener.linear,
				0 );

		if( rotationLoc > -1 )
			GLES20.glUniform3fv(
				rotationLoc,
				1,
				gyroscopeListener.rotation,
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

			GLES20.glUniform1f(
				batteryLoc,
				(float)level/scale );
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

		if ( textureLoc > -1 )
        {
			if (tx[2] == 0) {
				final Bitmap bitmap = BitmapFactory.decodeFile("/sdcard/input.png");
				createTexture(bitmap);
				bitmap.recycle();
			}

			GLES20.glUniform1i(textureLoc, 0);
			int t = tx[inputTexture];
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t);
		}

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

		if( onRendererListener != null )
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

		resetFps();
	}

	public void unregisterListeners()
	{
		accelerometerListener.unregister();
		gyroscopeListener.unregister();
	}

	public void touchAt( MotionEvent e )
	{
		float x = e.getX();
		float y = e.getY();

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
			pointers[offset++] = e.getX( n );
			pointers[offset++] = resolution[1]-e.getY( n );
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
		if( (program = Program.loadProgram(
			vertexShader,
			fragmentShader )) == 0 )
		{
			if( onRendererListener != null )
				onRendererListener.onInfoLog(
					Program.getInfoLog() );

			return;
		}

		indexLocations();

		GLES20.glEnableVertexAttribArray( positionLoc );

		if( gravityLoc > -1 ||
			linearLoc > -1 )
			accelerometerListener.register();

		if( rotationLoc > -1 )
			gyroscopeListener.register();

		if( batteryLoc > -1 &&
			batteryStatus == null )
			batteryStatus = context.registerReceiver(
				null,
				new IntentFilter(
					Intent.ACTION_BATTERY_CHANGED ) );
	}

	private void indexLocations()
	{
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
		offsetLoc = GLES20.glGetUniformLocation(
			program, "offset" );
		batteryLoc = GLES20.glGetUniformLocation(
			program, "battery" );
		backBufferLoc = GLES20.glGetUniformLocation(
			program, "backbuffer" );

        textureLoc = GLES20.glGetUniformLocation(
				program, "texture");
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
		catch( IllegalArgumentException e )
		{
			// will never happen because neither
			// width nor height <= 0
			return null;
		}
	}

	private void updateFps( long now )
	{
		sum += Math.min( NANO/(now-lastRender), 60f );

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

			nextFpsUpdate = now+FPS_UPDATE_FREQUENCY;
		}

		lastRender = now;
	}

	private void deleteTargets() {
		deleteFrameBuffers();
		deleteTextures();
	}

	private void deleteFrameBuffers() {
		if( fb[0] == 0 )
			return;

		GLES20.glDeleteFramebuffers(2, fb, 0);

		fb[0] = 0;
	}

	private void deleteTextures() {
		if (tx[0] == 0)
			return;

		GLES20.glDeleteTextures(3, tx, 0);

		tx[0] = tx[1] = tx[2] = 0;
	}

	private void createTargets( int width, int height )
	{
		deleteTargets();

		GLES20.glGenFramebuffers( 2, fb, 0 );
		if (tx[0] == 0)
			generateTextures(tx);

		createTarget( frontTarget, width, height );
		createTarget(backTarget, width, height);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER, 0 );
	}

	private void generateTextures(int[] tx) {
		GLES20.glGenTextures(3, tx, 0);
	}

	private void createTexture(Bitmap bitmap) {
		if (tx[0] == 0)
			generateTextures(tx);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tx[inputTexture]);

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
				GLES20.GL_TEXTURE_MAG_FILTER,
				GLES20.GL_LINEAR );
		GLES20.glTexParameteri(
				GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER,
				GLES20.GL_LINEAR_MIPMAP_LINEAR);

		if (bitmap != null) {
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, GLES20.GL_UNSIGNED_BYTE, 0);
			GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
		}
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
