package de.markusfisch.android.shadereditor.graphics;

import android.graphics.Bitmap;
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

final class GpuBitmapScaler {
	private static final String TAG = "GpuBitmapScaler";

	private static final String VERTEX_SHADER =
			"attribute vec2 aPosition;\n" +
			"attribute vec2 aTexCoord;\n" +
			"varying vec2 vTexCoord;\n" +
			"void main() {\n" +
			"\tvTexCoord = aTexCoord;\n" +
			"\tgl_Position = vec4(aPosition, 0.0, 1.0);\n" +
			"}";

	private static final String FRAGMENT_SHADER =
			"precision mediump float;\n" +
			"uniform sampler2D uTexture;\n" +
			"varying vec2 vTexCoord;\n" +
			"void main() {\n" +
			"\tgl_FragColor = texture2D(uTexture, vTexCoord);\n" +
			"}";

	private static final float[] QUAD = {
			-1f, -1f, 0f, 0f,
			1f, -1f, 1f, 0f,
			-1f, 1f, 0f, 1f,
			1f, 1f, 1f, 1f
	};

	private GpuBitmapScaler() {
		// Utility class.
	}

	@Nullable
	static Bitmap scale(@NonNull Bitmap bitmap, int dstWidth, int dstHeight) {
		if (bitmap.isRecycled() ||
				dstWidth <= 0 ||
				dstHeight <= 0 ||
				EGL14.eglGetCurrentContext() != EGL14.EGL_NO_CONTEXT) {
			return null;
		}

		Session session = null;
		try {
			session = new Session(dstWidth, dstHeight);
			return session.scale(bitmap);
		} catch (RuntimeException e) {
			Log.w(TAG, "Falling back to CPU bitmap scaling", e);
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
		private final FloatBuffer quadBuffer;

		private EGLDisplay display = EGL14.EGL_NO_DISPLAY;
		private EGLContext context = EGL14.EGL_NO_CONTEXT;
		private EGLSurface surface = EGL14.EGL_NO_SURFACE;
		private int program;
		private int textureId;
		private int positionLoc;
		private int texCoordLoc;
		private int textureLoc;

		private Session(int dstWidth, int dstHeight) {
			this.dstWidth = dstWidth;
			this.dstHeight = dstHeight;
			quadBuffer = ByteBuffer.allocateDirect(QUAD.length * 4)
					.order(ByteOrder.nativeOrder())
					.asFloatBuffer();
			quadBuffer.put(QUAD).position(0);

			initEgl();
			initProgram();
		}

		@NonNull
		private Bitmap scale(@NonNull Bitmap bitmap) {
			createTexture(bitmap);

			GLES20.glViewport(0, 0, dstWidth, dstHeight);
			GLES20.glClearColor(0f, 0f, 0f, 0f);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

			GLES20.glUseProgram(program);

			quadBuffer.position(0);
			GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_FLOAT,
					false, 16, quadBuffer);
			GLES20.glEnableVertexAttribArray(positionLoc);

			quadBuffer.position(2);
			GLES20.glVertexAttribPointer(texCoordLoc, 2, GLES20.GL_FLOAT,
					false, 16, quadBuffer);
			GLES20.glEnableVertexAttribArray(texCoordLoc);

			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
			GLES20.glUniform1i(textureLoc, 0);

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
			program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
			positionLoc = GLES20.glGetAttribLocation(program, "aPosition");
			texCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord");
			textureLoc = GLES20.glGetUniformLocation(program, "uTexture");
			if (positionLoc < 0 || texCoordLoc < 0 || textureLoc < 0) {
				throw new IllegalStateException("Failed to resolve shader handles");
			}
		}

		private void createTexture(@NonNull Bitmap bitmap) {
			int[] textures = new int[1];
			GLES20.glGenTextures(1, textures, 0);
			textureId = textures[0];
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
			GLES20.glTexParameteri(
					GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_MIN_FILTER,
					GLES20.GL_LINEAR);
			GLES20.glTexParameteri(
					GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_MAG_FILTER,
					GLES20.GL_LINEAR);
			GLES20.glTexParameteri(
					GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_WRAP_S,
					GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(
					GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_WRAP_T,
					GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexImage2D(
					GLES20.GL_TEXTURE_2D,
					0,
					GLES20.GL_RGBA,
					bitmap.getWidth(),
					bitmap.getHeight(),
					0,
					GLES20.GL_RGBA,
					GLES20.GL_UNSIGNED_BYTE,
					BitmapEditor.createRgbaBuffer(bitmap, true));
			checkGlError("glTexImage2D");
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

		private static int createProgram(String vertexShaderSource, String fragmentShaderSource) {
			int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource);
			int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource);
			int program = GLES20.glCreateProgram();
			if (program == 0) {
				throw new IllegalStateException("glCreateProgram failed");
			}
			GLES20.glAttachShader(program, vertexShader);
			GLES20.glAttachShader(program, fragmentShader);
			GLES20.glLinkProgram(program);

			int[] status = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
			GLES20.glDeleteShader(vertexShader);
			GLES20.glDeleteShader(fragmentShader);
			if (status[0] == 0) {
				String message = GLES20.glGetProgramInfoLog(program);
				GLES20.glDeleteProgram(program);
				throw new IllegalStateException("Program link failed: " + message);
			}
			return program;
		}

		private static int compileShader(int type, String source) {
			int shader = GLES20.glCreateShader(type);
			if (shader == 0) {
				throw new IllegalStateException("glCreateShader failed");
			}
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);

			int[] status = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
			if (status[0] == 0) {
				String message = GLES20.glGetShaderInfoLog(shader);
				GLES20.glDeleteShader(shader);
				throw new IllegalStateException("Shader compile failed: " + message);
			}
			return shader;
		}

		private static void checkGlError(String operation) {
			int error = GLES20.glGetError();
			if (error != GLES20.GL_NO_ERROR) {
				throw new IllegalStateException(
						operation + " failed with GL error 0x" + Integer.toHexString(error));
			}
		}
	}
}
