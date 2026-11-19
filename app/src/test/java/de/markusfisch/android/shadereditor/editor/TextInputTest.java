package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.editor.Document.Selection;
import de.markusfisch.android.shadereditor.editor.TextInput.Window;

/**
 * State is written as text with markers: | for the caret, { and } for
 * a selection from anchor to head, [ and ] for the composing region.
 */
public class TextInputTest {
	private static final String PAIR = "😀";
	private static final int MAX_READ = TextInput.MAX_READ;

	private final List<String> told = new ArrayList<>();
	private final List<String> extracted = new ArrayList<>();
	private final TextInput.Listener listener = new TextInput.Listener() {
		@Override
		public void onUpdateSelection(int selStart, int selEnd,
				int composingStart, int composingEnd) {
			told.add(selStart + " " + selEnd + " " + composingStart + " " +
					composingEnd);
		}

		@Override
		public void onUpdateExtractedText(int token, Window window) {
			extracted.add(token + " " + window);
		}
	};
	private Document document;
	private TextInput input;

	@Test
	public void commitReplacesTheSelectionAndPutsTheCaretBehind() {
		open("ab{cd}e");
		assertTrue(input.commitText("X", 1));
		assertMarked("abX|e");
		assertEquals(List.of("3 3 -1 -1"), told);
	}

	@Test
	public void commitPositionCountsFromTheEndOrBackFromTheStart() {
		open("a|b");
		input.commitText("XY", 2);
		assertMarked("aXYb|");
		open("a|b");
		input.commitText("XY", 0);
		assertMarked("a|XYb");
		open("a|b");
		input.commitText("XY", -1);
		assertMarked("|aXYb");
	}

	@Test
	public void caretPositionIsClampedToTheText() {
		open("a|b");
		input.commitText("X", Integer.MAX_VALUE);
		assertMarked("aXb|");
		input.commitText("Y", Integer.MIN_VALUE);
		assertMarked("|aXbY");
	}

	@Test
	public void composingTextReplacesTheLastComposingText() {
		open("x|");
		input.setComposingText("hel", 1);
		assertMarked("x[hel]|");
		input.setComposingText("hello world", 1);
		assertMarked("x[hello world]|");
		input.setComposingText("he", 1);
		assertMarked("x[he]|");
		input.setComposingText("", 1);
		assertMarked("x|");
		assertEquals(List.of("4 4 1 4", "12 12 1 12", "3 3 1 3", "1 1 -1 -1"),
				told);
	}

	@Test
	public void commitReplacesTheCompositionWithOtherText() {
		open("|;");
		input.setComposingText("teh", 1);
		input.commitText("the", 1);
		assertMarked("the|;");
		assertEquals(List.of("3 3 0 3", "3 3 -1 -1"), told);
	}

	@Test
	public void composingRegionMarksExistingTextWithoutChangingIt() {
		open("foo bar|");
		int revision = document.revision();
		input.setComposingRegion(4, 7);
		assertMarked("foo [bar]|");
		assertEquals(revision, document.revision());
		input.setComposingText("baz", 1);
		assertMarked("foo [baz]|");
		assertEquals(List.of("7 7 4 7"), told);
	}

	@Test
	public void composingRegionIsOrderedAndClamped() {
		open("ab|c");
		input.setComposingRegion(9, -1);
		assertMarked("[ab|c]");
		input.setComposingRegion(1, 1);
		assertMarked("ab|c");
	}

	@Test
	public void finishComposingKeepsTheText() {
		open("|");
		input.setComposingText("ab", 1);
		assertTrue(input.finishComposingText());
		assertMarked("ab|");
		assertTrue(input.finishComposingText());
		assertEquals(List.of("2 2 0 2", "2 2 -1 -1"), told);
	}

	@Test
	public void selectionMovedByTheAppKeepsTheComposition() {
		open("foo [bar]|");
		document.select(1, 1);
		assertMarked("f|oo [bar]");
		assertEquals(List.of("1 1 4 7"), told);
		input.setComposingText("baz", 1);
		assertMarked("foo [baz]|");
	}

	@Test
	public void textChangedByTheAppFinishesComposing() {
		open("foo [bar]|");
		document.replace(0, 0, Text.of("//"));
		assertMarked("//foo bar|");
		assertEquals(List.of("9 9 -1 -1"), told);
	}

	@Test
	public void deleteSparesTheSelection() {
		open("ab{cd}ef");
		assertTrue(input.deleteSurroundingText(1, 1));
		assertMarked("a{cd}f");
	}

	@Test
	public void deleteSparesTheComposition() {
		open("ab[cd]|ef");
		input.deleteSurroundingText(1, 1);
		assertMarked("a[cd]|f");
	}

