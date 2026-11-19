package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import org.junit.Test;

import de.markusfisch.android.shadereditor.editor.KeyCommands.Command;

public class KeyCommandsTest {
	private static final int SHIFT = KeyEvent.META_SHIFT_ON;
	private static final int CTRL = KeyEvent.META_CTRL_ON;

	@Test
	public void keysMapToCommands() {
		Object[][] table = {
				{KeyEvent.KEYCODE_DPAD_LEFT, 0, Command.BACKWARD},
				{KeyEvent.KEYCODE_DPAD_LEFT, SHIFT, Command.BACKWARD},
				{KeyEvent.KEYCODE_DPAD_LEFT, CTRL, Command.WORD_BACKWARD},
				{KeyEvent.KEYCODE_DPAD_RIGHT, 0, Command.FORWARD},
				{KeyEvent.KEYCODE_DPAD_RIGHT, CTRL | SHIFT,
						Command.WORD_FORWARD},
				{KeyEvent.KEYCODE_DPAD_UP, 0, Command.LINE_UP},
				{KeyEvent.KEYCODE_DPAD_DOWN, SHIFT, Command.LINE_DOWN},
				{KeyEvent.KEYCODE_DPAD_DOWN, CTRL, null},
				{KeyEvent.KEYCODE_PAGE_UP, 0, Command.PAGE_UP},
				{KeyEvent.KEYCODE_PAGE_DOWN, 0, Command.PAGE_DOWN},
				{KeyEvent.KEYCODE_MOVE_HOME, 0, Command.LINE_START},
				{KeyEvent.KEYCODE_MOVE_HOME, CTRL, Command.START},
				{KeyEvent.KEYCODE_MOVE_END, 0, Command.LINE_END},
				{KeyEvent.KEYCODE_MOVE_END, CTRL | SHIFT, Command.END},
				{KeyEvent.KEYCODE_DEL, 0, Command.DELETE_BACKWARD},
				{KeyEvent.KEYCODE_DEL, CTRL, Command.DELETE_WORD_BACKWARD},
				{KeyEvent.KEYCODE_FORWARD_DEL, 0, Command.DELETE_FORWARD},
				{KeyEvent.KEYCODE_FORWARD_DEL, CTRL,
						Command.DELETE_WORD_FORWARD},
				{KeyEvent.KEYCODE_ENTER, 0, Command.ENTER},
				{KeyEvent.KEYCODE_NUMPAD_ENTER, 0, Command.ENTER},
				{KeyEvent.KEYCODE_TAB, 0, Command.INDENT},
				{KeyEvent.KEYCODE_TAB, SHIFT, Command.UNINDENT},
				{KeyEvent.KEYCODE_TAB, CTRL, null},
				{KeyEvent.KEYCODE_A, CTRL, Command.SELECT_ALL},
				{KeyEvent.KEYCODE_Z, CTRL, Command.UNDO},
				{KeyEvent.KEYCODE_Z, CTRL | SHIFT, Command.REDO},
				{KeyEvent.KEYCODE_Y, CTRL, Command.REDO},
				{KeyEvent.KEYCODE_X, CTRL, Command.CUT},
				{KeyEvent.KEYCODE_C, CTRL, Command.COPY},
				{KeyEvent.KEYCODE_V, CTRL, Command.PASTE},
				{KeyEvent.KEYCODE_A, 0, null},
				{KeyEvent.KEYCODE_S, CTRL, null},
		};
		for (Object[] row : table) {
			assertEquals("key " + row[0] + " meta " + row[1], row[2],
					KeyCommands.command((int) row[0], (int) row[1]));
		}
	}

	@Test
	public void contextMenuActionsMapToCommands() {
		assertEquals(Command.SELECT_ALL,
				KeyCommands.contextMenuCommand(android.R.id.selectAll));
		assertEquals(Command.PASTE,
				KeyCommands.contextMenuCommand(android.R.id.paste));
		assertEquals(Command.REDO,
				KeyCommands.contextMenuCommand(android.R.id.redo));
		assertEquals(null,
				KeyCommands.contextMenuCommand(android.R.id.switchInputMethod));
	}

	@Test
	public void printableKeysTypeUnlessCtrlIsHeld() {
		Document document = new Document(Text.of("ab"));
		document.select(1, 1);
		KeyCommands keys = keys(document, new String[1], true);
		assertTrue(keys.keyDown(KeyEvent.KEYCODE_X, SHIFT, "X", 10));
		assertFalse(keys.keyDown(KeyEvent.KEYCODE_S, CTRL, "s", 10));
		assertFalse(keys.keyDown(KeyEvent.KEYCODE_VOLUME_UP, 0, null, 10));
		assertEquals("aXb", document.text().toString());
	}

