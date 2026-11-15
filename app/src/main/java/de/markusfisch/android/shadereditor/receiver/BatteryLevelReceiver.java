package de.markusfisch.android.shadereditor.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.BatteryManager;
import android.os.Build;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.service.ShaderWallpaperService;

public class BatteryLevelReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (Intent.ACTION_BATTERY_LOW.equals(action)) {
			setLowPowerMode(true);
		} else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
			setLowPowerMode(false);
		} else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
			int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			boolean isConnected = status == BatteryManager.BATTERY_STATUS_CHARGING ||
					status == BatteryManager.BATTERY_STATUS_FULL;
			setPowerConnected(isConnected);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				setLowPowerMode(intent.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW,
						ShaderEditorApp.preferences.isBatteryLow()));
			} else {
				updateRenderMode();
			}
		}
	}

	private static void setPowerConnected(boolean connected) {
		ShaderEditorApp.preferences.setPowerConnected(connected);
	}

	public static void setLowPowerMode(boolean low) {
		ShaderEditorApp.preferences.setBatteryLow(low);
		updateRenderMode();
	}

	public static void updateRenderMode() {
		ShaderWallpaperService.setRenderMode(ShaderEditorApp.preferences.isLowPowerMode()
				? GLSurfaceView.RENDERMODE_WHEN_DIRTY
				: GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	}
}
