package de.markusfisch.android.shadereditor.editor;

import android.view.KeyEvent;

import de.markusfisch.android.shadereditor.editor.Document.Selection;

// What a key does to a document. A key with Ctrl or Meta held types nothing, so
// other shortcuts still reach the app. Only constants of KeyEvent and android.R
// are used, so this runs on the JVM.
public final class KeyCommands {
	public enum Command {
		BACKWARD, FORWARD, WORD_BACKWARD, WORD_FORWARD, LINE_UP, LINE_DOWN,
		PAGE_UP, PAGE_DOWN, LINE_START, LINE_END, START, END,
		DELETE_BACKWARD, DELETE_FORWARD, DELETE_WORD_BACKWARD,
		DELETE_WORD_FORWARD, ENTER, INDENT, UNINDENT, SELECT_ALL, UNDO, REDO,
		CUT, COPY, PASTE
	}

	public interface Clipboard {
		// Reads and splits the clip away from the main thread and hands it
		// to pasted on the main thread; nothing if there is none.
		void read(Pasted pasted);

		// False if that failed.
		boolean set(String text);
	}

	public interface Pasted {
		// bytes is what Document.bytes counts for text.
		void paste(Text text, long bytes);
	}

	private static final int SHORTCUT = KeyEvent.META_CTRL_ON |
			KeyEvent.META_META_ON;

	private final Document document;
	private final EditCommands commands;
	private final Clipboard clipboard;

	public KeyCommands(Document document, EditCommands commands,
			Clipboard clipboard) {
		this.document = document;
		this.commands = commands;
		this.clipboard = clipboard;
	}

	// Null if it has none.
	public static Command command(int keyCode, int meta) {
		boolean ctrl = (meta & KeyEvent.META_CTRL_ON) != 0;
		boolean shift = (meta & KeyEvent.META_SHIFT_ON) != 0;
		switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return ctrl ? Command.WORD_BACKWARD : Command.BACKWARD;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return ctrl ? Command.WORD_FORWARD : Command.FORWARD;
			case KeyEvent.KEYCODE_DPAD_UP:
				return ctrl ? null : Command.LINE_UP;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return ctrl ? null : Command.LINE_DOWN;
			case KeyEvent.KEYCODE_PAGE_UP:
				return Command.PAGE_UP;
			case KeyEvent.KEYCODE_PAGE_DOWN:
				return Command.PAGE_DOWN;
			case KeyEvent.KEYCODE_MOVE_HOME:
				return ctrl ? Command.START : Command.LINE_START;
			case KeyEvent.KEYCODE_MOVE_END:
				return ctrl ? Command.END : Command.LINE_END;
			case KeyEvent.KEYCODE_DEL:
				return ctrl
						? Command.DELETE_WORD_BACKWARD
						: Command.DELETE_BACKWARD;
			case KeyEvent.KEYCODE_FORWARD_DEL:
				return ctrl
						? Command.DELETE_WORD_FORWARD
						: Command.DELETE_FORWARD;
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_NUMPAD_ENTER:
				return ctrl ? null : Command.ENTER;
			case KeyEvent.KEYCODE_TAB:
				return ctrl ? null : shift ? Command.UNINDENT : Command.INDENT;
			default:
				break;
		}
		if (!ctrl) {
			return null;
		}
		switch (keyCode) {
			case KeyEvent.KEYCODE_A:
				return Command.SELECT_ALL;
			case KeyEvent.KEYCODE_Z:
				return shift ? Command.REDO : Command.UNDO;
			case KeyEvent.KEYCODE_Y:
				return shift ? null : Command.REDO;
			case KeyEvent.KEYCODE_X:
				return Command.CUT;
			case KeyEvent.KEYCODE_C:
				return Command.COPY;
			case KeyEvent.KEYCODE_V:
				return Command.PASTE;
			default:
				return null;
		}
	}

	// A context menu action as keyboards ask for one; null if it has none.
	public static Command contextMenuCommand(int id) {
		if (id == android.R.id.selectAll) {
			return Command.SELECT_ALL;
		} else if (id == android.R.id.cut) {
			return Command.CUT;
		} else if (id == android.R.id.copy) {
			return Command.COPY;
		} else if (id == android.R.id.paste) {
			return Command.PASTE;
		} else if (id == android.R.id.undo) {
			return Command.UNDO;
		} else if (id == android.R.id.redo) {
			return Command.REDO;
		}
		return null;
	}

	public static boolean types(int meta) {
		return (meta & SHORTCUT) == 0;
	}

	// Its command, else typed, if not null. Returns whether the key did
	// anything.
	public boolean keyDown(int keyCode, int meta, String typed,
			int pageLines) {
		Command command = command(keyCode, meta);
		if (command != null) {
			run(command, (meta & KeyEvent.META_SHIFT_ON) != 0, pageLines);
			return true;
		}
		if (typed == null || typed.isEmpty() || !types(meta)) {
			return false;
		}
		Selection selection = document.selection();
		document.type(selection.start(), selection.end(), Text.of(typed));
		return true;
	}

	// Returns false if a cut or copy did nothing; the document is then as it
	// was. A paste comes later.
	public boolean run(Command command, boolean extend, int pageLines) {
		switch (command) {
			case BACKWARD -> commands.moveBackward(extend);
			case FORWARD -> commands.moveForward(extend);
			case WORD_BACKWARD -> commands.moveWordBackward(extend);
			case WORD_FORWARD -> commands.moveWordForward(extend);
			case LINE_UP -> commands.moveLines(-1, extend);
			case LINE_DOWN -> commands.moveLines(1, extend);
			case PAGE_UP -> commands.moveLines(-pageLines, extend);
			case PAGE_DOWN -> commands.moveLines(pageLines, extend);
			case LINE_START -> commands.moveToLineStart(extend);
			case LINE_END -> commands.moveToLineEnd(extend);
			case START -> commands.moveToStart(extend);
			case END -> commands.moveToEnd(extend);
			case DELETE_BACKWARD -> commands.deleteBackward();
			case DELETE_FORWARD -> commands.deleteForward();
			case DELETE_WORD_BACKWARD -> commands.deleteWordBackward();
			case DELETE_WORD_FORWARD -> commands.deleteWordForward();
			case ENTER -> commands.enter();
			case INDENT -> commands.indent();
			case UNINDENT -> commands.unindent();
			case SELECT_ALL -> commands.selectAll();
			case UNDO -> document.undo();
			case REDO -> document.redo();
			case CUT -> {
				return copy() && paste(Text.EMPTY);
			}
			case COPY -> {
				return copy();
			}
			case PASTE -> {
				// Only where it was asked for: an edit or a move made
				// before the clip arrives wins.
				Selection selection = document.selection();
				int revision = document.revision();
				clipboard.read((text, bytes) -> {
					if (document.revision() == revision &&
							document.selection().equals(selection)) {
						paste(text, bytes);
					}
				});
			}
		}
		return true;
	}

	// Puts the selection on the clipboard; returns false if there is
	// none or the clipboard refused it.
	private boolean copy() {
		Selection selection = document.selection();
		return !selection.isEmpty() && clipboard.set(document.text()
				.substring(selection.start(), selection.end()));
	}

	// False if the document refused it.
	public boolean paste(Text text) {
		return paste(text, Document.bytes(text));
	}

	private boolean paste(Text text, long bytes) {
		Selection selection = document.selection();
		int start = selection.start();
		document.beginBatch();
		try {
			if (!document.replace(start, selection.end(), text, bytes)) {
				return false;
			}
			document.select(start + text.length(), start + text.length());
		} finally {
			document.endBatch();
		}
		return true;
	}
}