	// The Tab key on the screen sends no character; a hardware Tab does.
	@Test
	public void tabIndentsSelectedLinesWithOrWithoutItsCharacter() {
		for (String typed : new String[]{null, "\t"}) {
			Document document = new Document(Text.of("a\nb\nc"));
			document.select(0, 5);
			KeyCommands keys = keys(document, new String[1], true);
			assertTrue(keys.keyDown(KeyEvent.KEYCODE_TAB, 0, typed, 10));
			assertEquals("\ta\n\tb\n\tc", document.text().toString());
			assertTrue(keys.keyDown(KeyEvent.KEYCODE_TAB, SHIFT, typed, 10));
			assertEquals("a\nb\nc", document.text().toString());
		}
	}

	@Test
	public void cutKeepsTheSelectionIfTheClipboardRefusesIt() {
		Document document = new Document(Text.of("abc"));
		document.select(0, 2);
		String[] clip = new String[1];
		keys(document, clip, false).run(Command.CUT, false, 0);
		assertEquals("abc", document.text().toString());
		keys(document, clip, true).run(Command.CUT, false, 0);
		assertEquals("c", document.text().toString());
		assertEquals("ab", clip[0]);
	}

	@Test
	public void pasteIsOneStepWithTheCaretBehindIt() {
		Document document = new Document(Text.of("a-c"));
		document.select(1, 2);
		String[] clip = {"x\r\ny"};
		keys(document, clip, true).run(Command.PASTE, false, 0);
		assertEquals("ax\nyc", document.text().toString());
		assertEquals(new Document.Selection(4, 4), document.selection());
		document.undo();
		assertEquals("a-c", document.text().toString());
	}

	@Test
	public void aPasteTheDocumentRefusesChangesNothing() {
		Document document = new Document(Text.of(
				"x".repeat((int) Document.MAX_BYTES - 1)));
		document.select(1, 3);
		String[] refused = new String[1];
		document.addListener(new Document.Listener() {
			@Override
			public void onRefused(String reason) {
				refused[0] = reason;
			}
		});
		String[] clip = {"long"};
		keys(document, clip, true).run(Command.PASTE, false, 0);
		assertNotNull(refused[0]);
		assertEquals(new Document.Selection(1, 3), document.selection());
		assertEquals(0, document.revision());
		assertFalse(document.canUndo());
	}

	@Test
	public void aPasteComesWhenTheClipIsRead() {
		Document document = new Document(Text.of("a-c"));
		document.select(1, 2);
		KeyCommands.Pasted[] waiting = new KeyCommands.Pasted[1];
		keys(document, waiting).run(Command.PASTE, false, 0);
		// Nothing of the clip is read or split until it comes.
		assertEquals("a-c", document.text().toString());
		assertEquals(0, document.revision());
		waiting[0].paste(Text.of("x\ny"), 3);
		assertEquals("ax\nyc", document.text().toString());
		assertEquals(new Document.Selection(4, 4), document.selection());
	}

	@Test
	public void aPasteAfterAnEditOrAMoveIsDropped() {
		Document document = new Document(Text.of("abc"));
		document.select(1, 1);
		KeyCommands.Pasted[] waiting = new KeyCommands.Pasted[1];
		KeyCommands keys = keys(document, waiting);
		keys.run(Command.PASTE, false, 0);
		document.type(1, 1, Text.of("t"));
		waiting[0].paste(Text.of("x"), 1);
		assertEquals("atbc", document.text().toString());
		keys.run(Command.PASTE, false, 0);
		document.select(0, 0);
		waiting[0].paste(Text.of("x"), 1);
		assertEquals("atbc", document.text().toString());
	}

	@Test
	public void aPasteTakesTheCountOfItsBytes() {
		Document document = new Document(Text.of("abc"));
		KeyCommands.Pasted[] waiting = new KeyCommands.Pasted[1];
		keys(document, waiting).run(Command.PASTE, false, 0);
		waiting[0].paste(Text.of("x"), Document.MAX_BYTES);
		assertEquals("abc", document.text().toString());
	}

	// The clip comes when the test hands it to waiting[0].
	private static KeyCommands keys(Document document,
			KeyCommands.Pasted[] waiting) {
		return new KeyCommands(document, new EditCommands(document),
				new KeyCommands.Clipboard() {
					@Override
					public void read(KeyCommands.Pasted pasted) {
						waiting[0] = pasted;
					}

					@Override
					public boolean set(String text) {
						return false;
					}
				});
	}

	private static KeyCommands keys(Document document, String[] clip,
			boolean accepts) {
		return new KeyCommands(document, new EditCommands(document),
				new KeyCommands.Clipboard() {
					@Override
					public void read(KeyCommands.Pasted pasted) {
						if (clip[0] != null) {
							Text text = Text.of(clip[0]);
							pasted.paste(text, Document.bytes(text));
						}
					}

					@Override
					public boolean set(String text) {
						if (accepts) {
							clip[0] = text;
						}
						return accepts;
					}
				});
	}
}
