package de.markusfisch.android.shadereditor.opengl;

interface GlTexture {
	int getId();

	int getTarget();

	int getWidth();

	int getHeight();

	boolean isValid();
}
