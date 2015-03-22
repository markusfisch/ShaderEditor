package de.markusfisch.android.shadereditor;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class FpsGauge
{
	private static final String vertexShader =
		"attribute vec4 position;"+
		"void main()"+
		"{"+
			"gl_Position = position;"+
		"}";
	private static final String fragmentShader =
		"precision mediump float;"+
		"uniform vec4 color;"+
		"void main()"+
		"{"+
			"gl_FragColor = color;"+
		"}";
	private static final int COORDS_PER_VERTEX = 2;
	private static final int BYTES_PER_VERTEX = COORDS_PER_VERTEX*4;
	private static final float color[] = {
		1f, 1f, 1f, 1f };

	private final FloatBuffer vertexBuffer;
	private final int program;
	private final int positionLoc;
	private final int colorLoc;
	private float lineWidth;

	public FpsGauge()
	{
		final int max = 60*2*COORDS_PER_VERTEX;
		final float coords[] = new float[max];
		float step = 2f/60f;
		float y = -1f;

		for( int i = 0; i < max; y += step )
		{
			coords[i] = i % 10 == 0 ? .97f : .99f;
			++i;
			coords[i++] = y;

			coords[i++] = 1f;
			coords[i++] = y;
		}

		ByteBuffer b = ByteBuffer.allocateDirect( coords.length*4 );
		b.order( ByteOrder.nativeOrder() );
		vertexBuffer = b.asFloatBuffer();
		vertexBuffer.put( coords ).position( 0 );

		program = Shader.loadProgram(
			vertexShader,
			fragmentShader );

		positionLoc = GLES20.glGetAttribLocation(
			program, "position" );

		colorLoc = GLES20.glGetUniformLocation(
			program, "color" );
	}

	public void resetHeight( float height )
	{
		lineWidth = height*.005f;
	}

	public void draw( int fps )
	{
		GLES20.glDisable( GLES20.GL_CULL_FACE );
		GLES20.glDisable( GLES20.GL_DEPTH_TEST );

		GLES20.glUseProgram( program );

		GLES20.glUniform4fv(
			colorLoc,
			1,
			color,
			0 );

		GLES20.glVertexAttribPointer(
			positionLoc,
			COORDS_PER_VERTEX,
			GLES20.GL_FLOAT,
			false,
			BYTES_PER_VERTEX,
			vertexBuffer );
		GLES20.glEnableVertexAttribArray( positionLoc );

		GLES20.glBlendFunc(
			GLES20.GL_ONE_MINUS_DST_COLOR,
			GLES20.GL_ZERO );
		GLES20.glEnable( GLES20.GL_BLEND );

		// increment to make first mark 0
		if( ++fps > 59 )
			fps = 59;

		GLES20.glLineWidth( lineWidth );
		GLES20.glDrawArrays(
			GLES20.GL_LINES,
			0,
			fps*2 );

		GLES20.glDisable( GLES20.GL_BLEND );

		GLES20.glDisableVertexAttribArray( positionLoc );
	}
}
