package de.markusfisch.android.shadereditor.syntax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A text lexed line by line gives the whole-text tokens cut at line ends.
public class LineTokensTest {
	private static final String[] TRICKY = {
			"",
			"/* a\nb",
			"x /*",
			"// a \\\nb\nc",
			"// a \\\n\nb",
			"// a \\\r\nb \\\rc\nd",
			"a\r\nb\rc\nd\n\re",
			"#define A /* c */ b \\\n c // d\ne /* f\n */ g",
			"#define A /* c\n */ b",
			"#define A /* c\n\n\n */ b",
			"#define A // c \\\n d\ne",
			"#define A 1 \\\n+ b c",
			"#define A 1\\\r\n+ b c",
			"/* a\n b */ #define X 1",
			"/*\n\n*/\n#if 0",
			"x \\\n#define Y 1",
			"#\\\ndefine X",
			"# \\\n \\\n include \"a\"",
			"#\ndefine",
			"#include \"a.glsl\nfloat x;",
			"#define S \"a\\\nb\"",
			"fl\\\noat x;",
			"struct\nRay r;",
			"struct /*\n*/ Ray r;",
			"a.\nb",
			"a. // c\n#define X\nb",
			"x \\",
			"x \\\n",
			"a \uD83D",
			"a \uD83D\n\uDE00",
			"x = \uD83D\uDE00;",
			"\n\n\n",
			"/*",
			"//",
			"x = 1.\n5e\n+3;",
	};
	// Lines that continue a text differently in each state.
	private static final String[] PROBES = {
			"x", "define", "*/x", "*/define", "\nx", "*/\nx"};

	@Test
	public void bundledShaders() throws IOException {
		for (Map.Entry<String, String> shader :
				BundledShaders.read().entrySet()) {
			String text = shader.getValue();
			assertSameTokens(shader.getKey(), text);
			assertSameTokens(shader.getKey() + " with CRLF",
					text.replace("\n", "\r\n"));
		}
	}

	@Test
	public void trickyInputs() {
		for (String text : TRICKY) {
			assertSameTokens("", text);
		}
	}

	@Test
	public void typeGuessStopsAtALineEnd() {
		// The one difference to the lexer before line by line: a name
		// followed by a name on a later line is no longer a type name.
		assertEquals(Style.IDENTIFIER, styleOf("x = y\nz = 1;", "y"));
		assertEquals(Style.IDENTIFIER, styleOf("Ray\nr;", "Ray"));
		assertEquals(Style.IDENTIFIER, styleOf("Ray /*\n*/ r;", "Ray"));
		assertEquals(Style.IDENTIFIER, styleOf("Ray \\\nr;", "Ray"));
		assertEquals(Style.TYPE, styleOf("Ray r;", "Ray"));
		assertEquals(Style.TYPE, styleOf("Ray /* c */ r;", "Ray"));
		assertEquals(Style.TYPE, styleOf("struct\nRay {};", "Ray"));
		assertEquals(Style.FIELD_SELECTION, styleOf("a.\nb;", "b"));
		assertSameTokens("", "x = y\nz = 1;");
		assertSameTokens("", "Ray\nr;");
	}

	@Test
	public void noLineBreakFollows() {
		LineTokens tokens = new LineTokens();
		assertEquals(Syntax.TEXT_START,
				GlslLexer.lexLine("/* a", false, Syntax.TEXT_START, tokens));
		int state = GlslLexer.lexLine("x \\", true, Syntax.TEXT_START,
				tokens);
		assertEquals(1, tokens.size());
		assertEquals(Syntax.TEXT_START, state);
		LexedLine line = LexedLine.of(GlslSyntax.INSTANCE, "x \\", false,
				Syntax.TEXT_START, tokens);
		assertEquals(2, line.size());
		assertEquals(Style.INVALID, line.style(1));
	}

	@Test
	public void statesAreEqualExactlyWhenTheRestIsTheSame() {
		Map<Integer, String> prefixes = new HashMap<>();
		List<String> texts = new ArrayList<>();
		for (String text : TRICKY) {
			texts.add(text);
		}
		// The last code token, then where the line ends: 3 * 7 states.
		for (String kind : new String[]{"", "a.", "struct"}) {
			for (String end : new String[]{"", "#define A \\", "#\\",
					"/* c", "#define A /* c", "// c \\",
					"#define A // c \\"}) {
				texts.add(kind + "\n" + end);
			}
		}
		for (String text : texts) {
			int state = endState(text);
			assertEquals(text, state, state & 0xff);
			String known = prefixes.get(state);
			if (known == null) {
				prefixes.put(state, text);
				continue;
			}
			for (String probe : PROBES) {
				assertEquals(text + " | " + known + " | " + probe,
						rest(known, probe), rest(text, probe));
			}
		}
		assertEquals(21, prefixes.size());
		for (Map.Entry<Integer, String> a : prefixes.entrySet()) {
			for (Map.Entry<Integer, String> b : prefixes.entrySet()) {
				if (a.getKey() < b.getKey()) {
					assertDistinguished(a.getValue(), b.getValue());
				}
			}
		}
	}

