package de.markusfisch.android.shadereditor.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.activity.AddUniformActivity
import de.markusfisch.android.shadereditor.opengl.BackBufferParameters
import de.markusfisch.android.shadereditor.opengl.ShaderRenderer
import de.markusfisch.android.shadereditor.opengl.TextureParameters
import de.markusfisch.android.shadereditor.widget.BackBufferParametersView
import de.markusfisch.android.shadereditor.widget.TextureParametersView

class TextureParametersFragment : Fragment() {

    private var samplerType: String? = null
    private var textureName: String? = null
    private lateinit var textureParameters: TextureParameters
    private var isBackBuffer: Boolean = false
    private lateinit var textureParameterView: TextureParametersView
    private var backBufferParametersView: BackBufferParametersView? = null

    companion object {
        private const val TYPE = "type"
        private const val NAME = "name"

        @JvmStatic
        fun newInstance(type: String, name: String): Fragment {
            return TextureParametersFragment().apply {
                arguments = Bundle().apply {
                    putString(TYPE, type)
                    putString(NAME, name)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val activity = requireActivity()
        activity.title = getString(R.string.texture_parameters)

        val args = requireArguments()
        samplerType = args.getString(TYPE)
        textureName = args.getString(NAME)
            ?: throw IllegalArgumentException("Missing type and name arguments")

        isBackBuffer = ShaderRenderer.UNIFORM_BACKBUFFER == textureName

        val layout = if (isBackBuffer) {
            textureParameters = BackBufferParameters()
            R.layout.fragment_backbuffer_parameters
        } else {
            textureParameters = TextureParameters()
            R.layout.fragment_texture_parameters
        }

        val view = inflater.inflate(layout, container, false)

        textureParameterView = view.findViewById(R.id.texture_parameters)
        textureParameterView.setDefaults(textureParameters)

        if (isBackBuffer) {
            backBufferParametersView = view.findViewById(R.id.backbuffer_parameters)
        }

        view.findViewById<View>(R.id.insert_code).setOnClickListener { insertUniform() }

        return view
    }

    private fun insertUniform() {
        val activity = activity ?: return

        textureParameterView.setParameters(textureParameters)
        if (isBackBuffer) {
            backBufferParametersView?.setParameters(textureParameters as BackBufferParameters)
        }

        AddUniformActivity.setAddUniformResult(
            activity, "uniform $samplerType $textureName;$textureParameters"
        )

        activity.finish()
    }
}