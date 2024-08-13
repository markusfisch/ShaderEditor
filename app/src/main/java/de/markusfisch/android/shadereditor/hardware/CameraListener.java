package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.FloatBuffer;
import java.util.concurrent.ExecutionException;

public class CameraListener {
	public final int facing;
	public final float[] addent = new float[]{0, 0};

	private final int cameraTextureId;

	@Nullable
	private SurfaceTexture surfaceTexture;
	@NonNull
	private final FloatBuffer orientationMatrix = FloatBuffer.allocate(4);

	@NonNull
	private final Context context;
	@NonNull
	private final Size frameSize;
	@Nullable
	private Camera camera;

	public CameraListener(
			int cameraTextureId,
			int facing,
			int width,
			int height,
			int deviceRotation,
			@NonNull Context context) {
		this.cameraTextureId = cameraTextureId;
		this.facing = facing;
		this.context = context;
		this.frameSize = new Size(width, height);
		setOrientationAndFlip(deviceRotation);
	}

	@NonNull
	public FloatBuffer getOrientationMatrix() {
		return orientationMatrix;
	}

	public void register(@NonNull LifecycleOwner lifecycleOwner) {
		Handler mainHandler = new Handler(Looper.getMainLooper());
		mainHandler.post(() -> {
			if (!lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
				return;
			}
			ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
					ProcessCameraProvider.getInstance(context);
			cameraProviderFuture.addListener(() -> {
				try {
					ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
					cameraProvider.unbindAll();
					Preview preview = new Preview.Builder()
							.setTargetResolution(frameSize)
							.build();
					preview.setSurfaceProvider(surfaceRequest -> {
						surfaceTexture = new SurfaceTexture(cameraTextureId);
						Size size = surfaceRequest.getResolution();
						surfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
						Surface surface = new Surface(surfaceTexture);
						surfaceRequest.provideSurface(surface,
								ContextCompat.getMainExecutor(context),
								result -> surface.release());
					});
					CameraSelector cameraSelector = new CameraSelector.Builder()
							.requireLensFacing(facing)
							.build();
					camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector,
							preview);
				} catch (ExecutionException | InterruptedException e) {
					// Handle exceptions
				}
			}, ContextCompat.getMainExecutor(context));
		});
	}

	public void unregister() {
		Handler mainHandler = new Handler(Looper.getMainLooper());
		mainHandler.post(() -> {
			if (camera != null) {
				ProcessCameraProvider cameraProvider;
				try {
					cameraProvider = ProcessCameraProvider.getInstance(context).get();
				} catch (ExecutionException | InterruptedException ignored) {
					return;
				}
				cameraProvider.unbindAll();
				camera = null;

				if (surfaceTexture != null) {
					surfaceTexture.release();
					surfaceTexture = null;
				}
			}
		});
	}

	public synchronized void update() {
		if (surfaceTexture != null) {
			surfaceTexture.updateTexImage();
		}
	}

	private void setOrientationAndFlip(int deviceRotation) {
		switch (deviceRotation) {
			default:
			case Surface.ROTATION_0:
				orientationMatrix.put(new float[]{
						0f, -1f,
						-1f, 0f,
				});
				addent[0] = 1f;
				addent[1] = 1f;
				break;
			case Surface.ROTATION_90:
				orientationMatrix.put(new float[]{
						1f, 0f,
						0f, -1f,
				});
				addent[0] = 0f;
				addent[1] = 1f;
				break;
			case Surface.ROTATION_180:
				orientationMatrix.put(new float[]{
						0f, 1f,
						1f, 0f,
				});
				addent[0] = 0f;
				addent[1] = 0f;
				break;
			case Surface.ROTATION_270:
				orientationMatrix.put(new float[]{
						-1f, 0f,
						0f, 1f,
				});
				addent[0] = 1f;
				addent[1] = 0f;
				break;
		}
		orientationMatrix.rewind();
	}
}