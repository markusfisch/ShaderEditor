package de.markusfisch.android.shadereditor.editor;

import de.markusfisch.android.shadereditor.syntax.Indentation;

// The indentation of a new line, as the editor has always made it: the white
// space that starts the line, the line comment inside a commented line, and one
// more level after a character that opens one, for C '{', an operator or a
// parenthesis. Only the lines after the first copy their indentation.
public final class AutoIndent {
	private AutoIndent() {
	}

	public static String indentAfterBreak(Text text, int from, int to,
			String level, Indentation rules) {
		int line = text.lineOf(from);
		int start = text.lineStart(line);
		String s = text.line(line);
		int caret = from - start;
		StringBuilder indent = new StringBuilder();
		if (line > 0) {
			indent.append(s, 0, copiedEnd(s, caret, to - start,
					rules.lineComment()));
		}
		if (opens(s, caret, rules)) {
			indent.append(level);
		}
		return indent.toString();
	}

	// Returns the end of the start of s that the new line repeats.
	private static int copiedEnd(String s, int caret, int limit,
			String comment) {
		int end = Math.min(limit, s.length());
		int i = 0;
		for (; i < end; ++i) {
			char c = s.charAt(i);
			// Only the first character is compared, so any line that
			// starts with it continues with as many as the comment has.
			if (comment != null && caret < s.length() &&
					c == comment.charAt(0) && i + 1 < limit) {
				return Math.min(i + comment.length(), s.length());
			}
			if (c != ' ' && c != '\t') {
				break;
			}
		}
		return i;
	}

	private static boolean opens(String s, int caret, Indentation rules) {
		int open = 0;
		boolean last = true;
		for (int i = caret; i-- > 0; ) {
			char c = s.charAt(i);
			if (c == ' ' || c == '\t') {
				continue;
			}
			if (last && rules.levelAfter().indexOf(c) > -1) {
				++open;
			}
			last = false;
			if (rules.opening().indexOf(c) > -1) {
				++open;
			} else if (rules.closing().indexOf(c) > -1) {
				--open;
			}
		}
		return open > 0;
	}
}
