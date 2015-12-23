package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.fragment.PreferencesFragment;

import android.support.v4.app.Fragment;

public class PreferencesActivity extends AbstractContentActivity
{
	@Override
	protected Fragment defaultFragment()
	{
		return new PreferencesFragment();
	}
}
