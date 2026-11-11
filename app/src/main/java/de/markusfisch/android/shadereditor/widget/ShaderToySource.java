package de.markusfisch.android.shadereditor.widget;

import java.util.regex.Pattern;

final class ShaderToySource {
	// Used with find(), so no leading or trailing ".*": that would make
	// the search cost the square of the line length.
	private static final Pattern PATTERN_SHADER_TOY = Pattern.compile(
			"void\\s+mainImage\\s*\\(");
	private static final Pattern PATTERN_MAIN = Pattern.compile(
			"void\\s+main\\s*\\(");

	private ShaderToySource() {
	}

	/**
	 * @return True if {@code src} defines mainImage() but no main().
	 */
	static boolean needsConversion(String src) {
		return PATTERN_SHADER_TOY.matcher(src).find() &&
				!PATTERN_MAIN.matcher(src).find();
	}
}
