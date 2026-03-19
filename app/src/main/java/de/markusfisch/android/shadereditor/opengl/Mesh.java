package de.markusfisch.android.shadereditor.opengl;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

final class Mesh {
	@NonNull
	private final Geometry geometry;

	private Mesh(@NonNull Geometry geometry) {
		this.geometry = geometry;
	}

	static Mesh fullScreenQuad() {
		ByteBuffer buffer = ByteBuffer.allocateDirect(8);
		buffer.put(new byte[]{
				-1, 1,
				-1, -1,
				1, 1,
				1, -1
		}).position(0);
		return new Mesh(new Geometry(
				buffer,
				GLES20.GL_TRIANGLE_STRIP,
				4,
				new VertexAttribute("position", 2, GLES20.GL_BYTE, false, 0, 0)));
	}

	static Mesh texturedQuad(@NonNull FloatBuffer vertexBuffer) {
		return new Mesh(new Geometry(
				vertexBuffer,
				GLES20.GL_TRIANGLE_STRIP,
				4,
				new VertexAttribute("position", 2, GLES20.GL_FLOAT, false, 16, 0),
				new VertexAttribute("texCoord", 2, GLES20.GL_FLOAT, false, 16, 8)));
	}

	@NonNull
	Geometry getGeometry() {
		return geometry;
	}
}
