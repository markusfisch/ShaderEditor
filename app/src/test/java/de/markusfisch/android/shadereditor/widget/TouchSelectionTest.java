package de.markusfisch.android.shadereditor.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.markusfisch.android.shadereditor.editor.Document;
import de.markusfisch.android.shadereditor.editor.Document.Selection;
import de.markusfisch.android.shadereditor.editor.Motion;
import de.markusfisch.android.shadereditor.editor.Text;

public class TouchSelectionTest {
	private final TouchSelection touch = new TouchSelection(new Motion());

	@Test
	public void aPointInsideACharacterMeansItsNearerEnd() {
		// An e with an accent, two units, is one character.
		Text text = Text.of("line\naéx");
		// A layout that puts every unit 10 pixels wide and knows nothing
		// of characters.
		TouchSelection.Line layout = new TouchSelection.Line() {
			@Override
			public float xOf(int offset) {
				return offset * 10;
			}

			@Override
			public int offsetAt(float x) {
				return Math.round(x / 10);
			}
		};
		assertEquals(6, touch.offsetAt(text, 1, 14, layout));
		assertEquals(8, touch.offsetAt(text, 1, 21, layout));
		assertEquals(5, touch.offsetAt(text, 1, -30, layout));
		assertEquals(9, touch.offsetAt(text, 1, 500, layout));
	}

	@Test
	public void aDoubleTapSelectsTheWord() {
		Text text = Text.of("float value = 1.0;");
		touch.dragWords(text, 8);
		assertEquals(6, touch.anchor());
		assertEquals(11, touch.head());
	}

	@Test
	public void aDragByWordKeepsTheFirstWordAndTakesWholeWords() {
		Text text = Text.of("vec3 color = mix(a, b);\nreturn color;");
		touch.dragWords(text, 7);
		assertEquals(5, touch.anchor());
		assertEquals(10, touch.head());
		assertFalse(touch.dragTo(text, 8));
		assertTrue(touch.dragTo(text, 15));
		assertEquals(5, touch.anchor());
		assertEquals(16, touch.head());
		assertTrue(touch.dragTo(text, 27));
		assertEquals(30, touch.head());
		assertTrue(touch.dragTo(text, 2));
		assertEquals(10, touch.anchor());
		assertEquals(0, touch.head());
	}

	@Test
	public void aHandleMovesItsEndAndNeverEmptiesTheSelection() {
		Text text = Text.of("float a;");
		touch.dragHandle(2, 5);
		assertTrue(touch.dragTo(text, 0));
		assertEquals(2, touch.anchor());
		assertEquals(0, touch.head());
		assertFalse(touch.dragTo(text, 2));
		assertEquals(0, touch.head());
		// The caret's handle moves the caret.
		touch.dragHandle(3, 3);
		assertTrue(touch.dragTo(text, 7));
		assertEquals(7, touch.anchor());
		assertEquals(7, touch.head());
	}

	@Test
	public void aHandleTakesAWordWholeOnceTheFingerIsPastItsMiddle() {
		Text text = Text.of("float alpha = beta;");
		touch.dragHandle(0, 5);
		// Into the first half of alpha: up to its start.
		assertTrue(touch.dragTo(text, 7));
		assertEquals(6, touch.head());
		// Past its middle: all of it.
		assertTrue(touch.dragTo(text, 9));
		assertEquals(11, touch.head());
		assertFalse(touch.dragTo(text, 10));
	}

	@Test
	public void aHandleMovedBackGoesByCharacterUntilAWordEnds() {
		Text text = Text.of("float alpha = beta;");
		touch.dragHandle(0, 5);
		touch.dragTo(text, 9);
		assertEquals(11, touch.head());
		// Back by two, keeping the distance to the finger.
		assertTrue(touch.dragTo(text, 7));
		assertEquals(9, touch.head());
		// Forward again by character, not to the end of the word.
		assertTrue(touch.dragTo(text, 8));
		assertEquals(10, touch.head());
		assertTrue(touch.dragTo(text, 9));
		assertEquals(11, touch.head());
		// At the end of a word, the next one joins whole again.
		assertTrue(touch.dragTo(text, 16));
		assertEquals(18, touch.head());
	}


