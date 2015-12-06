package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.TexturesActivity;
import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class TextureViewFragment extends Fragment
{
	private static final String TEXTURE_ID = "texture_id";

	private long textureId;

	public static TextureViewFragment newInstance( long id )
	{
		Bundle bundle = new Bundle();
		bundle.putLong( TEXTURE_ID, id );

		TextureViewFragment fragment = new TextureViewFragment();
		fragment.setArguments( bundle );

		return fragment;
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

		if( (activity = getActivity()) == null )
			return null;

		activity.setTitle( R.string.view_texture );

		Bitmap bitmap;
		Bundle bundle;
		View view;
		ImageView imageView;

		if( (bundle = getArguments()) == null ||
			(textureId = bundle.getLong( TEXTURE_ID )) < 1 ||
			(bitmap = ShaderEditorApplication
				.dataSource
				.getTexture( textureId )) == null ||
			(view = inflater.inflate(
				R.layout.fragment_view_texture,
				container,
				false )) == null ||
			(imageView = (ImageView)view.findViewById(
				R.id.texture_image )) == null )
		{
			activity.finish();
			return null;
		}

		imageView.setImageBitmap( bitmap );

		return view;
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate(
			R.menu.fragment_view_texture,
			menu );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case R.id.insert_code:
				insertUniformSamplerStatement();
				return true;
			case R.id.remove_texture:
				askToRemoveTexture( textureId );
				return true;
			default:
				return super.onOptionsItemSelected( item );
		}
	}

	private void askToRemoveTexture( final long id )
	{
		Activity activity = getActivity();

		if( activity == null )
			return;

		new AlertDialog.Builder( activity )
			.setTitle( R.string.remove_texture )
			.setMessage( R.string.want_to_remove_texture )
			.setPositiveButton(
				android.R.string.yes,
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(
						DialogInterface dialog,
						int whichButton )
					{
						removeTextureAsync( id );
					}
				} )
			.setNegativeButton(
				android.R.string.no,
				null ).show();
	}

	private void removeTextureAsync( final long id )
	{
		new AsyncTask<Void, Void, Void>()
		{
			@Override
			protected Void doInBackground( Void... nothings )
			{
				ShaderEditorApplication
					.dataSource
					.removeTexture( id );

				return null;
			}

			@Override
			protected void onPostExecute( Void nothing )
			{
				FragmentManager fragmentManager =
					getFragmentManager();

				if( fragmentManager != null )
					fragmentManager.popBackStack();
			}
		}.execute();
	}

	private void insertUniformSamplerStatement()
	{
		Activity activity = getActivity();

		if( activity == null )
			return;

		TexturesActivity.setAddUniformResult(
			activity,
			ShaderEditorApplication
				.dataSource
				.getTextureName( textureId ) );

		activity.finish();
	}
}
