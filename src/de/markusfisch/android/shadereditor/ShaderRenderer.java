package de.markusfisch.android.shadereditor;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;

import java.io.ByteArrayOutputStream;
import java.lang.RuntimeException;
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

	public volatile ErrorListener errorListener = null;
	public volatile String fragmentShader = null;
	public volatile float gravity[] = new float[]{ 0, 0, 0 };
	public volatile float linear[] = new float[]{ 0, 0, 0 };
	public volatile boolean showFps = false;

	private static final String vertexShader =
		"attribute vec2 position;"+
		"void main()"+
		"{"+
			"gl_Position = vec4( position, 0.0, 1.0 );"+
		"}";
	private ByteBuffer vertexBuffer;
	private int program = 0;
	private int timeLoc;
	private int resolutionLoc;
	private int mouseLoc;
	private int touchLoc;
	private int gravityLoc;
	private int linearLoc;
	private int positionLoc;
	private final float resolution[] = new float[]{ 0, 0 };
	private volatile float mouse[] = new float[]{ 0, 0 };
	private volatile float touch[] = new float[]{ 0, 0 };
	private long startTime;
	private long lastRender;
	private volatile byte thumbnail[] = new byte[1];
	private FpsGauge fpsGauge;

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

		GLES20.glClearColor( 0f, 0f, 0f, 1f );

		if( fragmentShader != null )
		{
			fpsGauge.reset();
			loadProgram();
		}
	}

	@Override
	public void onDrawFrame( GL10 gl )
	{
		GLES20.glClear( GLES20.GL_COLOR_BUFFER_BIT );
		GLES20.glDisable( GLES20.GL_CULL_FACE );
		GLES20.glDisable( GLES20.GL_BLEND );
		GLES20.glDisable( GLES20.GL_DEPTH_TEST );

		if( program == 0 )
			return;

		final long now = SystemClock.elapsedRealtime();

		GLES20.glUseProgram( program );

		GLES20.glUniform1f(
			timeLoc,
			(now-startTime)/1000f );

		GLES20.glUniform2fv(
			resolutionLoc,
			1,
			resolution,
			0 );

		GLES20.glUniform2fv(
			mouseLoc,
			1,
			mouse,
			0 );

		GLES20.glUniform2fv(
			touchLoc,
			1,
			touch,
			0 );

		GLES20.glUniform3fv(
			gravityLoc,
			1,
			gravity,
			0 );

		GLES20.glUniform3fv(
			linearLoc,
			1,
			linear,
			0 );

		GLES20.glVertexAttribPointer(
			positionLoc,
			2,
			GLES20.GL_BYTE,
			false,
			2,
			vertexBuffer );
		GLES20.glEnableVertexAttribArray( positionLoc );

		GLES20.glDrawArrays( GLES20.GL_TRIANGLE_STRIP, 0, 4 );

		GLES20.glDisableVertexAttribArray( positionLoc );

		if( thumbnail == null )
			thumbnail = saveThumbnail();

		if( showFps )
		{
			fpsGauge.draw( (int)(1000f/(now-lastRender)) );
			lastRender = now;
		}
	}

	@Override
	public void onSurfaceChanged( GL10 gl, int width, int height )
	{
		GLES20.glViewport( 0, 0, width, height );

		resolution[0] = width;
		resolution[1] = height;

		GLES20.glLineWidth( (float)height*.005f );
		fpsGauge.reset();
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
			while( thumbnail == null )
				Thread.sleep( 100 );
		}
		catch( Exception e )
		{
		}

		return thumbnail;
	}

	public static int loadShader( int type, String src )
	{
		int shader = GLES20.glCreateShader( type );

		if( shader == 0 )
			return 0;

		GLES20.glShaderSource( shader, src );
		GLES20.glCompileShader( shader );

		return shader;
	}

	private void loadProgram()
	{
		if( program != 0 )
		{
			GLES20.glDeleteProgram( program );
			program = 0;
		}

		int vs, fs;

		if( (vs = loadAndVerifyShader(
			GLES20.GL_VERTEX_SHADER,
			vertexShader )) != 0 )
		{
			if( (fs = loadAndVerifyShader(
				GLES20.GL_FRAGMENT_SHADER,
				fragmentShader )) != 0 )
			{
				if( (program = GLES20.glCreateProgram()) != 0 )
				{
					GLES20.glAttachShader( program, vs );
					GLES20.glAttachShader( program, fs );
					GLES20.glLinkProgram( program );
				}

				// mark shader objects as deleted so they get
				// deleted as soon as glDeleteProgram() does
				// detach them
				GLES20.glDeleteShader( fs );
			}

			// same as above
			GLES20.glDeleteShader( vs );
		}

		if( program == 0 )
			return;

		int[] linkStatus = new int[1];
		GLES20.glGetProgramiv(
			program,
			GLES20.GL_LINK_STATUS,
			linkStatus, 0 );

		if( linkStatus[0] != GLES20.GL_TRUE )
		{
			if( errorListener != null )
				errorListener.onShaderError(
					GLES20.glGetProgramInfoLog( program ) );

			GLES20.glDeleteProgram( program );
			program = 0;

			return;
		}

		positionLoc = GLES20.glGetAttribLocation(
			program, "position" );

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
	}

	private int loadAndVerifyShader( int type, String src )
	{
		int shader = loadShader( type, src );

		if( shader == 0 )
			return 0;

		int[] compiled = new int[1];
		GLES20.glGetShaderiv(
			shader,
			GLES20.GL_COMPILE_STATUS,
			compiled,
			0 );

		if( compiled[0] == 0 )
		{
			if( errorListener != null )
				errorListener.onShaderError(
					GLES20.glGetShaderInfoLog( shader ) );

			GLES20.glDeleteShader( shader );
			shader = 0;
		}

		return shader;
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
}
