package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;

record DiscoveredSampler(
		@NonNull String name,
		int target,
		@NonNull TextureParameters parameters) {
}
