package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import de.markusfisch.android.shadereditor.editor.KeyCommands.Command;

// Sessions of Gboard, Samsung Keyboard and SwiftKey on a phone with Android
// 16, in both input types, of the AOSP keyboard on Android 6, and one written
// by hand for a keyboard that composes every word.
public class KeyboardSessionTest {
	// Page Up and Down are not recorded with the height of the view.
	private static final int PAGE_LINES = 20;
	private static final Map<Document, KeyCommands> KEYS =
			new WeakHashMap<>();
	private static final KeyCommands.Clipboard CLIPBOARD =
			new KeyCommands.Clipboard() {
				@Override
				public Text read() {
					return null;
				}

				@Override
				public boolean set(Text text, int start, int end) {
					return true;
				}
			};

	@Test
	public void everySessionReplays() throws Exception {
		URL url = getClass().getClassLoader().getResource("keyboard-sessions");
		assertNotNull(url);
		File[] files = new File(url.toURI()).listFiles(
				(dir, name) -> name.endsWith(".txt"));
		assertNotNull(files);
		assertTrue(files.length > 0);
		Arrays.sort(files);
		for (File file : files) {
			KeyboardReplay.replay(file.getName(),
					Files.readAllLines(file.toPath()),
					KeyboardSessionTest::view);
		}
	}

	@Test
	public void undoAndPausesAreReplayed() {
		// Undo takes only what was typed after the pause; replayed
		// without the pause or without undo, it would change other text.
		KeyboardReplay.replay("pause", List.of(
				"0 1 open 0 0 \"\"",
				"0 1 commitText \"a\" 1 -> true",
				"0 1 updateSelection 1 1 -1 -1",
				"1500 1 commitText \"b\" 1 -> true",
				"1500 1 updateSelection 2 2 -1 -1",
				"1600 app undo",
				"1600 1 changed 1 2 \"\"",
				"1600 1 updateSelection 1 1 -1 -1",
				"1700 1 close -> 1 1 \"a\""), KeyboardSessionTest::view);
	}

	@Test
	public void replayFailsAtTheFirstDifference() {
		List<String> session = List.of(
				"0 1 open 0 0 \"\"",
				"0 1 commitText \"a\" 1 -> true",
				"0 1 updateSelection 2 2 -1 -1",
				"0 1 close -> 1 1 \"a\"");
		AssertionError e = assertThrows(AssertionError.class,
				() -> KeyboardReplay.replay("wrong", session,
						KeyboardSessionTest::view));
		assertTrue(e.getMessage(), e.getMessage().startsWith("wrong:3: "));
	}

	// What the view does for an action of the app.
	private static void view(Document document, String name,
			List<String> args) {
		KeyCommands keys = KEYS.computeIfAbsent(document, d ->
				new KeyCommands(d, new EditCommands(d), CLIPBOARD));
		switch (name) {
			case "command" -> {
				assertEquals("enter", args.get(0));
				keys.run(Command.ENTER, false, PAGE_LINES);
			}
			case "key" -> {
				if (!"1".equals(args.get(0))) {
					keys.keyDown(Integer.parseInt(args.get(1)),
							Integer.parseInt(args.get(2)), args.get(3),
							PAGE_LINES);
				}
			}
			default -> fail("unknown action " + name);
		}
	}
}
