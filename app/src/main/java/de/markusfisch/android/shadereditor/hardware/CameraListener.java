package de.markusfisch.android.shadereditor.hardware;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.Executors;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class CameraListener {
	public final int cameraId;
	public final float[] addent = new float[]{0, 0};

	private final int cameraTextureId;
	private final int frameOrientation;

	private int frameWidth;
	private int frameHeight;
	private boolean pausing = true;
	private boolean opening = false;
	private boolean available = false;
	private Camera cam;
	private SurfaceTexture surfaceTexture;
	private FloatBuffer orientationMatrix;

	public CameraListener(
			int cameraTextureId,
			int cameraId,
			int width,
			int height,
			int deviceRotation) {
		this.cameraTextureId = cameraTextureId;
		this.cameraId = cameraId;
		frameOrientation = getCameraDisplayOrientation(cameraId,
				deviceRotation);
		frameWidth = width;
		frameHeight = height;
		setOrientationAndFlip(frameOrientation);
	}

	public static int findCameraId(int facing) {
		for (int i = 0, l = Camera.getNumberOfCameras(); i < l; ++i) {
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == facing) {
				return i;
			}
		}
		return -1;
	}

	public FloatBuffer getOrientationMatrix() {
		return orientationMatrix;
	}

	public void register() {
		if (!pausing) {
			return;
		}
		pausing = false;
		stopPreview();
		openCameraAsync();
	}

	public void unregister() {
		pausing = true;
		stopPreview();
	}

	public synchronized void update() {
		if (surfaceTexture != null && available) {
			surfaceTexture.updateTexImage();
			available = false;
		}
	}

	private void openCameraAsync() {
		if (opening) {
			return;
		}
		opening = true;
		Handler handler = new Handler(Looper.getMainLooper());
		Executors.newSingleThreadExecutor().execute(() -> {
			if (pausing) {
				return;
			}
			try {
				Camera camera = Camera.open(cameraId);
				handler.post(() -> {
					if (camera != null && !startPreview(camera)) {
						camera.release();
					}
					opening = false;
				});
			} catch (RuntimeException ignored) {
			}
		});
	}

	private void stopPreview() {
		if (cam == null) {
			return;
		}

		cam.stopPreview();
		cam.release();
		cam = null;

		surfaceTexture.release();
		surfaceTexture = null;
	}

	private boolean startPreview(Camera camera) {
		if (pausing || camera == null) {
			return false;
		}

		camera.setDisplayOrientation(frameOrientation);

		Camera.Parameters parameters = camera.getParameters();
		parameters.setRotation(frameOrientation);
		setPreviewSize(parameters);
		setFastestFps(parameters);
		setFocusMode(parameters);
		camera.setParameters(parameters);

		surfaceTexture = new SurfaceTexture(cameraTextureId);
		surfaceTexture.setOnFrameAvailableListener(
				st -> {
					synchronized (CameraListener.this) {
						available = true;
					}
				});

		try {
			camera.setPreviewTexture(surfaceTexture);
		} catch (IOException e) {
			return false;
		}

		cam = camera;
		camera.startPreview();
		return true;
	}

	private void setOrientationAndFlip(int orientation) {
		switch (orientation) {
			default:
			case 0:
				orientationMatrix = FloatBuffer.wrap(new float[]{
						1f, 0f,
						0f, -1f,
				});
				addent[0] = 0f;
				addent[1] = 1f;
				break;
			case 90:
				orientationMatrix = FloatBuffer.wrap(new float[]{
						0f, -1f,
						-1f, 0f,
				});
				addent[0] = 1f;
				addent[1] = 1f;
				break;
			case 180:
				orientationMatrix = FloatBuffer.wrap(new float[]{
						-1f, 0f,
						0f, 1f,
				});
				addent[0] = 1f;
				addent[1] = 0f;
				break;
			case 270:
				orientationMatrix = FloatBuffer.wrap(new float[]{
						0f, 1f,
						1f, 0f,
				});
				addent[0] = 0f;
				addent[1] = 0f;
				break;
		}
	}

	private void setPreviewSize(Camera.Parameters parameters) {
		Camera.Size size = findBestSize(
				// Will always return at least one item.
				parameters.getSupportedPreviewSizes(),
				frameWidth,
				frameHeight,
				frameOrientation);

		if (size != null) {
			frameWidth = size.width;
			frameHeight = size.height;
			parameters.setPreviewSize(frameWidth, frameHeight);
		}
	}

	private static int getCameraDisplayOrientation(
			int cameraId,
			int deviceRotation) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		return (info.orientation - deviceRotation + 360) % 360;
	}

	private static Camera.Size findBestSize(
			List<Camera.Size> sizes,
			int width,
			int height,
			int orientation) {
		switch (orientation) {
			default:
				break;
			case 90:
			case 270:
				// Swap dimensions to match orientation
				// of preview sizes.
				int tmp = width;
				width = height;
				height = tmp;
				break;
		}

		double targetRatio = (double) width / height;
		double minDiff = Double.MAX_VALUE;
		double minDiffAspect = Double.MAX_VALUE;
		Camera.Size bestSize = null;
		Camera.Size bestSizeAspect = null;

		for (Camera.Size size : sizes) {
			double diff = (double)
					Math.abs(size.height - height) +
					Math.abs(size.width - width);

			if (diff < minDiff) {
				bestSize = size;
				minDiff = diff;
			}

			double ratio = (double) size.width / size.height;

			if (Math.abs(ratio - targetRatio) < 0.1 &&
					diff < minDiffAspect) {
				bestSizeAspect = size;
				minDiffAspect = diff;
			}
		}

		return bestSizeAspect != null ? bestSizeAspect : bestSize;
	}

	private static void setFastestFps(Camera.Parameters parameters) {
		try {
			int[] range = findFastestFpsRange(
					parameters.getSupportedPreviewFpsRange());

			if (range[0] > 0) {
				parameters.setPreviewFpsRange(range[0], range[1]);
			}
		} catch (RuntimeException e) {
			// Silently ignore that exception.
			// If the fps range can't be increased,
			// there's nothing to do.
		}
	}

	private static int[] findFastestFpsRange(List<int[]> ranges) {
		int[] fastest = new int[]{0, 0};

		for (int n = ranges.size(); n-- > 0; ) {
			int[] range = ranges.get(n);

			if (range[0] >= fastest[0] && range[1] > fastest[1]) {
				fastest = range;
			}
		}

		return fastest;
	}

	private static void setFocusMode(Camera.Parameters parameters) {
		// Best for taking pictures.
		String continuousPicture =
				Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
		// Less aggressive than CONTINUOUS_PICTURE.
		String continuousVideo =
				Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
		// Last resort.
		String autoFocus = Camera.Parameters.FOCUS_MODE_AUTO;

		// Prefer feature detection instead of checking BUILD.VERSION.
		List<String> focusModes = parameters.getSupportedFocusModes();

		if (focusModes.contains(continuousPicture)) {
			parameters.setFocusMode(continuousPicture);
		} else if (focusModes.contains(continuousVideo)) {
			parameters.setFocusMode(continuousVideo);
		} else if (focusModes.contains(autoFocus)) {
			parameters.setFocusMode(autoFocus);
		}
	}
}
