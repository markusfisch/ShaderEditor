package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.fragment.CropImageFragment;
import de.markusfisch.android.shadereditor.widget.CropImageView;
import de.markusfisch.android.shadereditor.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class CropImageActivity
	extends AbstractSubsequentActivity
	implements CropImageFragment.CropImageViewProvider
{
	private CropImageView cropImageView;

	public static void startActivityForImage(
		Context context,
		Uri imageUri )
	{
		if( context == null )
			return;

		Intent intent = new Intent(
			context,
			CropImageActivity.class );

		intent.putExtra( CropImageFragment.IMAGE_URI, imageUri );

		context.startActivity( intent );
	}

	@Override
	public CropImageView getCropImageView()
	{
		return cropImageView;
	}

	@Override
	protected void onCreate( Bundle state )
	{
		super.onCreate( state );
		setContentView( R.layout.activity_crop_image );

		if( (cropImageView = (CropImageView)findViewById(
			R.id.texture_image )) == null )
		{
			finish();
			return;
		}

		MainActivity.initSystemBars( this );
		AbstractSubsequentActivity.initToolbar( this );

		if( state == null )
			setFragmentForIntent( getIntent() );
	}

	private void setFragmentForIntent( Intent intent )
	{
		if( intent == null )
		{
			finish();
			return;
		}

		CropImageFragment fragment =
			new CropImageFragment();

		fragment.setArguments(
			intent.getExtras() );

		getSupportFragmentManager().beginTransaction()
			.replace(
				R.id.content_frame,
				fragment )
			.commit();
	}
}
