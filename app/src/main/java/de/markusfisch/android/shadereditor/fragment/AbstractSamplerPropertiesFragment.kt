package de.markusfisch.android.shadereditor.fragment

import android.app.Activity
import android.content.Context
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.activity.AddUniformActivity
import de.markusfisch.android.shadereditor.opengl.ShaderRenderer
import de.markusfisch.android.shadereditor.opengl.TextureParameters
import de.markusfisch.android.shadereditor.view.SoftKeyboard
import de.markusfisch.android.shadereditor.widget.TextureParametersView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.regex.Pattern

abstract class AbstractSamplerPropertiesFragment : Fragment() {
    companion object {
        const val TEXTURE_NAME_PATTERN = "[a-zA-Z0-9_]+"
        const val SAMPLER_2D = "sampler2D"
        const val SAMPLER_CUBE = "samplerCube"
        private val NAME_PATTERN = Pattern.compile("^$TEXTURE_NAME_PATTERN$")
    }

    private var inProgress = false
    private lateinit var sizeCaption: TextView
    private lateinit var sizeBarView: SeekBar
    private lateinit var sizeView: TextView
    private lateinit var nameView: EditText
    private lateinit var addUniformView: CheckBox
    private lateinit var textureParameterView: TextureParametersView
    private lateinit var progressView: View
    private var samplerType = SAMPLER_2D

    protected fun setSizeCaption(caption: String) {
        sizeCaption.text = caption
    }

    protected fun setMaxValue(max: Int) {
        sizeBarView.max = max
    }

    protected fun setSamplerType(name: String) {
        samplerType = name
    }

    protected abstract fun saveSampler(
        context: Context, name: String, size: Int
    ): Int

    protected fun initView(
        activity: Activity, inflater: LayoutInflater, container: ViewGroup?
    ): View {
        return inflater.inflate(R.layout.fragment_sampler_properties, container, false).apply {
            sizeCaption = findViewById(R.id.size_caption)
            sizeBarView = findViewById(R.id.size_bar)
            sizeView = findViewById(R.id.size)
            nameView = findViewById(R.id.name)
            addUniformView = findViewById(R.id.should_add_uniform)
            textureParameterView = findViewById(R.id.texture_parameters)
            progressView = findViewById(R.id.progress_view)

            findViewById<View>(R.id.save).setOnClickListener {
                saveSamplerAsync()
            }

            if (activity.callingActivity == null) {
                addUniformView.visibility = View.GONE
                addUniformView.isChecked = false
                textureParameterView.visibility = View.GONE
            }

            initSizeView()
            initNameView()
        }
    }

    private fun initSizeView() {
        setSizeView(sizeBarView.progress)
        sizeBarView.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progressValue: Int,
                fromUser: Boolean
            ) {
                setSizeView(progressValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun setSizeView(power: Int) {
        val size = getPower(power)
        sizeView.text = String.format(Locale.US, "%d x %d", size, size)
    }

    private fun initNameView() {
        nameView.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            if (NAME_PATTERN.matcher(source).find()) null else ""
        })
    }

    private fun saveSamplerAsync() {
        val context = activity ?: return

        if (inProgress) return

        val name = nameView.text.toString()
        val tp = TextureParameters().apply {
            textureParameterView.setParameters(this)
        }
        val params = tp.toString()

        if (name.trim().isEmpty()) {
            Toast.makeText(context, R.string.missing_name, Toast.LENGTH_SHORT).show()
            return
        } else if (!name.matches(Regex(TEXTURE_NAME_PATTERN)) || name == ShaderRenderer.UNIFORM_BACKBUFFER) {
            Toast.makeText(context, R.string.invalid_texture_name, Toast.LENGTH_SHORT).show()
            return
        }

        SoftKeyboard.hide(context, nameView)

        val size = getPower(sizeBarView.progress)

        inProgress = true
        progressView.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val messageId = saveSampler(context, name, size)
            withContext(Dispatchers.Main) {
                inProgress = false
                progressView.visibility = View.GONE
                activity?.let { activity ->
                    if (messageId > 0) {
                        Toast.makeText(activity, messageId, Toast.LENGTH_SHORT).show()
                    } else {
                        if (addUniformView.isChecked) {
                            AddUniformActivity.setAddUniformResult(
                                activity, "uniform $samplerType $name;$params"
                            )
                        }
                        activity.finish()
                    }
                }
            }
        }
    }

    private fun getPower(power: Int): Int {
        return 1 shl (power + 1)
    }
}