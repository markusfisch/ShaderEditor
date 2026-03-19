package de.markusfisch.android.shadereditor.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.graphics.BitmapEditor;

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

		int tw = tile.getWidth();
		int th = tile.getHeight();
		int scaledHeight = Math.round((float) width / tw * th);

		try {
			Bitmap scaledTile = BitmapEditor.createScaledBitmap(
					tile,
					width,
					scaledHeight);
			if (scaledTile != tile) {
				tile.recycle();
			}
			tile = scaledTile;
		} catch (IllegalArgumentException e) {
			tile.recycle();
			return null;
		}

		Bitmap background = Bitmap.createBitmap(
				width,
				height,
				Bitmap.Config.ARGB_8888);
		background.setPremultiplied(false);

		int[] tilePixels = new int[width * scaledHeight];
		tile.getPixels(tilePixels, 0, width, 0, 0, width, scaledHeight);
		int[] backgroundPixels = new int[width * height];
		for (int y = 0; y < height; ++y) {
			int mirroredY = y % (scaledHeight * 2);
			if (mirroredY >= scaledHeight) {
				mirroredY = (scaledHeight * 2) - mirroredY - 1;
			}
			System.arraycopy(
					tilePixels,
					mirroredY * width,
					backgroundPixels,
					y * width,
					width);
		}
		background.setPixels(backgroundPixels, 0, width, 0, 0, width, height);
		tile.recycle();

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
