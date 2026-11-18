package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import de.markusfisch.android.shadereditor.syntax.BundledShaders;
import de.markusfisch.android.shadereditor.syntax.GlslLexer;
import de.markusfisch.android.shadereditor.syntax.GlslSyntax;
import de.markusfisch.android.shadereditor.syntax.LineTokens;
import de.markusfisch.android.shadereditor.syntax.Style;
import de.markusfisch.android.shadereditor.syntax.Syntax;

public class LineStylesTest {
	private static final Syntax GLSL = GlslSyntax.INSTANCE;
	private static final String[] TYPED = {
			"/*", "*/", "//", "\n", "\n", "\\\n", "#define A ", "#", "struct ",
			"S s;", ".", " ", "x", "float y = 1.0;", "\"", "{", "}\n"};

	@Test
	public void staysEqualToAFreshComputationOverRandomEdits()
			throws IOException {
		String base = bundled(false);
		Document document = new Document(Text.of(base));
		LineStyles styles = new LineStyles(document, GLSL, 30);
		Random random = new Random(42);
		List<LineStyles.Result> late = new ArrayList<>();
		for (int i = 1; i <= 20_000; ++i) {
			edit(document, random, base);
			if (random.nextInt(8) == 0) {
				LineStyles.Continuation continuation = styles.continuation();
				if (continuation != null) {
					late.add(continuation.run(1 + random.nextInt(400)));
				}
			}
			if (!late.isEmpty() && random.nextInt(3) == 0) {
				LineStyles.Result result =
						late.get(random.nextInt(late.size()));
				if (!styles.apply(result)) {
					late.remove(result);
				}
			}
			if (random.nextInt(50) == 0) {
				int first = random.nextInt(document.text().lineCount());
				styles.show(first, first + random.nextInt(60));
			}
			if (i % 25 == 0) {
				int[] expected = startStates(document.text());
				for (int line = 0; line < styles.staleFrom(); ++line) {
					assertEquals("line " + line + " after " + i + " edits",
							expected[line], styles.startState(line));
				}
			}
			if (i % 500 == 0) {
				drain(styles, 1 + random.nextInt(1000));
				assertCurrent(styles, document.text());
			}
		}
	}

	@Test
	public void openingACommentAtTheTopIsBoundedByTheBudget()
			throws IOException {
		Text text = Text.of(repeat(bundled(true), 50_000));
		Document document = new Document(text);
		LineStyles styles = new LineStyles(document, GLSL, 200);
		drain(styles, Integer.MAX_VALUE);
		document.replace(0, 0, Text.of("/*"));
		assertEquals(200, styles.staleFrom());
		LineStyles.Continuation continuation = styles.continuation();
		assertNotNull(continuation);
		assertFalse(styles.apply(continuation.run(4096)));
		assertEquals(4296, styles.staleFrom());
		drain(styles, 4096);
		assertCurrent(styles, document.text());
		assertEquals(Style.COMMENT, styles.runs(40_000).style(0));
	}

	@Test
	public void anEditedShownLineInTheStaleRangeIsCurrent() {
		Document document = new Document(Text.of("x\n".repeat(30)));
		LineStyles styles = new LineStyles(document, GLSL, 2);
		drain(styles, 100);
		styles.show(15, 25);
		document.replace(0, 0, Text.of("/*"));
		// The budget and the shown lines, no more.
		assertEquals(12, styles.staleFrom());
		type(document, 20, "z");
		assertEquals(Style.COMMENT, styles.runs(20).style(0));
	}

	@Test
	public void applyStoresABoundedPartOfAResultPerCall() {
		Document document = new Document(Text.of(lines(10_000)));
		LineStyles styles = new LineStyles(document, GLSL, 10);
		drain(styles, Integer.MAX_VALUE);
		document.replace(0, 0, Text.of("/*"));
		LineStyles.Result result =
				styles.continuation().run(Integer.MAX_VALUE);
		assertTrue(styles.apply(result));
		assertEquals(10 + 4096, styles.staleFrom());
		while (styles.apply(result)) {
			assertTrue(styles.staleFrom() < 10_001);
		}
		assertCurrent(styles, document.text());
	}

