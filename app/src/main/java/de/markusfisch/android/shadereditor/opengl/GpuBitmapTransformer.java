package de.markusfisch.android.shadereditor.opengl;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import de.markusfisch.android.shadereditor.graphics.BitmapEditor;

public final class GpuBitmapTransformer {
	private static final String TAG = "GpuBitmapTransformer";
	private static final RectF FULL_BOUNDS = new RectF(0f, 0f, 1f, 1f);

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
		// Utility class.
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
				dstHeight <= 0 ||
				EGL14.eglGetCurrentContext() != EGL14.EGL_NO_CONTEXT) {
			return null;
		}

		Session session = null;
		try {
			session = new Session(dstWidth, dstHeight);
			return session.transform(bitmap, rect, rotation);
		} catch (RuntimeException e) {
			Log.w(TAG, "Falling back to CPU bitmap transform", e);
			return null;
		} finally {
			if (session != null) {
				session.release();
			}
		}
	}

	private static final class Session {
		private final int dstWidth;
		private final int dstHeight;
		private final FloatBuffer vertexBuffer;
		private final TextureParameters textureParameters =
				new TextureParameters(
						GLES20.GL_LINEAR,
						GLES20.GL_LINEAR,
						GLES20.GL_CLAMP_TO_EDGE,
						GLES20.GL_CLAMP_TO_EDGE);

		private EGLDisplay display = EGL14.EGL_NO_DISPLAY;
		private EGLContext context = EGL14.EGL_NO_CONTEXT;
		private EGLSurface surface = EGL14.EGL_NO_SURFACE;
		private int program;
		private int textureId;
		private int positionLoc;
		private int texCoordLoc;
		private int frameLoc;

		private Session(int dstWidth, int dstHeight) {
			this.dstWidth = dstWidth;
			this.dstHeight = dstHeight;
			vertexBuffer = ByteBuffer.allocateDirect(16 * 4)
					.order(ByteOrder.nativeOrder())
					.asFloatBuffer();

			initEgl();
			initProgram();
		}

		@NonNull
		private Bitmap transform(
				@NonNull Bitmap bitmap,
				@NonNull RectF rect,
				float rotation) {
			createTexture(bitmap);
			updateVertexBuffer(rect, rotation);

			GLES20.glViewport(0, 0, dstWidth, dstHeight);
			GLES20.glClearColor(0f, 0f, 0f, 0f);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

			GLES20.glUseProgram(program);

			vertexBuffer.position(0);
			GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_FLOAT,
					false, 16, vertexBuffer);
			GLES20.glEnableVertexAttribArray(positionLoc);

			vertexBuffer.position(2);
			GLES20.glVertexAttribPointer(texCoordLoc, 2, GLES20.GL_FLOAT,
					false, 16, vertexBuffer);
			GLES20.glEnableVertexAttribArray(texCoordLoc);

			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
			GLES20.glUniform1i(frameLoc, 0);

			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
			checkGlError("glDrawArrays");

			ByteBuffer pixels = ByteBuffer.allocateDirect(dstWidth * dstHeight * 4)
					.order(ByteOrder.nativeOrder());
			GLES20.glReadPixels(
					0,
					0,
					dstWidth,
					dstHeight,
					GLES20.GL_RGBA,
					GLES20.GL_UNSIGNED_BYTE,
					pixels);
			checkGlError("glReadPixels");

			return BitmapEditor.createBitmapFromRgbaBuffer(pixels, dstWidth, dstHeight, true);
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

			int[] surfaceAttribs = {
					EGL14.EGL_WIDTH, dstWidth,
					EGL14.EGL_HEIGHT, dstHeight,
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

			if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
				throw new IllegalStateException("eglMakeCurrent failed");
			}
		}

		private void initProgram() {
			program = Program.loadProgram(VERTEX_SHADER, FRAGMENT_SHADER);
			if (program == 0) {
				List<ShaderError> log = Program.getInfoLog();
				throw new IllegalStateException(
						"Cannot create transform program: " + log);
			}
			positionLoc = GLES20.glGetAttribLocation(program, "position");
			texCoordLoc = GLES20.glGetAttribLocation(program, "texCoord");
			frameLoc = GLES20.glGetUniformLocation(program, "frame");
			if (positionLoc < 0 || texCoordLoc < 0 || frameLoc < 0) {
				throw new IllegalStateException("Failed to resolve shader handles");
			}
		}

		private void createTexture(@NonNull Bitmap bitmap) {
			int[] textures = new int[1];
			GLES20.glGenTextures(1, textures, 0);
			textureId = textures[0];
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
			textureParameters.setParameters(GLES20.GL_TEXTURE_2D);
			String message = TextureParameters.setBitmap(bitmap);
			if (message != null) {
				throw new IllegalStateException(message);
			}
			checkGlError("setBitmap");
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

		private void release() {
			if (textureId != 0) {
				GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
				textureId = 0;
			}
			if (program != 0) {
				GLES20.glDeleteProgram(program);
				program = 0;
			}
			if (display != EGL14.EGL_NO_DISPLAY) {
				EGL14.eglMakeCurrent(
						display,
						EGL14.EGL_NO_SURFACE,
						EGL14.EGL_NO_SURFACE,
						EGL14.EGL_NO_CONTEXT);
				if (surface != EGL14.EGL_NO_SURFACE) {
					EGL14.eglDestroySurface(display, surface);
					surface = EGL14.EGL_NO_SURFACE;
				}
				if (context != EGL14.EGL_NO_CONTEXT) {
					EGL14.eglDestroyContext(display, context);
					context = EGL14.EGL_NO_CONTEXT;
				}
				EGL14.eglReleaseThread();
				EGL14.eglTerminate(display);
				display = EGL14.EGL_NO_DISPLAY;
			}
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
		float sourceY;
		switch (normalizeRotation(rotation)) {
			case 90:
				sourceX = y;
				sourceY = 1f - x;
				break;
			case 180:
				sourceX = 1f - x;
				sourceY = 1f - y;
				break;
			case 270:
				sourceX = 1f - y;
				sourceY = x;
				break;
			default:
				sourceX = x;
				sourceY = y;
				break;
		}
		return new float[]{sourceX, 1f - sourceY};
	}

	private static int normalizeRotation(float rotation) {
		int degrees = Math.round(rotation) % 360;
		if (degrees < 0) {
			degrees += 360;
		}
		return degrees;
	}

	private static void checkGlError(String operation) {
		int error = GLES20.glGetError();
		if (error != GLES20.GL_NO_ERROR) {
			throw new IllegalStateException(
					operation + " failed with GL error 0x" + Integer.toHexString(error));
		}
	}
}
