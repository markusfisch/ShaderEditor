package de.markusfisch.android.shadereditor.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.app.ShaderEditorApp
import de.markusfisch.android.shadereditor.graphics.BitmapEditor
import de.markusfisch.android.shadereditor.widget.CubeMapView

class SamplerCubePropertiesFragment : AbstractSamplerPropertiesFragment() {

    private var faces: ArrayList<CubeMapView.Face>? = null

    companion object {
        private const val FACES = "faces"

        @JvmStatic
        fun newInstance(faces: Array<CubeMapView.Face>): Fragment {
            return SamplerCubePropertiesFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArray(FACES, faces)
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
            faces = BundleCompat.getParcelableArrayList(args, FACES, CubeMapView.Face::class.java)
                ?: run {
                    activity.finish()
                    return null
                }
        }

        val view = initView(activity, inflater, container)
        setSizeCaption(getString(R.string.face_size))
        setMaxValue(7)
        setSamplerType(SAMPLER_CUBE)

        return view
    }

    override fun saveSampler(context: Context, name: String, size: Int): Int {
        val width = size * 2
        val height = size * 3
        val mapBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mapBitmap)

        var x = 0
        var y = 0

        faces?.forEach { face ->
            val faceUri = face.uri ?: return R.string.cannot_pick_image

            val clip = face.clip
            val rotation = face.rotation

            val nw = clip.width()
            val max = (size + size / nw * (1f - nw)).toInt()

            var bitmap = BitmapEditor.getBitmapFromUri(context, faceUri, max)
                ?: return R.string.cannot_pick_image

            bitmap = BitmapEditor.crop(bitmap, clip, rotation) ?: return R.string.cannot_pick_image

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
            canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
            scaledBitmap.recycle()

            x += size
            if (x >= width) {
                y += size
                x = 0
            }
        }

        return if (ShaderEditorApp.db.insertTexture(name, mapBitmap) < 1) {
            R.string.name_already_taken
        } else {
            0
        }
    }
}