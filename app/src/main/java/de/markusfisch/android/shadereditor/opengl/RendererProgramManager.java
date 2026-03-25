package de.markusfisch.android.shadereditor.opengl;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

final class RendererProgramManager {
	record ReloadResult(
			@NonNull List<ShaderError> textureErrors,
			@NonNull List<ShaderError> programErrors,
			boolean succeeded) {
		@NonNull
		static ReloadResult success(@NonNull List<ShaderError> textureErrors) {
			return new ReloadResult(List.copyOf(textureErrors), List.of(), true);
		}

		@NonNull
		static ReloadResult failure(
				@NonNull List<ShaderError> textureErrors,
				@NonNull List<ShaderError> programErrors) {
			return new ReloadResult(
					List.copyOf(textureErrors),
					List.copyOf(programErrors),
					false);
		}
	}

	private static final String FULL_SCREEN_VERTEX_SHADER = """
			attribute vec2 position;
			void main() {
				gl_Position = vec4(position, 0., 1.);
			}
			""";
	private static final String FULL_SCREEN_VERTEX_SHADER_3 = """
			in vec2 position;
			void main() {
				gl_Position = vec4(position, 0., 1.);
			}
			""";
	private static final String SURFACE_FRAGMENT_SHADER = """
			#ifdef GL_FRAGMENT_PRECISION_HIGH
			precision highp float;
			#else
			precision mediump float;
			#endif

			uniform vec2 resolution;
			uniform sampler2D frame;
			void main(void) {
				gl_FragColor = texture2D(frame,gl_FragCoord.xy / resolution.xy).rgba;
			}
			""";

	private final int maxTextures;

	@Nullable
	private String sourceText;
	@NonNull
	private PreparedShaderSource preparedShaderSource = PreparedShaderSource.empty();
	@NonNull
	private ShaderTextureResources textureResources =
			ShaderTextureResources.empty();
	private int version = 2;
	@Nullable
	private GlProgram surfaceProgram;
	@Nullable
	private GlProgram mainProgram;
	@Nullable
	private ProgramBindings surfaceBindings;

	RendererProgramManager(int maxTextures) {
		this.maxTextures = maxTextures;
	}

	void setVersion(int version) {
		this.version = version;
		prepareShaderSource(sourceText);
	}

	void setFragmentShader(@Nullable String source) {
		sourceText = source;
		prepareShaderSource(source);
	}

	boolean hasPreparedShader() {
		var fragmentShader = preparedShaderSource.getFragmentShader();
		return fragmentShader != null && !fragmentShader.getSource().isEmpty();
	}

	@NonNull
	ReloadResult reload(@NonNull Context context, @NonNull GlDevice device) {
		clearPrograms();

		var textureErrors = textureResources.load(context, device);
		var surfaceResult = device.createProgram(
				FULL_SCREEN_VERTEX_SHADER,
				SURFACE_FRAGMENT_SHADER,
				ShaderLineMapping.identity());
		if (!surfaceResult.succeeded() || surfaceResult.getProgram() == null) {
			return ReloadResult.failure(textureErrors, surfaceResult.getInfoLog());
		}

		var fragmentShader = preparedShaderSource.getFragmentShader();
		if (fragmentShader == null) {
			device.deleteProgram(surfaceResult.getProgram());
			return ReloadResult.failure(textureErrors, List.of());
		}

		var mainResult = device.createProgram(
				preparedShaderSource.getVertexShader(
						FULL_SCREEN_VERTEX_SHADER,
						FULL_SCREEN_VERTEX_SHADER_3,
						version),
				fragmentShader.getSource(),
				fragmentShader.getLineMapping());
		if (!mainResult.succeeded() || mainResult.getProgram() == null) {
			device.deleteProgram(surfaceResult.getProgram());
			return ReloadResult.failure(textureErrors, mainResult.getInfoLog());
		}

		surfaceProgram = surfaceResult.getProgram();
		mainProgram = mainResult.getProgram();
		surfaceBindings = new ProgramBindings(surfaceProgram);
		return ReloadResult.success(textureErrors);
	}

	void discardContextResources() {
		clearPrograms();
		textureResources.discard();
	}

	@Nullable
	GlProgram getSurfaceProgram() {
		return surfaceProgram;
	}

	@Nullable
	GlProgram getMainProgram() {
		return mainProgram;
	}

	@Nullable
	ProgramBindings getSurfaceBindings() {
		return surfaceBindings;
	}

	@NonNull
	BackBufferParameters getBackBufferParameters() {
		return preparedShaderSource.getBackBufferParameters();
	}

	float getFTimeMax() {
		return preparedShaderSource.getFTimeMax();
	}

	@NonNull
	ShaderTextureResources getTextureResources() {
		return textureResources;
	}

	private void prepareShaderSource(@Nullable String source) {
		preparedShaderSource = ShaderSourcePreparer.prepare(
				source,
				version,
				maxTextures);
		textureResources = ShaderTextureResources.create(
				preparedShaderSource.getSamplers());
	}

	private void clearPrograms() {
		surfaceProgram = null;
		mainProgram = null;
		surfaceBindings = null;
	}
}
