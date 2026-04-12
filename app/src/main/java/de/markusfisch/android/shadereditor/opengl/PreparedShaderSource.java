package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

final class PreparedShaderSource {
	@Nullable
	private final PreparedShaderInput fragmentShader;
	private final float fTimeMax;
	@Nullable
	private final String gles3VersionDirective;
	@NonNull
	private final BackBufferParameters backBufferParameters;
	@NonNull
	private final List<DiscoveredSampler> samplers;

	PreparedShaderSource(
			@Nullable PreparedShaderInput fragmentShader,
			float fTimeMax,
			@Nullable String gles3VersionDirective,
			@NonNull BackBufferParameters backBufferParameters,
			@NonNull List<DiscoveredSampler> samplers) {
		this.fragmentShader = fragmentShader;
		this.fTimeMax = fTimeMax;
		this.gles3VersionDirective = gles3VersionDirective;
		this.backBufferParameters = backBufferParameters;
		this.samplers = List.copyOf(samplers);
	}

	@NonNull
	static PreparedShaderSource empty() {
		return new PreparedShaderSource(
				null,
				3f,
				null,
				new BackBufferParameters(),
				List.of());
	}

	@Nullable
	PreparedShaderInput getFragmentShader() {
		return fragmentShader;
	}

	float getFTimeMax() {
		return fTimeMax;
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
