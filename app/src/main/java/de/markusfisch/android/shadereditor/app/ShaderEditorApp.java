package de.markusfisch.android.shadereditor.app;

import android.app.Application;
import android.os.StrictMode;

import de.markusfisch.android.shadereditor.BuildConfig;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.view.UndoRedo;

public class ShaderEditorApp extends Application {
	public static final Preferences preferences = new Preferences();
	public static final Database db = new Database();
	public static final UndoRedo.EditHistory editHistory =
			new UndoRedo.EditHistory();

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
	}
}