	@Test
	public void deleteReachesAcrossALineEnd() {
		open("ab\n|cd");
		input.deleteSurroundingText(2, 0);
		assertMarked("a|cd");
		open("ab|\ncd");
		input.deleteSurroundingText(0, 2);
		assertMarked("ab|d");
	}

	@Test
	public void deleteNeverSplitsASurrogatePair() {
		open("a" + PAIR + "|b");
		input.deleteSurroundingText(1, 0);
		assertMarked("a|b");
		open("a|" + PAIR + "b");
		input.deleteSurroundingText(0, 1);
		assertMarked("a|b");
	}

	@Test
	public void deleteOfNothingInsideAPairDeletesNothing() {
		open(PAIR);
		input.setSelection(1, 1);
		assertTrue(input.deleteSurroundingText(0, 0));
		assertEquals(PAIR, document.text().toString());
	}

	@Test
	public void deleteFromInsideAPairDeletesNothing() {
		open("a" + PAIR + "b");
		input.setSelection(2, 2);
		input.deleteSurroundingText(1, 0);
		input.deleteSurroundingText(0, 1);
		input.setComposingRegion(2, 4);
		input.setSelection(4, 4);
		input.deleteSurroundingText(1, 1);
		assertEquals("a" + PAIR + "b", document.text().toString());
	}

	@Test
	public void deleteInCodePointsOfHalfAPairDeletesNothing() {
		open("a\uD800|b");
		assertTrue(input.deleteSurroundingTextInCodePoints(1, 1));
		assertMarked("a\uD800|b");
		open("a|\uDC00b");
		input.deleteSurroundingTextInCodePoints(1, 1);
		assertMarked("a|\uDC00b");
	}

	@Test
	public void longDeleteInCodePointsCountsPairsAcrossItsReads() {
		String pairs = PAIR.repeat(3000);
		open("x" + pairs + "a|b" + pairs + "y");
		input.deleteSurroundingTextInCodePoints(3000, 3000);
		assertMarked("x" + PAIR + "|" + PAIR + "y");
	}

	@Test
	public void deleteInCodePointsCountsAPairAsOne() {
		open("a" + PAIR + PAIR + "|" + PAIR + "b");
		assertTrue(input.deleteSurroundingTextInCodePoints(2, 1));
		assertMarked("a|b");
	}

	@Test
	public void deleteStopsAtTheEndsOfTheText() {
		open("ab|cd");
		input.deleteSurroundingText(Integer.MAX_VALUE, Integer.MAX_VALUE);
		assertMarked("|");
		open("ab|cd");
		input.deleteSurroundingTextInCodePoints(Integer.MAX_VALUE, -1);
		assertMarked("|cd");
	}

	@Test
	public void setSelectionKeepsItsDirectionAndIgnoresRangesOutside() {
		open("abc|");
		input.setSelection(3, 1);
		assertEquals(new Selection(3, 1), document.selection());
		input.setSelection(0, 4);
		assertEquals(new Selection(3, 1), document.selection());
		assertEquals(List.of("3 1 -1 -1"), told);
	}

	@Test
	public void readsReturnTheTextAroundTheSelection() {
		open("ab{cd}ef");
		assertEquals("b", input.getTextBeforeCursor(1));
		assertEquals("ef", input.getTextAfterCursor(9));
		assertEquals("cd", input.getSelectedText());
		assertEquals(new Window(1, 1, 3, "bcde"),
				input.getSurroundingText(1, 1));
		open("ab|");
		assertNull(input.getSelectedText());
		assertEquals("", input.getTextAfterCursor(5));
	}

	@Test
	public void readsAreBoundedHoweverLargeTheText() {
		open("x\n".repeat(500_000) + "|" + "y\n".repeat(500_000));
		assertEquals(MAX_READ,
				input.getTextBeforeCursor(Integer.MAX_VALUE).length());
		assertEquals(MAX_READ,
				input.getTextAfterCursor(Integer.MAX_VALUE).length());
		Window window = input.getSurroundingText(Integer.MAX_VALUE,
				Integer.MAX_VALUE);
		assertEquals(1_000_000 - MAX_READ, window.offset());
		assertEquals(2 * MAX_READ, window.text().length());
	}

	@Test
	public void readsThatWouldCutTheSelectionDecline() {
		open("{" + "x".repeat(5000) + "}");
		assertNull(input.getSelectedText());
		assertNull(input.getSurroundingText(0, 0));
		Window window = input.getExtractedText(1, false);
		assertEquals(0, window.offset());
		assertEquals(0, window.selectionStart());
		assertEquals(5000, window.selectionEnd());
		assertEquals(MAX_READ, window.text().length());
	}

