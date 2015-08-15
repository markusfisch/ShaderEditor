package de.markusfisch.android.shadereditor.widget;

import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ShaderView extends GLSurfaceView
{
	private ShaderRenderer renderer;

	public ShaderView( Context context )
	{
		super( context );

		init( context );
	}

	public ShaderView( Context context, AttributeSet attrs )
	{
		super( context, attrs );

		init( context );
	}

	@Override
	public void onPause()
	{
		super.onPause();

		renderer.unregisterListeners();
	}

	@Override
	public boolean onTouchEvent( MotionEvent e )
	{
		renderer.touchAt( e.getX(), e.getY() );

		return true;
	}

	public void setFragmentShader( String src )
	{
		onPause();
		renderer.setFragmentShader( src );
		onResume();
	}

	public ShaderRenderer getRenderer()
	{
		return renderer;
	}

	private void init( Context context )
	{
		setEGLContextClientVersion( 2 );
		setRenderer( (renderer = new ShaderRenderer( context )) );
		setRenderMode( GLSurfaceView.RENDERMODE_CONTINUOUSLY );
	}
}
