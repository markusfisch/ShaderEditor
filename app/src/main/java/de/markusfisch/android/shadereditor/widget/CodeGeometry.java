package de.markusfisch.android.shadereditor.widget;

// The arithmetic of the gutter of a CodeView; where lines are is
// LineGeometry's.
final class CodeGeometry {
	static final int GUTTER_BLOCK = 32;

	private CodeGeometry() {
	}

	static int digits(int number) {
		int digits = 1;
		while (number >= 10) {
			number /= 10;
			++digits;
		}
		return digits;
	}

	// Line numbers are recorded together in blocks of GUTTER_BLOCK lines.
	static int numbersFrom(int line) {
		return line - line % GUTTER_BLOCK;
	}

	static int numbersTo(int line, int lineCount) {
		return Math.min(lineCount - 1, numbersFrom(line) + GUTTER_BLOCK - 1);
	}

	static float gutterWidth(int lineCount, float digitWidth,
			float spacing) {
		return digits(lineCount) * digitWidth + spacing;
	}
}
