package de.markusfisch.android.shadereditor.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.markusfisch.android.shadereditor.database.Database;

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

	@Nullable
	Bitmap getPresetBitmap(@NonNull Context context, int width, int height) {
		if (preset == null) {
			return null;
		}

		// Use the modern singleton to get the DataSource and fetch the bitmap.
		Bitmap tile = Database
				.getInstance(context)
				.getDataSource()
				.texture
				.getTextureBitmap(preset);

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

		// Use a try-catch block for bitmap operations that can fail.
		try {
			tile = Bitmap.createScaledBitmap(
					tile,
					width,
					scaledHeight,
					true);
		} catch (IllegalArgumentException e) {
			Log.e("BackBufferParameters", "Failed to scale bitmap", e);
			background.recycle();
			return null;
		}

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