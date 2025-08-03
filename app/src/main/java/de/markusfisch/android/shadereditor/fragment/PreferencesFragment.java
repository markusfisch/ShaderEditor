package de.markusfisch.android.shadereditor.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import de.markusfisch.android.shadereditor.database.DataRecords;
import de.markusfisch.android.shadereditor.database.DataSource;
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
	private ActivityResultLauncher<Intent> pickFileLauncher;
	private ActivityResultLauncher<String> requestReadPermissionLauncher;
	private ActivityResultLauncher<String> requestWritePermissionLauncher;
	private DataSource dataSource;

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);
		dataSource = Database.getInstance(requireContext()).getDataSource();

		// Register ActivityResultLaunchers
		pickFileLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> {
					if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
						Context context = getContext();
						if (context != null) {
							String message = DatabaseImporter.importDatabase(
									context, result.getData().getData());
							Toast.makeText(context, message, Toast.LENGTH_LONG).show();
						}
					}
				});

		requestReadPermissionLauncher = registerForActivityResult(
				new ActivityResultContracts.RequestPermission(),
				isGranted -> {
					if (isGranted) {
						ImportExportAsFiles.importFromDirectory(getContext());
					} else {
						Toast.makeText(getActivity(),
								getString(R.string.read_access_required),
								Toast.LENGTH_LONG).show();
					}
				});

		requestWritePermissionLauncher = registerForActivityResult(
				new ActivityResultContracts.RequestPermission(),
				isGranted -> {
					if (isGranted) {
						ImportExportAsFiles.exportToDirectory(getContext());
					} else {
						Toast.makeText(getActivity(),
								getString(R.string.write_access_required),
								Toast.LENGTH_LONG).show();
					}
				});

		// TODO: Is this needed given that `onCreatePreferences` does this too?
		addPreferencesFromResource(R.xml.preferences);
		wireImportExport();
	}

	@Override
	public void onCreatePreferences(Bundle state, String rootKey) {
		addPreferencesFromResource(R.xml.preferences);
		wireImportExport();
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences preferences = getPreferenceScreen().getSharedPreferences();
		if (preferences != null) {
			preferences.registerOnSharedPreferenceChangeListener(this);
		}

		setSummaries(getPreferenceScreen());
	}

	@Override
	public void onPause() {
		super.onPause();

		SharedPreferences preferences = getPreferenceScreen().getSharedPreferences();
		if (preferences != null) {
			preferences.unregisterOnSharedPreferenceChangeListener(this);
		}
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
		if (preference instanceof ShaderListPreference listPreference) {
			String key = listPreference.getKey();
			DialogFragment f = ShaderListPreferenceDialogFragment.newInstance(key);
			// FIXME: Periodically check whether androidx.preference still uses TargetFragment
			// noinspection deprecation
			f.setTargetFragment(this, 0);

			f.show(getParentFragmentManager(), "ShaderListPreferenceDialogFragment");

			return;
		}

		super.onDisplayPreferenceDialog(preference);
	}

	private void setSummaries(@NonNull PreferenceGroup screen) {
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
		if (id <= 0) {
			return getString(R.string.no_shader_selected);
		}
		DataRecords.Shader shader = dataSource.shader.getShader(id);
		if (shader == null) {
			return getString(R.string.no_shader_selected);
		}
		return shader.getTitle();
	}

	private boolean checkExternalStoragePermission(@NonNull String permission) {
		FragmentActivity activity = getActivity();
		if (activity != null &&
				ContextCompat.checkSelfPermission(activity, permission)
						!= PackageManager.PERMISSION_GRANTED) {

			if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				requestWritePermissionLauncher.launch(permission);
			} else if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
				requestReadPermissionLauncher.launch(permission);
			}
			return false;
		}

		return true;
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
							Manifest.permission.READ_EXTERNAL_STORAGE)) {
						ImportExportAsFiles.importFromDirectory(getContext());
					}
					return true;
				});
				exportToDirectory.setOnPreferenceClickListener(preference -> {
					if (checkExternalStoragePermission(
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
				pickFileLauncher.launch(
						Intent.createChooser(
								chooseFile,
								getString(R.string.import_database)));
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
