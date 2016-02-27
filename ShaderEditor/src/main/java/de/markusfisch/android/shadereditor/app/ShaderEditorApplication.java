package de.markusfisch.android.shadereditor.app;

import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.R;

import android.app.Application;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;

public class ShaderEditorApplication extends Application
{
	public static int systemBarColor;
	public static Preferences preferences;
	public static DataSource dataSource;
	public static boolean batteryLow = false;

	@Override
	public void onCreate()
	{
		super.onCreate();

		systemBarColor =
			ContextCompat.getColor(
				this,
				R.color.primary_dark_translucent );

		PreferenceManager.setDefaultValues(
			this,
			R.xml.preferences,
			false );

		preferences = new Preferences( this );
		dataSource = new DataSource( this );
	}
}
