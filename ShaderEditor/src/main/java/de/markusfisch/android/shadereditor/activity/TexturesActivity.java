package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.fragment.TexturesFragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class TexturesActivity extends AbstractContentActivity
{
	public static final String TEXTURE_NAME = "texture_name";

	public static void setAddUniformResult(
		Activity activity,
		String name )
	{
		Bundle bundle = new Bundle();
		bundle.putString( TEXTURE_NAME, name );

		Intent data = new Intent();
		data.putExtras( bundle );

		activity.setResult( RESULT_OK, data );
	}

	@Override
	protected Fragment defaultFragment()
	{
		startActivityForIntent( getIntent() );

		return new TexturesFragment();
	}

	private void startActivityForIntent( Intent intent )
	{
		if( intent == null )
			return;

		String type;

		if( !Intent.ACTION_SEND.equals( intent.getAction() ) ||
			(type = intent.getType()) == null ||
			!type.startsWith( "image/" ) )
			return;

		Uri imageUri = (Uri)intent.getParcelableExtra(
			Intent.EXTRA_STREAM );

		if( imageUri == null )
			return;

		startActivity(
			CropImageActivity.getIntentForImage(
				this,
				imageUri ) );
	}
}
