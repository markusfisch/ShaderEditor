package de.markusfisch.android.shadereditor.editor;

import java.text.BreakIterator;

import de.markusfisch.android.shadereditor.editor.Document.Selection;

// Where the caret goes. A character is a cluster as Boundaries finds it; the
// caret never stops inside one. A word is a run of letters, digits and
// underscores, or of other characters that are not white space.
public final class Motion {
	// BreakIterator by default; which clusters it knows depends on the
	// platform's Unicode version.
	public interface Boundaries {
		int after(String line, int offset);

		int before(String line, int offset);
	}

	private static final int SPACE = 0;
	private static final int WORD = 1;
	private static final int OTHER = 2;

	private final Boundaries boundaries;

	public Motion() {
		this(new Clusters());
	}

	public Motion(Boundaries boundaries) {
		this.boundaries = boundaries;
	}

	public int nextChar(Text text, int offset) {
		int line = text.lineOf(offset);
		int start = text.lineStart(line);
		String s = text.line(line);
		int i = offset - start;
		if (i < s.length()) {
			return start + boundaries.after(s, i);
		}
		return Math.min(offset + 1, text.length());
	}

	public int previousChar(Text text, int offset) {
		int line = text.lineOf(offset);
		int start = text.lineStart(line);
		int i = offset - start;
		if (i > 0) {
			return start + boundaries.before(text.line(line), i);
		}
		return Math.max(offset - 1, 0);
	}

	// White space is skipped, then one word. At the end of a line, the start of
	// the next.
	public int nextWord(Text text, int offset) {
		int line = text.lineOf(offset);
		int start = text.lineStart(line);
		String s = text.line(line);
		int length = s.length();
		int i = offset - start;
		if (i == length) {
			return Math.min(offset + 1, text.length());
		}
		while (i < length && kindOf(s, i) == SPACE) {
			i = boundaries.after(s, i);
		}
		if (i < length) {
			int kind = kindOf(s, i);
			do {
				i = boundaries.after(s, i);
			} while (i < length && kindOf(s, i) == kind);
		}
		return start + i;
	}

	// White space is skipped, then one word. At the start of a line, the end of
	// the previous.
	public int previousWord(Text text, int offset) {
		int line = text.lineOf(offset);
		int start = text.lineStart(line);
		String s = text.line(line);
		int i = offset - start;
		if (i == 0) {
			return Math.max(offset - 1, 0);
		}
		int kind = SPACE;
		for (int p; i > 0; i = p) {
			p = boundaries.before(s, i);
			int k = kindOf(s, p);
			if (kind == SPACE) {
				kind = k;
			} else if (k != kind) {
				break;
			}
		}
		return start + i;
	}

	public int lineStart(Text text, int offset) {
		return text.lineStart(text.lineOf(offset));
	}

	public int lineEnd(Text text, int offset) {
		int line = text.lineOf(offset);
		return text.lineStart(line) + text.line(line).length();
	}

	// Or the run of white space if there is no word; a word that ends at offset
	// wins over what follows.
	public Selection wordAt(Text text, int offset) {
		int line = text.lineOf(offset);
		int start = text.lineStart(line);
		String s = text.line(line);
		int length = s.length();
		if (length == 0) {
			return new Selection(offset, offset);
		}
		int i = offset - start;
		// The start of the character at i, or before i at the end.
		int at = i > 0 ? boundaries.before(s, i) : 0;
		if (i < length && boundaries.after(s, at) == i &&
				(kindOf(s, i) == WORD || kindOf(s, at) != WORD)) {
			at = i;
		}
		int kind = kindOf(s, at);
		int first = at;
		for (int p; first > 0 && kindOf(s,
				p = boundaries.before(s, first)) == kind; ) {
			first = p;
		}
		int last = boundaries.after(s, at);
		while (last < length && kindOf(s, last) == kind) {
			last = boundaries.after(s, last);
		}
		return new Selection(start + first, start + last);
	}

	public int column(Text text, int offset, int tabWidth) {
		int line = text.lineOf(offset);
		String s = text.line(line);
		int to = offset - text.lineStart(line);
		int column = 0;
		for (int i = 0; i < to; i = boundaries.after(s, i)) {
			column = advance(s, i, column, tabWidth);
		}
		return column;
	}

	// The earlier of two as close; the end of the line if it is shorter.
	public int offsetAt(Text text, int line, int column,
			int tabWidth) {
		String s = text.line(line);
		int i = 0;
		for (int at = 0; i < s.length(); ) {
			int next = advance(s, i, at, tabWidth);
			if (next > column) {
				if (next - column < column - at) {
					i = boundaries.after(s, i);
				}
				break;
			}
			at = next;
			i = boundaries.after(s, i);
		}
		return text.lineStart(line) + i;
	}

	private static int advance(String s, int i, int column, int tabWidth) {
		if (s.charAt(i) != '\t') {
			return column + 1;
		}
		int width = Math.max(1, tabWidth);
		return (column / width + 1) * width;
	}

	private static int kindOf(String s, int i) {
		int cp = s.codePointAt(i);
		if (Character.isWhitespace(cp)) {
			return SPACE;
		}
		return Character.isLetterOrDigit(cp) || cp == '_' ? WORD : OTHER;
	}

	private static final class Clusters implements Boundaries {
		private final BreakIterator iterator =
				BreakIterator.getCharacterInstance();
		private String line;

		@Override
		public int after(String line, int offset) {
			return iterate(line).following(offset);
		}

		@Override
		public int before(String line, int offset) {
			return iterate(line).preceding(offset);
		}

		private BreakIterator iterate(String line) {
			if (line != this.line) {
				this.line = line;
				iterator.setText(line);
			}
			return iterator;
		}
	}
}