	@Test
	public void extractedTextKeepsTheDirectionOfTheSelection() {
		open("ab}cde{f");
		assertEquals(new Window(0, 5, 2, "abcdef"),
				input.getExtractedText(1, false));
		assertEquals(new Window(0, 2, 5, "abcdef"),
				input.getSurroundingText(9, 9));
	}

	@Test
	public void extractedTextFollowsChangesWhenMonitored() {
		open("ab|");
		assertEquals(new Window(0, 2, 2, "ab"),
				input.getExtractedText(7, false));
		input.commitText("c", 1);
		assertEquals(List.of(), extracted);
		input.getExtractedText(7, true);
		input.commitText("d", 1);
		assertEquals(List.of("7 " + new Window(0, 4, 4, "abcd")), extracted);
	}

	@Test
	public void keyboardIsToldOnlyWhatChanged() {
		open("a|bc");
		input.setSelection(1, 1);
		input.finishComposingText();
		input.setComposingRegion(1, 1);
		document.replace(2, 3, Text.of("d"));
		assertEquals(List.of(), told);
		input.commitText("x", 1);
		assertEquals(List.of("2 2 -1 -1"), told);
	}

	@Test
	public void nestedBatchesAreToldOnceAtTheOutermostEnd() {
		open("|");
		assertTrue(input.beginBatchEdit());
		assertTrue(input.beginBatchEdit());
		input.commitText("ab", 1);
		input.setSelection(1, 1);
		assertTrue(input.endBatchEdit());
		assertEquals(List.of(), told);
		assertFalse(input.endBatchEdit());
		assertEquals(List.of("1 1 -1 -1"), told);
		assertFalse(input.endBatchEdit());
	}

	@Test
	public void appEditFromAnUpdateFinishesComposing() {
		open("|");
		document.addListener(new Document.Listener() {
			private boolean once;

			@Override
			public void onUpdate() {
				if (!once) {
					once = true;
					document.replace(0, 1, Text.of("X"));
				}
			}
		});
		input.setComposingText("ab", 1);
		assertMarked("Xb|");
		assertEquals(List.of("2 2 -1 -1"), told.subList(told.size() - 1,
				told.size()));
	}

	@Test
	public void callWhileTheKeyboardIsToldIsToldAfter() {
		Document document = new Document(Text.of("abc"));
		List<String> selections = new ArrayList<>();
		TextInput[] input = new TextInput[1];
		input[0] = new TextInput(document, new TextInput.Listener() {
			private boolean once;

			@Override
			public void onUpdateSelection(int selStart, int selEnd,
					int composingStart, int composingEnd) {
				selections.add(selStart + " " + selEnd);
			}

			@Override
			public void onUpdateExtractedText(int token, Window window) {
				if (!once) {
					once = true;
					input[0].setSelection(2, 2);
				}
			}
		});
		input[0].getExtractedText(1, true);
		input[0].setSelection(1, 1);
		assertEquals(List.of("1 1", "2 2"), selections);
	}

	@Test
	public void appChangeDuringABatchIsToldAtItsEnd() {
		open("[ab]|");
		input.beginBatchEdit();
		document.replace(0, 0, Text.of("x"));
		assertEquals(List.of(), told);
		input.endBatchEdit();
		assertEquals(List.of("3 3 -1 -1"), told);
	}

	@Test
	public void closedConnectionEndsItsBatchesAndIgnoresCalls() {
		open("|");
		int[] updates = {0};
		document.addListener(new Document.Listener() {
			@Override
			public void onUpdate() {
				++updates[0];
			}
		});
		input.beginBatchEdit();
		input.setComposingText("ab", 1);
		input.closeConnection();
		assertEquals(1, updates[0]);
		assertEquals(-1, input.composingStart());
		assertFalse(input.commitText("c", 1));
		assertTrue(input.finishComposingText());
		assertNull(input.getTextBeforeCursor(1));
		document.select(0, 0);
		assertMarked("|ab");
		assertEquals(List.of(), told);
	}

	@Test
	public void incomingTextIsCleaned() {
		open("|");
		input.commitText("a\r\nb", 1);
		assertMarked("a\nb|");
		input.setComposingText("x y", 1);
		assertMarked("a\nb[x y]|");
		input.commitText(null, 1);
		assertMarked("a\nb|");
	}

	@Test
	public void composedWordJoinsTheRunOfTyping() {
		open("|");
		input.commitText("a", 1);
		input.setComposingText("b", 1);
		input.setComposingText("bcx", 1);
		input.commitText("bcd", 1);
		input.commitText(" ", 1);
		assertMarked("abcd |");
		document.undo();
		assertMarked("|");
		assertFalse(document.canUndo());
	}

