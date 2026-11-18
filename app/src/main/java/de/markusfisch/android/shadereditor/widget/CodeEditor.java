package de.markusfisch.android.shadereditor.widget;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.editor.Document;
import de.markusfisch.android.shadereditor.editor.LineMarks;
import de.markusfisch.android.shadereditor.editor.LineStyles;
import de.markusfisch.android.shadereditor.editor.Text;
import de.markusfisch.android.shadereditor.opengl.ShaderError;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.syntax.GlslSyntax;

// What EditorFragment asks of ShaderEditor, done with a CodeView: one document,
// its colours and its error marks, made again for every new text.
public final class CodeEditor {
	// The lines LineStyles colours per edit: a screen and a margin.
	private static final int STYLE_BUDGET = 200;
	private static final Pattern TRAILING_WHITE_SPACE = Pattern.compile(
			"[\\t ]+$", Pattern.MULTILINE);

	private final CodeView view;
	private final Runnable editPaused = this::editPaused;
	private final Document.Listener documentListener = new Document.Listener() {
		@Override
		public void onUpdate() {
			updated();
		}

		@Override
		public void onRefused(String reason) {
			Snackbar.make(view, R.string.text_too_large,
					Snackbar.LENGTH_LONG).show();
		}
	};
	@Nullable
	private ShaderEditor.OnEditPausedListener editPausedListener;
	@Nullable
	private ShaderEditor.OnTextModifiedListener textModifiedListener;
	@Nullable
	private ShaderEditor.CodeCompletionListener completionListener;
	private Document document;
	private LineStyles styles;
	private LineMarks marks;
	private int revision;
	private int updateDelay = 1000;
	private int inputType = -1;
	@NonNull
	private List<ShaderError> errors = Collections.emptyList();

	public CodeEditor(@NonNull CodeView view) {
		this.view = view;
		show(new Document(Text.EMPTY));
	}

	public void setOnEditPausedListener(
			@Nullable ShaderEditor.OnEditPausedListener listener) {
		editPausedListener = listener;
	}

	public void setOnTextModifiedListener(
			@Nullable ShaderEditor.OnTextModifiedListener listener) {
		textModifiedListener = listener;
	}

	public void setOnCompletionsListener(
			@Nullable ShaderEditor.CodeCompletionListener listener) {
		completionListener = listener;
	}

	@NonNull
	public String getText() {
		return document.text().toString();
	}

	@NonNull
	public String getCleanText() {
		return TRAILING_WHITE_SPACE.matcher(getText()).replaceAll("");
	}

	// As ShaderEditor.setTextHighlighted: without history, and the listeners
	// hear of it.
	public void setText(@NonNull String text) {
		String converted = ShaderEditor.convertShaderToySource(text);
		show(new Document(Text.of(converted != null ? converted : text)));
		if (textModifiedListener != null) {
			textModifiedListener.onTextModified();
		}
		editPaused();
	}

	// The history of the same text, from a view model, else the selection
	// of a saved state; neither is told to the listeners.
	public void restore(@NonNull String text, @Nullable Object history,
			int start, int end) {
		Document restored = new Document(Text.of(text));
		int length = restored.text().length();
		if (!(history instanceof Document.History h && restored.restore(h)) &&
				start >= 0 && start <= length && end >= 0 && end <= length) {
			restored.select(start, end);
		}
		show(restored);
		view.bringCaretIntoView();
	}

	@NonNull
	public Object getHistory() {
		return document.history();
	}

	public int getSelectionStart() {
		return document.selection().start();
	}

	public int getSelectionEnd() {
		return document.selection().end();
	}

	// The strip's Tab key indents as the Tab key of a keyboard does.
	public void insert(@NonNull CharSequence text) {
		if ("\t".contentEquals(text)) {
			view.tab();
			return;
		}
		Document.Selection selection = document.selection();
		document.replace(selection.start(), selection.end(), Text.of(text));
		view.bringCaretIntoView();
	}

