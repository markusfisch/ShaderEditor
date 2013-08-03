package de.markusfisch.android.shadereditor;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class ShaderWallpaperService extends WallpaperService
{
	@Override
	public final Engine onCreateEngine()
	{
		return new ShaderWallpaperEngine();
	}

	private class ShaderWallpaperEngine
		extends Engine
		implements SharedPreferences.OnSharedPreferenceChangeListener
	{
		private ShaderWallpaperView view = null;
		private String fragmentShader = null;

		public ShaderWallpaperEngine()
		{
			super();

			PreferenceManager.setDefaultValues(
				ShaderWallpaperService.this,
				R.xml.preferences,
				false );

			SharedPreferences p =
				ShaderWallpaperService.this.getSharedPreferences(
					ShaderPreferenceActivity.SHARED_PREFERENCES_NAME,
					0 );

			p.registerOnSharedPreferenceChangeListener( this );
			onSharedPreferenceChanged( p, null );

			setTouchEventsEnabled( true );
		}

		@Override
		public void onSharedPreferenceChanged(
			SharedPreferences p,
			String key )
		{
			ShaderDataSource dataSource = new ShaderDataSource(
				ShaderWallpaperService.this );

			dataSource.open();

			final long id = Long.parseLong(
				p.getString( ShaderPreferenceActivity.SHADER, "1" ) );

			if( (fragmentShader = dataSource.getShader( id )) == null )
			{
				Cursor c = dataSource.getRandomShader();

				if( c != null )
				{
					fragmentShader = c.getString( c.getColumnIndex(
						ShaderDataSource.COLUMN_SHADER ) );

					ShaderListPreference.saveShader(
						p,
						c.getLong( c.getColumnIndex(
							ShaderDataSource.COLUMN_ID ) ) );
				}
			}

			if( view != null )
				view.renderer.fragmentShader = fragmentShader;

			dataSource.close();
		}

		@Override
		public void onCreate( SurfaceHolder holder )
		{
			super.onCreate( holder );

			view = new ShaderWallpaperView();
			view.renderer.fragmentShader = fragmentShader;
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
			{
				view.onResume();
				view.requestRender();
			}
			else
				view.onPause();
		}

		@Override
		public void onTouchEvent( MotionEvent e )
		{
			super.onTouchEvent( e );

			view.renderer.onTouch( e.getX(), e.getY() );
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
			view.renderer.offset[0] = xOffset;
			view.renderer.offset[1] = yOffset;
		}

		private class ShaderWallpaperView extends ShaderView
		{
			public ShaderWallpaperView()
			{
				super( ShaderWallpaperService.this );
			}

			@Override
			public final SurfaceHolder getHolder()
			{
				return ShaderWallpaperEngine.this.getSurfaceHolder();
			}

			public void destroy()
			{
				super.onDetachedFromWindow();
			}
		}
	}
}
