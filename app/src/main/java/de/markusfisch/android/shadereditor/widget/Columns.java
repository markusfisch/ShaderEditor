package de.markusfisch.android.shadereditor.widget;

import java.util.Arrays;

import de.markusfisch.android.shadereditor.editor.LineStyles;

// A line in a font whose characters all have one width: a character takes one
// column, a tab reaches the next multiple of the tab width.
final class Columns {
	private Columns() {
	}

	static boolean isSimple(String line) {
		for (int i = 0, n = line.length(); i < n; ++i) {
			char c = line.charAt(i);
			if ((c < 0x20 && c != '\t') || c > 0x7e) {
				return false;
			}
		}
		return true;
	}

	static int columnOf(String line, int offset, int tabWidth) {
		int column = 0;
		for (int i = 0, n = Math.min(offset, line.length()); i < n; ++i) {
			column = next(line.charAt(i), column, tabWidth);
		}
		return column;
	}

	static int next(char c, int column, int tabWidth) {
		if (c != '\t') {
			return column + 1;
		}
		int width = Math.max(1, tabWidth);
		return (column / width + 1) * width;
	}

	// column may be fractional: the start of the character it falls in, or its
	// end if column is in its right half.
	static int offsetAt(String line, float column, int tabWidth) {
		int at = 0;
		for (int i = 0, n = line.length(); i < n; ++i) {
			int next = next(line.charAt(i), at, tabWidth);
			if (column < (at + next) * .5f) {
				return i;
			}
			at = next;
		}
		return line.length();
	}

	// What one drawText each draws: start, end, column and colour, four ints a
	// piece. A piece holds no tab and is not only spaces; pieces of one colour
	// that only spaces separate are one piece.
	static int[] pieces(String line, LineStyles.Runs runs, int[] colors,
			int plain, int tabWidth) {
		Pieces pieces = new Pieces(line);
		int count = runs == null ? 0 : runs.size();
		int column = 0;
		for (int i = 0, r = 0, length = line.length(); i < length; ) {
			int end;
			int color;
			if (r < count && runs.start(r) <= i) {
				end = Math.min(length, runs.end(r));
				color = colors[runs.style(r).ordinal()];
				++r;
			} else {
				end = r < count ? Math.min(length, runs.start(r)) : length;
				color = plain;
			}
			int start = i;
			int startColumn = column;
			for (; i < end; ++i) {
				char c = line.charAt(i);
				if (c == '\t') {
					pieces.add(start, i, startColumn, color);
					pieces.tabbed = true;
					start = i + 1;
					startColumn = next(c, column, tabWidth);
				}
				column = next(c, column, tabWidth);
			}
			pieces.add(start, end, startColumn, color);
		}
		return Arrays.copyOf(pieces.out, pieces.size);
	}

	private static final class Pieces {
		private final String line;
		private int[] out = new int[16];
		private int size;
		// Whether a tab came after the last piece, so it cannot grow.
		private boolean tabbed = true;

		private Pieces(String line) {
			this.line = line;
		}

		private void add(int start, int end, int column, int color) {
			int i = start;
			while (i < end && line.charAt(i) == ' ') {
				++i;
			}
			if (i >= end) {
				return;
			}
			column += i - start;
			start = i;
			if (!tabbed && out[size - 1] == color) {
				out[size - 3] = end;
				return;
			}
			if (size + 4 > out.length) {
				out = Arrays.copyOf(out, out.length * 2);
			}
			out[size++] = start;
			out[size++] = end;
			out[size++] = column;
			out[size++] = color;
			tabbed = false;
		}
	}
}
