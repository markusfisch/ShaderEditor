package de.markusfisch.android.shadereditor.opengl;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import de.markusfisch.android.shadereditor.graphics.BitmapEditor;

public final class GpuBitmapTransformer {
	private static final String TAG = "GpuBitmapTransformer";
	private static final RectF FULL_BOUNDS = new RectF(0f, 0f, 1f, 1f);
	private static final Engine ENGINE = new Engine();
	private static final int MAX_RENDER_ATTEMPTS = 2;

	private static final String VERTEX_SHADER = """
			attribute vec2 position;
			attribute vec2 texCoord;
			varying vec2 uv;
			void main() {
				uv = texCoord;
				gl_Position = vec4(position, 0., 1.);
			}
			""";
	private static final String FRAGMENT_SHADER = """
			#ifdef GL_FRAGMENT_PRECISION_HIGH
			precision highp float;
			#else
			precision mediump float;
			#endif

			uniform sampler2D frame;
			varying vec2 uv;
			void main(void) {
				gl_FragColor = texture2D(frame, uv).rgba;
			}
			""";
	private static final float[] POSITIONS = {
			-1f, -1f,
			1f, -1f,
			-1f, 1f,
			1f, 1f
	};

	private GpuBitmapTransformer() {
	}

	@Nullable
	public static Bitmap scale(
			@NonNull Bitmap bitmap,
			int dstWidth,
			int dstHeight) {
		return transform(bitmap, FULL_BOUNDS, 0f, dstWidth, dstHeight);
	}

	@Nullable
	public static Bitmap transform(
			@NonNull Bitmap bitmap,
			@NonNull RectF rect,
			float rotation,
			int dstWidth,
			int dstHeight) {
		if (bitmap.isRecycled() ||
				dstWidth <= 0 ||
				dstHeight <= 0) {
			return null;
		}

		try {
			return ENGINE.transform(
					bitmap,
					new RectF(rect),
					rotation,
					dstWidth,
					dstHeight);
		} catch (RuntimeException | OutOfMemoryError e) {
			Log.w(TAG, "Falling back to CPU bitmap transform", e);
			return null;
		}
	}

	private static void putVertex(
			float x,
			float y,
			@NonNull float[] texCoord,
			@NonNull FloatBuffer buffer) {
		buffer.put(x);
		buffer.put(y);
		buffer.put(texCoord[0]);
		buffer.put(texCoord[1]);
	}

	@NonNull
	private static float[] mapCorner(float x, float y, float rotation) {
		float sourceX;
		float sourceY = switch (normalizeRotation(rotation)) {
			case 90 -> {
				//noinspection SuspiciousNameCombination
				sourceX = y;
				yield 1f - x;
			}
			case 180 -> {
				sourceX = 1f - x;
				yield 1f - y;
			}
			case 270 -> {
				sourceX = 1f - y;
				yield x;
			}
			default -> {
				sourceX = x;
				yield y;
			}
		};
		return new float[]{sourceX, 1f - sourceY};
	}

	private static int normalizeRotation(float rotation) {
		int degrees = Math.round(rotation) % 360;
		if (degrees < 0) {
			degrees += 360;
		}
		return degrees;
	}

	private static final class Engine {
		private final Object lock = new Object();
		private final FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(16 * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		private final Mesh texturedQuad = Mesh.texturedQuad(vertexBuffer);
		private final GlDevice device = new GlDevice(1);
		private final TextureParameters textureParameters =
				new TextureParameters(
						GLES20.GL_LINEAR,
						GLES20.GL_LINEAR,
						GLES20.GL_CLAMP_TO_EDGE,
						GLES20.GL_CLAMP_TO_EDGE);

		private HandlerThread thread;
		private Handler handler;

		private EGLDisplay display = EGL14.EGL_NO_DISPLAY;
		private EGLContext context = EGL14.EGL_NO_CONTEXT;
		private EGLSurface surface = EGL14.EGL_NO_SURFACE;
		private GlProgram program;
		private ProgramBindings programBindings;
		private GlTexture2D sourceTexture;
		private GlFramebuffer framebuffer;
		private GlTexture2D framebufferTexture;
		private int framebufferWidth;
		private int framebufferHeight;
		private ByteBuffer pixels;

		@NonNull
		private Bitmap transform(
				@NonNull Bitmap bitmap,
				@NonNull RectF rect,
				float rotation,
				int dstWidth,
				int dstHeight) {
			if (isEngineThread()) {
				return transformOnThread(bitmap, rect, rotation, dstWidth, dstHeight);
			}

			FutureTask<Bitmap> task = new FutureTask<>(
					() -> transformOnThread(bitmap, rect, rotation, dstWidth, dstHeight));
			post(task);

			try {
				return task.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Bitmap transform interrupted", e);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException) {
					throw (RuntimeException) cause;
				}
				if (cause instanceof Error) {
					throw (Error) cause;
				}
				throw new IllegalStateException(cause);
			}
		}

