package de.markusfisch.android.shadereditor.syntax;

import java.util.Arrays;

/**
 * Tokenizer for GLSL. It accepts any text and never throws. A token never
 * spans a line end, except a comment, and a name followed by another name
 * is only taken for a type name on the same line, so the tokens of a line
 * depend on the lines before it only through comments and directives.
 */
public final class GlslLexer {
	private static final String OPERATORS = "<>+-*/%&|^=!,~;:?(){}[].";
	// What the last token outside directives was, for the guess of type
	// names and field selections.
	private static final int OTHER = 0;
	private static final int DOT = 1;
	private static final int STRUCT = 2;
	private static final int NAME = 3;

	private final CharSequence text;
	private final int length;
	private int[] starts;
	private int[] ends;
	private int[] kinds;
	private int size;
	private boolean directive;
	private boolean lineStart = true;
	private boolean nameExpected;
	private int last = -1;
	private int lastKind = OTHER;

	private GlslLexer(CharSequence text) {
		this.text = text;
		length = text.length();
		int capacity = length / 3 + 16;
		starts = new int[capacity];
		ends = new int[capacity];
		kinds = new int[capacity];
	}

	public static Tokens lex(CharSequence text) {
		GlslLexer lexer = new GlslLexer(text);
		lexer.run();
		return new Tokens(lexer.starts, lexer.ends, lexer.kinds, lexer.size);
	}

	private void run() {
		int i = 0;
		while (i < length) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\r') {
				i = afterLineEnd(i);
				startLine(false);
			} else if (c == '\\' && isLineEnd(i + 1)) {
				i = afterLineEnd(i + 1);
				startLine(true);
			} else if (Character.isWhitespace(c)) {
				++i;
			} else if (c == '/' && charAt(i + 1) == '/') {
				i = add(i, lineCommentEnd(i + 2), Style.COMMENT);
			} else if (c == '/' && charAt(i + 1) == '*') {
				add(i, i + 2, Style.COMMENT);
				i = ends[size - 1] = blockCommentEnd(i + 2);
			} else {
				i = token(i, c);
			}
		}
	}

	private void startLine(boolean continued) {
		if (!continued) {
			directive = false;
		}
		lineStart = true;
		if (lastKind == NAME) {
			lastKind = OTHER;
		}
	}

	private int token(int i, char c) {
		boolean first = lineStart;
		boolean name = nameExpected;
		lineStart = false;
		nameExpected = false;
		if (c == '#' && first && !directive) {
			directive = true;
			nameExpected = true;
			return add(i, i + 1, Style.DIRECTIVE);
		} else if (Character.isDigit(c) ||
				(c == '.' && Character.isDigit(charAt(i + 1)))) {
			return number(i);
		} else if (Character.isLetter(c) || c == '_') {
			return word(i, name);
		} else if (OPERATORS.indexOf(c) > -1) {
			return code(i, i + 1, Style.OPERATOR, c == '.' ? DOT : OTHER);
		}
		int end = i + 1;
		if (Character.isHighSurrogate(c) &&
				Character.isLowSurrogate(charAt(end))) {
			++end;
		}
		return code(i, end, Style.INVALID, OTHER);
	}

	private int word(int start, boolean name) {
		int end = start + 1;
		for (char c; end < length && ((c = text.charAt(end)) == '_' ||
				Character.isLetterOrDigit(c)); ) {
			++end;
		}
		Style style = GlslWords.style(text, start, end);
		if (directive) {
			if (name && GlslWords.isDirective(text, start, end)) {
				style = Style.DIRECTIVE;
			}
			return add(start, end, style);
		}
		int kind = OTHER;
		// "struct" is the only keyword of six letters that starts with s.
		if (style == Style.KEYWORD && end - start == 6 &&
				text.charAt(start) == 's') {
			kind = STRUCT;
		} else if (style == Style.IDENTIFIER) {
			if (lastKind == DOT) {
				style = Style.FIELD_SELECTION;
			} else if (lastKind == STRUCT) {
				style = Style.TYPE;
			} else {
				if (lastKind == NAME) {
					kinds[last] = Style.TYPE.ordinal();
				}
				kind = NAME;
			}
		}
		return code(start, end, style, kind);
	}

	private int number(int start) {
		int end = start;
		boolean octal = false;
		boolean notOctal = false;
		boolean real = false;
		if (text.charAt(end) == '0') {
			octal = true;
			char c = charAt(++end);
			if (c == 'x' || c == 'X') {
				do {
					++end;
				} while (end < length &&
						Character.digit(text.charAt(end), 16) != -1);
				c = charAt(end);
				if (end > start + 2 && (c == 'u' || c == 'U')) {
					++end;
				}
				return code(start, end, Style.NUMBER, OTHER);
			}
		}
		for (char c; end < length && Character.isDigit(c = text.charAt(end));
				++end) {
			notOctal |= c > '7';
		}
		if (charAt(end) == '.') {
			real = true;
			end = digits(end + 1);
		}
		char c = charAt(end);
		if (c == 'e' || c == 'E') {
			real = true;
			c = charAt(++end);
			if (c == '+' || c == '-') {
				++end;
			}
			if (!Character.isDigit(charAt(end))) {
				return code(start, end, Style.INVALID, OTHER);
			}
			end = digits(end);
			c = charAt(end);
		}
		if (real) {
			if (c == 'f' || c == 'F') {
				++end;
			}
			return code(start, end, Style.NUMBER, OTHER);
		}
		if (c == 'u' || c == 'U') {
			++end;
		}
		return code(start, end, octal && notOctal
				? Style.INVALID
				: Style.NUMBER, OTHER);
	}

	private int digits(int i) {
		while (i < length && Character.isDigit(text.charAt(i))) {
			++i;
		}
		return i;
	}

	private int lineCommentEnd(int i) {
		while (i < length) {
			char c = text.charAt(i);
			if (c == '\\' && isLineEnd(i + 1)) {
				i = afterLineEnd(i + 1);
			} else if (c == '\n' || c == '\r') {
				return i;
			} else {
				++i;
			}
		}
		return length;
	}

	private int blockCommentEnd(int i) {
		for (; i < length; ++i) {
			char c = text.charAt(i);
			if (c == '*' && charAt(i + 1) == '/') {
				return i + 2;
			} else if (c == '\n' || c == '\r') {
				startLine(false);
			}
		}
		return length;
	}

	private int code(int start, int end, Style style, int kind) {
		if (!directive) {
			last = size;
			lastKind = kind;
		}
		return add(start, end, style);
	}

	private int add(int start, int end, Style style) {
		if (size == starts.length) {
			int capacity = size * 2;
			starts = Arrays.copyOf(starts, capacity);
			ends = Arrays.copyOf(ends, capacity);
			kinds = Arrays.copyOf(kinds, capacity);
		}
		starts[size] = start;
		ends[size] = end;
		kinds[size] = style.ordinal() | (directive ? Tokens.IN_DIRECTIVE : 0);
		++size;
		return end;
	}

	private boolean isLineEnd(int i) {
		char c = charAt(i);
		return c == '\n' || c == '\r';
	}

	private int afterLineEnd(int i) {
		return charAt(i) == '\r' && charAt(i + 1) == '\n' ? i + 2 : i + 1;
	}

	private char charAt(int i) {
		return i < length ? text.charAt(i) : 0;
	}
}
