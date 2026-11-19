package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.markusfisch.android.shadereditor.editor.Document.Selection;

public class MotionTest {
	private final Motion motion = new Motion();

	@Test
	public void marksJoinTheCharBeforeThem() {
		Text text = Text.of("e\u0301\u0302x\u2764\ufe0f");
		assertEquals(List.of(3, 4, 6), forward(text));
		assertEquals(List.of(4, 3, 0), backward(text));
	}

	// Java 17 splits skin tones, joined emoji and flags into their code
	// points; Android's ICU from Unicode 9 on, and Java from 20 on, keep
	// each of them one char. Both never stop inside a surrogate pair and
	// agree going either way.
	@Test
	public void emojiNeverSplitASurrogatePair() {
		for (String s : new String[]{
				"\ud83d\udc4d\ud83c\udffd",
				"\ud83d\udc69\ud83c\udffd\u200d\ud83d\udcbb\ufe0f",
				"\ud83c\udde9\ud83c\uddea\ud83c\uddeb\ud83c\uddf7"}) {
			Text text = Text.of(s);
			List<Integer> stops = forward(text);
			List<Integer> back = backward(text);
			Collections.reverse(back);
			assertEquals(stops.subList(0, stops.size() - 1),
					back.subList(1, back.size()));
			for (int stop : stops) {
				assertFalse(stop < s.length() &&
						Character.isLowSurrogate(s.charAt(stop)));
			}
		}
	}

	@Test
	public void goingBackAsksOnlyAboutWhatItPasses() {
		int[] questions = new int[1];
		Motion counting = new Motion(new Motion.Boundaries() {
			@Override
			public int after(String line, int offset) {
				++questions[0];
				return offset + 1;
			}

			@Override
			public int before(String line, int offset) {
				++questions[0];
				return offset - 1;
			}
		});
		Text text = Text.of("x".repeat(100_000) + " yz");
		assertEquals(100_002, counting.previousChar(text, text.length()));
		assertEquals(100_001, counting.previousWord(text, text.length()));
		assertEquals(4, questions[0]);
	}

	@Test
	public void aLineBreakIsOneChar() {
		Text text = Text.of("a\nb");
		assertEquals(2, motion.nextChar(text, 1));
		assertEquals(1, motion.previousChar(text, 2));
		assertEquals(3, motion.nextChar(text, 3));
		assertEquals(0, motion.previousChar(text, 0));
	}

	@Test
	public void nextWordSkipsSpaceThenOneWord() {
		Text text = Text.of("vec2 uv  += 1.0;");
		assertEquals(4, motion.nextWord(text, 0));
		assertEquals(7, motion.nextWord(text, 4));
		assertEquals(11, motion.nextWord(text, 7));
		assertEquals(13, motion.nextWord(text, 11));
	}

	@Test
	public void previousWordSkipsSpaceThenOneWord() {
		Text text = Text.of("vec2 uv  += 1.0;");
		assertEquals(9, motion.previousWord(text, 11));
		assertEquals(5, motion.previousWord(text, 9));
		assertEquals(0, motion.previousWord(text, 3));
		assertEquals(0, motion.previousWord(Text.of("  "), 2));
	}

	@Test
	public void wordMovesCrossLineEndsOneAtATime() {
		Text text = Text.of("a  \n  b");
		assertEquals(3, motion.nextWord(text, 1));
		assertEquals(4, motion.nextWord(text, 3));
		assertEquals(3, motion.previousWord(text, 4));
		assertEquals(0, motion.previousWord(text, 3));
	}

	@Test
	public void wordsKeepTheirMarks() {
		Text text = Text.of("cafe\u0301 x");
		assertEquals(5, motion.nextWord(text, 0));
		assertEquals(0, motion.previousWord(text, 5));
	}

	@Test
	public void lineStartAndEnd() {
		Text text = Text.of("ab\ncde\n");
		assertEquals(3, motion.lineStart(text, 5));
		assertEquals(6, motion.lineEnd(text, 4));
		assertEquals(7, motion.lineEnd(text, 7));
	}

	@Test
	public void wordAtOffset() {
		Text text = Text.of("a = foo_1+bar;");
		assertEquals(new Selection(4, 9), motion.wordAt(text, 6));
		// A word that ends at the offset wins over what follows.
		assertEquals(new Selection(4, 9), motion.wordAt(text, 9));
		assertEquals(new Selection(10, 13), motion.wordAt(text, 10));
		assertEquals(new Selection(13, 14), motion.wordAt(text, 14));
		assertEquals(new Selection(0, 1), motion.wordAt(text, 1));
		assertEquals(new Selection(3, 4), motion.wordAt(text, 3));
		assertEquals(new Selection(3, 3), motion.wordAt(Text.of("ab\n\n"), 3));
	}

	@Test
	public void tabsAdvanceToTheNextStop() {
		Text text = Text.of("\tx\nab\tx\nabcd\tx");
		assertEquals(4, motion.column(text, 1, 4));
		assertEquals(4, motion.column(text, 6, 4));
		assertEquals(8, motion.column(text, 13, 4));
		assertEquals(2, motion.column(text, 1, 2));
	}

	@Test
	public void marksTakeNoColumn() {
		assertEquals(2, motion.column(Text.of("e\u0301x"), 3, 4));
	}

	@Test
	public void offsetAtTheClosestColumn() {
		Text text = Text.of("\tx");
		assertEquals(0, motion.offsetAt(text, 0, 2, 4));
		assertEquals(1, motion.offsetAt(text, 0, 3, 4));
		assertEquals(2, motion.offsetAt(text, 0, 5, 4));
		assertEquals(2, motion.offsetAt(text, 0, 40, 4));
	}

	private List<Integer> forward(Text text) {
		List<Integer> stops = new ArrayList<>();
		for (int i = 0; i < text.length(); ) {
			stops.add(i = motion.nextChar(text, i));
		}
		return stops;
	}

	private List<Integer> backward(Text text) {
		List<Integer> stops = new ArrayList<>();
		for (int i = text.length(); i > 0; ) {
			stops.add(i = motion.previousChar(text, i));
		}
		return stops;
	}
}
