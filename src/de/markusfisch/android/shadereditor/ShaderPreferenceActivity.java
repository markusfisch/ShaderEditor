package de.markusfisch.android.shadereditor;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ShaderPreferenceActivity extends PreferenceActivity
{
	public static final String SHARED_PREFERENCES_NAME = "ShaderEditorSettings";

	@Override
	public void onCreate( Bundle state )
	{
		super.onCreate( state );

		getPreferenceManager().setSharedPreferencesName(
			SHARED_PREFERENCES_NAME );

		addPreferencesFromResource( R.xml.preferences );
	}
}
