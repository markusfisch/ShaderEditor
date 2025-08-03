package de.markusfisch.android.shadereditor.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.ShaderAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.DataRecords;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.preference.Preferences;

public class ShaderListPreferenceDialogFragment
		extends MaterialPreferenceDialogFragmentCompat {

	private ShaderAdapter adapter;

	@NonNull
	public static ShaderListPreferenceDialogFragment newInstance(
			String key) {
		Bundle bundle = new Bundle();
		bundle.putString(ARG_KEY, key);

		ShaderListPreferenceDialogFragment fragment =
				new ShaderListPreferenceDialogFragment();
		fragment.setArguments(bundle);

		return fragment;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		clearAdapter();
	}

	@Override
	public void onMaterialDialogClosed(boolean positiveResult) {
		clearAdapter();
	}

	@Override
	protected void onPrepareDialogBuilder(@NonNull AlertDialog.Builder builder) {
		// Do not call super.onPrepareDialogBuilder().
		Context context = requireContext();
		DataSource dataSource = Database.getInstance(context).getDataSource();
		final String key = getPreference().getKey();

		// 1. Get the list of shaders from the DataSource.
		List<DataRecords.ShaderInfo> shaders = dataSource.shader.getShaders(
				ShaderEditorApp.preferences.sortByLastModification());

		// 2. Add an empty "(no shader selected)" item if required.
		if (Preferences.DEFAULT_NEW_SHADER.equals(key)) {
			shaders = addEmptyItem(shaders);
		}

		// 3. Create the adapter and set its data.
		adapter = new ShaderAdapter(context);
		adapter.setData(shaders);

		// 4. Find the index of the currently selected shader.
		long currentId = Preferences.WALLPAPER_SHADER.equals(key)
				? ShaderEditorApp.preferences.getWallpaperShader()
				: ShaderEditorApp.preferences.getDefaultNewShader();
		int selectedIndex = 0;
		for (int i = 0; i < shaders.size(); ++i) {
			if (shaders.get(i).id() == currentId) {
				selectedIndex = i;
				break;
			}
		}

		// 5. Build the dialog with the list-based adapter.
		builder.setSingleChoiceItems(
				adapter,
				selectedIndex,
				(dialog, which) -> {
					long selectedId = adapter.getItemId(which);
					if (Preferences.WALLPAPER_SHADER.equals(key)) {
						ShaderEditorApp.preferences.setWallpaperShader(selectedId);
					} else {
						ShaderEditorApp.preferences.setDefaultNewShader(selectedId);
					}

					ShaderListPreferenceDialogFragment.this.onClick(
							dialog,
							DialogInterface.BUTTON_POSITIVE);

					dialog.dismiss();
				});

		builder.setPositiveButton(null, null);
	}

	private void clearAdapter() {
		if (adapter != null) {
			adapter.setData(null);
			adapter = null;
		}
	}

	@NonNull
	private List<DataRecords.ShaderInfo> addEmptyItem(List<DataRecords.ShaderInfo> shaders) {
		// Create a new list to add the empty item to the beginning.
		List<DataRecords.ShaderInfo> listWithEmpty = new ArrayList<>();
		listWithEmpty.add(new DataRecords.ShaderInfo(
				0, // Use 0 as the ID for "no shader".
				getString(R.string.no_shader_selected),
				null,
				null
		));
		listWithEmpty.addAll(shaders);
		return listWithEmpty;
	}
}