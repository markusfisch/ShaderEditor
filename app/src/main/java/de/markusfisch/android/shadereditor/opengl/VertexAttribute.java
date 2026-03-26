package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;

final class VertexAttribute {
	@NonNull
	private final String name;
	private final int componentCount;
	private final int type;
	private final boolean normalized;
	private final int stride;
	private final int byteOffset;

	VertexAttribute(@NonNull String name,
			int componentCount,
			int type,
			boolean normalized,
			int stride,
			int byteOffset) {
		this.name = name;
		this.componentCount = componentCount;
		this.type = type;
		this.normalized = normalized;
		this.stride = stride;
		this.byteOffset = byteOffset;
	}

	@NonNull
	String getName() {
		return name;
	}

	int getComponentCount() {
		return componentCount;
	}

	int getType() {
		return type;
	}

	boolean isNormalized() {
		return normalized;
	}

	int getStride() {
		return stride;
	}

	int getByteOffset() {
		return byteOffset;
	}
}
