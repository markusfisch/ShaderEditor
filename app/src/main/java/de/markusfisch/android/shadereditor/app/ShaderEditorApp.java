package de.markusfisch.android.shadereditor.app;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.StrictMode;

import de.markusfisch.android.shadereditor.BuildConfig;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.receiver.BatteryLevelReceiver;
import de.markusfisch.android.shadereditor.view.UndoRedo;

public class ShaderEditorApp extends Application {
	public static final Preferences preferences = new Preferences();
	public static final Database db = new Database();
	public static final UndoRedo.EditHistory editHistory =
			new UndoRedo.EditHistory();

	private static final BatteryLevelReceiver batteryLevelReceiver =
			new BatteryLevelReceiver();

	@Override
	public void onCreate() {
		super.onCreate();

		if (BuildConfig.DEBUG) {
			StrictMode.setThreadPolicy(
					new StrictMode.ThreadPolicy.Builder()
							.detectAll()
							.penaltyLog()
							.build());

			StrictMode.setVmPolicy(
					new StrictMode.VmPolicy.Builder()
							.detectLeakedSqlLiteObjects()
							.penaltyLog()
							.penaltyDeath()
							.build());
		}

		preferences.init(this);
		db.open(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			registerBatteryReceiver();
		}
	}

	private void registerBatteryReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_LOW);
		filter.addAction(Intent.ACTION_BATTERY_OKAY);
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(batteryLevelReceiver, filter);
		// Note it's not required to unregister the receiver because it
		// needs to be there as long as this application is running.
	}
}
