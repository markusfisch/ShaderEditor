package de.markusfisch.android.shadereditor.preference;

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

public class ShaderListPreference extends ListPreference
{
	private String dummy[] = { "nix" };

	public ShaderListPreference( Context context, AttributeSet attrs )
	{
		super( context, attrs );

		// is never used
		setEntries( dummy );
		setEntryValues( dummy );
	}
}
