package de.markusfisch.android.shadereditor.fragment;

import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreferenceDialogFragmentCompat;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.ShaderSpinnerAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.preference.Preferences;

public class ShaderListPreferenceDialogFragment
		extends ListPreferenceDialogFragmentCompat {
	private ShaderSpinnerAdapter adapter;

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
		closeCursor();
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {
		closeCursor();
		super.onDialogClosed(positiveResult);
	}

	@Override
	protected void onPrepareDialogBuilder(@NonNull AlertDialog.Builder builder) {
		// Don't call super.onPrepareDialogBuilder() because it'll check
		// for Entries and set up a setSingleChoiceItems() for them that
		// will never be used.

		final String key = getPreference().getKey();
		Cursor cursor = ShaderEditorApp.db.getShaders();
		if (Preferences.DEFAULT_NEW_SHADER.equals(key)) {
			cursor = addEmptyItem(cursor);
		}
		adapter = new ShaderSpinnerAdapter(getContext(), cursor);

		builder.setSingleChoiceItems(
				adapter,
				0,
				(dialog, which) -> {
					if (Preferences.WALLPAPER_SHADER.equals(key)) {
						ShaderEditorApp.preferences.setWallpaperShader(
								adapter.getItemId(which));
					} else {
						ShaderEditorApp.preferences.setDefaultNewShader(
								adapter.getItemId(which));
					}

					ShaderListPreferenceDialogFragment.this.onClick(
							dialog,
							DialogInterface.BUTTON_POSITIVE);

					dialog.dismiss();
				});

		builder.setPositiveButton(null, null);
	}

	private void closeCursor() {
		if (adapter != null) {
			adapter.changeCursor(null);
			adapter = null;
		}
	}

	private Cursor addEmptyItem(Cursor cursor) {
		MatrixCursor matrixCursor = null;
		try {
			matrixCursor = new MatrixCursor(new String[]{
					Database.SHADERS_ID,
					Database.SHADERS_THUMB,
					Database.SHADERS_NAME,
					Database.SHADERS_MODIFIED
			});
			matrixCursor.addRow(new Object[]{
					0,
					null,
					getString(R.string.no_shader_selected),
					null
			});

			return new MergeCursor(new Cursor[]{
					matrixCursor,
					cursor
			});
		} finally {
			if (matrixCursor != null) {
				matrixCursor.close();
			}
		}
	}
}
