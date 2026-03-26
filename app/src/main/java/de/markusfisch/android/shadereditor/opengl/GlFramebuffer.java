package de.markusfisch.android.shadereditor.opengl;

final class GlFramebuffer {
	private int id;
	private int width;
	private int height;
	private GlTexture2D colorAttachment;

	GlFramebuffer(int id) {
		this.id = id;
	}

	int getId() {
		return id;
	}

	int getWidth() {
		return width;
	}

	int getHeight() {
		return height;
	}

	boolean isValid() {
		return id != 0;
	}

	GlTexture2D getColorAttachment() {
		return colorAttachment;
	}

	void setColorAttachment(GlTexture2D colorAttachment) {
		this.colorAttachment = colorAttachment;
		if (colorAttachment != null) {
			width = colorAttachment.getWidth();
			height = colorAttachment.getHeight();
		}
	}

	void invalidate() {
		id = 0;
		width = 0;
		height = 0;
		colorAttachment = null;
	}
}
