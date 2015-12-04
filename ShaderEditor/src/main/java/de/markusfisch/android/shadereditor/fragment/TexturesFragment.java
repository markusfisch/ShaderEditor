package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.AbstractSecondaryActivity;
import de.markusfisch.android.shadereditor.adapter.TexturesAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;

public class TexturesFragment extends Fragment
{
	public static final int PICK_IMAGE_REQUEST = 1;

	private ListView listView;
	private TexturesAdapter texturesAdapter;

	@Override
	public View onCreateView(
		LayoutInflater inflater,
		ViewGroup container,
		Bundle state )
	{
		Activity activity;
		View view;
		View fab;

		if( (activity = getActivity()) == null )
			return null;

		activity.setTitle( R.string.textures );

		if( (view = inflater.inflate(
				R.layout.fragment_textures,
				container,
				false )) == null ||
			(fab = view.findViewById(
				R.id.add_texture )) == null )
		{
			activity.finish();
			return null;
		}

		fab.setOnClickListener(
			new View.OnClickListener()
			{
				@Override
				public void onClick( View v )
				{
					pickImage();
				}
			} );

		initListView( view );

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();

		queryTexturesAsync( getActivity() );
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		if( texturesAdapter != null )
		{
			texturesAdapter.changeCursor( null );
			texturesAdapter = null;
		}
	}

	@Override
	public void onActivityResult(
		int requestCode,
		int resultCode,
		Intent data )
	{
		super.onActivityResult( requestCode, resultCode, data );

		Activity activity;

		if( requestCode == PICK_IMAGE_REQUEST &&
			resultCode == Activity.RESULT_OK &&
			data != null &&
			data.getData() != null &&
			(activity = getActivity()) != null )
		{
			Uri uri = data.getData();

			try
			{
				AssetFileDescriptor fd = activity
					.getContentResolver()
					.openAssetFileDescriptor( uri, "r" );

				BitmapFactory.Options options =
					new BitmapFactory.Options();
				options.inSampleSize = getSampleSizeForBitmap(
					fd,
					1024,
					1024 );

				cropImage( BitmapFactory.decodeFileDescriptor(
					fd.getFileDescriptor(),
					null,
					options ) );
			}
			catch( IOException e )
			{
				Toast.makeText(
					activity,
					R.string.error_pick_image,
					Toast.LENGTH_SHORT ).show();
			}
		}
	}

	private void initListView( View view )
	{
		listView = (ListView)view.findViewById( R.id.textures );
		listView.setEmptyView( view.findViewById( R.id.no_textures ) );
		listView.setOnItemClickListener(
			new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(
					AdapterView<?> parent,
					View view,
					int position,
					long id )
				{
					showTexture( id );
				}
			} );
	}

	private void showTexture( long id )
	{
		AbstractSecondaryActivity.addFragment(
			getFragmentManager(),
			TextureViewFragment.newInstance( id ) );
	}

	private void queryTexturesAsync( final Context context )
	{
		if( context == null )
			return;

		new AsyncTask<Void, Void, Cursor>()
		{
			@Override
			protected Cursor doInBackground( Void... nothings )
			{
				return ShaderEditorApplication
					.dataSource
					.queryTextures();
			}

			@Override
			protected void onPostExecute( Cursor cursor )
			{
				if( cursor == null )
					return;

				if( texturesAdapter != null )
				{
					texturesAdapter.changeCursor( cursor );
					texturesAdapter.notifyDataSetChanged();
				}
				else
				{
					texturesAdapter = new TexturesAdapter(
						context,
						cursor );

					listView.setAdapter( texturesAdapter );
				}
			}
		}.execute();
	}

	private void pickImage()
	{
		Intent intent = new Intent( Intent.ACTION_GET_CONTENT );

		intent.setType( "image/*" );

		startActivityForResult(
			Intent.createChooser(
				intent,
				getString( R.string.choose_image ) ),
			PICK_IMAGE_REQUEST);
	}

	private void cropImage( Bitmap bitmap )
	{
		AbstractSecondaryActivity.addFragment(
			getFragmentManager(),
			CropImageFragment.newInstance( bitmap ) );
	}

	private static int getSampleSizeForBitmap(
		AssetFileDescriptor fd,
		int maxWidth,
		int maxHeight )
	{
		BitmapFactory.Options options =
			new BitmapFactory.Options();

		options.inJustDecodeBounds = true;

		BitmapFactory.decodeFileDescriptor(
			fd.getFileDescriptor(),
			null,
			options );

		return calculateSampleSize(
			options.outWidth,
			options.outHeight,
			maxWidth,
			maxHeight );
	}

	private static int calculateSampleSize(
		int width,
		int height,
		int maxWidth,
		int maxHeight )
	{
		int size = 1;

		if( width > maxWidth ||
			height > maxHeight )
		{
			final int hw = width/2;
			final int hh = height/2;

			while(
				hw/size > maxWidth &&
				hh/size > maxHeight )
				size *= 2;
		}

		return size;
	}
}
