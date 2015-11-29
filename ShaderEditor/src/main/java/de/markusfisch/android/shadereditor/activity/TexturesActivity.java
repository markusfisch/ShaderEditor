package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.fragment.TexturesFragment;

import android.support.v4.app.Fragment;

public class TexturesActivity extends AbstractSecondaryActivity
{
	@Override
	protected Fragment defaultFragment()
	{
		return new TexturesFragment();
	}
}
