package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.CubeMapActivity;
import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;

public class UniformSamplerCubePageFragment
	extends UniformSampler2dPageFragment
{
	@Override
	protected void addTexture()
	{
		Activity activity = getActivity();

		if( activity == null )
			return;

		startActivity( new Intent(
			activity,
			CubeMapActivity.class ) );
	}

	@Override
	protected Cursor loadTextures()
	{
		return ShaderEditorApplication
			.dataSource
			.getSamplerCubeTextures();
	}
}
