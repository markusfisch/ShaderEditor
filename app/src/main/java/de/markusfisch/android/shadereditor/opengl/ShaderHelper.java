// app/src/main/java/de/markusfisch/android/shadereditor/opengl/ShaderHelper.java
package de.markusfisch.android.shadereditor.opengl;

import android.opengl.GLES20;

import java.nio.FloatBuffer;

/**
 * A stateless utility class for common, low-level OpenGL operations.
 * This class is a "headless" renderer; it executes commands but manages no state.
 */
public final class ShaderHelper {

	// Your existing vertex shader source
	private static final String DEFAULT_VERTEX_SHADER =
			"attribute vec4 a_Position;void main(){gl_Position=a_Position;}";

	/**
	 * Compiles, links, and validates a new shader program.
	 *
	 * @param fragmentShader The GLSL source code for the fragment shader.
	 * @return The program ID, or 0 on failure.
	 */
	public static int linkProgram(String fragmentShader) {
		// Your existing shader compilation logic (loadShader, etc.) goes here.
		// For brevity, assuming you have a Shader.compileShader() helper.
		int vertexShaderId = Shader.compileShader(GLES20.GL_VERTEX_SHADER, DEFAULT_VERTEX_SHADER);
		int fragmentShaderId = Shader.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

		if (vertexShaderId == 0 || fragmentShaderId == 0) {
			return 0;
		}

		int programId = GLES20.glCreateProgram();
		if (programId == 0) {
			return 0;
		}

		GLES20.glAttachShader(programId, vertexShaderId);
		GLES20.glAttachShader(programId, fragmentShaderId);
		GLES20.glLinkProgram(programId);
		// ... (add error checking for linking)

		return programId;
	}

	/**
	 * Executes a draw call for a simple quad.
	 *
	 * @param programId    The program to use for drawing.
	 * @param vertexBuffer A FloatBuffer containing the quad's vertices.
	 */
	public static void drawQuad(int programId, FloatBuffer vertexBuffer) {
		int positionHandle = GLES20.glGetAttribLocation(programId, "a_Position");
		if (positionHandle == -1) {
			return;
		}

		GLES20.glEnableVertexAttribArray(positionHandle);
		GLES20.glVertexAttribPointer(
				positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		GLES20.glDisableVertexAttribArray(positionHandle);
	}
}