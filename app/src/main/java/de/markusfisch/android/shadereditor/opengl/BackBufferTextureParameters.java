package de.markusfisch.android.shadereditor.opengl;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.opengl.GLES20;

public class BackBufferTextureParameters extends TextureParameters {
	private static final String PRESET = "p";

	private String preset;

	public static String create(
			String min,
			String mag,
			String wrapS,
			String wrapT,
			String preset) {
		return create(min, mag, wrapS, wrapT) +
				PRESET + ASSIGN + preset + SEPARATOR;
	}

	BackBufferTextureParameters() {
		super(GLES20.GL_NEAREST,
				GLES20.GL_NEAREST,
				GLES20.GL_CLAMP_TO_EDGE,
				GLES20.GL_CLAMP_TO_EDGE);
	}

	void reset() {
		set(GLES20.GL_NEAREST,
				GLES20.GL_NEAREST,
				GLES20.GL_CLAMP_TO_EDGE,
				GLES20.GL_CLAMP_TO_EDGE);
		preset = null;
	}

	boolean setPreset(int width, int height) {
		if (preset == null) {
			return false;
		}

		Bitmap tile = ShaderEditorApp.db.getTextureBitmap(preset);
		if (tile == null) {
			return false;
		}

		Bitmap background = Bitmap.createBitmap(
				width,
				height,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(background);

		int tw = tile.getWidth();
		int th = tile.getHeight();
		int scaledHeight = Math.round((float) width / tw * th);
		tile = Bitmap.createScaledBitmap(
				tile,
				width,
				scaledHeight,
				true);

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setShader(new BitmapShader(tile,
				Shader.TileMode.CLAMP,
				Shader.TileMode.MIRROR));
		canvas.drawPaint(paint);

		setBitmap(background);

		return true;
	}

	@Override
	protected void parseParameter(String name, String value) {
		if (PRESET.equals(name)) {
			preset = value;
		} else {
			super.parseParameter(name, value);
		}
	}
}
