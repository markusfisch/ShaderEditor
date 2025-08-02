package de.markusfisch.android.shadereditor.service;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.DataRecords;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.receiver.BatteryLevelReceiver;
import de.markusfisch.android.shadereditor.widget.ShaderView;

public class ShaderWallpaperService extends WallpaperService {
	private static ShaderWallpaperEngine engine;

	private ComponentName batteryLevelComponent;

	public static boolean isRunning() {
		return engine != null;
	}

	public static void setRenderMode(int renderMode) {
		if (engine != null) {
			engine.setRenderMode(renderMode);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		batteryLevelComponent = new ComponentName(this,
				BatteryLevelReceiver.class);
		enableComponent(batteryLevelComponent, true);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		enableComponent(batteryLevelComponent, false);
		engine = null;
	}

	@Override
	public Engine onCreateEngine() {
		engine = new ShaderWallpaperEngine();
		return engine;
	}

	private class ShaderWallpaperEngine
			extends Engine
			implements SharedPreferences.OnSharedPreferenceChangeListener {
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
			// Unregister listener to prevent memory leaks.
			ShaderEditorApp.preferences.getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(this);
			if (view != null) {
				view.destroy();
				view = null;
			}
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			super.onVisibilityChanged(visible);
			if (view == null) return;
			if (visible) {
				view.onResume();
			} else {
				view.onPause();
			}
		}

		@Override
		public void onTouchEvent(MotionEvent e) {
			super.onTouchEvent(e);
			if (view != null) {
				view.getRenderer().touchAt(e);
			}
		}

		@Override
		public void onOffsetsChanged(
				float xOffset,
				float yOffset,
				float xStep,
				float yStep,
				int xPixels,
				int yPixels) {
			if (view != null) {
				view.getRenderer().setOffset(xOffset, yOffset);
			}
		}

		private ShaderWallpaperEngine() {
			super();
			ShaderEditorApp.preferences.getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);
			setTouchEventsEnabled(true);
		}

		private void setRenderMode(int renderMode) {
			if (view != null) {
				view.setRenderMode(renderMode);
			}
		}

		private void setShader() {
			DataSource dataSource = Database.getInstance(
					ShaderWallpaperService.this).getDataSource();

			long shaderId = ShaderEditorApp.preferences.getWallpaperShader();
			DataRecords.Shader shader = dataSource.shader.getShader(shaderId);

			// If the saved shader doesn't exist, pick a random one.
			if (shader == null) {
				shader = dataSource.shader.getRandomShader();

				// If there are no shaders at all, we can't do anything.
				if (shader == null) {
					return;
				}

				// Update the preferences to store the new random shader ID.
				ShaderEditorApp.preferences.setWallpaperShader(shader.id());
			}

			if (view != null) {
				view.getRenderer().setFragmentShader(
						shader.fragmentShader(),
						shader.quality());
			}
		}

		private class ShaderWallpaperView extends ShaderView {
			public ShaderWallpaperView() {
				super(ShaderWallpaperService.this,
						ShaderEditorApp.preferences.isBatteryLow()
								? GLSurfaceView.RENDERMODE_WHEN_DIRTY
								: GLSurfaceView.RENDERMODE_CONTINUOUSLY);
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

	private void enableComponent(ComponentName name, boolean enable) {
		getPackageManager().setComponentEnabledSetting(name,
				enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
						PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}
}