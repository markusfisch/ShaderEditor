package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.editor.Document.Change;
import de.markusfisch.android.shadereditor.editor.Document.Selection;

public class DocumentTest {
	@Test
	public void startsWithItsText() {
		Text text = Text.of("float x;");
		Document document = new Document(text);
		assertSame(text, document.text());
		assertEquals(0, document.revision());
		assertEquals(new Selection(0, 0), document.selection());
	}


	@Test
	public void replaceTellsWhatChanged() {
		Text text = Text.of("ab\ncd\nef");
		Document document = new Document(text);
		Events events = new Events(document);
		document.replace(1, 7, Text.of("XY"));
		assertEquals("aXYf", document.text().toString());
		assertEquals(1, document.revision());
		assertEquals(List.of("change 1 -b\\ncd\\ne +XY", "update"), events.log);
		assertSame(text, events.befores.get(0));
		Change change = events.changes.get(0);
		// The lines a change touches follow from it without a search.
		assertEquals(0, text.lineOf(change.start()));
		assertEquals(2, change.removed().lineCount() - 1);
		assertEquals(0, change.inserted().lineCount() - 1);
	}

	@Test
	public void listenersSeeTheTextAfterTheChange() {
		Document document = new Document(Text.of("ab"));
		List<String> seen = new ArrayList<>();
		document.addListener(new Document.Listener() {
			@Override
			public void onChange(Change change, Text before) {
				seen.add(before + ">" + document.text());
			}
		});
		document.replace(1, 1, Text.of("x"));
		assertEquals(List.of("ab>axb"), seen);
	}

	@Test
	public void refusesAnEditThatWouldGrowTheTextPastTheLimit() {
		int limit = (int) Document.MAX_BYTES;
		Document document = new Document(Text.of("x".repeat(limit - 2)));
		List<String> refused = new ArrayList<>();
		document.addListener(new Document.Listener() {
			@Override
			public void onRefused(String reason) {
				refused.add(reason);
			}
		});

		assertTrue(document.type(0, 0, Text.of("\u00e9")));
		assertFalse(document.type(0, 0, Text.of("y")));

		assertEquals(List.of("the text would be larger than 16 MiB"),
				refused);
		assertEquals(limit - 1, document.text().length());
		assertEquals(1, document.revision());
		assertTrue(document.replace(0, 1, Text.of("z")));
		assertTrue(document.type(0, 0, Text.of("y")));
		assertEquals(1, refused.size());
		assertEquals(8, Document.bytes(Text.of("a\n\u00e9\uD83D\uDE00")));
	}

	@Test
	public void textOverTheLimitMayShrinkButNotGrow() {
		Document document = new Document(Text.of("x".repeat(
				(int) Document.MAX_BYTES + 2)));

		assertTrue(document.replace(0, 1, Text.EMPTY));
		assertFalse(document.replace(0, 0, Text.of("y")));
		assertTrue(document.replace(0, 1, Text.of("y")));
	}

	@Test
	public void replacingNothingByNothingIsNoChange() {
		Document document = new Document(Text.of("ab"));
		Events events = new Events(document);
		document.replace(1, 1, Text.EMPTY);
		assertEquals(0, document.revision());
		assertTrue(events.log.isEmpty());
	}

	@Test
	public void incomingTextIsCleaned() {
		Document document = new Document(Text.EMPTY);
		document.replace(0, 0, Text.of("a\r\nb\rc\u00a0d"));
		assertEquals("a\nb\nc d", document.text().toString());
		assertEquals(3, document.text().lineCount());
	}

	@Test
	public void changeMapsPositions() {
		Change change = new Change(4, Text.of("abc"), Text.of("XY"));
		assertEquals(3, change.map(3));
		assertEquals(6, change.map(4));
		assertEquals(6, change.map(5));
		assertEquals(6, change.map(7));
		assertEquals(7, change.map(8));
		Change insertion = new Change(4, Text.EMPTY, Text.of("XY"));
		assertEquals(3, insertion.map(3));
		assertEquals(6, insertion.map(4));
		assertEquals(7, insertion.map(5));
	}

	@Test
	public void caretStaysBehindTypedText() {
		Document document = new Document(Text.of("abcd"));
		document.select(2, 2);
		document.replace(2, 2, Text.of("x"));
		assertEquals(new Selection(3, 3), document.selection());
		document.replace(2, 3, Text.EMPTY);
		assertEquals(new Selection(2, 2), document.selection());
	}