		@NonNull
		private Bitmap transformOnThread(
				@NonNull Bitmap bitmap,
				@NonNull RectF rect,
				float rotation,
				int dstWidth,
				int dstHeight) {
			for (int attempt = 0; ; ++attempt) {
				try {
					ensureReady();
					return render(bitmap, rect, rotation, dstWidth, dstHeight);
				} catch (RuntimeException error) {
					if (attempt + 1 >= MAX_RENDER_ATTEMPTS) {
						throw error;
					}
					Log.w(TAG, "Re-initializing bitmap transform context", error);
					releaseUnsafe();
				}
			}
		}

		private void ensureReady() {
			if (!isReady()) {
				initEgl();
				initProgram();
				return;
			}
			makeCurrent();
		}

		private void initEgl() {
			display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
			if (display == EGL14.EGL_NO_DISPLAY) {
				throw new IllegalStateException("eglGetDisplay failed");
			}

			int[] version = new int[2];
			if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
				throw new IllegalStateException("eglInitialize failed");
			}

			int[] attribs = {
					EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
					EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
					EGL14.EGL_RED_SIZE, 8,
					EGL14.EGL_GREEN_SIZE, 8,
					EGL14.EGL_BLUE_SIZE, 8,
					EGL14.EGL_ALPHA_SIZE, 8,
					EGL14.EGL_NONE
			};
			EGLConfig[] configs = new EGLConfig[1];
			int[] numConfigs = new int[1];
			if (!EGL14.eglChooseConfig(
					display,
					attribs,
					0,
					configs,
					0,
					configs.length,
					numConfigs,
					0) || numConfigs[0] < 1) {
				throw new IllegalStateException("eglChooseConfig failed");
			}
			int[] contextAttribs = {
					EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
					EGL14.EGL_NONE
			};
			context = EGL14.eglCreateContext(
					display,
					configs[0],
					EGL14.EGL_NO_CONTEXT,
					contextAttribs,
					0);
			if (context == null || context == EGL14.EGL_NO_CONTEXT) {
				throw new IllegalStateException("eglCreateContext failed");
			}

			// Keep the pbuffer minimal; the actual output lives in an FBO-attached texture.
			int[] surfaceAttribs = {
					EGL14.EGL_WIDTH, 1,
					EGL14.EGL_HEIGHT, 1,
					EGL14.EGL_NONE
			};
			surface = EGL14.eglCreatePbufferSurface(
					display,
					configs[0],
					surfaceAttribs,
					0);
			if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
				throw new IllegalStateException("eglCreatePbufferSurface failed");
			}

