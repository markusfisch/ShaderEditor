package de.markusfisch.android.shadereditor.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.app.ShaderEditorApp
import de.markusfisch.android.shadereditor.graphics.BitmapEditor

class Sampler2dPropertiesFragment : AbstractSamplerPropertiesFragment() {

    private lateinit var imageUri: Uri
    private lateinit var cropRect: RectF
    private var imageRotation: Float = 0f

    companion object {
        private const val IMAGE_URI = "image_uri"
        private const val CROP_RECT = "crop_rect"
        private const val ROTATION = "rotation"

        @JvmStatic
        fun newInstance(uri: Uri, rect: RectF, rotation: Float): Fragment {
            return Sampler2dPropertiesFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(IMAGE_URI, uri)
                    putParcelable(CROP_RECT, rect)
                    putFloat(ROTATION, rotation)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val activity = requireActivity()
        activity.title = getString(R.string.texture_properties)

        arguments?.let { args ->
            imageUri =
                BundleCompat.getParcelable(args, IMAGE_URI, Uri::class.java) ?: return abort()
            cropRect =
                BundleCompat.getParcelable(args, CROP_RECT, RectF::class.java) ?: return abort()
            imageRotation = args.getFloat(ROTATION)
        }

        return initView(activity, inflater, container)
    }

    private fun abort(): View? {
        requireActivity().finish()
        return null
    }

    override fun saveSampler(context: Context, name: String, size: Int): Int {
        val bitmap = BitmapEditor.getBitmapFromUri(context, imageUri, 1024)
        return saveTexture(bitmap, cropRect, imageRotation, name, size)
    }

    private fun saveTexture(
        bitmap: Bitmap?, rect: RectF, rotation: Float, name: String, size: Int
    ): Int {
        val croppedBitmap =
            BitmapEditor.crop(bitmap, rect, rotation) ?: return R.string.illegal_rectangle

        val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, size, size, true)
        return if (ShaderEditorApp.db.insertTexture(name, scaledBitmap) < 1) {
            R.string.name_already_taken
        } else {
            0
        }
    }
}