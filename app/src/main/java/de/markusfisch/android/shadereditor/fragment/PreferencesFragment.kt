package de.markusfisch.android.shadereditor.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.app.ShaderEditorApp
import de.markusfisch.android.shadereditor.database.Database
import de.markusfisch.android.shadereditor.io.DatabaseExporter
import de.markusfisch.android.shadereditor.io.DatabaseImporter
import de.markusfisch.android.shadereditor.io.ImportExportAsFiles
import de.markusfisch.android.shadereditor.preference.Preferences
import de.markusfisch.android.shadereditor.preference.ShaderListPreference
import de.markusfisch.android.shadereditor.receiver.BatteryLevelReceiver

class PreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        context.takeIf { result.resultCode == Activity.RESULT_OK && result.data != null }?.let {
            val message = DatabaseImporter.importDatabase(it, result.data?.data)
            Toast.makeText(it, message, Toast.LENGTH_LONG).show()
        }
    }

    private val requestReadPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ImportExportAsFiles.importFromDirectory(requireContext())
        } else {
            Toast.makeText(
                requireActivity(),
                getString(R.string.read_access_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val requestWritePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ImportExportAsFiles.exportToDirectory(requireContext())
        } else {
            Toast.makeText(
                requireActivity(),
                getString(R.string.write_access_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        wireImportExport()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        wireImportExport()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        setSummaries(preferenceScreen)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val preference = key?.let { findPreference<Preference>(it) } ?: return
        ShaderEditorApp.preferences.update(requireContext())
        setSummary(preference)

        if (key == Preferences.SAVE_BATTERY && ShaderEditorApp.preferences.isBatteryLow) {
            BatteryLevelReceiver.setLowPowerMode(ShaderEditorApp.preferences.saveBattery())
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ShaderListPreference) {
            val dialogFragment = ShaderListPreferenceDialogFragment.newInstance(preference.key)
            // FIXME: Periodically check whether androidx.preference still uses TargetFragment
            @Suppress("DEPRECATION")
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, "ShaderListPreferenceDialogFragment")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun setSummaries(screen: PreferenceGroup) {
        for (i in 0 until screen.preferenceCount) {
            setSummary(screen.getPreference(i))
        }
    }

    private fun setSummary(preference: Preference) {
        when (preference) {
            is ShaderListPreference -> {
                val id = if (preference.key == Preferences.WALLPAPER_SHADER) {
                    ShaderEditorApp.preferences.getWallpaperShader()
                } else {
                    ShaderEditorApp.preferences.getDefaultNewShader()
                }
                preference.summary = getShaderSummary(id)
            }

            is ListPreference -> preference.summary = preference.entry
            is PreferenceGroup -> setSummaries(preference)
        }
    }

    private fun getShaderSummary(id: Long): String {
        val cursor = ShaderEditorApp.db.getShader(id)
        if (Database.closeIfEmpty(cursor)) {
            return getString(R.string.no_shader_selected)
        }

        val summary = Database.getString(cursor, Database.SHADERS_NAME)
            ?: Database.getString(cursor, Database.SHADERS_MODIFIED)

        cursor.close()
        return summary ?: getString(R.string.no_shader_selected)
    }

    private fun checkExternalStoragePermission(permission: String): Boolean {
        val activity = activity ?: return false
        if (ContextCompat.checkSelfPermission(
                activity,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            when (permission) {
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                    requestWritePermissionLauncher.launch(permission)
                }

                Manifest.permission.READ_EXTERNAL_STORAGE -> {
                    requestReadPermissionLauncher.launch(permission)
                }
            }
            return false
        }
        return true
    }

    private fun wireImportExport() {
        val importFromDirectory = findPreference<Preference>(Preferences.IMPORT_FROM_DIRECTORY)
        val exportToDirectory = findPreference<Preference>(Preferences.EXPORT_TO_DIRECTORY)

        if (importFromDirectory != null && exportToDirectory != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                importFromDirectory.setOnPreferenceClickListener {
                    if (checkExternalStoragePermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        ImportExportAsFiles.importFromDirectory(requireContext())
                    }
                    true
                }
                exportToDirectory.setOnPreferenceClickListener {
                    if (checkExternalStoragePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        ImportExportAsFiles.exportToDirectory(requireContext())
                    }
                    true
                }
            } else {
                findPreference<PreferenceCategory>("import_export")?.apply {
                    removePreference(importFromDirectory)
                    removePreference(exportToDirectory)
                }
            }
        }

        findPreference<Preference>(Preferences.IMPORT_DATABASE)?.setOnPreferenceClickListener {
            val chooseFile = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/octet-stream"
            }
            pickFileLauncher.launch(
                Intent.createChooser(chooseFile, getString(R.string.import_database))
            )
            true
        }

        findPreference<Preference>(Preferences.EXPORT_DATABASE)?.setOnPreferenceClickListener {
            context?.let {
                Toast.makeText(it, DatabaseExporter.exportDatabase(it), Toast.LENGTH_LONG).show()
            }
            true
        }
    }
}