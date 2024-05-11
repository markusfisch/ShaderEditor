package de.markusfisch.android.shadereditor.view;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

import java.util.LinkedList;

/**
 * A generic undo/redo implementation for TextViews.
 * <p>
 * Based on this gist:
 * https://gist.github.com/zeleven/0cfa738c1e8b65b23ff7df1fc30c9f7e
 */
public class UndoRedo {
	public static final class EditHistory {
		private final LinkedList<EditItem> history = new LinkedList<>();

		/**
		 * The position from which an EditItem will be retrieved when getNext()
		 * is called. If getPrevious() has not been called, this has the same
		 * value as history.size().
		 */
		private int position = 0;
		private int maxHistorySize = -1;

		private void clear() {
			position = 0;
			history.clear();
		}

		/**
		 * Adds a new edit operation to the history at the current position. If
		 * executed after a call to getPrevious() removes all the future history
		 * (elements with positions >= current history position).
		 */
		private void add(EditItem item) {
			while (history.size() > position) {
				history.removeLast();
			}
			history.add(item);
			position++;

			if (maxHistorySize >= 0) {
				trimHistory();
			}
		}

		/**
		 * Set the maximum history size. If size is negative, then history size
		 * is only limited by the device memory.
		 */
		private void setMaxHistorySize(int size) {
			maxHistorySize = size;
			if (maxHistorySize >= 0) {
				trimHistory();
			}
		}

		/**
		 * Trim history when it exceeds max history size.
		 */
		private void trimHistory() {
			while (history.size() > maxHistorySize) {
				history.removeFirst();
				position--;
			}

			if (position < 0) {
				position = 0;
			}
		}

		/**
		 * Traverses the history backward by one position, returns and item at
		 * that position.
		 */
		private EditItem getPrevious() {
			if (position == 0) {
				return null;
			}
			--position;
			return history.get(position);
		}

		/**
		 * Traverses the history forward by one position, returns and item at
		 * that position.
		 */
		private EditItem getNext() {
			if (position >= history.size()) {
				return null;
			}

			EditItem item = history.get(position);
			++position;
			return item;
		}

		private boolean hasNext() {
			return position < history.size();
		}

		private boolean hasPrevious() {
			return position > 0;
		}
	}

	private final EditTextChangeListener changeListener =
			new EditTextChangeListener();
	private final EditHistory editHistory;
	private final TextView textView;

	/**
	 * Is undo/redo being performed? This member signals if an undo/redo
	 * operation is currently being performed. Changes in the text during
	 * undo/redo are not recorded because it would mess up the undo history.
	 */
	private boolean isUndoOrRedo = false;
	private boolean isListening = false;

	public UndoRedo(TextView textView, EditHistory editHistory) {
		this.textView = textView;
		this.editHistory = editHistory;
		textView.addTextChangedListener(changeListener);
	}

	public UndoRedo(TextView textView) {
		this(textView, new EditHistory());
	}

	public void listenForChanges() {
		isListening = true;
	}

	public void stopListeningForChanges() {
		isListening = false;
	}

	public void detachListener() {
		textView.removeTextChangedListener(changeListener);
	}

	/**
	 * Set the maximum history size. If size is negative, then history size is
	 * only limited by the device memory.
	 */
	public void setMaxHistorySize(int size) {
		editHistory.setMaxHistorySize(size);
	}

	public void clearHistory() {
		editHistory.clear();
	}

	public boolean canUndo() {
		return editHistory.hasPrevious();
	}

	public void undo() {
		EditItem edit = editHistory.getPrevious();
		if (edit == null) {
			return;
		}

		Editable text = textView.getEditableText();
		int start = edit.start;
		int end = start + (edit.after != null ? edit.after.length() : 0);

		isUndoOrRedo = true;
		text.replace(start, end, edit.before);
		isUndoOrRedo = false;

		removeUnderlineSpans(text);

		Selection.setSelection(text, edit.before == null ? start
				: (start + edit.before.length()));
	}

	public boolean canRedo() {
		return editHistory.hasNext();
	}

	public void redo() {
		EditItem edit = editHistory.getNext();
		if (edit == null) {
			return;
		}

		Editable text = textView.getEditableText();
		int start = edit.start;
		int end = start + (edit.before != null ? edit.before.length() : 0);

		isUndoOrRedo = true;
		text.replace(start, end, edit.after);
		isUndoOrRedo = false;

		removeUnderlineSpans(text);

		Selection.setSelection(text, edit.after == null ? start
				: (start + edit.after.length()));
	}

	/**
	 * Get rid of underlines inserted when editor tries to come
	 * up with a suggestion.
	 */
	private static void removeUnderlineSpans(Editable text) {
		for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) {
			text.removeSpan(o);
		}
	}

	/**
	 * Represents the changes performed by a single edit operation.
	 */
	private static final class EditItem {
		private final int start;
		private final CharSequence before;
		private final CharSequence after;

		/**
		 * Constructs EditItem of a modification that was applied at position
		 * start and replaced CharSequence before with CharSequence after.
		 */
		private EditItem(int start, CharSequence before, CharSequence after) {
			this.start = start;
			this.before = before;
			this.after = after;
		}
	}

	/**
	 * Class that listens to changes in the text.
	 */
	private final class EditTextChangeListener implements TextWatcher {
		/**
		 * The text that will be removed by the change event.
		 */
		private CharSequence beforeChange;

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			if (isUndoOrRedo || !isListening) {
				return;
			}
			beforeChange = s.subSequence(start, start + count);
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			if (isUndoOrRedo || !isListening) {
				return;
			}
			editHistory.add(new EditItem(start, beforeChange,
					s.subSequence(start, start + count)));
		}

		@Override
		public void afterTextChanged(Editable s) {
		}
	}
}
