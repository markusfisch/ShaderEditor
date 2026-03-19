package de.markusfisch.android.shadereditor.opengl;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.markusfisch.android.shadereditor.fragment.AbstractSamplerPropertiesFragment;

final class ShaderSourcePreparer {
	private static final String SAMPLER_2D = "2D";
	private static final String SAMPLER_CUBE = "Cube";
	private static final String SAMPLER_EXTERNAL_OES = "ExternalOES";
	private static final Pattern PATTERN_SAMPLER = Pattern.compile(
			String.format(
					"uniform[ \t]+sampler(" +
							SAMPLER_2D + "|" +
							SAMPLER_CUBE + "|" +
							SAMPLER_EXTERNAL_OES +
							")+[ \t]+(%s);[ \t]*(.*)",
					AbstractSamplerPropertiesFragment.TEXTURE_NAME_PATTERN));
	private static final Pattern PATTERN_FTIME = Pattern.compile(
			"^#define[ \\t]+FTIME_PERIOD[ \\t]+([0-9.]+)[ \\t]*$",
			Pattern.MULTILINE);
	private static final Pattern PATTERN_GLES3_VERSION = Pattern.compile(
			"^#version 3[0-9]{2} es$",
			Pattern.MULTILINE);
	private static final String OES_EXTERNAL =
			"#extension GL_OES_EGL_image_external : require\n";
	private static final String OES_EXTERNAL_ESS3 =
			"#extension GL_OES_EGL_image_external_essl3 : require\n";
	private static final String SHADER_EDITOR =
			"#define SHADER_EDITOR 1\n";

	private ShaderSourcePreparer() {
	}

	@NonNull
	static PreparedShaderSource prepare(
			@Nullable String source,
			int version,
			int maxTextures) {
		float fTimeMax = parseFTime(source);
		if (source == null) {
			return new PreparedShaderSource(
					null,
					fTimeMax,
					0,
					null,
					new BackBufferParameters(),
					List.of());
		}

		String gles3Version = getGLES3Version(source, version);
		String preparedSource = source;
		int fragmentShaderExtraLines = 0;
		BackBufferParameters backBufferParameters = new BackBufferParameters();
		ArrayList<DiscoveredSampler> samplers = new ArrayList<>();

		for (Matcher matcher = PATTERN_SAMPLER.matcher(source);
				matcher.find() && samplers.size() < maxTextures; ) {
			String type = matcher.group(1);
			String name = matcher.group(2);
			String params = matcher.group(3);

			if (type == null || name == null) {
				continue;
			}

			if (ShaderRenderer.UNIFORM_BACKBUFFER.equals(name)) {
				backBufferParameters.parse(params);
				continue;
			}

			int target = switch (type) {
				case SAMPLER_2D -> GLES20.GL_TEXTURE_2D;
				case SAMPLER_CUBE -> GLES20.GL_TEXTURE_CUBE_MAP;
				case SAMPLER_EXTERNAL_OES -> {
					String pattern = gles3Version != null
							? OES_EXTERNAL_ESS3
							: OES_EXTERNAL;
					if (!preparedSource.contains(pattern)) {
						preparedSource = addPreprocessorDirective(
								preparedSource,
								pattern);
						++fragmentShaderExtraLines;
					}
					yield GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
				}
				default -> -1;
			};
			if (target < 0) {
				continue;
			}

			samplers.add(new DiscoveredSampler(
					name,
					target,
					new TextureParameters(params)));
		}

		if (!preparedSource.contains(SHADER_EDITOR)) {
			preparedSource = addPreprocessorDirective(
					preparedSource,
					SHADER_EDITOR);
			++fragmentShaderExtraLines;
		}

		return new PreparedShaderSource(
				preparedSource,
				fTimeMax,
				fragmentShaderExtraLines,
				gles3Version,
				backBufferParameters,
				samplers);
	}

	private static float parseFTime(@Nullable String source) {
		if (source != null) {
			Matcher matcher = PATTERN_FTIME.matcher(source);
			String period;
			if (matcher.find() &&
					matcher.groupCount() > 0 &&
					(period = matcher.group(1)) != null) {
				return Float.parseFloat(period);
			}
		}
		return 3f;
	}

	@Nullable
	private static String getGLES3Version(@NonNull String source, int version) {
		Matcher matcher = PATTERN_GLES3_VERSION.matcher(source);
		return version == 3 && matcher.find() ? matcher.group(0) : null;
	}

	@NonNull
	private static String addPreprocessorDirective(
			@NonNull String source,
			@NonNull String directive) {
		if (source.trim().startsWith("#version")) {
			int lineFeed = source.indexOf("\n");
			if (lineFeed < 0) {
				return source;
			}
			++lineFeed;
			return source.substring(0, lineFeed) +
					directive +
					source.substring(lineFeed);
		}
		return directive + source;
	}
}
