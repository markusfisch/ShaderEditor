package de.markusfisch.android.shadereditor.service;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.MutableLiveData;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.receiver.BatteryLevelReceiver;
import de.markusfisch.android.shadereditor.widget.ShaderView;

class LockScreenObserver extends BroadcastReceiver implements DefaultLifecycleObserver {
	private final Context context;

	private final MutableLiveData<Boolean> isScreenLockedObserver;

	LockScreenObserver(Context context, MutableLiveData<Boolean> isScreenLockedObserver) {
		this.context = context;
		this.isScreenLockedObserver = isScreenLockedObserver;
	}

	@Override
	public void onCreate(@NonNull LifecycleOwner owner) {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_USER_PRESENT);
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);

		context.registerReceiver(this, intentFilter);
	}

	@Override
	public void onDestroy(@NonNull LifecycleOwner owner) {
		context.unregisterReceiver(this);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			return;
		}
		switch (intent.getAction()) {
			case Intent.ACTION_SCREEN_ON:
				KeyguardManager keyguardManager = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
				if (!keyguardManager.isKeyguardLocked()) {
					isScreenLockedObserver.setValue(false);
				}
				break;
			case Intent.ACTION_SCREEN_OFF:
				isScreenLockedObserver.setValue(true);
				break;
			case Intent.ACTION_USER_PRESENT:
				isScreenLockedObserver.setValue(false);
				break;
		}
	}
}

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
			implements SharedPreferences.OnSharedPreferenceChangeListener, LifecycleOwner {
		private final Handler handler = new Handler();

		private ShaderWallpaperView view;

		private final MutableLiveData<Boolean> isScreenLockedObserver = new MutableLiveData<Boolean>(false);

		private final LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

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

			isScreenLockedObserver.observe(this, (isScreenLocked) -> {
				setShader();
			});

			lifecycleRegistry.addObserver(new LockScreenObserver(view.getContext(), isScreenLockedObserver));
			lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
		}

		@Override
		public void onDestroy() {
			lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
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

		@NonNull
		@Override
		public Lifecycle getLifecycle() {
			return lifecycleRegistry;
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

			Cursor cursor;
			if (Boolean.TRUE.equals(isScreenLockedObserver.getValue())) {
				cursor = ShaderEditorApp.db.getShader(2);
//						ShaderEditorApp.preferences.getLockScreenWallpaperShader());
			} else {
				cursor = ShaderEditorApp.db.getShader(1);
//						ShaderEditorApp.preferences.getWallpaperShader());
			}

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
				Log.d("@@@", "name" + Database.getString(cursor, Database.SHADERS_NAME));
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

	private void enableComponent(ComponentName name, boolean enable) {
		getPackageManager().setComponentEnabledSetting(name,
				enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
						PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}
}
