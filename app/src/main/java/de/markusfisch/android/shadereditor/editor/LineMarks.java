package de.markusfisch.android.shadereditor.editor;

import java.util.Arrays;
import java.util.List;

import de.markusfisch.android.shadereditor.opengl.ShaderError;

// The lines of a document that errors mark. A mark follows the start of its
// line through edits and is dropped when its line can no longer be told apart:
// its text removed, joined to text in front of it, or for an empty line, text
// removed at its start. A missing mark is better than one on the wrong line.
// Replacing lines by as many, as indenting does, keeps every mark.
public final class LineMarks {
	private final Document document;
	private final Document.Listener listener = new Document.Listener() {
		@Override
		public void onChange(Document.Change change, Text before) {
			move(change, before);
		}
	};
	// Sorted by line, one entry per line.
	private int[] lines = new int[0];
	private int size;
	private int changes;

	public LineMarks(Document document) {
		this.document = document;
		document.addListener(listener);
	}

	public void close() {
		document.removeListener(listener);
	}

	// Errors without a line or past the end are left out.
	public void set(List<ShaderError> errors) {
		int lineCount = document.text().lineCount();
		int[] marked = new int[errors.size()];
		int count = 0;
		for (ShaderError error : errors) {
			int line = error.getLine() - 1;
			if (line >= 0 && line < lineCount) {
				marked[count++] = line;
			}
		}
		Arrays.sort(marked, 0, count);
		lines = new int[count];
		size = 0;
		++changes;
		for (int i = 0; i < count; ++i) {
			add(marked[i]);
		}
	}

	public boolean has(int line) {
		return Arrays.binarySearch(lines, 0, size, line) >= 0;
	}

	// Changes whenever a mark is set, moves or is dropped.
	public int changes() {
		return changes;
	}

	private void move(Document.Change change, Text before) {
		int removed = change.removed().lineCount();
		if (size == 0 || (removed > 1 &&
				removed == change.inserted().lineCount())) {
			return;
		}
		int start = change.start();
		int end = start + change.removed().length();
		int first = before.lineOf(start);
		int last = first + change.removed().lineCount() - 1;
		// Where what follows the change starts, how far the lines below
		// it move, and whether text stands in front of it on its line.
		Text inserted = change.inserted();
		int after = first + inserted.lineCount() - 1;
		int shift = after - last;
		boolean joined = !inserted.line(inserted.lineCount() - 1).isEmpty() ||
				(after == first && start > before.lineStart(first));
		int count = size;
		size = 0;
		boolean moved = false;
		for (int i = 0; i < count; ++i) {
			int line = lines[i];
			if (line > last) {
				line += shift;
			} else if (line >= first) {
				int lineStart = before.lineStart(line);
				int lineEnd = lineStart + before.line(line).length();
				if ((start < end && start <= lineStart && lineEnd <= end) ||
						(lineStart > start && joined)) {
					moved = true;
					continue;
				}
				line = lineStart < start ? first : after;
			}
			moved |= line != lines[i];
			add(line);
		}
		if (moved || size != count) {
			++changes;
		}
	}

	// Lines arrive in order.
	private void add(int line) {
		if (size == 0 || lines[size - 1] != line) {
			lines[size++] = line;
		}
	}
}
