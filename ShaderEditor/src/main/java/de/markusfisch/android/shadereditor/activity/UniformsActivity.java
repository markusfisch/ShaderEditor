package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.fragment.UniformsFragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class UniformsActivity extends AbstractContentActivity
{
	public static final String UNIFORM_NAME = "uniform_name";

	public static void setAddUniformResult(
		Activity activity,
		String name )
	{
		Bundle bundle = new Bundle();
		bundle.putString( UNIFORM_NAME, name );

		Intent data = new Intent();
		data.putExtras( bundle );

		activity.setResult( RESULT_OK, data );
	}

	@Override
	protected Fragment defaultFragment()
	{
		return new UniformsFragment();
	}
}
