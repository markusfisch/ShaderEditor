package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity;
import de.markusfisch.android.shadereditor.activity.CubeMapActivity;
import de.markusfisch.android.shadereditor.widget.CubeMapView;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class CubeMapFragment extends Fragment
{
	public static final int PICK_IMAGE = 1;

	public interface CubeMapViewProvider
	{
		CubeMapView getCubeMapView();
	}

	private CubeMapView cubeMapView;

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

		activity.setTitle( R.string.compose_sampler_cube );

		try
		{
			cubeMapView = ((CubeMapActivity)activity)
				.getCubeMapView();
		}
		catch( ClassCastException e )
		{
			throw new ClassCastException(
				activity.toString()+
				" must implement "+
				"CubeMapFragment.CubeMapViewProvider" );
		}

		View view;
		View fab;

		if( (view = inflater.inflate(
				R.layout.fragment_cube_map,
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
					addTexture();
				}
			} );

		// make cubeMapView in activity visible (again)
		cubeMapView.setVisibility( View.VISIBLE );

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
			case R.id.crop:
				composeMap();
				return true;
			case R.id.rotate_clockwise:
				rotateClockwise();
				return true;
			default:
				return super.onOptionsItemSelected( item );
		}
	}

	@Override
	public void onActivityResult(
		int requestCode,
		int resultCode,
		Intent data )
	{
		super.onActivityResult( requestCode, resultCode, data );

		Uri imageUri;

		if( requestCode == PICK_IMAGE &&
			resultCode == Activity.RESULT_OK &&
			(imageUri = data.getData()) != null )
			cubeMapView.setSelectedFaceImage( imageUri );
	}

	private void composeMap()
	{
		CubeMapView.Face faces[] = cubeMapView.getFaces();

		for( int n = faces.length; n-- > 0; )
			if( faces[n].getUri() == null )
			{
				Activity activity = getActivity();

				if( activity == null )
					return;

				Toast.makeText(
					activity,
					R.string.not_enough_faces,
					Toast.LENGTH_SHORT ).show();

				return;
			}

		AbstractSubsequentActivity.addFragment(
			getFragmentManager(),
			SamplerCubePropertiesFragment.newInstance( faces ) );

		cubeMapView.setVisibility( View.GONE );
	}

	private void rotateClockwise()
	{
		cubeMapView.setImageRotation(
			(cubeMapView.getImageRotation()+90) % 360 );
	}

	private void addTexture()
	{
		Activity activity = getActivity();

		if( activity == null )
			return;

		Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
		intent.setType( "image/*" );

		// use Activity.startActivityForResult() to keep
		// requestCode; Fragment.startActivityForResult()
		// will modify the requestCode
		startActivityForResult(
			Intent.createChooser(
				intent,
				getString( R.string.choose_image ) ),
			CubeMapFragment.PICK_IMAGE );
	}
}
