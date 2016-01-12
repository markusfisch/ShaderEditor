package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;
import de.markusfisch.android.shadereditor.widget.ShaderView;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class PreviewActivity extends AppCompatActivity
{
	public static final String FRAGMENT_SHADER = "fragment_shader";
	public static final String QUALITY = "quality";
	public static int fps;
	public static String infoLog;
	public static byte thumbnail[];

	private final Runnable finishRunnable =
		new Runnable()
		{
			@Override
			public void run()
			{
				finish();
			}
		};
	private final Runnable thumbnailRunnable =
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

	private ShaderView shaderView;

	public static void reset()
	{
		fps = 0;
		infoLog = null;
		thumbnail = null;
	}

	@Override
	protected void onCreate( Bundle state )
	{
		super.onCreate( state );

		Intent intent = getIntent();
		String fragmentShader;

		if( (intent = getIntent()) == null ||
			(fragmentShader = intent.getStringExtra(
				FRAGMENT_SHADER )) == null )
		{
			finish();
			return;
		}

		float quality = intent.getFloatExtra( QUALITY, 1f );

		shaderView = new ShaderView( this );
		shaderView.setFragmentShader( fragmentShader, quality );
		shaderView.getRenderer().setOnRendererListener(
			new ShaderRenderer.OnRendererListener()
			{
				@Override
				public void onFramesPerSecond( int fps )
				{
					// invoked from the GL thread
					PreviewActivity.this.fps = fps;
				}

				@Override
				public void onInfoLog( String infoLog )
				{
					// invoked from the GL thread
					PreviewActivity.this.infoLog = infoLog;
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
