package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;

final class PreparedShaderInput {
	@NonNull
	private final String source;
	@NonNull
	private final ShaderLineMapping lineMapping;

	PreparedShaderInput(
			@NonNull String source,
			@NonNull ShaderLineMapping lineMapping) {
		this.source = source;
		this.lineMapping = lineMapping;
	}

	@NonNull
	String getSource() {
		return source;
	}

	@NonNull
	ShaderLineMapping getLineMapping() {
		return lineMapping;
	}
}
