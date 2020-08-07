package de.markusfisch.android.shadereditor.activity;

import android.support.v4.app.Fragment;

import de.markusfisch.android.shadereditor.fragment.PreferencesFragment;

public class PreferencesActivity extends AbstractContentActivity {
	@Override
	protected Fragment defaultFragment() {
		return new PreferencesFragment();
	}
}
