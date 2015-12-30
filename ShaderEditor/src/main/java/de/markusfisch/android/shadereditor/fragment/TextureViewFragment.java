package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.TexturesActivity;
import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.widget.ScalingImageView;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class TextureViewFragment extends Fragment
{
	public static final String TEXTURE_ID = "texture_id";

	public interface ScalingImageViewProvider
	{
		public ScalingImageView getScalingImageView();
	}

	private long textureId;

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

		ScalingImageView imageView;

		try
		{
			imageView = ((ScalingImageViewProvider)activity)
				.getScalingImageView();
		}
		catch( ClassCastException e )
		{
			throw new ClassCastException(
				activity.toString()+
				" must implement "+
				"TextureViewFragment.ScalingImageViewProvider" );
		}

		Bundle args;
		Bitmap bitmap;

		if( imageView == null ||
			(args = getArguments()) == null ||
			(textureId = args.getLong( TEXTURE_ID )) < 1 ||
			(bitmap = ShaderEditorApplication
				.dataSource
				.getTexture( textureId )) == null )
		{
			activity.finish();
			return null;
		}

		imageView.setImageBitmap( bitmap );

		return null;
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
				Activity activity = getActivity();

				if( activity == null )
					return;

				activity.finish();
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
