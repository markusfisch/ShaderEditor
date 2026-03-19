package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;

record DiscoveredSampler(
		@NonNull String name,
		int target,
		@NonNull TextureParameters parameters) {
	boolean isCameraTexture() {
		return ShaderRenderer.UNIFORM_CAMERA_BACK.equals(name) ||
				ShaderRenderer.UNIFORM_CAMERA_FRONT.equals(name);
	}
}
