package de.markusfisch.android.shadereditor.activity

import android.os.Bundle
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.fragment.TextureViewFragment
import de.markusfisch.android.shadereditor.view.SystemBarMetrics
import de.markusfisch.android.shadereditor.widget.ScalingImageView

class TextureViewActivity : AbstractSubsequentActivity(),
    TextureViewFragment.ScalingImageViewProvider {
    private lateinit var scalingImageView: ScalingImageView

    override fun getScalingImageView(): ScalingImageView {
        return scalingImageView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_texture)

        scalingImageView = findViewById(R.id.scaling_image_view)

        SystemBarMetrics.initSystemBars(this)
        initToolbar(this)

        if (savedInstanceState == null) {
            setFragmentForIntent(TextureViewFragment(), intent)
        }
    }
}
