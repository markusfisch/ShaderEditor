package de.markusfisch.android.shadereditor.highlighter;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import java.util.Arrays;

import de.markusfisch.android.shadereditor.syntax.GlslLexer;
import de.markusfisch.android.shadereditor.syntax.Style;
import de.markusfisch.android.shadereditor.syntax.Tokens;

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

	@NonNull
	static String of(@NonNull String source) {
		char[] styles = new char[source.length()];
		Arrays.fill(styles, '.');
		Tokens tokens = GlslLexer.lex(source);
		for (int t = 0; t < tokens.size(); ++t) {
			char letter = letter(tokens.style(t));
			for (int i = tokens.start(t); i < tokens.end(t); ++i) {
				styles[i] = letter;
			}
		}
		return new String(styles);
	}

	private static char letter(@NonNull Style style) {
		switch (style) {
			case IDENTIFIER:
			case OPERATOR:
				return '.';
			case INVALID:
				return 'x';
			case KEYWORD:
				return 'k';
			case DIRECTIVE:
				return 'd';
			case PRIMITIVE:
				return 'p';
			case TYPE_VECTOR:
				return 'v';
			case TYPE_MATRIX:
				return 'm';
			case ATOMIC_COUNTER:
				return 'a';
			case TYPE_SAMPLER:
				return 's';
			case TYPE_IMAGE:
				return 'i';
			case TYPE:
				return 't';
			case BUILTIN_FUNCTION:
				return 'b';
			case FIELD_SELECTION:
				return 'f';
			case NUMBER:
				return 'n';
			case BOOL:
				return 'l';
			case PRECISION:
				return 'r';
			case COMMENT:
				return 'c';
			case CONTROL_FLOW:
				return 'o';
			case TYPE_QUALIFIER:
				return 'q';
		}
		throw new AssertionError("No letter for " + style);
	}
}
