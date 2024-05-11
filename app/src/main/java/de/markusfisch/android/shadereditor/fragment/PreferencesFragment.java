package de.markusfisch.android.shadereditor.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.io.DatabaseExporter;
import de.markusfisch.android.shadereditor.io.DatabaseImporter;
import de.markusfisch.android.shadereditor.io.ImportExportAsFiles;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.preference.ShaderListPreference;
import de.markusfisch.android.shadereditor.receiver.BatteryLevelReceiver;

public class PreferencesFragment
		extends PreferenceFragmentCompat
		implements SharedPreferences.OnSharedPreferenceChangeListener {
	private static final int READ_EXTERNAL_STORAGE_REQUEST = 1;
	private static final int WRITE_EXTERNAL_STORAGE_REQUEST = 2;
	private static final int PICK_FILE_RESULT_CODE = 1;

	@Override
	public void onActivityResult(int requestCode, int resultCode,
			Intent resultData) {
		if (requestCode == PICK_FILE_RESULT_CODE &&
				resultCode == Activity.RESULT_OK && resultData != null) {
			Context context = getContext();
			if (context == null) {
				return;
			}
			String message = DatabaseImporter.importDatabase(
					context, resultData.getData());
			Toast.makeText(context, message,
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onCreatePreferences(Bundle state, String rootKey) {
		addPreferencesFromResource(R.xml.preferences);
		wireImportExport();
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

		ShaderEditorApp.preferences.update(getContext());
		setSummary(preference);

		if (Preferences.SAVE_BATTERY.equals(key) &&
				ShaderEditorApp.preferences.isBatteryLow()) {
			BatteryLevelReceiver.setLowPowerMode(
					ShaderEditorApp.preferences.saveBattery());
		}
	}

	@Override
	public void onDisplayPreferenceDialog(@NonNull Preference preference) {
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
			long id = Preferences.WALLPAPER_SHADER.equals(preference.getKey())
					? ShaderEditorApp.preferences.getWallpaperShader()
					: ShaderEditorApp.preferences.getDefaultNewShader();
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

		String summary = Database.getString(
				cursor, Database.SHADERS_NAME);

		if (summary == null || summary.isEmpty()) {
			summary = Database.getString(
					cursor, Database.SHADERS_MODIFIED);
		}

		cursor.close();

		return summary;
	}

	public boolean checkExternalStoragePermission(
			int request,
			String permission) {
		FragmentActivity activity = getActivity();
		if (activity != null &&
				ContextCompat.checkSelfPermission(activity, permission)
						!= PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(
					activity,
					permission)) {
				int strId = request == WRITE_EXTERNAL_STORAGE_REQUEST
						? R.string.write_access_required
						: R.string.read_access_required;
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
		for (int grantResult : grantResults) {
			if (grantResult == PackageManager.PERMISSION_GRANTED) {
				switch (requestCode) {
					default:
						// Make FindBugs happy.
						continue;
					case WRITE_EXTERNAL_STORAGE_REQUEST:
						ImportExportAsFiles.exportToDirectory(getContext());
						break;
					case READ_EXTERNAL_STORAGE_REQUEST:
						ImportExportAsFiles.importFromDirectory(getContext());
						break;
				}
			} else {
				int messageId;
				switch (requestCode) {
					default:
						// Make FindBugs happy.
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

	private void wireImportExport() {
		Preference importFromDirectory = findPreference(
				Preferences.IMPORT_FROM_DIRECTORY);
		Preference exportToDirectory = findPreference(
				Preferences.EXPORT_TO_DIRECTORY);
		if (importFromDirectory != null && exportToDirectory != null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
				importFromDirectory.setOnPreferenceClickListener(preference -> {
					if (checkExternalStoragePermission(
							READ_EXTERNAL_STORAGE_REQUEST,
							Manifest.permission.READ_EXTERNAL_STORAGE)) {
						ImportExportAsFiles.importFromDirectory(getContext());
					}
					return true;
				});
				exportToDirectory.setOnPreferenceClickListener(preference -> {
					if (checkExternalStoragePermission(
							WRITE_EXTERNAL_STORAGE_REQUEST,
							Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
						ImportExportAsFiles.exportToDirectory(getContext());
					}
					return true;
				});
			} else {
				PreferenceCategory cat = findPreference("import_export");
				if (cat != null) {
					cat.removePreference(importFromDirectory);
					cat.removePreference(exportToDirectory);
				}
			}
		}
		Preference importDatabase = findPreference(Preferences.IMPORT_DATABASE);
		Preference exportDatabase = findPreference(Preferences.EXPORT_DATABASE);
		if (importDatabase != null && exportDatabase != null) {
			importDatabase.setOnPreferenceClickListener(preference -> {
				Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
				// In theory, it should be "application/x-sqlite3"
				// or the newer "application/vnd.sqlite3" but
				// only "application/octet-stream" works.
				chooseFile.setType("application/octet-stream");
				startActivityForResult(
						Intent.createChooser(
								chooseFile,
								getString(R.string.import_database)),
						PICK_FILE_RESULT_CODE);
				return true;
			});
			exportDatabase.setOnPreferenceClickListener(preference -> {
				Context context = getContext();
				if (context != null) {
					Toast.makeText(context,
							DatabaseExporter.exportDatabase(context),
							Toast.LENGTH_LONG).show();
				}
				return true;
			});
		}
	}
}
