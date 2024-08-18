package de.markusfisch.android.shadereditor.hardware

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.nio.FloatBuffer
import java.util.concurrent.ExecutionException

class CameraListener(
    private val cameraTextureId: Int,
    val facing: Int,
    width: Int,
    height: Int,
    deviceRotation: Int,
    private val context: Context
) {
    val addent = floatArrayOf(0f, 0f)

    private var surfaceTexture: SurfaceTexture? = null
    private val orientationMatrix: FloatBuffer = FloatBuffer.allocate(4)
    private val frameSize: Size = Size(width, height)
    private var camera: Camera? = null

    init {
        setOrientationAndFlip(deviceRotation)
    }

    fun getOrientationMatrix(): FloatBuffer = orientationMatrix

    fun register(lifecycleOwner: LifecycleOwner): Boolean = Handler(Looper.getMainLooper()).post {
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@post

        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val preview = Preview.Builder().setTargetResolution(frameSize).build()

                preview.setSurfaceProvider { surfaceRequest ->
                    surfaceTexture = SurfaceTexture(cameraTextureId).apply {
                        setDefaultBufferSize(
                            surfaceRequest.resolution.width,
                            surfaceRequest.resolution.height
                        )
                    }

                    val surface = Surface(surfaceTexture)
                    surfaceRequest.provideSurface(
                        surface, ContextCompat.getMainExecutor(context)
                    ) { surface.release() }
                }

                val cameraSelector = CameraSelector.Builder().requireLensFacing(facing).build()

                camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

            } catch (e: ExecutionException) {
                // Handle exception
            } catch (e: InterruptedException) {
                // Handle exception
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun unregister(): Boolean = Handler(Looper.getMainLooper()).post {
        camera?.let {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
                camera = null
            } catch (e: ExecutionException) {
                // Handle exception
            } catch (e: InterruptedException) {
                // Handle exception
            }

            surfaceTexture?.release()
            surfaceTexture = null
        }
    }

    @Synchronized
    fun update() {
        surfaceTexture?.updateTexImage()
    }

    private fun setOrientationAndFlip(deviceRotation: Int) {
        val orientationValues = when (deviceRotation) {
            Surface.ROTATION_0 -> floatArrayOf(0f, -1f, -1f, 0f).also {
                addent[0] = 1f
                addent[1] = 1f
            }

            Surface.ROTATION_90 -> floatArrayOf(1f, 0f, 0f, -1f).also {
                addent[0] = 0f
                addent[1] = 1f
            }

            Surface.ROTATION_180 -> floatArrayOf(0f, 1f, 1f, 0f).also {
                addent[0] = 0f
                addent[1] = 0f
            }

            Surface.ROTATION_270 -> floatArrayOf(-1f, 0f, 0f, 1f).also {
                addent[0] = 1f
                addent[1] = 0f
            }

            else -> floatArrayOf(0f, -1f, -1f, 0f).also {
                addent[0] = 1f
                addent[1] = 1f
            }
        }
        orientationMatrix.put(orientationValues).rewind()
    }
}