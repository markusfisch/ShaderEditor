package de.markusfisch.android.shadereditor.activity

import android.os.Bundle
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.fragment.CubeMapFragment
import de.markusfisch.android.shadereditor.view.SystemBarMetrics
import de.markusfisch.android.shadereditor.widget.CubeMapView

class CubeMapActivity : AbstractSubsequentActivity(), CubeMapFragment.CubeMapViewProvider {

    private lateinit var cubeMapImageView: CubeMapView

    override fun getCubeMapView(): CubeMapView = cubeMapImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cube_map)

        cubeMapImageView = findViewById(R.id.cube_map_view)

        SystemBarMetrics.initSystemBars(this, cubeMapImageView.insets)
        initToolbar(this)

        if (savedInstanceState == null) {
            setFragment(supportFragmentManager, CubeMapFragment())
        }
    }
}