package de.markusfisch.android.shadereditor.widget;

import android.os.Build;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.SurroundingText;
import android.view.inputmethod.TextAttribute;
import android.view.inputmethod.TextSnapshot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import de.markusfisch.android.shadereditor.editor.Document;
import de.markusfisch.android.shadereditor.editor.TextInput;

// Every call goes to one TextInput, except a line break committed on its own
// while nothing composes, which is the view's Enter command because keyboards
// send Enter so; replaceText, made of the other calls; and context menu
// actions, which are the view's. A keyboard may use a connection after it was
// given a newer one, so each connection makes itself the view's current one
// when called, and only its updates reach the keyboard.
final class CodeInputConnection extends BaseInputConnection {
	private final CodeView view;
	private final Document document;
	private final TextInput input;
	private boolean closed;

	CodeInputConnection(CodeView view, Document document) {
		super(view, true);
		this.view = view;
		this.document = document;
		input = new TextInput(document, new TextInput.Listener() {
			@Override
			public void onUpdateSelection(int selStart, int selEnd,
					int composingStart, int composingEnd) {
				view.updateSelection(CodeInputConnection.this, selStart,
						selEnd, composingStart, composingEnd);
			}

			@Override
			public void onUpdateExtractedText(int token,
					TextInput.Window window) {
				view.updateExtractedText(CodeInputConnection.this, token,
						window);
			}
		});
	}

	static ExtractedText extracted(TextInput.Window window) {
		ExtractedText extracted = new ExtractedText();
		extracted.text = window.text();
		extracted.startOffset = window.offset();
		extracted.selectionStart = window.selectionStart();
		extracted.selectionEnd = window.selectionEnd();
		extracted.partialStartOffset = -1;
		extracted.partialEndOffset = -1;
		return extracted;
	}

	@Override
	public Editable getEditable() {
		return null;
	}

	@Override
	public boolean beginBatchEdit() {
		return input().beginBatchEdit();
	}

	@Override
	public boolean endBatchEdit() {
		return input().endBatchEdit();
	}

	@Override
	public boolean commitText(CharSequence text, int newCursorPosition) {
		if (!closed && input().composingStart() < 0 && text != null &&
				text.length() == 1 && text.charAt(0) == '\n') {
			view.enter();
			return true;
		}
		return input().commitText(text, newCursorPosition);
	}

	@Override
	public boolean setComposingText(CharSequence text,
			int newCursorPosition) {
		return input().setComposingText(text, newCursorPosition);
	}

	@Override
	public boolean setComposingRegion(int start, int end) {
		return input().setComposingRegion(start, end);
	}

	@Override
	public boolean finishComposingText() {
		return input().finishComposingText();
	}

	@Override
	public boolean deleteSurroundingText(int beforeLength,
			int afterLength) {
		return input().deleteSurroundingText(beforeLength, afterLength);
	}

	@Override
	public boolean deleteSurroundingTextInCodePoints(int beforeLength,
			int afterLength) {
		return input().deleteSurroundingTextInCodePoints(beforeLength,
				afterLength);
	}

	@Override
	public boolean setSelection(int start, int end) {
		return input().setSelection(start, end);
	}

	@Override
	public CharSequence getTextBeforeCursor(int length, int flags) {
		return input().getTextBeforeCursor(length);
	}

	@Override
	public CharSequence getTextAfterCursor(int length, int flags) {
		return input().getTextAfterCursor(length);
	}

	@Override
	public CharSequence getSelectedText(int flags) {
		return input().getSelectedText();
	}

	@RequiresApi(api = Build.VERSION_CODES.S)
	@Nullable
	@Override
	public SurroundingText getSurroundingText(int beforeLength,
			int afterLength, int flags) {
		TextInput.Window window = input().getSurroundingText(beforeLength,
				afterLength);
		return window == null ? null : new SurroundingText(window.text(),
				window.selectionStart(), window.selectionEnd(),
				window.offset());
	}

	@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
	@Nullable
	@Override
	public TextSnapshot takeSnapshot() {
		SurroundingText surrounding = getSurroundingText(
				TextInput.MAX_READ / 4, TextInput.MAX_READ / 4, 0);
		return surrounding == null ? null : new TextSnapshot(surrounding,
				input().composingStart(), input().composingEnd(), 0);
	}

	@Override
	public ExtractedText getExtractedText(ExtractedTextRequest request,
			int flags) {
		TextInput.Window window = input().getExtractedText(
				request != null ? request.token : 0,
				(flags & GET_EXTRACTED_TEXT_MONITOR) != 0);
		return window == null ? null : extracted(window);
	}

	@Override
	public int getCursorCapsMode(int reqModes) {
		return 0;
	}

	@RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	@Override
	public boolean replaceText(int start, int end, @NonNull CharSequence text,
			int newCursorPosition, @Nullable TextAttribute textAttribute) {
		int from = Math.min(start, end);
		int to = Math.max(start, end);
		if (closed || from < 0 || to > document.text().length()) {
			return false;
		}
		input().beginBatchEdit();
		input().finishComposingText();
		input().setSelection(from, to);
		input().commitText(text, newCursorPosition);
		input().endBatchEdit();
		return true;
	}

	@Override
	public boolean performContextMenuAction(int id) {
		if (closed) {
			return false;
		}
		view.use(this);
		return view.contextMenuAction(id);
	}

	@Override
	public boolean sendKeyEvent(KeyEvent event) {
		if (!closed) {
			view.use(this);
		}
		return super.sendKeyEvent(event);
	}

	int composingStart() {
		return input.composingStart();
	}

	int composingEnd() {
		return input.composingEnd();
	}

	@Override
	public void closeConnection() {
		if (closed) {
			return;
		}
		closed = true;
		input.closeConnection();
		view.closed(this);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			super.closeConnection();
		}
	}

	// Returns the TextInput for a call of the keyboard, which uses this
	// connection now.
	private TextInput input() {
		if (!closed) {
			view.use(this);
		}
		return input;
	}
}
