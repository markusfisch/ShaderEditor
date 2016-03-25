package de.markusfisch.android.shadereditor.app;

import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.BuildConfig;

import android.app.Application;
import android.os.StrictMode;

public class ShaderEditorApplication extends Application
{
	public static final Preferences preferences = new Preferences();
	public static final DataSource dataSource = new DataSource();

	@Override
	public void onCreate()
	{
		super.onCreate();

		preferences.init( this );
		dataSource.openAsync( this );

		if( BuildConfig.DEBUG )
		{
			StrictMode.setThreadPolicy(
				new StrictMode.ThreadPolicy.Builder()
					.detectAll()
					.penaltyLog()
					.build() );

			StrictMode.setVmPolicy(
				new StrictMode.VmPolicy.Builder()
					.detectLeakedSqlLiteObjects()
					.penaltyLog()
					.penaltyDeath()
					.build() );
		}
	}
}
