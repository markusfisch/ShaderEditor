package de.markusfisch.android.shadereditor.app;

import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.preference.Preferences;

import android.app.Application;

public class ShaderEditorApplication extends Application
{
	public static final Preferences preferences = new Preferences();
	public static final DataSource dataSource = new DataSource();

	@Override
	public void onCreate()
	{
		super.onCreate();

		preferences.init( this );
		dataSource.init( this );
	}
}
