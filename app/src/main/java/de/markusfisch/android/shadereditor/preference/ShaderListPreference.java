package de.markusfisch.android.shadereditor.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

public class ShaderListPreference extends ListPreference {
	public ShaderListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Never used.
		String[] dummy = {"nix"};
		setEntries(dummy);
		setEntryValues(dummy);
	}
}
