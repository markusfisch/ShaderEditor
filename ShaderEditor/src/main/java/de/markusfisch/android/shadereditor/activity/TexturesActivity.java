package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.fragment.TexturesFragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class TexturesActivity extends AbstractSecondaryActivity
{
	public static final String TEXTURE_NAME = "texture_name";

	public static void setAddUniformResult(
		Activity activity,
		String name )
	{
		Bundle bundle = new Bundle();
		bundle.putString( TEXTURE_NAME, name );

		Intent intent = new Intent();
		intent.putExtras( bundle );

		activity.setResult( RESULT_OK, intent );
	}

	@Override
	protected Fragment defaultFragment()
	{
		return new TexturesFragment();
	}
}
