package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.fragment.CubeMapFragment;
import de.markusfisch.android.shadereditor.widget.CubeMapView;
import de.markusfisch.android.shadereditor.R;

import android.os.Bundle;

public class CubeMapActivity
	extends AbstractSubsequentActivity
	implements CubeMapFragment.CubeMapViewProvider
{
	private CubeMapView cubeMapImageView;

	@Override
	public CubeMapView getCubeMapView()
	{
		return cubeMapImageView;
	}

	@Override
	protected void onCreate( Bundle state )
	{
		super.onCreate( state );
		setContentView( R.layout.activity_cube_map );

		if( (cubeMapImageView = (CubeMapView)findViewById(
			R.id.cube_map_view )) == null )
		{
			finish();
			return;
		}

		MainActivity.initSystemBars( this );
		AbstractSubsequentActivity.initToolbar( this );

		if( state == null )
			setFragment(
				getSupportFragmentManager(),
				new CubeMapFragment() );
	}
}
