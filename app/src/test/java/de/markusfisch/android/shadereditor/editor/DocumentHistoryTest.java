package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.editor.Document.Change;
import de.markusfisch.android.shadereditor.editor.Document.Selection;

public class DocumentHistoryTest {
	private final long[] now = {0};




	@Test
	public void undoToTheStartRestoresTheFirstText() {
		Document document = document("first\ntext\n");
		edit(document);
		String last = document.text().toString();
		int undone = 0;
		while (document.canUndo()) {
			document.undo();
			++undone;
		}
		assertEquals("first\ntext\n", document.text().toString());
		for (int i = 0; i < undone; ++i) {
			document.redo();
		}
		assertEquals(last, document.text().toString());
	}

	@Test
	public void restoredHistoryUndoesAndRedoesAsTheOriginal() {
		Document original = document("start\nof the text\n");
		edit(original);
		original.undo();
		Document restored = document(original.text().toString());

		assertTrue(restored.restore(original.history()));
		assertFalse(restored.restore(original.history()));

		assertEquals(original.selection(), restored.selection());
		while (original.canUndo()) {
			assertTrue(restored.canUndo());
			original.undo();
			restored.undo();
			assertSame(original, restored);
		}
		assertFalse(restored.canUndo());
		while (original.canRedo()) {
			original.redo();
			restored.redo();
			assertSame(original, restored);
		}
		assertFalse(restored.canRedo());
		original.replace(0, 0, Text.of("x"));
		restored.replace(0, 0, Text.of("x"));
		original.undo();
		restored.undo();
		assertSame(original, restored);
	}

	@Test
	public void historyIsNotTakenWhereItsSelectionCannotBe() {
		Document original = document("a longer text");
		original.select(10, 10);
		original.replace(10, 10, Text.of("!"));

		assertFalse(document("short").restore(original.history()));
	}

	// Asserts that b holds what a holds.
	private static void assertSame(Document a, Document b) {
		assertEquals(a.text().toString(), b.text().toString());
		assertEquals(a.selection(), b.selection());
	}

	@Test
	public void redoReturnsWhatUndoTook() {
		Document document = document("ab");
		document.select(2, 2);
		document.replace(2, 2, Text.of("cd"));
		document.select(0, 1);
		document.replace(0, 1, Text.of("X"));
		document.undo();
		assertEquals("abcd", document.text().toString());
		assertEquals(new Selection(0, 1), document.selection());
		document.undo();
		assertEquals("ab", document.text().toString());
		assertEquals(new Selection(2, 2), document.selection());
		assertFalse(document.canUndo());
		document.redo();
		assertEquals("abcd", document.text().toString());
		assertEquals(new Selection(4, 4), document.selection());
		document.redo();
		assertEquals("Xbcd", document.text().toString());
		assertEquals(new Selection(1, 1), document.selection());
		assertFalse(document.canRedo());
	}

	@Test
	public void runOfTypingIsOneStep() {
		Document document = document("x");
		document.select(1, 1);
		type(document, "abc");
		backspace(document);
		type(document, "d");
		assertEquals("xabd", document.text().toString());
		document.undo();
		assertEquals("x", document.text().toString());
		assertEquals(new Selection(1, 1), document.selection());
		assertFalse(document.canUndo());
		document.redo();
		assertEquals("xabd", document.text().toString());
		assertEquals(new Selection(4, 4), document.selection());
	}

	@Test
	public void deletingBeforeTheRunJoinsIt() {
		Document document = document("xy");
		document.select(2, 2);
		type(document, "a");
		backspace(document);
		backspace(document);
		document.type(1, 1, Text.of("b"));
		assertEquals("xb", document.text().toString());
		document.undo();
		assertEquals("xy", document.text().toString());
		assertFalse(document.canUndo());
	}

	@Test
	public void pauseEndsTheRun() {
		Document document = document("");
		type(document, "ab");
		now[0] += 999;
		type(document, "c");
		now[0] += 1000;
		type(document, "d");
		document.undo();
		assertEquals("abc", document.text().toString());
		document.undo();
		assertEquals("", document.text().toString());
	}

	@Test
	public void movingTheCaretEndsTheRun() {
		Document document = document("");
		type(document, "ab");
		document.select(0, 0);
		document.select(2, 2);
		type(document, "c");
		document.undo();
		assertEquals("ab", document.text().toString());
	}

	@Test
	public void typingElsewhereStartsAStep() {
		Document document = document("0123456789");
		document.type(2, 2, Text.of("a"));
		document.type(8, 8, Text.of("b"));
		document.undo();
		assertEquals("01a23456789", document.text().toString());
	}

	@Test
	public void otherEditsEndTheRun() {
		Document document = document("");
		type(document, "ab");
		document.replace(2, 2, Text.of("PASTE"));
		type(document, "c");
		assertEquals("abPASTEc", document.text().toString());
		document.undo();
		assertEquals("abPASTE", document.text().toString());
		document.undo();
		assertEquals("ab", document.text().toString());
		document.undo();
		assertEquals("", document.text().toString());
	}

	@Test
	public void undoEndsTheRun() {
		Document document = document("");
		type(document, "ab");
		document.undo();
		type(document, "c");
		type(document, "d");
		document.undo();
		assertEquals("", document.text().toString());
		document.redo();
		assertEquals("cd", document.text().toString());
	}

