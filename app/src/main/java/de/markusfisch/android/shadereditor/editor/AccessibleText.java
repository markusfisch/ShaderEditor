package de.markusfisch.android.shadereditor.editor;

// The window of a text accessibility services get, never all of a large text.
// A service reads the node's text whole on every focus, so the node holds the
// lines of the selection and one line on either side: TalkBack moves by lines
// over that text itself. A change, and a selection event, is told in a larger
// window around its start: TalkBack tells what a move passed over from the
// event's text between the index of the event before and the new one, so the
// window must stay while the selection moves. Movement by a unit works as
// TextView's iterators for accessibility.
public final class AccessibleText {
	public static final int BLOCK_LINES = 128;
	public static final int MAX_CHARS = 32_768;

	public enum Unit {
		CHARACTER, WORD, LINE, PARAGRAPH, PAGE
	}

	public record Segment(int start, int end) {
	}

	// Windows of the text before and after the change, from the same start.
	public record Edit(Window before, Window after, int from, int removed,
			int added) {
	}

	public record Window(int start, int end) {
		public int length() {
			return end - start;
		}

		// Clamped to the window.
		public int index(int offset) {
			return Math.max(0, Math.min(offset - start, end - start));
		}
	}

	private AccessibleText() {
	}

	public static Window window(Text text, int from, int to) {
		int lineCount = text.lineCount();
		int first = Math.max(0,
				(text.lineOf(from) / BLOCK_LINES - 1) * BLOCK_LINES);
		int last = Math.min(lineCount - 1,
				(text.lineOf(to) / BLOCK_LINES + 2) * BLOCK_LINES - 1);
		int start = text.lineStart(first);
		int end = text.lineStart(last) + text.line(last).length();
		if (end - start > MAX_CHARS) {
			int quarter = MAX_CHARS / 4;
			start = Math.max(start, (from / quarter - 1) * quarter);
			end = Math.min(end, start + MAX_CHARS);
			if (start > 0 && Character.isLowSurrogate(charAt(text, start))) {
				++start;
			}
			end = cut(text, end);
		}
		return new Window(start, end);
	}

	public static Window reading(Text text, int start, int end) {
		int first = Math.max(0, text.lineOf(start) - 1);
		int last = Math.min(text.lineCount() - 1, text.lineOf(end) + 1);
		int from = text.lineStart(first);
		int to = text.lineStart(last) + text.line(last).length();
		return to - from > MAX_CHARS
				? window(text, start, end)
				: new Window(from, to);
	}

	// Whether start to end, in the offsets of window, is all of a window that
	// is not all of the text. A service selects all of the text it was given
	// to select all, and that text is the window. A selection anchored at the
	// window's start is not: TalkBack's moves in its selection mode extend it
	// from its anchor to the same range, and at the text's start the window's
	// offsets are the text's.
	public static boolean all(Text text, Window window,
			Document.Selection selection, int start, int end) {
		return start == 0 && end == window.length() &&
				window.length() < text.length() && (selection.isEmpty() ||
				selection.anchor() != window.start());
	}

	// Whether start to end, in the offsets of a window that does not start
	// the text, runs from the anchor to another place in the window. TalkBack
	// extends a selection so, in the offsets of the text it was given; a
	// keyboard's range from there would start window.start() before the anchor.
	public static boolean extending(Window window, int anchor, int start,
			int end) {
		return window.start() > 0 && start != end &&
				start == anchor - window.start() && end >= 0 &&
				end <= window.length();
	}

	public static Edit edit(Text before, Text after, Document.Change change) {
		int from = change.start();
		Window window = window(before, from, from);
		int start = window.start();
		int limit = start + MAX_CHARS;
		int removedEnd = from + change.removed().length();
		int addedEnd = from + change.inserted().length();
		int beforeEnd = cut(before, Math.min(Math.max(window.end(), removedEnd),
				limit));
		int afterEnd = cut(after, Math.min(beforeEnd >= removedEnd
				? beforeEnd - removedEnd + addedEnd
				: addedEnd, limit));
		return new Edit(new Window(start, beforeEnd),
				new Window(start, afterEnd),
				from - start,
				Math.max(0, Math.min(removedEnd, beforeEnd) - from),
				Math.max(0, Math.min(addedEnd, afterEnd) - from));
	}

