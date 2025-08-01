package de.markusfisch.android.shadereditor.opengl;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Utility class for compiling and linking OpenGL shaders.
 */
public final class Shader {
	private static final String TAG = "Shader";

	/**
	 * Compiles a shader from the given source string.
	 *
	 * @param type   The type of shader (GLES20.GL_VERTEX_SHADER or GLES20.GL_FRAGMENT_SHADER).
	 * @param source The GLSL source code.
	 * @return The shader ID, or 0 if compilation fails.
	 */
	public static int compileShader(int type, String source) {
		int shader = GLES20.glCreateShader(type);
		if (shader == 0) {
			Log.e(TAG, "Could not create shader, type: " + type);
			return 0;
		}

		GLES20.glShaderSource(shader, source);
		GLES20.glCompileShader(shader);

		final int[] compiled = new int[1];
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

		if (compiled[0] == 0) {
			Log.e(TAG, "Could not compile shader, type: " + type);
			Log.e(TAG, "Info log:\n" + GLES20.glGetShaderInfoLog(shader));
			GLES20.glDeleteShader(shader);
			return 0;
		}

		return shader;
	}

	/**
	 * Links a vertex and fragment shader into a shader program.
	 *
	 * @param vertexShaderId   The ID of the compiled vertex shader.
	 * @param fragmentShaderId The ID of the compiled fragment shader.
	 * @return The program ID, or 0 if linking fails.
	 */
	public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
		if (vertexShaderId == 0 || fragmentShaderId == 0) {
			return 0;
		}

		int program = GLES20.glCreateProgram();
		if (program == 0) {
			Log.e(TAG, "Could not create program");
			return 0;
		}

		GLES20.glAttachShader(program, vertexShaderId);
		GLES20.glAttachShader(program, fragmentShaderId);
		GLES20.glLinkProgram(program);

		final int[] linked = new int[1];
		GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);

		if (linked[0] == 0) {
			Log.e(TAG, "Could not link program");
			Log.e(TAG, "Info log:\n" + GLES20.glGetProgramInfoLog(program));
			GLES20.glDeleteProgram(program);
			return 0;
		}

		return program;
	}
}