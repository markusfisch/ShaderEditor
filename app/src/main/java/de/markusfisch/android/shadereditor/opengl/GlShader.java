package de.markusfisch.android.shadereditor.opengl;

final class GlShader {
	private int id;
	private final int type;
	private final String source;

	GlShader(int id, int type, String source) {
		this.id = id;
		this.type = type;
		this.source = source;
	}

	int getId() {
		return id;
	}

	int getType() {
		return type;
	}

	String getSource() {
		return source;
	}

	boolean isValid() {
		return id != 0;
	}

	void invalidate() {
		id = 0;
	}
}
