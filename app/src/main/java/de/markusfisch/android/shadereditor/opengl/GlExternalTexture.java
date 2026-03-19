package de.markusfisch.android.shadereditor.opengl;

import android.opengl.GLES11Ext;

final class GlExternalTexture implements GlTexture {
	private int id;

	GlExternalTexture(int id) {
		this.id = id;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public int getTarget() {
		return GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
	}

	@Override
	public int getWidth() {
		return 0;
	}

	@Override
	public int getHeight() {
		return 0;
	}

	@Override
	public boolean isValid() {
		return id != 0;
	}

	void invalidate() {
		id = 0;
	}
}
