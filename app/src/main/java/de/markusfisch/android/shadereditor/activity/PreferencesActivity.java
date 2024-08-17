package de.markusfisch.android.shadereditor.activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import de.markusfisch.android.shadereditor.fragment.PreferencesFragment;

public class PreferencesActivity extends AbstractContentActivity {
	@NonNull
	@Override
	protected Fragment defaultFragment() {
		return new PreferencesFragment();
	}
}
