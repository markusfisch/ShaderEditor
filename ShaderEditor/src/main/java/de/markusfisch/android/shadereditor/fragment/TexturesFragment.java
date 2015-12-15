package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.AbstractSecondaryActivity;
import de.markusfisch.android.shadereditor.adapter.TexturesAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class TexturesFragment extends Fragment
{
	public static final int PICK_IMAGE_REQUEST = 1;

	private ListView listView;
	private TexturesAdapter texturesAdapter;
	private View progessBar;
	private View noTextureMessage;

	@Override
	public View onCreateView(
		LayoutInflater inflater,
		ViewGroup container,
		Bundle state )
	{
		Activity activity;

		if( (activity = getActivity()) == null )
			return null;

		activity.setTitle( R.string.textures );

		View view;
		View fab;

		if( (view = inflater.inflate(
				R.layout.fragment_textures,
				container,
				false )) == null ||
			(fab = view.findViewById(
				R.id.add_texture )) == null ||
			(progessBar = view.findViewById(
				R.id.progress_bar )) == null ||
			(noTextureMessage = view.findViewById(
				R.id.no_textures_message )) == null )
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
			cropImage( data.getData() );
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

				if( cursor.getCount() < 1 )
					showNoTextures();
			}
		}.execute();
	}

	private void showNoTextures()
	{
		progessBar.setVisibility( View.GONE );
		noTextureMessage.setVisibility( View.VISIBLE );
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

	private void cropImage( Uri imageUri )
	{
		AbstractSecondaryActivity.addFragment(
			getFragmentManager(),
			CropImageFragment.newInstance( imageUri ) );
	}
}
