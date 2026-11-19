package de.markusfisch.android.shadereditor.editor;

import de.markusfisch.android.shadereditor.editor.Document.Selection;
import de.markusfisch.android.shadereditor.syntax.Indentation;

// The commands keys and gestures map to. Moves up and down aim at the column
// the first of them started from, until the selection, the text or the tab
// width changes. Deleting a character, a line break or an indentation at the
// caret is typing, for undo.
public final class EditCommands {
	private final Document document;
	private final Motion motion;
	private int tabWidth = 4;
	private Indentation indentation = Indentation.C;
	// The column of a run of vertical moves, while the selection is the
	// one the last of them made. The document makes a new selection for
	// every change, and undo, which restores an old one, a new revision.
	private int wantedColumn;
	private Selection wantedSelection;
	private int wantedRevision;

	public EditCommands(Document document) {
		this.document = document;
		motion = new Motion();
	}

	public void setTabWidth(int tabWidth) {
		this.tabWidth = tabWidth;
		wantedSelection = null;
	}

	public void setIndentation(Indentation indentation) {
		this.indentation = indentation;
	}

	// With a selection, to its start.
	public void moveBackward(boolean extend) {
		Selection selection = document.selection();
		moveTo(extend || selection.isEmpty()
				? motion.previousChar(document.text(), selection.head())
				: selection.start(), extend);
	}

	// With a selection, to its end.
	public void moveForward(boolean extend) {
		Selection selection = document.selection();
		moveTo(extend || selection.isEmpty()
				? motion.nextChar(document.text(), selection.head())
				: selection.end(), extend);
	}

	public void moveWordBackward(boolean extend) {
		moveTo(motion.previousWord(document.text(), head()), extend);
	}

	public void moveWordForward(boolean extend) {
		moveTo(motion.nextWord(document.text(), head()), extend);
	}

	// Past the first line to the start of the text, past the last to its end.
	public void moveLines(int count, boolean extend) {
		Text text = document.text();
		int head = head();
		int column = document.selection() == wantedSelection &&
				document.revision() == wantedRevision
				? wantedColumn
				: motion.column(text, head, tabWidth);
		int line = text.lineOf(head) + count;
		int target;
		if (line < 0) {
			target = 0;
		} else if (line >= text.lineCount()) {
			target = text.length();
		} else {
			target = motion.offsetAt(text, line, column, tabWidth);
		}
		moveTo(target, extend);
		wantedColumn = column;
		wantedSelection = document.selection();
		wantedRevision = document.revision();
	}

	public void moveToLineStart(boolean extend) {
		moveTo(motion.lineStart(document.text(), head()), extend);
	}

	public void moveToLineEnd(boolean extend) {
		moveTo(motion.lineEnd(document.text(), head()), extend);
	}

	public void moveToStart(boolean extend) {
		moveTo(0, extend);
	}

	public void moveToEnd(boolean extend) {
		moveTo(document.text().length(), extend);
	}

	public void selectAll() {
		document.select(0, document.text().length());
	}

	public void deleteBackward() {
		int head = head();
		if (!deleteSelection() && head > 0) {
			Text text = document.text();
			int from = motion.previousChar(text, head);
			document.type(from, motion.nextChar(text, from), Text.EMPTY);
		}
	}

	public void deleteForward() {
		int head = head();
		if (!deleteSelection() && head < document.text().length()) {
			Text text = document.text();
			int to = motion.nextChar(text, head);
			document.type(motion.previousChar(text, to), to, Text.EMPTY);
		}
	}

	public void deleteWordBackward() {
		int head = head();
		if (!deleteSelection()) {
			document.replace(motion.previousWord(document.text(), head), head,
					Text.EMPTY);
		}
	}

	public void deleteWordForward() {
		int head = head();
		if (!deleteSelection()) {
			document.replace(head, motion.nextWord(document.text(), head),
					Text.EMPTY);
		}
	}

	public void enter() {
		Selection selection = document.selection();
		int start = selection.start();
		int end = selection.end();
		document.type(start, end, Text.of("\n" + AutoIndent.indentAfterBreak(
				document.text(), start, end, "\t", indentation)));
	}

	// A selection within a line is replaced by a level; one over several lines
	// indents each line, and an empty line stays empty.
	public void indent() {
		Text text = document.text();
		Selection selection = document.selection();
		int start = selection.start();
		if (text.lineOf(start) == text.lineOf(selection.end())) {
			document.type(start, selection.end(), Text.of("\t"));
			return;
		}
		shiftLines("\t");
	}

	// The white space before the first tab stop of each line.
	public void unindent() {
		shiftLines(null);
	}

	// Unindents if level is null. An end of the selection at the start of a
	// line stays there; one inside a line moves with its text.
	private void shiftLines(String level) {
		Text text = document.text();
		Selection selection = document.selection();
		int first = text.lineOf(selection.start());
		int last = text.lineOf(selection.end());
		if (last > first && selection.end() == text.lineStart(last)) {
			--last;
		}
		int anchorLine = text.lineOf(selection.anchor());
		int headLine = text.lineOf(selection.head());
		int anchor = selection.anchor() - text.lineStart(anchorLine);
		int head = selection.head() - text.lineStart(headLine);
		StringBuilder shifted = new StringBuilder();
		boolean changed = false;
		for (int line = first; line <= last; ++line) {
			String s = text.line(line);
			if (line > first) {
				shifted.append('\n');
			}
			int added;
			if (level != null) {
				added = s.isEmpty() ? 0 : level.length();
				shifted.append(added > 0 ? level : "").append(s);
			} else {
				added = -leadingLevel(s);
				shifted.append(s, -added, s.length());
			}
			changed |= added != 0;
			if (line == anchorLine) {
				anchor = shift(anchor, added);
			}
			if (line == headLine) {
				head = shift(head, added);
			}
		}
		if (!changed) {
			return;
		}
		document.beginBatch();
		document.replace(text.lineStart(first),
				text.lineStart(last) + text.line(last).length(),
				Text.of(shifted));
		text = document.text();
		document.select(text.lineStart(anchorLine) + anchor,
				text.lineStart(headLine) + head);
		document.endBatch();
	}

	// Returns the length of the white space before the first tab stop.
	private int leadingLevel(String s) {
		int i = 0;
		for (int column = 0; i < s.length() && column < tabWidth; ++i) {
			char c = s.charAt(i);
			if (c == '\t') {
				column = tabWidth;
			} else if (c == ' ') {
				++column;
			} else {
				break;
			}
		}
		return i;
	}

	private static int shift(int column, int added) {
		return column == 0 ? 0 : Math.max(0, column + added);
	}
	private boolean deleteSelection() {
		Selection selection = document.selection();
		if (selection.isEmpty()) {
			return false;
		}
		document.replace(selection.start(), selection.end(), Text.EMPTY);
		return true;
	}

	private int head() {
		return document.selection().head();
	}

	private void moveTo(int offset, boolean extend) {
		document.select(extend ? document.selection().anchor() : offset,
				offset);
	}
}
