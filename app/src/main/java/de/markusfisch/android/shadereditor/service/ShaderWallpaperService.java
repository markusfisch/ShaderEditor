package de.markusfisch.android.shadereditor.service;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.receiver.BatteryLevelReceiver;
import de.markusfisch.android.shadereditor.widget.ShaderView;

public class ShaderWallpaperService extends WallpaperService {
	@Nullable
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

	@NonNull
	@Override
	public Engine onCreateEngine() {
		engine = new ShaderWallpaperEngine();
		return engine;
	}

	private class ShaderWallpaperEngine
			extends Engine
			implements SharedPreferences.OnSharedPreferenceChangeListener {
		private final Handler handler = new Handler();

		@Nullable
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
		public void onTouchEvent(@NonNull MotionEvent e) {
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

			ShaderEditorApp.preferences.getSharedPreferences()
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
			if (!ShaderEditorApp.db.isOpen()) {
				handler.postDelayed(this::setShader, 100);

				return;
			}

			Cursor cursor = ShaderEditorApp.db.getShader(
					ShaderEditorApp.preferences.getWallpaperShader());

			boolean randomShader = false;

			while (cursor == null || !cursor.moveToFirst()) {
				if (cursor != null) {
					cursor.close();
				}

				if (randomShader) {
					return;
				}

				randomShader = true;
				cursor = ShaderEditorApp.db.getRandomShader();
			}

			if (randomShader) {
				ShaderEditorApp.preferences.setWallpaperShader(
						Database.getLong(cursor, Database.SHADERS_ID));
			}

			if (view != null) {
				view.getRenderer().setFragmentShader(
						Database.getString(cursor, Database.SHADERS_FRAGMENT_SHADER),
						Database.getFloat(cursor, Database.SHADERS_QUALITY));
			}

			cursor.close();
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

	private void enableComponent(@NonNull ComponentName name, boolean enable) {
		getPackageManager().setComponentEnabledSetting(name,
				enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
						PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}
}
