package de.markusfisch.android.shadereditor.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.markusfisch.android.shadereditor.opengl.ShaderError
import de.markusfisch.android.shadereditor.opengl.ShaderRenderer
import de.markusfisch.android.shadereditor.view.SystemBarMetrics
import de.markusfisch.android.shadereditor.widget.ShaderView

class PreviewActivity : AppCompatActivity() {

    companion object {
        const val FRAGMENT_SHADER = "fragment_shader"
        const val QUALITY = "quality"
        @JvmField
        val renderStatus = RenderStatus()
    }

    class RenderStatus {
        @Volatile
        var fps: Int = 0

        @Volatile
        var infoLog: List<ShaderError>? = null

        @Volatile
        var thumbnail: ByteArray? = null

        fun reset() {
            fps = 0
            infoLog = null
            thumbnail = null
        }
    }

    private val finishRunnable = Runnable { finish() }
    private val thumbnailRunnable = Runnable {
        shaderView?.let {
            renderStatus.thumbnail = it.renderer.thumbnail
        }
    }

    private var shaderView: ShaderView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderStatus.reset()
        shaderView = ShaderView(this)

        if (!setShaderFromIntent(intent)) {
            finish()
            return
        }

        shaderView?.renderer?.setOnRendererListener(object : ShaderRenderer.OnRendererListener {
            override fun onFramesPerSecond(fps: Int) {
                renderStatus.fps = fps
            }

            override fun onInfoLog(infoLog: List<ShaderError>) {
                renderStatus.infoLog = infoLog
                if (infoLog.isNotEmpty()) {
                    runOnUiThread(finishRunnable)
                }
            }
        })

        setContentView(shaderView)
        SystemBarMetrics.hideNavigation(window)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!setShaderFromIntent(intent)) {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        shaderView?.onResume()
        renderStatus.reset()
        shaderView?.postDelayed(thumbnailRunnable, 500)
    }

    override fun onStop() {
        super.onStop()
        shaderView?.onPause()
    }

    private fun setShaderFromIntent(intent: Intent?): Boolean {
        val fragmentShader = intent?.getStringExtra(FRAGMENT_SHADER) ?: return false
        shaderView?.setFragmentShader(fragmentShader, intent.getFloatExtra(QUALITY, 1f))
        return true
    }
}
