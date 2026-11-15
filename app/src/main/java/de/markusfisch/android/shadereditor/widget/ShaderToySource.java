package de.markusfisch.android.shadereditor.widget;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

final class ShaderToySource {
	// Used with find(), so no leading or trailing ".*": that would make
	// the search cost the square of the line length.
	private static final Pattern PATTERN_SHADER_TOY = Pattern.compile(
			"void\\s+mainImage\\s*\\(");
	private static final Pattern PATTERN_MAIN = Pattern.compile(
			"void\\s+main\\s*\\(");
	// ShaderToy input, its uniform here and the declaration of that.
	private static final String[][] INPUTS = {
			{"iResolution", "resolution", "uniform vec2 resolution;"},
			{"iGlobalTime", "time", "uniform float time;"},
			{"iTime", "time", "uniform float time;"},
			{"iFrame", "frame", "uniform int frame;"},
			{"iMouse", "mouse", "uniform vec4 mouse;"},
			{"iDate", "date", "uniform vec4 date;"},
	};

	private ShaderToySource() {
	}

	/**
	 * @return Declarations of the uniforms for the inputs {@code src} uses.
	 */
	static String declarations(String src) {
		Set<String> declarations = new LinkedHashSet<>();
		for (String[] input : INPUTS) {
			if (Pattern.compile("\\b" + input[0] + "\\b").matcher(src).find()) {
				declarations.add(input[2]);
			}
		}
		StringBuilder sb = new StringBuilder();
		for (String declaration : declarations) {
			sb.append(declaration).append('\n');
		}
		return sb.length() > 0 ? sb.append('\n').toString() : "";
	}

	static String rename(String src) {
		for (String[] input : INPUTS) {
			src = src.replaceAll("\\b" + input[0] + "\\b", input[1]);
		}
		return src;
	}

	/**
	 * @return True if {@code src} defines mainImage() but no main().
	 */
	static boolean needsConversion(String src) {
		return PATTERN_SHADER_TOY.matcher(src).find() &&
				!PATTERN_MAIN.matcher(src).find();
	}
}
