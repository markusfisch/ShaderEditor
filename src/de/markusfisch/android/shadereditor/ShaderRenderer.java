package de.markusfisch.android.shadereditor;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
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

	public ErrorListener errorListener = null;
	public String fragmentShader = null;
	public final float gravity[] = new float[]{ 0, 0, 0 };
	public final float linear[] = new float[]{ 0, 0, 0 };
	public float fps = 0;

	private static final String vertexShader =
		"attribute vec2 position;"+
		"void main()"+
		"{"+
			"gl_Position = vec4( position, 0.0, 1.0 );"+
		"}";
	private ByteBuffer screenVertices;
	private int program = 0;
	private final float resolution[] = new float[]{ 0, 0 };
	private final float mouse[] = new float[]{ 0, 0 };
	private final float touch[] = new float[]{ 0, 0 };
	private long startTime;
	private long lastTime;
	private byte thumbnail[] = new byte[1];

	@Override
	public void onSurfaceCreated( GL10 gl, EGLConfig config )
	{
		startTime = lastTime = SystemClock.elapsedRealtime();
		fps = 0;

		final byte screenCoords[] = {
			-1, 1,
			-1, -1,
			1, 1,
			1, -1 };
		screenVertices = ByteBuffer.allocateDirect( 8 );
		screenVertices.put( screenCoords ).position( 0 );

		GLES20.glClearColor( 0f, 0f, 0f, 1f );

		if( fragmentShader != null )
			loadFragmentShader();
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
		fps = Math.round( 1000f/(now-lastTime) );
		lastTime = now;

		GLES20.glUseProgram( program );

		GLES20.glUniform1f(
			GLES20.glGetUniformLocation( program, "time" ),
			(now-startTime)/1000f );

		GLES20.glUniform2fv(
			GLES20.glGetUniformLocation( program, "resolution" ),
			1,
			resolution,
			0 );

		GLES20.glUniform2fv(
			GLES20.glGetUniformLocation( program, "mouse" ),
			1,
			mouse,
			0 );

		GLES20.glUniform2fv(
			GLES20.glGetUniformLocation( program, "touch" ),
			1,
			touch,
			0 );

		GLES20.glUniform3fv(
			GLES20.glGetUniformLocation( program, "gravity" ),
			1,
			gravity,
			0 );

		GLES20.glUniform3fv(
			GLES20.glGetUniformLocation( program, "linear" ),
			1,
			linear,
			0 );

		final int p = GLES20.glGetAttribLocation( program, "position" );
		GLES20.glVertexAttribPointer(
			p,
			2,
			GLES20.GL_BYTE,
			false,
			2,
			screenVertices );

		GLES20.glEnableVertexAttribArray( p );
		GLES20.glDrawArrays( GLES20.GL_TRIANGLE_STRIP, 0, 4 );
		GLES20.glDisableVertexAttribArray( p );

		if( thumbnail == null )
			thumbnail = saveThumbnail();
	}

	@Override
	public void onSurfaceChanged( GL10 gl, int width, int height )
	{
		GLES20.glViewport( 0, 0, width, height );

		resolution[0] = width;
		resolution[1] = height;
	}

	public void onTouch( final float x, final float y )
	{
		// to be compatible with glsl.heroku.com
		mouse[0] = x/resolution[0];
		mouse[1] = 1-y/resolution[1];

		touch[0] = x;
		touch[1] = resolution[1]-y;
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

	private void loadFragmentShader()
	{
		if( program != 0 )
			GLES20.glDeleteProgram( program );

		int vs, fs;

		if( (vs = loadShader(
				GLES20.GL_VERTEX_SHADER,
				vertexShader )) == 0 ||
			(fs = loadShader(
				GLES20.GL_FRAGMENT_SHADER,
				fragmentShader )) == 0 )
			return;

		if( (program = GLES20.glCreateProgram()) != 0 )
		{
			GLES20.glAttachShader( program, vs );
			GLES20.glAttachShader( program, fs );
			GLES20.glLinkProgram( program );

			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(
				program,
				GLES20.GL_LINK_STATUS,
				linkStatus, 0);

			if( linkStatus[0] != GLES20.GL_TRUE )
			{
				if( errorListener != null )
					errorListener.onShaderError(
						GLES20.glGetProgramInfoLog( program ) );

				GLES20.glDeleteProgram( program );
				program = 0;
			}
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

	private final int loadShader( int type, String src )
	{
		int shader = GLES20.glCreateShader( type );

		if( shader != 0 )
		{
			GLES20.glShaderSource( shader, src );
			GLES20.glCompileShader( shader);

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
		}

		return shader;
	}
}