	@Test
	public void aLargePasteTokenizesTheBudgetNow() throws IOException {
		String shaders = bundled(false);
		Text text = Text.of(repeat(shaders, 5_000));
		Document document = new Document(text);
		LineStyles styles = new LineStyles(document, GLSL, 200);
		drain(styles, Integer.MAX_VALUE);
		Text paste = Text.of(repeat(shaders, 5_000));
		document.replace(text.lineStart(100), text.lineStart(100), paste);
		assertEquals(300, styles.staleFrom());
		drain(styles, 1000);
		assertCurrent(styles, document.text());
	}

	@Test
	public void aResultCountsUpToTheFirstLineChangedSince() {
		Document document = new Document(Text.of(lines(2000)));
		LineStyles styles = new LineStyles(document, GLSL, 10);
		drain(styles, Integer.MAX_VALUE);
		document.replace(0, 0, Text.of("/*"));
		assertEquals(10, styles.staleFrom());
		LineStyles.Result result = styles.continuation().run(5000);
		// A key typed in the stale part: the result holds above it.
		type(document, 1500, "x");
		assertFalse(styles.apply(result));
		assertEquals(1500, styles.staleFrom());
		drain(styles, 100);
		assertCurrent(styles, document.text());
	}

	@Test
	public void aResultIsDroppedAfterAChangeAboveIt() {
		Document document = new Document(Text.of(lines(2000)));
		LineStyles styles = new LineStyles(document, GLSL, 10);
		drain(styles, Integer.MAX_VALUE);
		document.replace(0, 0, Text.of("/*"));
		LineStyles.Result result = styles.continuation().run(5000);
		// Closing the comment changes what the result started from.
		type(document, 5, "*/");
		int staleFrom = styles.staleFrom();
		assertFalse(styles.apply(result));
		assertEquals(staleFrom, styles.staleFrom());
		drain(styles, 100);
		assertCurrent(styles, document.text());
	}

	@Test
	public void aResultIsDroppedAfterTooManyChanges() {
		Document document = new Document(Text.of(lines(2000)));
		LineStyles styles = new LineStyles(document, GLSL, 10);
		document.replace(0, 0, Text.of("/*"));
		LineStyles.Result result = styles.continuation().run(5000);
		for (int i = 0; i < 65; ++i) {
			type(document, 1999, "x");
		}
		assertFalse(styles.apply(result));
		assertEquals(10, styles.staleFrom());
		drain(styles, 100);
		assertCurrent(styles, document.text());
	}

	@Test
	public void runsAreCachedByLineAndStartState() {
		Document document = new Document(Text.of("a\nb /* c\nd */ e\nf"));
		LineStyles styles = new LineStyles(document, GLSL, 100);
		LineStyles.Runs runs = styles.runs(3);
		assertSame(runs, styles.runs(3));
		// A line that only moved keeps its runs.
		document.replace(0, 0, Text.of("\n"));
		assertSame(runs, styles.runs(4));
		LineStyles.Runs comment = styles.runs(3);
		// The same text in another state is lexed again.
		document.replace(document.text().lineStart(2) + 2,
				document.text().lineStart(2) + 4, Text.EMPTY);
		assertEquals("b  c", document.text().line(2));
		assertNotSame(comment, styles.runs(3));
		assertEquals(Style.IDENTIFIER, styles.runs(3).style(0));
	}

	@Test
	public void runsJoinNeighboursOfOneStyle() {
		Document document = new Document(Text.of("a != b; // c"));
		LineStyles.Runs runs = new LineStyles(document, GLSL, 100).runs(0);
		assertEquals(5, runs.size());
		assertEquals(2, runs.start(1));
		assertEquals(4, runs.end(1));
		assertEquals(Style.OPERATOR, runs.style(1));
		assertEquals(Style.COMMENT, runs.style(4));
	}


