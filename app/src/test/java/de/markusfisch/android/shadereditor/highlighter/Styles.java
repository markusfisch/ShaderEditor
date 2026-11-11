package de.markusfisch.android.shadereditor.highlighter;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Turns a text into one letter per character for the colour the editor
 * gives it, so expectations read like the source they describe.
 *
 * <pre>
 * .  no colour: whitespace, identifiers and operators (text colour)
 * x  invalid         k  keyword         d  directive
 * p  primitive type  v  vector type     m  matrix type
 * a  atomic counter  s  sampler type    i  image type
 * t  type name       b  builtin func    f  field selection
 * n  number          l  bool literal    r  precision
 * c  comment         o  control flow    q  type qualifier
 * </pre>
 */
final class Styles {
	private Styles() {
	}

	static void assertStyles(@NonNull String source, @NonNull String expected) {
		assertEquals(source, expected, of(source));
	}

	/**
	 * Tokenizes like the editor does: all tokens are collected before any
	 * is coloured, and a token covers its start to end offset.
	 */
	@NonNull
	static List<Token> tokens(@NonNull String source) {
		List<Token> tokens = new ArrayList<>();
		for (Token token : new Lexer(source)) {
			tokens.add(token);
		}
		return tokens;
	}

	@NonNull
	static String of(@NonNull String source) {
		char[] styles = new char[source.length()];
		Arrays.fill(styles, '.');
		for (Token token : tokens(source)) {
			char letter = letter(Highlight.from(token.type()));
			for (int i = token.startOffset(); i < token.endOffset(); ++i) {
				styles[i] = letter;
			}
		}
		return new String(styles);
	}

	private static char letter(@NonNull Highlight highlight) {
		switch (highlight) {
			case HIGHLIGHT_IDENTIFIER:
			case HIGHLIGHT_OPERATOR:
				return '.';
			case HIGHLIGHT_INVALID:
				return 'x';
			case HIGHLIGHT_KEYWORD:
				return 'k';
			case HIGHLIGHT_DIRECTIVE:
				return 'd';
			case HIGHLIGHT_PRIMITIVE:
				return 'p';
			case HIGHLIGHT_TYPE_VECTOR:
				return 'v';
			case HIGHLIGHT_TYPE_MATRIX:
				return 'm';
			case HIGHLIGHT_HIGHLIGHT_ATOMIC_COUNTER:
				return 'a';
			case HIGHLIGHT_TYPE_SAMPLER:
				return 's';
			case HIGHLIGHT_TYPE_IMAGE:
				return 'i';
			case HIGHLIGHT_TYPE:
				return 't';
			case HIGHLIGHT_BUILTIN_FUNCTION:
				return 'b';
			case HIGHLIGHT_FIELD_SELECTION:
				return 'f';
			case HIGHLIGHT_NUMBER_LITERAL:
				return 'n';
			case HIGHLIGHT_BOOL_LITERAL:
				return 'l';
			case HIGHLIGHT_PRECISION:
				return 'r';
			case HIGHLIGHT_COMMENT:
				return 'c';
			case HIGHLIGHT_CONTROL_FLOW:
				return 'o';
			case HIGHLIGHT_TYPE_QUALIFIER:
				return 'q';
		}
		throw new AssertionError("No letter for " + highlight);
	}
}