	@Test
	public void backspaceJoinsTheRunOfTyping() {
		open("|");
		input.commitText("a", 1);
		input.commitText(PAIR, 1);
		input.deleteSurroundingText(1, 0);
		input.commitText("b", 1);
		assertMarked("ab|");
		document.undo();
		assertMarked("|");
		assertFalse(document.canUndo());
	}

	@Test
	public void longerDeleteIsAStepOfItsOwn() {
		open("|");
		input.commitText("t", 1);
		input.commitText("e", 1);
		input.commitText("h", 1);
		input.beginBatchEdit();
		input.deleteSurroundingText(3, 0);
		input.commitText("the", 1);
		input.endBatchEdit();
		assertMarked("the|");
		document.undo();
		assertMarked("teh|");
		document.undo();
		assertMarked("|");
	}

	@Test
	public void keyboardPasteIsTypingButAPasteOfTheViewIsNot() {
		open("|");
		input.commitText("a", 1);
		input.commitText("pasted", 1);
		document.undo();
		assertMarked("|");
		input.commitText("a", 1);
		document.replace(1, 1, Text.of("pasted"));
		document.undo();
		assertMarked("a|");
	}

	@Test
	public void committedLinesAreAStepOfTheirOwn() {
		open("|");
		input.commitText("a", 1);
		input.commitText("\n", 1);
		input.commitText("x\ny", 1);
		document.undo();
		assertMarked("a\n|");
		document.undo();
		assertMarked("|");
	}

	@Test
	public void recomposedWordIsUndoneToItsOldText() {
		open("teh|");
		input.setComposingRegion(0, 3);
		input.setComposingText("the", 1);
		input.finishComposingText();
		document.undo();
		assertMarked("teh|");
	}

	@Test
	public void recommittedUnchangedCompositionIsNoEdit() {
		open("|abc");
		int revision = document.revision();
		input.setComposingRegion(0, 3);
		input.commitText("abc", 1);
		assertMarked("abc|");
		assertEquals(revision, document.revision());
		assertFalse(document.canUndo());
	}

	@Test
	public void undoWhileComposingFinishesIt() {
		open("|");
		input.setComposingText("ab", 1);
		told.clear();
		document.undo();
		assertMarked("|");
		assertEquals(List.of("0 0 -1 -1"), told);
	}

	@Test
	public void refusedTextChangesNothingAndTellsTheKeyboardSo() {
		document = new Document(Text.of("x".repeat(
				(int) Document.MAX_BYTES - 1)), () -> 0);
		document.select(3, 3);
		input = new TextInput(document, listener);
		input.setComposingRegion(1, 3);
		List<String> refused = new ArrayList<>();
		document.addListener(new Document.Listener() {
			@Override
			public void onRefused(String reason) {
				refused.add(reason);
			}
		});
		told.clear();
		int revision = document.revision();

		assertTrue(input.commitText("yyyy", 1));
		assertTrue(input.setComposingText("yyyy", 1));

		assertEquals(revision, document.revision());
		assertEquals(new Selection(3, 3), document.selection());
		assertEquals(1, input.composingStart());
		assertEquals(3, input.composingEnd());
		assertEquals(List.of("3 3 1 3", "3 3 1 3"), told);
		assertEquals(2, refused.size());
		told.clear();
		assertTrue(input.commitText("yyy", 1));
		assertEquals(List.of("4 4 -1 -1"), told);
	}

	private void open(String marked) {
		StringBuilder text = new StringBuilder();
		int anchor = 0;
		int head = 0;
		int start = -1;
		int end = -1;
		for (char c : marked.toCharArray()) {
			switch (c) {
				case '|' -> anchor = head = text.length();
				case '{' -> anchor = text.length();
				case '}' -> head = text.length();
				case '[' -> start = text.length();
				case ']' -> end = text.length();
				default -> text.append(c);
			}
		}
		document = new Document(Text.of(text), () -> 0);
		document.select(anchor, head);
		input = new TextInput(document, listener);
		if (start > -1) {
			input.setComposingRegion(start, end);
		}
		told.clear();
		extracted.clear();
	}

	private void assertMarked(String expected) {
		String text = document.text().toString();
		Selection selection = document.selection();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i <= text.length(); ++i) {
			if (i == input.composingEnd()) {
				sb.append(']');
			}
			if (selection.isEmpty() && i == selection.head()) {
				sb.append('|');
			} else if (i == selection.end()) {
				sb.append('}');
			} else if (i == selection.start()) {
				sb.append('{');
			}
			if (i == input.composingStart()) {
				sb.append('[');
			}
			if (i < text.length()) {
				sb.append(text.charAt(i));
			}
		}
		assertEquals(expected, sb.toString());
	}
}