	private static void assertDistinguished(String a, String b) {
		for (String probe : PROBES) {
			if (!rest(a, probe).equals(rest(b, probe))) {
				return;
			}
		}
		throw new AssertionError("No probe tells apart " + a + " | " + b);
	}

	private static Style styleOf(String text, String word) {
		Tokens tokens = GlslLexer.lex(text);
		return tokens.style(tokens.indexAt(text.indexOf(word)));
	}

	// The state of the line after text and a line break.
	private static int endState(String text) {
		LineTokens tokens = new LineTokens();
		int state = Syntax.TEXT_START;
		for (String line : completeLines(text)) {
			state = GlslLexer.lexLine(line, true, state, tokens);
		}
		return state;
	}

	// The cut whole-text tokens after text and a line break.
	private static List<Piece> rest(String text, String probe) {
		String whole = text + "\n" + probe;
		int restStart = lineStarts(whole)[completeLines(text).size()];
		List<Piece> rest = new ArrayList<>();
		for (Piece piece : wholePieces(whole)) {
			if (piece.start >= restStart) {
				rest.add(new Piece(piece.start - restStart,
						piece.end - restStart, piece.style, piece.inDirective));
			}
		}
		return rest;
	}

	private static void assertSameTokens(String where, String text) {
		where += ": " + text.replace("\n", "\\n").replace("\r", "\\r");
		assertEquals(where, wholePieces(text), linePieces(text));
	}

	// The tokens of lex(text), cut at line ends.
	private static List<Piece> wholePieces(String text) {
		Tokens tokens = GlslLexer.lex(text);
		int[] starts = lineStarts(text);
		List<Piece> pieces = new ArrayList<>();
		for (int t = 0; t < tokens.size(); ++t) {
			for (int l = 0; l < starts.length; ++l) {
				int lineStart = starts[l];
				if (lineStart >= tokens.end(t)) {
					break;
				}
				int start = Math.max(tokens.start(t), lineStart);
				int end = Math.min(tokens.end(t), contentEnd(text, starts, l));
				if (start < end) {
					pieces.add(new Piece(start, end, tokens.style(t),
							tokens.inDirective(t)));
				}
			}
		}
		return pieces;
	}

	// The tokens of lexLine for each line, at offsets into text.
	private static List<Piece> linePieces(String text) {
		int[] starts = lineStarts(text);
		List<String> lines = lines(text);
		List<Piece> pieces = new ArrayList<>();
		LineTokens scratch = new LineTokens();
		int state = Syntax.TEXT_START;
		for (int l = 0; l < lines.size(); ++l) {
			boolean breakFollows = l < lines.size() - 1;
			LexedLine line = LexedLine.of(GlslSyntax.INSTANCE, lines.get(l),
					breakFollows, state, scratch);
			state = GlslLexer.lexLine(lines.get(l), breakFollows, state,
					scratch);
			int offset = starts[l];
			for (int t = 0; t < line.size(); ++t) {
				assertTrue(line.start(t) < line.end(t));
				assertTrue(line.end(t) <= lines.get(l).length());
				pieces.add(new Piece(offset + line.start(t),
						offset + line.end(t), line.style(t),
						line.inDirective(t)));
			}
		}
		return pieces;
	}

	private static int[] lineStarts(String text) {
		List<String> lines = lines(text);
		int[] starts = new int[lines.size()];
		for (int l = 1, at = 0; l < starts.length; ++l) {
			at += lines.get(l - 1).length();
			at += text.startsWith("\r\n", at) ? 2 : 1;
			starts[l] = at;
		}
		return starts;
	}

	private record Piece(int start, int end, Style style,
			boolean inDirective) {
	}

	// The lines of text as Tokens counts them, without their line ends.
	private static List<String> lines(String text) {
		List<String> lines = new ArrayList<>();
		int start = 0;
		for (int i = 0; i < text.length(); ++i) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\r') {
				lines.add(text.substring(start, i));
				if (c == '\r' && i + 1 < text.length() &&
						text.charAt(i + 1) == '\n') {
					++i;
				}
				start = i + 1;
			}
		}
		lines.add(text.substring(start));
		return lines;
	}

	// The lines of text followed by a line break.
	private static List<String> completeLines(String text) {
		List<String> lines = lines(text + "\n");
		return lines.subList(0, lines.size() - 1);
	}

	private static int contentEnd(String text, int[] starts, int line) {
		if (line + 1 == starts.length) {
			return text.length();
		}
		int end = starts[line + 1] - 1;
		return end > 0 && text.charAt(end) == '\n' &&
				text.charAt(end - 1) == '\r' ? end - 1 : end;
	}
}
