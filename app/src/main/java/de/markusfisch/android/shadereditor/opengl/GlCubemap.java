package de.markusfisch.android.shadereditor.opengl;

import android.opengl.GLES20;

final class GlCubemap implements GlTexture {
	private int id;
	private int sideLength;

	GlCubemap(int id) {
		this.id = id;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public int getTarget() {
		return GLES20.GL_TEXTURE_CUBE_MAP;
	}

	@Override
	public int getWidth() {
		return sideLength;
	}

	@Override
	public int getHeight() {
		return sideLength;
	}

	@Override
	public boolean isValid() {
		return id != 0;
	}

	void setSideLength(int sideLength) {
		this.sideLength = sideLength;
	}

	@Override
	public void invalidate() {
		id = 0;
		sideLength = 0;
	}
}
