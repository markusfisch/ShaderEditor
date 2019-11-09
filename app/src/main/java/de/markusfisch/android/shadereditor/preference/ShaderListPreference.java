package de.markusfisch.android.shadereditor.preference;

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

public class ShaderListPreference extends ListPreference {
	public ShaderListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		// never used
		String[] dummy = {"nix"};
		setEntries(dummy);
		setEntryValues(dummy);
	}
}
