package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.AbstractSecondaryActivity;
import de.markusfisch.android.shadereditor.widget.CropImageView;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class CropImageFragment extends Fragment
{
	public static Bitmap bitmap;
	public static Rect rect;

	private CropImageView cropImageView;

	public static CropImageFragment newInstance( Bitmap bitmap )
	{
		// since there's no easy way to put a bitmap into
		// a bundle, I'm just using a static member here
		// because simple is always better
		CropImageFragment.bitmap = bitmap;

		return new CropImageFragment();
	}

	public static void recycle()
	{
		if( bitmap == null )
			return;

		bitmap.recycle();
		bitmap = null;
	}

	@Override
	public void onCreate( Bundle state )
	{
		super.onCreate( state );

		setHasOptionsMenu( true );
	}

	@Override
	public View onCreateView(
		LayoutInflater inflater,
		ViewGroup container,
		Bundle state )
	{
		Activity activity;
		View view;

		if( bitmap == null ||
			(activity = getActivity()) == null )
			return null;

		activity.setTitle( R.string.crop_image );

		if( (view = inflater.inflate(
				R.layout.fragment_crop_image,
				container,
				false )) == null ||
			(cropImageView = (CropImageView)view.findViewById(
				R.id.texture_image )) == null )
		{
			activity.finish();
			return null;
		}

		cropImageView.setImageBitmap( bitmap );

		return view;
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate(
			R.menu.fragment_crop_image,
			menu );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case R.id.cut:
				cutImage();
				return true;
			default:
				return super.onOptionsItemSelected( item );
		}
	}

	private void cutImage()
	{
		rect = cropImageView.getRectInBounds();

		AbstractSecondaryActivity.addFragment(
			getFragmentManager(),
			new TexturePropertiesFragment() );
	}
}
