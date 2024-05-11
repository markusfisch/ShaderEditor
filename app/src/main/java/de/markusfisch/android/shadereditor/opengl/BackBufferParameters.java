package de.markusfisch.android.shadereditor.opengl;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

public class BackBufferParameters extends TextureParameters {
	private static final String PRESET = "p";

	private String preset;

	public BackBufferParameters() {
		super(GLES20.GL_NEAREST,
				GLES20.GL_NEAREST,
				GLES20.GL_CLAMP_TO_EDGE,
				GLES20.GL_CLAMP_TO_EDGE);
	}

	public void setPreset(String preset) {
		this.preset = preset;
	}

	@NonNull
	@Override
	public String toString() {
		String params = super.toString();
		if (preset != null) {
			if (params.isEmpty()) {
				params = HEADER;
			}
			params += PRESET + ASSIGN + preset + SEPARATOR;
		}
		return params;
	}

	void reset() {
		set(GLES20.GL_NEAREST,
				GLES20.GL_NEAREST,
				GLES20.GL_CLAMP_TO_EDGE,
				GLES20.GL_CLAMP_TO_EDGE);
		preset = null;
	}

	Bitmap getPresetBitmap(int width, int height) {
		if (preset == null) {
			return null;
		}

		Bitmap tile = ShaderEditorApp.db.getTextureBitmap(preset);
		if (tile == null) {
			return null;
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

		return background;
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
