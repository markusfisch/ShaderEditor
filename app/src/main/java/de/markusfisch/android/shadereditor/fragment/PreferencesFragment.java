package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.database.ImportExport;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.preference.ShaderListPreference;
import de.markusfisch.android.shadereditor.receiver.BatteryLevelReceiver;
import de.markusfisch.android.shadereditor.R;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.widget.Toast;

public class PreferencesFragment
		extends PreferenceFragmentCompat
		implements SharedPreferences.OnSharedPreferenceChangeListener {
	private static final int READ_EXTERNAL_STORAGE_REQUEST = 1;
	private static final int WRITE_EXTERNAL_STORAGE_REQUEST = 2;

	@Override
	public void onCreatePreferences(Bundle state, String rootKey) {
		addPreferencesFromResource(R.xml.preferences);

		Preference importPreference = findPreference(
				Preferences.IMPORT_FROM_DIRECTORY);
		importPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (checkExternalStoragePermission(
						READ_EXTERNAL_STORAGE_REQUEST,
						Manifest.permission.READ_EXTERNAL_STORAGE)) {
					ImportExport.importFromDirectory(getContext());
				}
				return true;
			}
		});

		Preference exportPreference = findPreference(
				Preferences.EXPORT_TO_DIRECTORY);
		exportPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (checkExternalStoragePermission(
						WRITE_EXTERNAL_STORAGE_REQUEST,
						Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					ImportExport.exportToDirectory(getContext());
				}
				return true;
			}
		});
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
			long id = Preferences.WALLPAPER_SHADER.equals(preference.getKey()) ?
					ShaderEditorApp.preferences.getWallpaperShader() :
					ShaderEditorApp.preferences.getDefaultNewShader();
			preference.setSummary(getShaderSummary(id));
		} else if (preference instanceof ListPreference) {
			preference.setSummary(((ListPreference) preference).getEntry());
		} else if (preference instanceof PreferenceGroup) {
			setSummaries((PreferenceGroup) preference);
		}
	}

	private String getShaderSummary(long id) {
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

	public boolean checkExternalStoragePermission(
			int request,
			String permission) {
		FragmentActivity activity = getActivity();
		if (ContextCompat.checkSelfPermission(activity, permission)
				!= PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(
					activity,
					permission)) {
				int strId = request == WRITE_EXTERNAL_STORAGE_REQUEST ?
						R.string.write_access_required :
						R.string.read_access_required;
				Toast.makeText(getActivity(), getString(strId),
						Toast.LENGTH_LONG).show();
			}
			ActivityCompat.requestPermissions(
					activity,
					new String[]{permission},
					request);
			return false;
		}

		return true;
	}

	@Override
	public void onRequestPermissionsResult(
			int requestCode,
			@NonNull String[] permissions,
			@NonNull int[] grantResults) {
		for (int i = 0, l = grantResults.length; i < l; ++i) {
			if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
				switch (requestCode) {
					default:
						// make FindBugs happy
						continue;
					case WRITE_EXTERNAL_STORAGE_REQUEST:
						ImportExport.exportToDirectory(getContext());
						break;
					case READ_EXTERNAL_STORAGE_REQUEST:
						ImportExport.importFromDirectory(getContext());
						break;
				}
			} else {
				int messageId = 0;
				switch (requestCode) {
					default:
						// make FindBugs happy
						continue;
					case WRITE_EXTERNAL_STORAGE_REQUEST:
						messageId = R.string.write_access_required;
						break;
					case READ_EXTERNAL_STORAGE_REQUEST:
						messageId = R.string.read_access_required;
						break;
				}
				Toast.makeText(getActivity(),
						getString(messageId),
						Toast.LENGTH_LONG).show();
			}
		}
	}
}
