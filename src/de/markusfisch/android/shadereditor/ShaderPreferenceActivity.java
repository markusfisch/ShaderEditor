package de.markusfisch.android.shadereditor;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ShaderPreferenceActivity extends PreferenceActivity
{
	public static final String SHARED_PREFERENCES_NAME = "ShaderEditorSettings";
	public static final String SHADER = "shader";
	public static final String COMPILE_ON_CHANGE = "compile_on_change";
	public static final String SHOW_FPS_GAUGE = "show_fps_gauge";

	@Override
	public void onCreate( Bundle state )
	{
		super.onCreate( state );

		getPreferenceManager().setSharedPreferencesName(
			SHARED_PREFERENCES_NAME );

		addPreferencesFromResource( R.xml.preferences );
	}
}
