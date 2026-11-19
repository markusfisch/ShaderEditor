package de.markusfisch.android.shadereditor.widget;

import androidx.annotation.Nullable;

import de.markusfisch.android.shadereditor.editor.Document.Change;
import de.markusfisch.android.shadereditor.editor.Document.Selection;
import de.markusfisch.android.shadereditor.editor.Motion;
import de.markusfisch.android.shadereditor.editor.Text;

// What a touch selects in a CodeView. A handle moves as the platform's do:
// while the selection grows, a word joins whole once the finger is past its
// middle; moved back into a word, it goes by character. A change to the text
// moves the offsets as the document moves its selection, so a drag goes on in
// the text the change left.
final class TouchSelection {
	interface Line {
		float xOf(int offset);

		int offsetAt(float x);
	}

	private static final int CARET = 0;
	private static final int WORDS = 1;
	private static final int HANDLE = 2;

	private final Motion motion;
	private int unit;
	private int anchor;
	private int head;
	private int wordStart;
	private int wordEnd;
	private int reached;
	private int delta;

	TouchSelection(Motion motion) {
		this.motion = motion;
	}

	// Moved to the nearer end of the character x falls in.
	int offsetAt(Text text, int n, float x, Line layout) {
		int start = text.lineStart(n);
		int length = text.line(n).length();
		int i = layout.offsetAt(x);
		if (i <= 0 || i >= length) {
			return start + Math.max(0, Math.min(i, length));
		}
		int before = motion.previousChar(text, start + i) - start;
		int after = motion.nextChar(text, start + before) - start;
		if (after == i) {
			return start + i;
		}
		return start + (x - layout.xOf(before) <= layout.xOf(after) - x
				? before
				: after);
	}

	void dragHandle(int anchor, int head) {
		unit = anchor == head ? CARET : HANDLE;
		this.anchor = anchor;
		this.head = head;
		reached = head;
		delta = 0;
	}

	void dragWords(Text text, int offset) {
		Selection word = motion.wordAt(text, offset);
		unit = WORDS;
		anchor = wordStart = word.start();
		head = wordEnd = word.end();
		reached = offset;
	}

	// Returns whether the selection changed. A handle never makes the selection
	// empty; by word, the selection spans the start word and the word at
	// offset.
	boolean dragTo(Text text, int offset) {
		if (offset == reached) {
			return false;
		}
		int from = reached;
		reached = offset;
		int oldAnchor = anchor;
		int oldHead = head;
		if (unit == CARET) {
			anchor = head = offset;
		} else if (unit == HANDLE) {
			moveHandle(text, from, offset);
		} else if (offset >= wordStart) {
			anchor = wordStart;
			head = Math.max(wordEnd, motion.wordAt(text, offset).end());
		} else {
			anchor = wordEnd;
			head = motion.wordAt(text, offset).start();
		}
		return anchor != oldAnchor || head != oldHead;
	}

	private void moveHandle(Text text, int from, int offset) {
		int direction = head > anchor ? 1 : -1;
		boolean grows = (offset - from) * direction > 0;
		int line = text.lineOf(offset);
		boolean otherLine = line != text.lineOf(head);
		int to;
		if (otherLine || (grows && !inWord(text, head))) {
			to = snap(text, offset, direction);
		} else {
			int start = text.lineStart(line);
			to = Math.max(start, Math.min(offset + delta,
					start + text.line(line).length()));
			// Not inside a character.
			to = motion.previousChar(text, motion.nextChar(text, to));
		}
		if (to != anchor && (to - head) * direction * (grows ? 1 : -1) > 0) {
			head = to;
		}
		delta = otherLine ? 0 : head - offset;
	}

	// Returns the end of the word at offset towards direction if offset
	// is past its middle, else its other end; offset if it is not inside
	// a word.
	private int snap(Text text, int offset, int direction) {
		Selection word = motion.wordAt(text, offset);
		if (!inWord(text, word, offset)) {
			return offset;
		}
		int middle = word.start() + (word.end() - word.start()) / 2;
		return direction > 0
				? offset >= middle ? word.end() : word.start()
				: offset <= middle ? word.start() : word.end();
	}

	private boolean inWord(Text text, int offset) {
		return inWord(text, motion.wordAt(text, offset), offset);
	}

	// Motion's word is white space if there is no word.
	private static boolean inWord(Text text, Selection word, int offset) {
		if (word.start() >= offset || offset >= word.end()) {
			return false;
		}
		int line = text.lineOf(offset);
		return !Character.isWhitespace(text.line(line).charAt(
				word.start() - text.lineStart(line)));
	}

	// As Change.map moves the document's selection.
	void map(Change change) {
		anchor = change.map(anchor);
		head = change.map(head);
		wordStart = change.map(wordStart);
		wordEnd = change.map(wordEnd);
		reached = change.map(reached);
	}

	// touched while it is the document's selection and the handles show at all,
	// else null. Every change to the text replaces the document's selection,
	// also inside a batch, so the handles never show at offsets of a text that
	// is gone.
	@Nullable
	Selection handles(@Nullable Selection touched, Selection current,
			boolean dragging) {
		return touched != null && touched == current &&
				showsHandles(dragging) ? current : null;
	}

	// Not while a drag by word is in progress.
	boolean showsHandles(boolean dragging) {
		return !dragging || unit != WORDS;
	}

	// As the platform's text views place the magnifier; NaN hides it.
	static float magnifierX(float x, float left, float right,
			float halfWidth) {
		if (x < left - halfWidth || x > right + halfWidth) {
			return Float.NaN;
		}
		return Math.max(left, Math.min(right, x));
	}

	int anchor() {
		return anchor;
	}

	int head() {
		return head;
	}
}