	@Test
	public void selectionFollowsEditsElsewhere() {
		Document document = new Document(Text.of("0123456789"));
		document.select(6, 2);
		document.replace(0, 1, Text.of("ab"));
		assertEquals(new Selection(7, 3), document.selection());
		document.replace(9, 10, Text.EMPTY);
		assertEquals(new Selection(7, 3), document.selection());
		// Text inserted at an end of the selection is behind that end.
		document.replace(3, 3, Text.of("x"));
		assertEquals(new Selection(8, 4), document.selection());
		document.replace(8, 8, Text.of("y"));
		assertEquals(new Selection(9, 4), document.selection());
	}

	@Test
	public void replacingTheSelectionLeavesTheCaretBehind() {
		Document document = new Document(Text.of("0123456789"));
		document.select(7, 2);
		document.replace(2, 7, Text.of("x"));
		assertEquals("01x789", document.text().toString());
		assertEquals(new Selection(3, 3), document.selection());
	}

	@Test
	public void positionsHeldElsewhereFollowEdits() {
		Document document = new Document(Text.of("abc\ndef"));
		int[] held = {document.text().toString().indexOf('e')};
		document.addListener(new Document.Listener() {
			@Override
			public void onChange(Change change, Text before) {
				held[0] = change.map(held[0]);
			}
		});
		document.beginBatch();
		document.replace(0, 0, Text.of("12\n"));
		document.replace(6, 8, Text.of("xyz"));
		document.replace(9, 9, Text.of("\n\n"));
		document.endBatch();
		assertEquals("12\nabcxyz\n\nef", document.text().toString());
		assertEquals("e", document.text().substring(held[0], held[0] + 1));
	}

	@Test
	public void batchUpdatesOnceAtTheOutermostEnd() {
		Document document = new Document(Text.of("abc"));
		Events events = new Events(document);
		document.beginBatch();
		document.replace(0, 1, Text.EMPTY);
		document.beginBatch();
		document.replace(0, 0, Text.of("x"));
		document.select(0, 2);
		document.endBatch();
		assertEquals(2, events.log.size());
		document.endBatch();
		assertEquals(List.of("change 0 -a +", "change 0 - +x", "update"),
				events.log);
		assertEquals(2, document.revision());
	}

	@Test
	public void selectingUpdates() {
		Document document = new Document(Text.of("abc"));
		Events events = new Events(document);
		document.select(1, 3);
		assertEquals(new Selection(1, 3), document.selection());
		document.select(1, 3);
		assertEquals(List.of("update"), events.log);
	}

	@Test
	public void emptyBatchDoesNotUpdate() {
		Document document = new Document(Text.of("abc"));
		Events events = new Events(document);
		document.beginBatch();
		document.endBatch();
		assertTrue(events.log.isEmpty());
	}

	@Test
	public void rejectsPositionsOutside() {
		Document document = new Document(Text.of("abc"));
		assertThrows(IndexOutOfBoundsException.class,
				() -> document.replace(2, 4, Text.EMPTY));
		assertThrows(IndexOutOfBoundsException.class,
				() -> document.replace(2, 1, Text.EMPTY));
		assertThrows(IndexOutOfBoundsException.class,
				() -> document.select(0, 4));
		assertThrows(IndexOutOfBoundsException.class,
				() -> document.select(-1, 0));
		assertThrows(IllegalStateException.class, document::endBatch);
		assertEquals(0, document.revision());
		assertEquals("abc", document.text().toString());
	}

	@Test
	public void removedListenerHearsNothing() {
		Document document = new Document(Text.of("abc"));
		Events first = new Events(document);
		Events second = new Events(document);
		document.removeListener(first);
		document.replace(0, 0, Text.of("x"));
		assertTrue(first.log.isEmpty());
		assertEquals(2, second.log.size());
	}

	@Test
	public void listenerMayRemoveItselfWhileNotified() {
		Document document = new Document(Text.of("abc"));
		Document.Listener once = new Document.Listener() {
			@Override
			public void onUpdate() {
				document.removeListener(this);
			}
		};
		document.addListener(once);
		Events events = new Events(document);
		document.replace(0, 0, Text.of("x"));
		document.replace(0, 0, Text.of("y"));
		assertEquals(4, events.log.size());
	}

	private static final class Events implements Document.Listener {
		private final List<String> log = new ArrayList<>();
		private final List<Change> changes = new ArrayList<>();
		private final List<Text> befores = new ArrayList<>();

		private Events(Document document) {
			document.addListener(this);
		}

		@Override
		public void onChange(Change change, Text before) {
			changes.add(change);
			befores.add(before);
			log.add(("change " + change.start() + " -" + change.removed() +
					" +" + change.inserted()).replace("\n", "\\n"));
		}

		@Override
		public void onUpdate() {
			log.add("update");
		}
	}
}