	@Test
	public void handlesWaitForTheFingerToLiftAfterAWordDrag() {
		Text text = Text.of("float alpha = beta;");
		assertTrue(touch.showsHandles(false));
		touch.dragWords(text, 7);
		touch.dragTo(text, 16);
		assertFalse(touch.showsHandles(true));
		assertTrue(touch.showsHandles(false));
	}

	@Test
	public void aDraggedHandleShows() {
		touch.dragHandle(0, 5);
		assertTrue(touch.showsHandles(true));
		touch.dragHandle(3, 3);
		assertTrue(touch.showsHandles(true));
	}

	@Test
	public void theHandlesHideOnceTheTextChangesEvenInsideABatch() {
		// As a keyboard's batch edit that replaces the whole text: a
		// frame may draw before the batch ends.
		Document document = new Document(Text.of("float a;\nfloat b;"));
		document.select(17, 17);
		Selection touched = document.selection();
		assertSame(touched, touch.handles(touched, document.selection(),
				false));
		document.beginBatch();
		document.replace(0, 17, Text.EMPTY);
		assertNull(touch.handles(touched, document.selection(), false));
		document.replace(0, 0, Text.of("x"));
		assertNull(touch.handles(touched, document.selection(), false));
		document.endBatch();
		assertNull(touch.handles(touched, document.selection(), false));
	}

	@Test
	public void theHandlesHideAfterAChangeThatLeavesTheirOffsets() {
		Document document = new Document(Text.of("float a;"));
		document.select(0, 5);
		Selection touched = document.selection();
		document.replace(8, 8, Text.of(" // a"));
		assertEquals(touched, document.selection());
		assertNull(touch.handles(touched, document.selection(), false));
	}

	@Test
	public void aDraggedHandleGoesOnInTheTextAChangeLeft() {
		Document document = new Document(
				Text.of("vec3 a;\nfloat value = 1.0;"));
		document.addListener(new Document.Listener() {
			@Override
			public void onChange(Document.Change change, Text before) {
				touch.map(change);
			}
		});
		// The end handle of "value = 1.0;" at the end of the text.
		touch.dragHandle(14, 26);
		// The whole text replaced by a shorter one while the finger is
		// down.
		document.replace(0, 26, Text.of("float b;"));
		// Both ends behind the new text, as the document's selection.
		assertTrue(touch.dragTo(document.text(), 2));
		assertEquals(8, touch.anchor());
		assertEquals(0, touch.head());
	}

	@Test
	public void aDragByWordKeepsItsWordWhenTextIsInsertedBeforeIt() {
		Document document = new Document(Text.of("float alpha = beta;"));
		document.addListener(new Document.Listener() {
			@Override
			public void onChange(Document.Change change, Text before) {
				touch.map(change);
			}
		});
		touch.dragWords(document.text(), 7);
		document.replace(0, 0, Text.of("vec2 b;\n"));
		assertEquals(14, touch.anchor());
		assertEquals(19, touch.head());
		assertTrue(touch.dragTo(document.text(), 24));
		assertEquals(14, touch.anchor());
		assertEquals(26, touch.head());
	}

	@Test
	public void theMagnifierStaysOverTheLineAndHidesFarFromIt() {
		assertEquals(50, TouchSelection.magnifierX(50, 20, 200, 40), 0);
		assertEquals(20, TouchSelection.magnifierX(0, 20, 200, 40), 0);
		assertEquals(200, TouchSelection.magnifierX(230, 20, 200, 40), 0);
		assertTrue(Float.isNaN(TouchSelection.magnifierX(241, 20, 200, 40)));
		assertTrue(Float.isNaN(TouchSelection.magnifierX(-21, 20, 200, 40)));
	}
}