	// Null at the end; a page is pageLines lines.
	public static Segment following(Motion motion, Text text, Unit unit,
			int offset, int pageLines) {
		int length = text.length();
		if (offset >= length) {
			return null;
		}
		int n = text.lineOf(offset);
		switch (unit) {
			case CHARACTER:
				return new Segment(offset, motion.nextChar(text, offset));
			case WORD:
				int end = offset;
				int start;
				// One step may still cross a line of one long word or of
				// white space: then the line bounds it, not MAX_CHARS.
				do {
					start = end;
					if (start >= length || start - offset > MAX_CHARS) {
						return null;
					}
					end = motion.nextWord(text, start);
				} while (blank(text, start, end));
				return new Segment(Math.max(offset,
						motion.previousWord(text, end)), end);
			case LINE:
				if (motion.lineStart(text, offset) != offset) {
					++n;
				}
				return n < text.lineCount() ? line(text, n) : null;
			case PARAGRAPH:
				int lineEnd = motion.lineEnd(text, offset);
				if (offset < lineEnd) {
					return new Segment(offset, lineEnd);
				}
				for (int last = n + MAX_CHARS; ++n < text.lineCount() &&
						n < last; ) {
					if (!text.line(n).isEmpty()) {
						return paragraph(text, n);
					}
				}
				return null;
			case PAGE:
				int next = n + Math.max(1, pageLines);
				return new Segment(offset, next < text.lineCount()
						? text.lineStart(next)
						: length);
		}
		return null;
	}

	// Null at the start; a page is pageLines lines.
	public static Segment preceding(Motion motion, Text text, Unit unit,
			int offset, int pageLines) {
		if (offset <= 0) {
			return null;
		}
		int n = text.lineOf(offset);
		int lineStart = text.lineStart(n);
		switch (unit) {
			case CHARACTER:
				return new Segment(motion.previousChar(text, offset), offset);
			case WORD:
				int start = offset;
				int end;
				do {
					end = start;
					if (end <= 0 || offset - end > MAX_CHARS) {
						return null;
					}
					start = motion.previousWord(text, end);
				} while (blank(text, start, end));
				return new Segment(start, Math.min(end,
						motion.nextWord(text, start)));
			case LINE:
				if (offset < text.length() || lineStart == offset) {
					--n;
				}
				return n >= 0 ? line(text, n) : null;
			case PARAGRAPH:
				if (offset > lineStart) {
					return new Segment(lineStart, offset);
				}
				for (int last = n - MAX_CHARS; --n >= 0 && n > last; ) {
					if (!text.line(n).isEmpty()) {
						return paragraph(text, n);
					}
				}
				return null;
			case PAGE:
				int first = Math.max(0, n - Math.max(1, pageLines));
				if (offset == text.length() && first < n) {
					++first;
				}
				return new Segment(text.lineStart(first), offset);
		}
		return null;
	}

	// Line n with its line break.
	private static Segment line(Text text, int n) {
		return new Segment(text.lineStart(n), n + 1 < text.lineCount()
				? text.lineStart(n + 1)
				: text.length());
	}

	// Line n without its line break.
	private static Segment paragraph(Text text, int n) {
		int start = text.lineStart(n);
		return new Segment(start, start + text.line(n).length());
	}

	private static boolean blank(Text text, int start, int end) {
		String s = text.substring(start, end);
		for (int i = 0; i < s.length(); ++i) {
			if (!Character.isWhitespace(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	// Returns end, or the offset before it if it splits a surrogate pair.
	public static int cut(Text text, int end) {
		return end > 0 && end < text.length() &&
				Character.isLowSurrogate(charAt(text, end)) ? end - 1 : end;
	}

	private static char charAt(Text text, int offset) {
		return text.substring(offset, offset + 1).charAt(0);
	}
}