			makeCurrent();
			device.resetState();
			device.disable(GLES20.GL_BLEND);
			device.disable(GLES20.GL_CULL_FACE);
			device.disable(GLES20.GL_DEPTH_TEST);
			device.setClearColor(0f, 0f, 0f, 0f);
		}

		private void initProgram() {
			GlProgramBuildResult result = device.createProgram(
					VERTEX_SHADER,
					FRAGMENT_SHADER,
					0);
			if (!result.succeeded() || result.getProgram() == null) {
				List<ShaderError> log = result.getInfoLog();
				throw new IllegalStateException(
						"Cannot create transform program: " + log);
			}
			program = result.getProgram();
			programBindings = new ProgramBindings(program);
		}

		private void makeCurrent() {
			if (EGL14.eglGetCurrentContext() == context &&
					EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW) == surface &&
					EGL14.eglGetCurrentSurface(EGL14.EGL_READ) == surface) {
				return;
			}
			if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
				throw new IllegalStateException("eglMakeCurrent failed");
			}
		}

		private boolean isReady() {
			return display != EGL14.EGL_NO_DISPLAY &&
					context != EGL14.EGL_NO_CONTEXT &&
					surface != EGL14.EGL_NO_SURFACE &&
					program != null &&
					program.isValid();
		}

		private void createTexture(@NonNull Bitmap bitmap) {
			if (sourceTexture == null) {
				sourceTexture = device.createTexture2D();
			}
			device.applyTextureParameters(sourceTexture, textureParameters);
			String message = device.uploadTexture2D(sourceTexture, bitmap, true);
			if (message != null) {
				throw new IllegalStateException(message);
			}
		}

		private void ensureFramebuffer(int dstWidth, int dstHeight) {
			if (framebuffer == null) {
				framebuffer = device.createFramebuffer();
			}
			if (framebufferTexture == null) {
				framebufferTexture = device.createTexture2D();
			}

			device.applyTextureParameters(framebufferTexture, textureParameters);
			if (framebufferWidth != dstWidth || framebufferHeight != dstHeight) {
				device.allocateTexture2D(framebufferTexture, dstWidth, dstHeight);
				framebufferWidth = dstWidth;
				framebufferHeight = dstHeight;
			}

			device.attachColor(framebuffer, framebufferTexture);
			int status = device.checkFramebufferStatus(framebuffer);
			if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
				throw new IllegalStateException(
						"Framebuffer incomplete: 0x" + Integer.toHexString(status));
			}
		}

		@NonNull
		private Bitmap render(
				@NonNull Bitmap bitmap,
				@NonNull RectF rect,
				float rotation,
				int dstWidth,
				int dstHeight) {
			createTexture(bitmap);
			ensureFramebuffer(dstWidth, dstHeight);
			updateVertexBuffer(rect, rotation);

			device.bindFramebuffer(framebuffer);
			device.setViewport(0, 0, dstWidth, dstHeight);
			device.clear(GLES20.GL_COLOR_BUFFER_BIT);

			programBindings.clear();
			programBindings.setTexture("frame", sourceTexture);
			device.applyBindings(programBindings);
			device.draw(texturedQuad, program);

			ByteBuffer pixelBuffer = getPixelBuffer(dstWidth * dstHeight * 4);
			pixelBuffer.position(0);
			device.readPixels(
					0,
					0,
					dstWidth,
					dstHeight,
					pixelBuffer);

			device.bindFramebuffer(null);

			return BitmapEditor.createBitmapFromRgbaBuffer(
					pixelBuffer,
					dstWidth,
					dstHeight,
					true);
		}

		private void updateVertexBuffer(@NonNull RectF rect, float rotation) {
			float[] topLeft = mapCorner(rect.left, rect.top, rotation);
			float[] topRight = mapCorner(rect.right, rect.top, rotation);
			float[] bottomLeft = mapCorner(rect.left, rect.bottom, rotation);
			float[] bottomRight = mapCorner(rect.right, rect.bottom, rotation);

			vertexBuffer.position(0);
			putVertex(POSITIONS[0], POSITIONS[1], bottomLeft, vertexBuffer);
			putVertex(POSITIONS[2], POSITIONS[3], bottomRight, vertexBuffer);
			putVertex(POSITIONS[4], POSITIONS[5], topLeft, vertexBuffer);
			putVertex(POSITIONS[6], POSITIONS[7], topRight, vertexBuffer);
			vertexBuffer.position(0);
		}

		@NonNull
		private ByteBuffer getPixelBuffer(int size) {
			if (pixels == null || pixels.capacity() < size) {
				pixels = ByteBuffer.allocateDirect(size)
						.order(ByteOrder.nativeOrder());
			}
			return pixels;
		}

		private void post(@NonNull Runnable runnable) {
			synchronized (lock) {
				ensureThreadLocked();
				if (!handler.post(runnable)) {
					throw new IllegalStateException("Cannot post bitmap transform task");
				}
			}
		}

		private boolean isEngineThread() {
			synchronized (lock) {
				return thread != null &&
						thread.isAlive() &&
						Looper.myLooper() == thread.getLooper();
			}
		}

		private void ensureThreadLocked() {
			if (thread != null && thread.isAlive()) {
				return;
			}
			thread = new HandlerThread(TAG);
			thread.start();
			handler = new Handler(thread.getLooper());
		}

		private void releaseUnsafe() {
			if (display != EGL14.EGL_NO_DISPLAY &&
					context != EGL14.EGL_NO_CONTEXT &&
					surface != EGL14.EGL_NO_SURFACE &&
					EGL14.eglMakeCurrent(display, surface, surface, context)) {
				deleteGlObjects();
			}
			if (display != EGL14.EGL_NO_DISPLAY) {
				EGL14.eglMakeCurrent(
						display,
						EGL14.EGL_NO_SURFACE,
						EGL14.EGL_NO_SURFACE,
						EGL14.EGL_NO_CONTEXT);
				if (surface != EGL14.EGL_NO_SURFACE) {
					EGL14.eglDestroySurface(display, surface);
				}
				if (context != EGL14.EGL_NO_CONTEXT) {
					EGL14.eglDestroyContext(display, context);
				}
				EGL14.eglReleaseThread();
				EGL14.eglTerminate(display);
			}

			display = EGL14.EGL_NO_DISPLAY;
			context = EGL14.EGL_NO_CONTEXT;
			surface = EGL14.EGL_NO_SURFACE;
			device.resetState();
			program = null;
			programBindings = null;
			sourceTexture = null;
			framebuffer = null;
			framebufferTexture = null;
			framebufferWidth = 0;
			framebufferHeight = 0;
			pixels = null;
		}

		private void deleteGlObjects() {
			device.deleteTexture(sourceTexture);
			device.deleteTexture(framebufferTexture);
			device.deleteFramebuffer(framebuffer);
			device.deleteProgram(program);
		}
	}
}
