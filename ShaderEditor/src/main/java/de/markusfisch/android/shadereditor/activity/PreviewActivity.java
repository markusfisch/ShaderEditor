package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;
import de.markusfisch.android.shadereditor.widget.ShaderView;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class PreviewActivity extends AppCompatActivity
{
	public static final String FRAGMENT_SHADER = "fragment_shader";
	public static byte thumbnail[];

	private ShaderView shaderView;
	private Runnable finishRunnable =
		new Runnable()
		{
			@Override
			public void run()
			{
				finish();
			}
		};
	private Runnable thumbnailRunnable =
		new Runnable()
		{
			@Override
			public void run()
			{
				if( shaderView == null )
					return;

				thumbnail = shaderView
					.getRenderer()
					.getThumbnail();
			}
		};

	@Override
	protected void onCreate( Bundle state )
	{
		super.onCreate( state );

		String fragmentShader = getIntent().getStringExtra(
			FRAGMENT_SHADER );

		if( fragmentShader == null )
		{
			finish();
			return;
		}

		shaderView = new ShaderView( this );
		shaderView.setFragmentShader( fragmentShader );
		shaderView.getRenderer().setOnRendererListener(
			new ShaderRenderer.OnRendererListener()
			{
				@Override
				public void onFramesPerSecond( int fps )
				{
					// invoked from the GL thread
					MainActivity.postUpdateFps( fps );
				}

				@Override
				public void onInfoLog( String infoLog )
				{
					// invoked from the GL thread
					MainActivity.postInfoLog( infoLog );
					runOnUiThread( finishRunnable );
				}
			} );

		setContentView( shaderView );

		MainActivity.setSystemBarColor(
			getWindow(),
			0,
			true );
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		shaderView.onResume();

		thumbnail = null;
		shaderView.postDelayed(
			thumbnailRunnable,
			500 );
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		shaderView.onPause();
	}
}
