package de.markusfisch.android.shadereditor.opengl;

import java.util.HashMap;
import java.util.Map;

final class GlProgram {
	private int id;
	private final Map<String, Integer> uniformLocations = new HashMap<>();
	private final Map<String, Integer> attribLocations = new HashMap<>();

	GlProgram(int id) {
		this.id = id;
	}

	int getId() {
		return id;
	}

	boolean isValid() {
		return id != 0;
	}

	Map<String, Integer> getUniformLocations() {
		return uniformLocations;
	}

	Map<String, Integer> getAttribLocations() {
		return attribLocations;
	}

	void invalidate() {
		id = 0;
		uniformLocations.clear();
		attribLocations.clear();
	}
}
