package de.markusfisch.android.shadereditor.opengl;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class TextureParameters {
	protected static final String SEPARATOR = ";";
	protected static final String ASSIGN = ":";

	private static final int DEFAULT_MIN = GLES20.GL_NEAREST;
	private static final int DEFAULT_MAG = GLES20.GL_LINEAR;
	private static final int DEFAULT_WRAP_S = GLES20.GL_REPEAT;
	private static final int DEFAULT_WRAP_T = GLES20.GL_REPEAT;
	private static final String HEADER = "///";
	private static final String MIN = "min";
	private static final String MAG = "mag";
	private static final String WRAP_S = "s";
	private static final String WRAP_T = "t";
	private static final Matrix flipMatrix = new Matrix();
	static {
		flipMatrix.postScale(1f, -1f);
	}

	private int min = DEFAULT_MIN;
	private int mag = DEFAULT_MAG;
	private int wrapS = DEFAULT_WRAP_S;
	private int wrapT = DEFAULT_WRAP_T;

	public static String create(
			String min,
			String mag,
			String wrapS,
			String wrapT) {
		if (mapMinParameter(min) == DEFAULT_MIN &&
				mapMagParameter(mag) == DEFAULT_MAG &&
				mapWrapParameter(wrapS) == DEFAULT_WRAP_S &&
				mapWrapParameter(wrapT) == DEFAULT_WRAP_T) {
			// use empty string for default values
			return "";
		}
		return HEADER +
				MIN + ASSIGN + min + SEPARATOR +
				MAG + ASSIGN + mag + SEPARATOR +
				WRAP_S + ASSIGN + wrapS + SEPARATOR +
				WRAP_T + ASSIGN + wrapT + SEPARATOR;
	}

	TextureParameters(int min, int mag, int wrapS, int wrapT) {
		set(min, mag, wrapS, wrapT);
	}

	TextureParameters(String params) {
		parse(params);
	}

	void set(int min, int mag, int wrapS, int wrapT) {
		this.min = min;
		this.mag = mag;
		this.wrapS = wrapS;
		this.wrapT = wrapT;
	}

	void setParameters(int target) {
		GLES20.glTexParameteri(
				target,
				GLES20.GL_TEXTURE_MIN_FILTER,
				min);
		GLES20.glTexParameteri(
				target,
				GLES20.GL_TEXTURE_MAG_FILTER,
				mag);
		GLES20.glTexParameteri(
				target,
				GLES20.GL_TEXTURE_WRAP_S,
				wrapS);
		GLES20.glTexParameteri(
				target,
				GLES20.GL_TEXTURE_WRAP_T,
				wrapT);
	}

	static void setBitmap(Bitmap bitmap) {
		if (bitmap == null) {
			return;
		}
		// flip bitmap because 0/0 is bottom left in OpenGL
		Bitmap flippedBitmap = Bitmap.createBitmap(
				bitmap,
				0,
				0,
				bitmap.getWidth(),
				bitmap.getHeight(),
				flipMatrix,
				true);
		GLUtils.texImage2D(
				GLES20.GL_TEXTURE_2D,
				0,
				GLES20.GL_RGBA,
				flippedBitmap,
				GLES20.GL_UNSIGNED_BYTE,
				0);
		flippedBitmap.recycle();
	}

	void parse(String params) {
		if (params == null) {
			return;
		}
		params = params.trim();
		int p = params.indexOf(HEADER);
		if (p != 0) {
			return;
		}
		params = params.substring(p + 3);
		for (String param : params.split(SEPARATOR)) {
			String exp[] = param.split(ASSIGN);
			if (exp.length != 2) {
				continue;
			}
			parseParameter(exp[0], exp[1]);
		}
	}

	protected void parseParameter(String name, String value) {
		switch (name) {
			default:
			case MIN:
				min = mapMinParameter(value);
				break;
			case MAG:
				mag = mapMagParameter(value);
				break;
			case WRAP_S:
				wrapS = mapWrapParameter(value);
				break;
			case WRAP_T:
				wrapT = mapWrapParameter(value);
				break;
		}
	}

	private static int mapMinParameter(String shortcut) {
		switch (shortcut) {
			case "n":
				return GLES20.GL_NEAREST;
			case "l":
				return GLES20.GL_LINEAR;
			case "nn":
				return GLES20.GL_NEAREST_MIPMAP_NEAREST;
			case "ln":
				return GLES20.GL_LINEAR_MIPMAP_NEAREST;
			case "ll":
				return GLES20.GL_LINEAR_MIPMAP_LINEAR;
			default:
				return GLES20.GL_NEAREST_MIPMAP_LINEAR;
		}
	}

	private static int mapMagParameter(String shortcut) {
		if (shortcut.equals("n")) {
			return GLES20.GL_NEAREST;
		} else {
			return GLES20.GL_LINEAR;
		}
	}

	private static int mapWrapParameter(String shortcut) {
		switch (shortcut) {
			case "c":
				return GLES20.GL_CLAMP_TO_EDGE;
			case "m":
				return GLES20.GL_MIRRORED_REPEAT;
			default:
				return GLES20.GL_REPEAT;
		}
	}
}
