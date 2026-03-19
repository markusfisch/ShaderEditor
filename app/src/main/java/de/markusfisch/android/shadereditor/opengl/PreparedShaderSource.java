package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

final class PreparedShaderSource {
	@Nullable
	private final String fragmentShader;
	private final float fTimeMax;
	private final int fragmentShaderExtraLines;
	@Nullable
	private final String gles3VersionDirective;
	@NonNull
	private final BackBufferParameters backBufferParameters;
	@NonNull
	private final List<DiscoveredSampler> samplers;

	PreparedShaderSource(
			@Nullable String fragmentShader,
			float fTimeMax,
			int fragmentShaderExtraLines,
			@Nullable String gles3VersionDirective,
			@NonNull BackBufferParameters backBufferParameters,
			@NonNull List<DiscoveredSampler> samplers) {
		this.fragmentShader = fragmentShader;
		this.fTimeMax = fTimeMax;
		this.fragmentShaderExtraLines = fragmentShaderExtraLines;
		this.gles3VersionDirective = gles3VersionDirective;
		this.backBufferParameters = backBufferParameters;
		this.samplers = List.copyOf(samplers);
	}

	@NonNull
	static PreparedShaderSource empty() {
		return new PreparedShaderSource(
				null,
				3f,
				0,
				null,
				new BackBufferParameters(),
				List.of());
	}

	@Nullable
	String getFragmentShader() {
		return fragmentShader;
	}

	float getFTimeMax() {
		return fTimeMax;
	}

	int getFragmentShaderExtraLines() {
		return fragmentShaderExtraLines;
	}

	@NonNull
	BackBufferParameters getBackBufferParameters() {
		return backBufferParameters;
	}

	@NonNull
	List<DiscoveredSampler> getSamplers() {
		return samplers;
	}

	@NonNull
	String getVertexShader(
			@NonNull String vertexShader,
			@NonNull String vertexShader3,
			int version) {
		return version == 3 && gles3VersionDirective != null
				? gles3VersionDirective + "\n" + vertexShader3
				: vertexShader;
	}
}
