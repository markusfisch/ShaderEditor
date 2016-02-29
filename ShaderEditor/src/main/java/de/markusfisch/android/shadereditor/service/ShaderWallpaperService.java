package de.markusfisch.android.shadereditor.service;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.widget.ShaderView;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class ShaderWallpaperService extends WallpaperService
{
	private static ShaderWallpaperEngine engine;

	@Override
	public Engine onCreateEngine()
	{
		return (engine = new ShaderWallpaperEngine());
	}

	@Override
	public void onDestroy()
	{
		engine = null;
	}

	public static void setRenderMode( int renderMode )
	{
		if( engine != null )
			engine.setRenderMode( renderMode );
	}

	private class ShaderWallpaperEngine
		extends Engine
		implements SharedPreferences.OnSharedPreferenceChangeListener
	{
		private final Handler handler = new Handler();

		private ShaderWallpaperView view;

		public ShaderWallpaperEngine()
		{
			super();

			ShaderEditorApplication
				.preferences
				.getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(
					this );

			setTouchEventsEnabled( true );
		}

		@Override
		public void onSharedPreferenceChanged(
			SharedPreferences preferences,
			String key )
		{
			if( Preferences.WALLPAPER_SHADER.equals( key ) )
				setShader();
		}

		@Override
		public void onCreate( SurfaceHolder holder )
		{
			super.onCreate( holder );

			view = new ShaderWallpaperView();

			setShader();
		}

		@Override
		public void onDestroy()
		{
			super.onDestroy();

			view.destroy();
			view = null;
		}

		@Override
		public void onVisibilityChanged( boolean visible )
		{
			super.onVisibilityChanged( visible );

			if( visible )
				view.onResume();
			else
				view.onPause();
		}

		@Override
		public void onTouchEvent( MotionEvent e )
		{
			super.onTouchEvent( e );

			view.getRenderer().touchAt( e );
		}

		@Override
		public void onOffsetsChanged(
			float xOffset,
			float yOffset,
			float xStep,
			float yStep,
			int xPixels,
			int yPixels )
		{
			view.getRenderer().setOffset(
				xOffset,
				yOffset );
		}

		public void setRenderMode( int renderMode )
		{
			if( view == null )
				return;

			view.setRenderMode( renderMode );
		}

		private void setShader()
		{
			if( !ShaderEditorApplication.dataSource.isOpen() )
			{
				handler.postDelayed(
					new Runnable()
					{
						@Override
						public void run()
						{
							setShader();
						}
					},
					100 );

				return;
			}

			Cursor cursor = ShaderEditorApplication
				.dataSource
				.getShader( ShaderEditorApplication
					.preferences
					.getWallpaperShader() );

			boolean randomShader = false;

			while( cursor == null ||
				!cursor.moveToFirst() )
			{
				if( cursor != null )
					cursor.close();

				if( randomShader )
					return;

				randomShader = true;
				cursor = ShaderEditorApplication
					.dataSource
					.getRandomShader();
			}

			if( randomShader )
				ShaderEditorApplication
					.preferences
					.setWallpaperShader( cursor.getLong(
						cursor.getColumnIndex(
							DataSource.SHADERS_ID ) ) );

			if( view != null )
				view.getRenderer().setFragmentShader(
					cursor.getString( cursor.getColumnIndex(
						DataSource.SHADERS_FRAGMENT_SHADER ) ),
					cursor.getFloat( cursor.getColumnIndex(
						DataSource.SHADERS_QUALITY ) ) );

			cursor.close();
		}

		private class ShaderWallpaperView extends ShaderView
		{
			public ShaderWallpaperView()
			{
				super(
					ShaderWallpaperService.this,
					ShaderEditorApplication.preferences.isBatteryLow() ?
						GLSurfaceView.RENDERMODE_WHEN_DIRTY :
						GLSurfaceView.RENDERMODE_CONTINUOUSLY );
			}

			@Override
			public final SurfaceHolder getHolder()
			{
				return ShaderWallpaperEngine
					.this
					.getSurfaceHolder();
			}

			public void destroy()
			{
				super.onDetachedFromWindow();
			}
		}
	}
}
