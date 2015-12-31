package de.markusfisch.android.shadereditor.widget;

import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ShaderView extends GLSurfaceView
{
	private ShaderRenderer renderer;

	public ShaderView( Context context, int renderMode )
	{
		super( context );

		init( context, renderMode );
	}

	public ShaderView( Context context )
	{
		super( context );

		init(
			context,
			GLSurfaceView.RENDERMODE_CONTINUOUSLY );
	}

	public ShaderView( Context context, AttributeSet attrs )
	{
		super( context, attrs );

		init(
			context,
			GLSurfaceView.RENDERMODE_CONTINUOUSLY );
	}

	@Override
	public void onPause()
	{
		super.onPause();

		renderer.unregisterListeners();
	}

	@Override
	public boolean onTouchEvent( MotionEvent event )
	{
		renderer.touchAt( event );

		return true;
	}

	public void setFragmentShader( String src )
	{
		setFragmentShader( src, 1f );
	}

	public void setFragmentShader( String src, float quality )
	{
		onPause();
		renderer.setFragmentShader( src, quality );
		onResume();
	}

	public ShaderRenderer getRenderer()
	{
		return renderer;
	}

	private void init(
		Context context,
		int renderMode )
	{
		setEGLContextClientVersion( 2 );
		setRenderer( (renderer = new ShaderRenderer( context )) );
		setRenderMode( renderMode );
	}
}