	@Test
	public void batchIsOneStep() {
		Document document = document("one two");
		document.beginBatch();
		document.replace(0, 3, Text.of("1"));
		document.replace(2, 5, Text.of("2"));
		document.select(0, 3);
		document.endBatch();
		assertEquals("1 2", document.text().toString());
		document.undo();
		assertEquals("one two", document.text().toString());
		assertFalse(document.canUndo());
		document.redo();
		assertEquals(new Selection(0, 3), document.selection());
	}

	@Test
	public void typingInBatchesContinuesTheRun() {
		Document document = document("");
		for (char c : "abc".toCharArray()) {
			document.beginBatch();
			type(document, String.valueOf(c));
			document.endBatch();
		}
		document.undo();
		assertEquals("", document.text().toString());
	}

	@Test
	public void correctionInABatchIsAStepOfItsOwn() {
		Document document = document("");
		type(document, "teh");
		document.beginBatch();
		document.replace(0, 3, Text.EMPTY);
		document.type(0, 0, Text.of("the"));
		document.endBatch();
		type(document, "y");
		document.undo();
		assertEquals("the", document.text().toString());
		document.undo();
		assertEquals("teh", document.text().toString());
	}

	@Test
	public void batchWithAnEditLeavesTheTypingBeforeIt() {
		Document document = document("");
		type(document, "a");
		document.beginBatch();
		type(document, "b");
		document.replace(0, 1, Text.of("A"));
		document.endBatch();
		document.undo();
		assertEquals("a", document.text().toString());
	}

	@Test
	public void undoInsideABatch() {
		Document document = document("");
		Events events = new Events(document);
		document.replace(0, 0, Text.of("ab"));
		events.updates = 0;
		document.beginBatch();
		document.type(2, 2, Text.of("c"));
		document.undo();
		document.type(0, 0, Text.of("x"));
		document.endBatch();
		assertEquals("xab", document.text().toString());
		assertEquals(1, events.updates);
		document.undo();
		assertEquals("ab", document.text().toString());
		document.undo();
		assertEquals("", document.text().toString());
	}

	@Test
	public void listenersHearUndoAsChanges() {
		Document document = document("abc");
		document.beginBatch();
		document.replace(0, 1, Text.of("X"));
		document.replace(3, 3, Text.of("Y"));
		document.endBatch();
		Events events = new Events(document);
		int revision = document.revision();
		document.undo();
		assertEquals(List.of("3 -Y +", "0 -X +a"), events.changes);
		assertEquals(1, events.updates);
		assertEquals(revision + 2, document.revision());
		document.redo();
		assertEquals(List.of("3 -Y +", "0 -X +a", "0 -a +X", "3 - +Y"),
				events.changes);
	}






	@Test
	public void historyKeepsAThousandSteps() {
		Document document = document("");
		for (int i = 0; i < 1500; ++i) {
			document.replace(i, i, Text.of("x"));
		}
		int steps = 0;
		while (document.canUndo()) {
			document.undo();
			++steps;
		}
		assertEquals(1000, steps);
		assertEquals(500, document.text().length());
	}

	@Test
	public void historyKeepsFourMillionCharacters() {
		Document document = document("");
		Text million = Text.of("y".repeat(1_000_000));
		for (int i = 0; i < 6; ++i) {
			document.replace(0, 0, million);
		}
		int steps = 0;
		while (document.canUndo()) {
			document.undo();
			++steps;
		}
		assertEquals(4, steps);
		assertEquals(2_000_000, document.text().length());
	}

	@Test
	public void newestStepIsKeptWhateverItsSize() {
		Document document = document("abc");
		document.replace(0, 3, Text.of("z".repeat(5_000_000)));
		assertTrue(document.canUndo());
		document.undo();
		assertEquals("abc", document.text().toString());
	}






	private Document document(String text) {
		return new Document(Text.of(text), () -> now[0]);
	}

	// Typing with pauses, a correction, a paste, a batch and undo and redo.
	private void edit(Document document) {
		document.select(5, 5);
		type(document, " line");
		now[0] += 2000;
		type(document, "s\nmore");
		backspace(document);
		document.replace(0, 5, Text.of("pasted\ntext"));
		document.beginBatch();
		document.replace(0, 0, Text.of("a"));
		document.select(3, 8);
		document.type(3, 8, Text.of("b"));
		document.endBatch();
		document.undo();
		document.redo();
		now[0] += 2000;
		document.select(document.text().length(), document.text().length());
		type(document, "end");
	}

	private static void type(Document document, String typed) {
		for (char c : typed.toCharArray()) {
			Selection selection = document.selection();
			document.type(selection.start(), selection.end(),
					Text.of(String.valueOf(c)));
		}
	}

	private static void backspace(Document document) {
		int caret = document.selection().head();
		document.type(caret - 1, caret, Text.EMPTY);
	}

	private static final class Events implements Document.Listener {
		private final List<String> changes = new ArrayList<>();
		private int updates;

		private Events(Document document) {
			document.addListener(this);
		}

		@Override
		public void onChange(Change change, Text before) {
			changes.add(change.start() + " -" + change.removed() + " +" +
					change.inserted());
		}

		@Override
		public void onUpdate() {
			++updates;
		}
	}
}
