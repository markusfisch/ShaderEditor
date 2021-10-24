package de.markusfisch.android.shadereditor.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.BatteryManager;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.service.ShaderWallpaperService;

public class BatteryLevelReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (Intent.ACTION_BATTERY_LOW.equals(action)) {
			setLowPowerMode(true);
			setPowerConnected(false);
		} else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
			setLowPowerMode(false);
			setPowerConnected(false);
		} else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
			int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			boolean isConnected = status == BatteryManager.BATTERY_STATUS_CHARGING ||
					status == BatteryManager.BATTERY_STATUS_FULL;
			if (isConnected) {
				setLowPowerMode(false);
			}
			setPowerConnected(isConnected);
		}
	}

	private static void setPowerConnected(boolean connected) {
		ShaderEditorApp.preferences.setPowerConnected(connected);
	}

	public static void setLowPowerMode(boolean low) {
		if (!ShaderEditorApp.preferences.saveBattery()) {
			low = false;
		}
		// fall through to update battery flag and
		// render mode because the preference may
		// have changed while battery is low

		ShaderEditorApp.preferences.setBatteryLow(low);
		ShaderWallpaperService.setRenderMode(low
				? GLSurfaceView.RENDERMODE_WHEN_DIRTY
				: GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	}
}
