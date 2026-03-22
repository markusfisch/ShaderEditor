package de.markusfisch.android.shadereditor.opengl;

import android.opengl.GLES20;

final class GlTexture2D implements GlTexture {
	private int id;
	private int width;
	private int height;

	GlTexture2D(int id) {
		this.id = id;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public int getTarget() {
		return GLES20.GL_TEXTURE_2D;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public boolean isValid() {
		return id != 0;
	}

	void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public void invalidate() {
		id = 0;
		width = 0;
		height = 0;
	}
}
