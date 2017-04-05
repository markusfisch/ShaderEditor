package de.markusfisch.android.shadereditor.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.receiver.BatteryLevelReceiver;
import de.markusfisch.android.shadereditor.widget.ShaderView;

public class ShaderWallpaperService extends WallpaperService {
	public static final String RENDER_MODE = "render_mode";

	private ShaderWallpaperEngine engine;

	@Override
	public void onCreate() {
		super.onCreate();
		PackageManager pm  = getPackageManager();
		ComponentName componentName = new ComponentName(ShaderWallpaperService.this, BatteryLevelReceiver.class);
		pm.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
	}

	@Override
	public int onStartCommand(
			Intent intent,
			int flags,
			int startId) {
		if (intent != null && engine != null) {
			int renderMode = intent.getIntExtra(RENDER_MODE, -1);

			if (renderMode > -1) {
				engine.setRenderMode(renderMode);
			}
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public Engine onCreateEngine() {
		return (engine = new ShaderWallpaperEngine());
	}

	private class ShaderWallpaperEngine
			extends Engine
			implements SharedPreferences.OnSharedPreferenceChangeListener {
		private final Handler handler = new Handler();

		private ShaderWallpaperView view;

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences preferences,
				String key) {
			if (Preferences.WALLPAPER_SHADER.equals(key)) {
				setShader();
			}
		}

		@Override
		public void onCreate(SurfaceHolder holder) {
			super.onCreate(holder);
			view = new ShaderWallpaperView();
			setShader();
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			view.destroy();
			view = null;
			PackageManager pm  = getPackageManager();
			ComponentName componentName = new ComponentName(ShaderWallpaperService.this, BatteryLevelReceiver.class);
			pm.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			super.onVisibilityChanged(visible);

			if (visible) {
				view.onResume();
			} else {
				view.onPause();
			}
		}

		@Override
		public void onTouchEvent(MotionEvent e) {
			super.onTouchEvent(e);
			view.getRenderer().touchAt(e);
		}

		@Override
		public void onOffsetsChanged(
				float xOffset,
				float yOffset,
				float xStep,
				float yStep,
				int xPixels,
				int yPixels) {
			view.getRenderer().setOffset(xOffset, yOffset);
		}

		private ShaderWallpaperEngine() {
			super();

			ShaderEditorApplication
					.preferences
					.getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);

			setTouchEventsEnabled(true);
		}

		private void setRenderMode(int renderMode) {
			if (view == null) {
				return;
			}

			view.setRenderMode(renderMode);
		}

		private void setShader() {
			if (!ShaderEditorApplication.dataSource.isOpen()) {
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						setShader();
					}
				}, 100);

				return;
			}

			Cursor cursor = ShaderEditorApplication
					.dataSource
					.getShader(ShaderEditorApplication
							.preferences
							.getWallpaperShader());

			boolean randomShader = false;

			while (cursor == null || !cursor.moveToFirst()) {
				if (cursor != null) {
					cursor.close();
				}

				if (randomShader) {
					return;
				}

				randomShader = true;
				cursor = ShaderEditorApplication
						.dataSource
						.getRandomShader();
			}

			if (randomShader) {
				ShaderEditorApplication.preferences.setWallpaperShader(
						cursor.getLong(cursor.getColumnIndex(
								DataSource.SHADERS_ID)));
			}

			if (view != null) {
				view.getRenderer().setFragmentShader(
						cursor.getString(cursor.getColumnIndex(
								DataSource.SHADERS_FRAGMENT_SHADER)),
						cursor.getFloat(cursor.getColumnIndex(
								DataSource.SHADERS_QUALITY)));
			}

			cursor.close();
		}

		private class ShaderWallpaperView extends ShaderView {
			public ShaderWallpaperView() {
				super(ShaderWallpaperService.this,
						ShaderEditorApplication.preferences.isBatteryLow() ?
								GLSurfaceView.RENDERMODE_WHEN_DIRTY :
								GLSurfaceView.RENDERMODE_CONTINUOUSLY);
			}

			@Override
			public final SurfaceHolder getHolder() {
				return ShaderWallpaperEngine.this.getSurfaceHolder();
			}

			public void destroy() {
				super.onDetachedFromWindow();
			}
		}
	}
}
