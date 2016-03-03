package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.fragment.ShaderListPreferenceDialogFragment;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.preference.ShaderListPreference;
import de.markusfisch.android.shadereditor.receiver.BatteryLevelReceiver;
import de.markusfisch.android.shadereditor.R;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;

public class PreferencesFragment
	extends PreferenceFragmentCompat
	implements SharedPreferences.OnSharedPreferenceChangeListener
{
	@Override
	public void onCreatePreferences( Bundle state, String rootKey )
	{
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

		setSummaries( (PreferenceGroup)getPreferenceScreen() );
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

		setSummary( preference );

		if( Preferences.SAVE_BATTERY.equals( key ) &&
			ShaderEditorApplication.preferences.isBatteryLow() )
			BatteryLevelReceiver.setLowPowerMode(
				getActivity(),
				ShaderEditorApplication
					.preferences
					.saveBattery() );
	}

	@Override
	public void onDisplayPreferenceDialog( Preference preference )
	{
		if( preference instanceof ShaderListPreference )
		{
			DialogFragment f =
				ShaderListPreferenceDialogFragment
					.newInstance( preference.getKey() );

			f.setTargetFragment( this, 0 );
			f.show(
				getFragmentManager(),
				"ShaderListPreferenceDialogFragment" );

			return;
		}

		super.onDisplayPreferenceDialog( preference );
	}

	private void setSummaries( PreferenceGroup screen )
	{
		for( int n = screen.getPreferenceCount(); n-- > 0; )
			setSummary( screen.getPreference( n ) );
	}

	private void setSummary( Preference preference )
	{
		if( preference instanceof ShaderListPreference )
			preference.setSummary(
				getWallpaperShaderSummary() );
		else if( preference instanceof ListPreference )
			preference.setSummary(
				((ListPreference)preference).getEntry() );
		else if( preference instanceof PreferenceGroup )
			setSummaries(
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