	// By the old editor's rule, as one step that can be undone.
	public void addUniform(@NonNull String statement) {
		String before = getText();
		SpannableStringBuilder after = new SpannableStringBuilder(before);
		ShaderEditor.addUniform(after, statement);
		int start = 0;
		int end = before.length();
		int newEnd = after.length();
		while (start < end && start < newEnd &&
				before.charAt(start) == after.charAt(start)) {
			++start;
		}
		while (end > start && newEnd > start &&
				before.charAt(end - 1) == after.charAt(newEnd - 1)) {
			--end;
			--newEnd;
		}
		document.replace(start, end,
				Text.of(after.subSequence(start, newEnd)));
	}

	// From 1; the caret stays if it is on that line already.
	public void navigateToLine(int line) {
		Text text = document.text();
		if (line < 1 || line > text.lineCount()) {
			return;
		}
		int start = text.lineStart(line - 1);
		Document.Selection selection = document.selection();
		if (selection.start() < start ||
				selection.end() > start + text.line(line - 1).length()) {
			document.select(start, start);
		}
		view.bringCaretIntoView();
	}

	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	@NonNull
	public List<ShaderError> getErrors() {
		return errors;
	}

	public void setErrors(@NonNull List<ShaderError> errors) {
		this.errors = errors;
	}

	public void markErrors() {
		marks.set(errors);
		view.marksChanged();
	}

	public boolean canUndo() {
		return document.canUndo();
	}

	public void undo() {
		document.undo();
		view.bringCaretIntoView();
	}

	public boolean canRedo() {
		return document.canRedo();
	}

	public void redo() {
		document.redo();
		view.bringCaretIntoView();
	}

	public void setShowLineNumbers(boolean show) {
		view.setShowLineNumbers(show);
	}

	public void applyPreferences(@NonNull Preferences preferences) {
		updateDelay = preferences.getUpdateDelay();
		view.setTextSize(TypedValue.COMPLEX_UNIT_SP, preferences.getTextSize());
		Typeface font = preferences.getFont();
		view.setTypeface(font);
		view.setFontFeatureSettings(font == Typeface.MONOSPACE ? null :
				preferences.useLigatures() ? "normal" : "calt off");
		view.setTabWidth(preferences.getTabWidth());
		view.setTabIndents(preferences.useTabForIndent());
		int type = preferences.hideNativeSuggestions()
				? CodeView.INPUT_TYPE_NO_SUGGESTIONS
				: CodeView.INPUT_TYPE_TEXT;
		if (type != inputType) {
			inputType = type;
			view.setInputType(type);
		}
	}

	public void destroy() {
		view.removeCallbacks(editPaused);
		release();
	}

	private void show(@NonNull Document document) {
		view.removeCallbacks(editPaused);
		if (this.document != null) {
			release();
		}
		this.document = document;
		revision = document.revision();
		errors = Collections.emptyList();
		styles = new LineStyles(document, GlslSyntax.INSTANCE, STYLE_BUDGET);
		marks = new LineMarks(document);
		view.setDocument(document, styles, marks);
		document.addListener(documentListener);
	}

	private void release() {
		document.removeListener(documentListener);
		styles.close();
		marks.close();
	}

	private void updated() {
		complete();
		if (document.revision() == revision) {
			return;
		}
		revision = document.revision();
		if (textModifiedListener != null) {
			textModifiedListener.onTextModified();
		}
		view.removeCallbacks(editPaused);
		view.postDelayed(editPaused, updateDelay);
	}

	// From the caret's line; nothing while text is selected.
	private void complete() {
		Document.Selection selection = document.selection();
		if (completionListener == null || !selection.isEmpty()) {
			return;
		}
		Text text = document.text();
		int caret = selection.head();
		int n = text.lineOf(caret);
		styles.syntax().complete(styles.runs(n).line(),
				caret - text.lineStart(n), completionListener::onCodeCompletions);
	}

	// A shader in ShaderToy's form is converted, as the old editor does,
	// once typing pauses.
	private void editPaused() {
		String text = getText();
		String converted = ShaderEditor.convertShaderToySource(text);
		if (converted != null) {
			document.beginBatch();
			document.replace(0, document.text().length(), Text.of(converted));
			document.select(0, 0);
			document.endBatch();
			view.removeCallbacks(editPaused);
			view.bringCaretIntoView();
			text = converted;
		}
		if (editPausedListener != null) {
			editPausedListener.onEditPaused(text);
		}
	}
}
