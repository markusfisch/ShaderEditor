package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.fragment.CropImageFragment;
import de.markusfisch.android.shadereditor.fragment.TexturesFragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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
		Fragment fragment = getFragmentForIntent( getIntent() );

		return fragment != null ?
			fragment :
			new TexturesFragment();
	}

	private Fragment getFragmentForIntent( Intent intent )
	{
		if( intent == null )
			return null;

		String type;

		if( !Intent.ACTION_SEND.equals( intent.getAction() ) ||
			(type = intent.getType()) == null ||
			!type.startsWith( "image/" ) )
			return null;

		Uri imageUri = (Uri)intent.getParcelableExtra(
			Intent.EXTRA_STREAM );

		if( imageUri == null )
			return null;

		Bitmap bitmap =
			TexturesFragment.getBitmapFromUri(
				this,
				imageUri );

		return bitmap != null ?
			CropImageFragment.newInstance( bitmap ) :
			null;
	}
}
