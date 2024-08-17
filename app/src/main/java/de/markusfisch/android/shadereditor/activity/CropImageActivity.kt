package de.markusfisch.android.shadereditor.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.fragment.CropImageFragment
import de.markusfisch.android.shadereditor.view.SystemBarMetrics
import de.markusfisch.android.shadereditor.widget.CropImageView

class CropImageActivity : AbstractSubsequentActivity(), CropImageFragment.CropImageViewProvider {

    private lateinit var cropImageView: CropImageView

    companion object {
        @JvmStatic
        fun getIntentForImage(context: Context, imageUri: Uri): Intent {
            return Intent(context, CropImageActivity::class.java).apply {
                putExtra(CropImageFragment.IMAGE_URI, imageUri)
            }
        }
    }

    override fun getCropImageView(): CropImageView = cropImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_image)

        cropImageView = findViewById(R.id.crop_image_view)

        SystemBarMetrics.initSystemBars(this, cropImageView.insets)
        initToolbar(this)

        if (savedInstanceState == null) {
            setFragmentForIntent(CropImageFragment(), intent)
        }
    }
}