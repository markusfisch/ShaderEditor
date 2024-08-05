package de.markusfisch.android.shadereditor.opengl;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

class Program {
	@NonNull
	private static List<ShaderError> infoLog = Collections.emptyList();

	@NonNull
	static List<ShaderError> getInfoLog() {
		return infoLog;
	}

	static int loadProgram(
			String vertexShader,
			String fragmentShader) {
		int vs, fs, p = 0;

		if ((vs = compileShader(
				GLES20.GL_VERTEX_SHADER,
				vertexShader)) != 0) {
			if ((fs = compileShader(
					GLES20.GL_FRAGMENT_SHADER,
					fragmentShader)) != 0) {
				p = linkProgram(vs, fs);

				// Mark shader objects as deleted so they get
				// deleted as soon as glDeleteProgram() does
				// detach them.
				GLES20.glDeleteShader(fs);
			}

			// Same as above.
			GLES20.glDeleteShader(vs);
		}

		return p;
	}

	private static int linkProgram(int... shaders) {
		int p = GLES20.glCreateProgram();

		if (p == 0) {
			infoLog = List.of(ShaderError.createGeneral("Cannot create program"));
			return 0;
		}

		for (int shader : shaders) {
			GLES20.glAttachShader(p, shader);
		}

		GLES20.glLinkProgram(p);

		int[] linkStatus = new int[1];
		GLES20.glGetProgramiv(
				p,
				GLES20.GL_LINK_STATUS,
				linkStatus, 0);

		if (linkStatus[0] != GLES20.GL_TRUE) {
			infoLog = ShaderError.parseAll(GLES20.glGetProgramInfoLog(p));

			GLES20.glDeleteProgram(p);
			p = 0;
		}

		return p;
	}

	private static int compileShader(int type, String src) {
		int s = GLES20.glCreateShader(type);

		if (s == 0) {
			infoLog = List.of(ShaderError.createGeneral("Cannot create shader"));
			return 0;
		}

		GLES20.glShaderSource(s, src);
		GLES20.glCompileShader(s);

		int[] compiled = new int[1];
		GLES20.glGetShaderiv(
				s,
				GLES20.GL_COMPILE_STATUS,
				compiled,
				0);

		if (compiled[0] == 0) {
			infoLog = ShaderError.parseAll(GLES20.glGetShaderInfoLog(s));

			GLES20.glDeleteShader(s);
			s = 0;
		}

		return s;
	}
}