	@Test
	public void aCurrentDocumentHasNoContinuation() {
		Document document = new Document(Text.of(lines(50)));
		LineStyles styles = new LineStyles(document, GLSL, 100);
		assertNull(styles.continuation());
		type(document, 0, "/*");
		assertNull(styles.continuation());
	}

	@Test
	public void ignoresEditsOnceClosed() {
		Document document = new Document(Text.of("x\ny"));
		LineStyles styles = new LineStyles(document, GLSL, 10);
		styles.close();
		document.replace(0, 0, Text.of("/*"));
		assertNotEquals(Style.COMMENT, styles.runs(1).style(0));
	}

	private static void edit(Document document, Random random, String base) {
		Text text = document.text();
		int length = text.length();
		int from = random.nextInt(length + 1);
		int roll = random.nextInt(100);
		if (roll < 2 && document.canUndo()) {
			document.undo();
			return;
		} else if (roll < 3 && document.canRedo()) {
			document.redo();
			return;
		}
		int to = from;
		if (roll < 25 || length > base.length() * 2) {
			to = Math.min(length, from + random.nextInt(
					roll < 5 || length > base.length() * 2 ? 5000 : 8));
		}
		String insert;
		if (roll > 97) {
			int start = random.nextInt(base.length());
			insert = base.substring(start, Math.min(base.length(),
					start + random.nextInt(8000)));
		} else {
			insert = TYPED[random.nextInt(TYPED.length)];
		}
		document.type(from, to, Text.of(insert));
	}

	private static void type(Document document, int line, String s) {
		int at = document.text().lineStart(line);
		document.type(at, at, Text.of(s));
	}

	private static void drain(LineStyles styles, int chunk) {
		for (int i = 0; i < 1_000_000; ++i) {
			LineStyles.Continuation continuation = styles.continuation();
			if (continuation == null) {
				return;
			}
			int staleFrom = styles.staleFrom();
			LineStyles.Result result = continuation.run(chunk);
			while (styles.apply(result)) {
				assertTrue(styles.staleFrom() > staleFrom);
			}
		}
		throw new AssertionError("Continuations do not end");
	}

	private static void assertCurrent(LineStyles styles, Text text) {
		assertNull(styles.continuation());
		assertEquals(text.lineCount(), styles.staleFrom());
		int[] expected = startStates(text);
		LineStyles fresh = new LineStyles(new Document(text), GLSL,
				Integer.MAX_VALUE);
		for (int line = 0; line < expected.length; ++line) {
			assertEquals("line " + line, expected[line],
					styles.startState(line));
			assertSameRuns(fresh.runs(line), styles.runs(line));
		}
	}

	private static void assertSameRuns(LineStyles.Runs expected,
			LineStyles.Runs actual) {
		assertEquals(expected.size(), actual.size());
		for (int i = 0; i < expected.size(); ++i) {
			assertEquals(expected.start(i), actual.start(i));
			assertEquals(expected.end(i), actual.end(i));
			assertEquals(expected.style(i), actual.style(i));
		}
	}

	private static int[] startStates(Text text) {
		int lines = text.lineCount();
		int[] states = new int[lines];
		LineTokens tokens = new LineTokens();
		int state = Syntax.TEXT_START;
		for (int line = 0; line < lines; ++line) {
			states[line] = state;
			state = GlslLexer.lexLine(text.line(line), line < lines - 1,
					state, tokens);
		}
		return states;
	}

	private static String bundled(boolean withoutCommentEnds)
			throws IOException {
		StringBuilder sb = new StringBuilder();
		for (String shader : BundledShaders.read().values()) {
			if (!withoutCommentEnds || !shader.contains("*/")) {
				sb.append(shader).append('\n');
			}
		}
		return sb.toString();
	}

	private static String repeat(String s, int lines) {
		StringBuilder sb = new StringBuilder();
		int count = 0;
		int newlines = s.split("\n", -1).length - 1;
		while (count < lines) {
			sb.append(s);
			count += newlines;
		}
		return sb.toString();
	}

	private static String lines(int count) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; ++i) {
			sb.append("float v").append(i).append(" = 1.0;\n");
		}
		return sb.toString();
	}
}
