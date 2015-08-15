package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.preference.ShaderListPreference;
import de.markusfisch.android.shadereditor.R;

import android.database.Cursor;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;

public class PreferencesActivity
	extends PreferenceActivity
	implements SharedPreferences.OnSharedPreferenceChangeListener
{
	@Override
	public void onCreate( Bundle state )
	{
		super.onCreate( state );

		getPreferenceManager().setSharedPreferencesName(
			Preferences.NAME );

		addPreferencesFromResource( R.xml.preferences );
	}

	@Override
	public void onResume()
	{
		super.onResume();

		getPreferenceScreen()
			.getSharedPreferences()
			.registerOnSharedPreferenceChangeListener(
				this );

		initSummaries( getPreferenceScreen() );
	}

	@Override
	public void onPause()
	{
		super.onPause();

		getPreferenceScreen()
			.getSharedPreferences()
			.unregisterOnSharedPreferenceChangeListener(
				this );
	}

	@Override
	public void onSharedPreferenceChanged(
		SharedPreferences sharedPreferences,
		String key )
	{
		Preference preference = findPreference( key );

		if( preference == null )
			return;

		ShaderEditorApplication
			.preferences
			.update();

		initSummary( preference );
	}

	@Override
	protected boolean isValidFragment( String fragmentName )
	{
		// Prevents fragment ingnition; required on KITKAT and later.
		// Except, this app does not use a settings fragment and
		// therefore this method never gets called.
		// But without it, we would make lint cry and it's probably
		// better to implement it than to suppress the warning.
		return false;
	}

	private void initSummaries( PreferenceGroup screen )
	{
		for( int n = screen.getPreferenceCount(); n-- > 0; )
			initSummary( screen.getPreference( n ) );
	}

	private void initSummary( Preference preference )
	{
		if( preference instanceof ShaderListPreference )
			preference.setSummary(
				getWallpaperShaderSummary() );
		else if( preference instanceof ListPreference )
			preference.setSummary(
				((ListPreference)preference).getEntry() );
		else if( preference instanceof PreferenceGroup )
			initSummaries(
				(PreferenceGroup)preference );
	}

	private String getWallpaperShaderSummary()
	{
		long id = ShaderEditorApplication
			.preferences
			.getWallpaperShader();
		Cursor cursor = ShaderEditorApplication
			.dataSource
			.getShader( id );

		if( DataSource.closeIfEmpty( cursor ) )
			return getString( R.string.no_shader_selected );

		String summary = cursor.getString(
			cursor.getColumnIndex(
				DataSource.SHADERS_MODIFIED ) );

		cursor.close();

		return summary;
	}
}
