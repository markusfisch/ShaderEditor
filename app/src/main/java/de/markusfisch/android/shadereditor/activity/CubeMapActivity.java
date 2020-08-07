package de.markusfisch.android.shadereditor.activity;

import android.os.Bundle;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.fragment.CubeMapFragment;
import de.markusfisch.android.shadereditor.view.SystemBarMetrics;
import de.markusfisch.android.shadereditor.widget.CubeMapView;

public class CubeMapActivity
		extends AbstractSubsequentActivity
		implements CubeMapFragment.CubeMapViewProvider {
	private CubeMapView cubeMapImageView;

	@Override
	public CubeMapView getCubeMapView() {
		return cubeMapImageView;
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_cube_map);

		cubeMapImageView = (CubeMapView) findViewById(R.id.cube_map_view);

		SystemBarMetrics.initSystemBars(this, cubeMapImageView.insets);
		AbstractSubsequentActivity.initToolbar(this);

		if (state == null) {
			setFragment(getSupportFragmentManager(), new CubeMapFragment());
		}
	}
}
