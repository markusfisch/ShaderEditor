package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.Database;
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
		implements SharedPreferences.OnSharedPreferenceChangeListener {
	@Override
	public void onCreatePreferences(Bundle state, String rootKey) {
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public void onResume() {
		super.onResume();

		getPreferenceScreen()
				.getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

		setSummaries(getPreferenceScreen());
	}

	@Override
	public void onPause() {
		super.onPause();

		getPreferenceScreen()
				.getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(
			SharedPreferences sharedPreferences,
			String key) {
		Preference preference = findPreference(key);
		if (preference == null) {
			return;
		}

		ShaderEditorApp.preferences.update();
		setSummary(preference);

		if (Preferences.SAVE_BATTERY.equals(key) &&
				ShaderEditorApp.preferences.isBatteryLow()) {
			BatteryLevelReceiver.setLowPowerMode(
					getActivity(),
					ShaderEditorApp.preferences.saveBattery());
		}
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		if (preference instanceof ShaderListPreference) {
			DialogFragment f = ShaderListPreferenceDialogFragment
					.newInstance(preference.getKey());

			f.setTargetFragment(this, 0);
			f.show(getFragmentManager(),
					"ShaderListPreferenceDialogFragment");

			return;
		}

		super.onDisplayPreferenceDialog(preference);
	}

	private void setSummaries(PreferenceGroup screen) {
		for (int i = screen.getPreferenceCount(); i-- > 0; ) {
			setSummary(screen.getPreference(i));
		}
	}

	private void setSummary(Preference preference) {
		if (preference instanceof ShaderListPreference) {
			preference.setSummary(getWallpaperShaderSummary());
		} else if (preference instanceof ListPreference) {
			preference.setSummary(((ListPreference) preference).getEntry());
		} else if (preference instanceof PreferenceGroup) {
			setSummaries((PreferenceGroup) preference);
		}
	}

	private String getWallpaperShaderSummary() {
		long id = ShaderEditorApp.preferences.getWallpaperShader();
		Cursor cursor = ShaderEditorApp.db.getShader(id);

		if (Database.closeIfEmpty(cursor)) {
			return getString(R.string.no_shader_selected);
		}

		String summary = cursor.getString(
				cursor.getColumnIndex(Database.SHADERS_NAME));

		if (summary == null || summary.length() < 0) {
			summary = cursor.getString(
					cursor.getColumnIndex(Database.SHADERS_MODIFIED));
		}

		cursor.close();

		return summary;
	}
}
