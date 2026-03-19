package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;

import java.nio.Buffer;

final class Geometry {
	@NonNull
	private final Buffer vertexBuffer;
	private final int drawMode;
	private final int vertexCount;
	@NonNull
	private final VertexAttribute[] attributes;

	Geometry(@NonNull Buffer vertexBuffer,
			int drawMode,
			int vertexCount,
			@NonNull VertexAttribute... attributes) {
		this.vertexBuffer = vertexBuffer;
		this.drawMode = drawMode;
		this.vertexCount = vertexCount;
		this.attributes = attributes;
	}

	@NonNull
	Buffer getVertexBuffer() {
		return vertexBuffer;
	}

	int getDrawMode() {
		return drawMode;
	}

	int getVertexCount() {
		return vertexCount;
	}

	@NonNull
	VertexAttribute[] getAttributes() {
		return attributes;
	}
}
