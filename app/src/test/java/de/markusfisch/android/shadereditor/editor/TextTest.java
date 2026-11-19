package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TextTest {
	@Test
	public void emptyText() {
		for (Text text : new Text[]{Text.EMPTY, Text.of("")}) {
			assertEquals(0, text.length());
			assertEquals(1, text.lineCount());
			assertEquals("", text.line(0));
			assertEquals(0, text.lineStart(0));
			assertEquals(0, text.lineOf(0));
			assertEquals("", text.toString());
			assertEquals("", text.substring(0, 0));
		}
	}

	@Test
	public void lastLineWithoutLineBreak() {
		Text text = Text.of("a\nbc");
		assertEquals(4, text.length());
		assertEquals(2, text.lineCount());
		assertEquals("a", text.line(0));
		assertEquals("bc", text.line(1));
		assertEquals(2, text.lineStart(1));
		assertEquals(0, text.lineOf(1));
		assertEquals(1, text.lineOf(2));
		assertEquals(1, text.lineOf(4));
	}

	@Test
	public void finalLineBreakStartsAnEmptyLine() {
		Text text = Text.of("a\n");
		assertEquals(2, text.lineCount());
		assertEquals("", text.line(1));
		assertEquals(2, text.lineStart(1));
		assertEquals(0, text.lineOf(1));
		assertEquals(1, text.lineOf(2));
	}

	@Test
	public void crLfAndLoneCrEndLines() {
		assertText("a\nb\nc\n\nd", Text.of("a\r\nb\rc\n\r\nd"));
		assertText("\n\n", Text.of("\r\r\n"));
		assertText("\n", Text.of("\r"));
		assertText("a\n", Text.of("a\r"));
	}

	@Test
	public void noBreakSpaceBecomesSpace() {
		assertText("a b  ", Text.of("a\u00a0b\u00a0\u00a0"));
	}

	@Test
	public void oneMegabyteLine() {
		String line = "abcdefgh".repeat(1 << 17);
		Text text = Text.of(line);
		assertEquals(1 << 20, text.length());
		assertEquals(1, text.lineCount());
		assertSame(line, text.line(0));
		assertEquals(0, text.lineStart(0));
		assertEquals(0, text.lineOf(0));
		assertEquals(0, text.lineOf(1 << 19));
		assertEquals(0, text.lineOf(1 << 20));
		assertEquals("habc", text.substring((1 << 19) - 1, (1 << 19) + 3));
	}

	@Test
	public void offsetsAndLinesAtEveryBoundary() {
		Random random = new Random(1);
		// Around the sizes of a leaf, a node of leaves, and a level more.
		for (int lines : new int[]{1, 2, 31, 32, 33, 64, 65, 1023, 1024,
				1025, 1057, 3000, 32769}) {
			String string = randomLines(random, lines);
			assertBoundaries(string, Text.of(string));
		}
	}

	@Test
	public void substrings() {
		Random random = new Random(2);
		String string = randomLines(random, 2000);
		Text text = Text.of(string);
		List<Integer> breaks = lineBreaks(string);
		for (int i = 0; i < 5000; ++i) {
			int a = pick(random, string, breaks);
			int b = pick(random, string, breaks);
			int from = Math.min(a, b);
			int to = Math.max(a, b);
			assertEquals(string.substring(from, to), text.substring(from, to));
		}
		assertEquals(string, text.toString());
	}

	@Test
	public void buildsAShallowTree() throws Exception {
		Text text = Text.of(randomLines(new Random(3), 500_000));
		assertEquals(500_000, text.lineCount());
		// 32 lines a leaf, 32 children a node: 15,625 leaves in 4 levels.
		assertEquals(4, depth(text));
	}

	@Test
	public void rejectsPositionsOutside() {
		Text text = Text.of("ab\ncd");
		assertThrows(IndexOutOfBoundsException.class, () -> text.line(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> text.line(2));
		assertThrows(IndexOutOfBoundsException.class, () -> text.lineStart(2));
		assertThrows(IndexOutOfBoundsException.class, () -> text.lineOf(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> text.lineOf(6));
		assertThrows(IndexOutOfBoundsException.class,
				() -> text.substring(4, 6));
		assertThrows(IndexOutOfBoundsException.class,
				() -> text.substring(2, 1));
	}

	@Test
	public void slices() {
		Random random = new Random(4);
		String string = randomLines(random, 2000);
		Text text = Text.of(string);
		List<Integer> breaks = lineBreaks(string);
		for (int i = 0; i < 2000; ++i) {
			int a = pick(random, string, breaks);
			int b = pick(random, string, breaks);
			String expected = string.substring(Math.min(a, b), Math.max(a, b));
			Text slice = text.slice(Math.min(a, b), Math.max(a, b));
			assertText(expected, slice);
			if (i % 20 == 0) {
				assertBoundaries(expected, slice);
			}
		}
		assertSame(Text.EMPTY, text.slice(5, 5));
	}

	@Test
	public void lineEndsOfTwoTextsStayApart() {
		Text text = Text.of("a\r").replace(2, 2, Text.of("\nb"));
		assertText("a\n\nb", text);
	}

	@Test
	public void editInAMegabyteLine() {
		String line = "abcdefgh".repeat(1 << 17);
		int middle = 1 << 19;
		Text text = Text.of(line).replace(middle, middle + 1, Text.of("X\nY"));
		assertEquals(2, text.lineCount());
		assertEquals(middle + 1, text.line(0).length());
		assertEquals(middle + 2, text.lineStart(1));
		assertEquals("hX\nYb", text.substring(middle - 1, middle + 4));
		assertEquals(line.substring(0, middle) + "X\nY" +
				line.substring(middle + 1), text.toString());
	}

	@Test
	public void offsetsAndLinesAtEveryBoundaryAfterEdits() {
		String string = randomLines(new Random(5), 3000);
		String[] lines = string.split("\n", -1);
		Text appended = Text.of(lines[0]);
		for (int i = 1; i < lines.length; ++i) {
			appended = appended.replace(appended.length(), appended.length(),
					Text.of("\n" + lines[i]));
		}
		assertBoundaries(string, appended);
		Text prepended = Text.of(lines[lines.length - 1]);
		for (int i = lines.length - 1; i-- > 0; ) {
			prepended = prepended.replace(0, 0, Text.of(lines[i] + "\n"));
		}
		assertBoundaries(string, prepended);
	}

	@Test
	public void longestLineAfterRandomEdits() {
		Random random = new Random(9);
		StringBuilder expected = new StringBuilder();
		Text text = Text.EMPTY;
		for (int i = 0; i < 5_000; ++i) {
			int length = expected.length();
			int from = random.nextInt(length + 1);
			int to = Math.min(length, from + removal(random, length));
			StringBuilder insert = new StringBuilder();
			for (int n = insertion(random); n-- > 0; ) {
				// Lines of very different lengths, so that the longest
				// one moves between leaves and is often removed.
				insert.append(random.nextInt(random.nextInt(200) + 2) == 0
						? '\n'
						: 'x');
			}
			text = text.replace(from, to, Text.of(insert));
			expected.replace(from, to, insert.toString());
			int longest = 0;
			for (String line : expected.toString().split("\n", -1)) {
				longest = Math.max(longest, line.length());
			}
			assertEquals(longest, text.longestLineLength());
		}
	}

	@Test
	public void randomEditsAgreeWithStringBuilder() {
		Random random = new Random(6);
		StringBuilder expected = new StringBuilder();
		Text text = Text.EMPTY;
		for (int i = 0; i < 40_000; ++i) {
			int length = expected.length();
			int from = random.nextInt(length + 1);
			int to = Math.min(length, from + removal(random, length));
			Text insert;
			if (random.nextInt(20) == 0 && length > 0) {
				int a = random.nextInt(length + 1);
				insert = text.slice(a, Math.min(length, a + random.nextInt(4000)));
			} else {
				insert = Text.of(randomString(random,
						length > 40_000 ? 4 : insertion(random)));
			}
			String inserted = insert.toString();
			text = text.replace(from, to, insert);
			expected.replace(from, to, inserted);
			assertEquals(expected.length(), text.length());
			int around = Math.max(0, from - 20);
			int after = Math.min(expected.length(), from + inserted.length() + 20);
			assertEquals(expected.substring(around, after),
					text.substring(around, after));
			if (i % 500 == 0) {
				assertBoundaries(expected.toString(), text);
			}
			if (i % 50 == 0) {
				assertText(expected.toString(), text);
			}
		}
		assertText(expected.toString(), text);
	}

	@Test
	public void editOfOneLineSharesTheRest() throws Exception {
		Random random = new Random(7);
		Text text = Text.of(randomLines(random, 500_000));
		String[] edits = {"x", "\n", "", "ab\ncd", "\n".repeat(40)};
		for (int i = 0; i < 300; ++i) {
			int line = random.nextInt(text.lineCount());
			int start = text.lineStart(line);
			int from = start + random.nextInt(text.line(line).length() + 1);
			// Removing a line break joins this line and the next.
			int to = Math.min(text.length(), from + random.nextInt(2));
			Text edited = text.replace(from, to,
					Text.of(edits[random.nextInt(edits.length)]));
			int created = countNewNodes(edited, nodesOf(text));
			// A leaf or two and a copy of each node above them, of the
			// more than 15,000 leaves and nodes of the text.
			assertTrue("an edit of one line made " + created + " nodes",
					created <= 12);
			if (line > 0) {
				assertSame(text.line(line - 1), edited.line(line - 1));
			}
			int last = text.lineCount() - 1;
			assertSame(text.line(last),
					edited.line(last + edited.lineCount() - text.lineCount()));
			text = edited;
		}
		assertTrue(depth(text) <= 5);
	}

	@Test
	public void staysShallowWhenGrownAtOnePlace() throws Exception {
		Text atEnd = Text.EMPTY;
		Text atStart = Text.EMPTY;
		Text inMiddle = Text.of("\n");
		Text line = Text.of("line\n");
		for (int i = 0; i < 100_000; ++i) {
			atEnd = atEnd.replace(atEnd.length(), atEnd.length(), line);
			atStart = atStart.replace(0, 0, line);
			int middle = inMiddle.lineStart(inMiddle.lineCount() / 2);
			inMiddle = inMiddle.replace(middle, middle, line);
		}
		assertEquals(100_001, atEnd.lineCount());
		assertEquals(100_001, atStart.lineCount());
		assertEquals(100_002, inMiddle.lineCount());
		// The fewest levels for 100,000 lines are 4; a list would be
		// thousands deep.
		assertTrue(depth(atEnd) <= 7);
		assertTrue(depth(atStart) <= 7);
		assertTrue(depth(inMiddle) <= 7);
	}

	@Test
	public void rejectsRangesOutside() {
		Text text = Text.of("ab\ncd");
		assertThrows(IndexOutOfBoundsException.class,
				() -> text.replace(3, 2, Text.EMPTY));
		assertThrows(IndexOutOfBoundsException.class,
				() -> text.replace(0, 6, Text.EMPTY));
		assertThrows(IndexOutOfBoundsException.class, () -> text.slice(-1, 1));
	}

	static void assertText(String expected, Text text) {
		assertEquals(expected, text.toString());
		assertEquals(expected.length(), text.length());
		assertEquals(expected.split("\n", -1).length, text.lineCount());
	}

	// Checks lineOf for every offset and lineStart and line for every line.
	static void assertBoundaries(String expected, Text text) {
		String[] lines = expected.split("\n", -1);
		assertEquals(lines.length, text.lineCount());
		int start = 0;
		for (int n = 0; n < lines.length; ++n) {
			assertEquals(lines[n], text.line(n));
			assertEquals(start, text.lineStart(n));
			int end = start + lines[n].length();
			for (int offset = start; offset <= end; ++offset) {
				assertEquals(n, text.lineOf(offset));
			}
			start = end + 1;
		}
	}

	static String randomLines(Random random, int count) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; ++i) {
			if (i > 0) {
				sb.append('\n');
			}
			int length = random.nextInt(8) == 0 ? 0 : random.nextInt(60);
			for (int j = 0; j < length; ++j) {
				sb.append((char) ('a' + random.nextInt(26)));
			}
		}
		return sb.toString();
	}

	static List<Integer> lineBreaks(String string) {
		List<Integer> breaks = new ArrayList<>();
		for (int i = string.indexOf('\n'); i > -1;
				i = string.indexOf('\n', i + 1)) {
			breaks.add(i);
		}
		return breaks;
	}

	// Picks a line break, the offset behind one, an end, or any offset.
	static int pick(Random random, String string, List<Integer> breaks) {
		switch (random.nextInt(4)) {
			case 0:
				return breaks.get(random.nextInt(breaks.size()));
			case 1:
				return breaks.get(random.nextInt(breaks.size())) + 1;
			case 2:
				return random.nextBoolean() ? 0 : string.length();
			default:
				return random.nextInt(string.length() + 1);
		}
	}

	private static String randomString(Random random, int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; ++i) {
			sb.append(random.nextInt(4) == 0 ? '\n' : 'x');
		}
		return sb.toString();
	}

	private static int insertion(Random random) {
		int kind = random.nextInt(100);
		return kind < 70 ? random.nextInt(3) : kind < 95
				? random.nextInt(100)
				: random.nextInt(5000);
	}

	private static int removal(Random random, int length) {
		int kind = random.nextInt(100);
		return kind < 50 ? 0 : kind < 85
				? random.nextInt(3)
				: kind < 99 ? random.nextInt(2000) : length;
	}

	private static Set<Text> nodesOf(Text text) throws Exception {
		Set<Text> nodes = Collections.newSetFromMap(new IdentityHashMap<>());
		List<Text> pending = new ArrayList<>();
		pending.add(text);
		while (!pending.isEmpty()) {
			Text node = pending.remove(pending.size() - 1);
			nodes.add(node);
			Collections.addAll(pending, children(node));
		}
		return nodes;
	}

	private static int countNewNodes(Text text, Set<Text> old)
			throws Exception {
		if (old.contains(text)) {
			return 0;
		}
		int count = 1;
		for (Text child : children(text)) {
			count += countNewNodes(child, old);
		}
		return count;
	}

	static Text[] children(Text text) throws Exception {
		Class<?> type = text.getClass();
		if (!type.getSimpleName().equals("Node")) {
			return new Text[0];
		}
		Field field = type.getDeclaredField("children");
		field.setAccessible(true);
		return (Text[]) field.get(text);
	}

	static int depth(Text text) throws Exception {
		int depth = 0;
		for (Text child : children(text)) {
			depth = Math.max(depth, depth(child));
		}
		return depth + 1;
	}
}
