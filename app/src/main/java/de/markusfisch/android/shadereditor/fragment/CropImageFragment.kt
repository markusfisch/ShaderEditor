package de.markusfisch.android.shadereditor.fragment

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.BundleCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity
import de.markusfisch.android.shadereditor.graphics.BitmapEditor
import de.markusfisch.android.shadereditor.widget.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CropImageFragment : Fragment() {

    fun interface CropImageViewProvider {
        fun getCropImageView(): CropImageView
    }

    companion object {
        const val IMAGE_URI = "image_uri"
        private var inProgress = false
    }

    private lateinit var cropImageView: CropImageView
    private lateinit var progressView: View
    private lateinit var imageUri: Uri
    private var bitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val activity = activity ?: return null
        activity.title = getString(R.string.crop_image)

        cropImageView = (activity as? CropImageViewProvider)?.getCropImageView()
            ?: throw ClassCastException("$activity must implement CropImageViewProvider")

        imageUri =
            BundleCompat.getParcelable(requireArguments(), IMAGE_URI, Uri::class.java) ?: run {
                abort(activity)
                return null
            }

        val view = inflater.inflate(R.layout.fragment_crop_image, container, false)
        progressView = view.findViewById(R.id.progress_view)

        // Make cropImageView in activity visible again
        cropImageView.visibility = View.VISIBLE

        view.findViewById<View>(R.id.crop).setOnClickListener { cropImage() }

        activity.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_crop_image, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.rotate_clockwise) {
                    rotateClockwise()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return view
    }

    override fun onResume() {
        super.onResume()
        loadBitmapAsync()
    }

    private fun loadBitmapAsync() {
        val activity = activity ?: return
        if (inProgress) return

        inProgress = true
        progressView.visibility = View.VISIBLE

        // Launch background task to load the bitmap
        lifecycleScope.launch {
            val b = withContext(Dispatchers.IO) {
                BitmapEditor.getBitmapFromUri(activity, imageUri, 1024)
            }

            inProgress = false
            progressView.visibility = View.GONE

            if (b == null) {
                abort(activity)
            } else if (isAdded) {
                bitmap = b
                cropImageView.setImageBitmap(b)
            }
        }
    }

    private fun abort(activity: Activity) {
        Toast.makeText(activity, R.string.cannot_pick_image, Toast.LENGTH_SHORT).show()
        activity.finish()
    }

    private fun cropImage() {
        val uri = imageUri
        AbstractSubsequentActivity.addFragment(
            parentFragmentManager, Sampler2dPropertiesFragment.newInstance(
                uri, cropImageView.normalizedRectInBounds, cropImageView.imageRotation
            )
        )
        bitmap?.recycle()
        bitmap = null

        cropImageView.setImageBitmap(null)
        cropImageView.visibility = View.GONE
    }

    private fun rotateClockwise() {
        cropImageView.imageRotation = (cropImageView.imageRotation + 90) % 360
    }
}