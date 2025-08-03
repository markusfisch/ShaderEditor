package de.markusfisch.android.shadereditor.fragment;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Collections;
import java.util.List;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.opengl.ShaderError;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.view.SoftKeyboard;
import de.markusfisch.android.shadereditor.view.UndoRedo;
import de.markusfisch.android.shadereditor.widget.ErrorListModal;
import de.markusfisch.android.shadereditor.widget.ShaderEditor;

public class EditorFragment extends Fragment {
	public static final String TAG = "EditorFragment";

	private View editorContainer;
	private ShaderEditor shaderEditor;
	private UndoRedo undoRedo;

	@Nullable
	private ShaderEditor.OnTextChangedListener textChangedListener;
	@Nullable
	private ShaderEditor.CodeCompletionListener codeCompletionListener;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		View view = inflater.inflate(R.layout.fragment_editor, container, false);

		editorContainer = view.findViewById(R.id.editor_container);
		shaderEditor = view.findViewById(R.id.editor);
		shaderEditor.setOnTextChangedListener((text) -> {
			if (textChangedListener != null) {
				textChangedListener.onTextChanged(text);
			}
		});
		shaderEditor.setOnCompletionsListener((completions, position) -> {
			if (codeCompletionListener != null) {
				codeCompletionListener.onCodeCompletions(completions, position);
			}
		});
		setShowLineNumbers(ShaderEditorApp.preferences.showLineNumbers());
		undoRedo = new UndoRedo(shaderEditor, ShaderEditorApp.editHistory);

		return view;
	}

	public void setOnTextChangedListener(@Nullable ShaderEditor.OnTextChangedListener listener) {
		textChangedListener = listener;
	}

	public void setCodeCompletionListener(@Nullable ShaderEditor.CodeCompletionListener listener) {
		codeCompletionListener = listener;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		undoRedo.detachListener();
	}

	@Override
	public void onResume() {
		super.onResume();
		updateToPreferences();
		// Only start listening after EditText restored its content
		// to make sure the initial change is not recorded.
		undoRedo.listenForChanges();
	}

	public void undo() {
		undoRedo.undo();
	}

	public boolean canUndo() {
		return undoRedo.canUndo();
	}

	public void redo() {
		undoRedo.redo();
	}

	public boolean canRedo() {
		return undoRedo.canRedo();
	}

	public boolean hasErrors() {
		return shaderEditor.hasErrors();
	}

	public void clearError() {
		shaderEditor.setErrors(Collections.emptyList());
	}

	public void updateHighlighting() {
		shaderEditor.updateHighlighting();
	}

	public void highlightErrors() {
		shaderEditor.updateErrorHighlighting();
	}

	public void setErrors(@NonNull List<ShaderError> errors) {
		shaderEditor.setErrors(errors);
		highlightErrors();
	}

	public void showErrors() {
		List<ShaderError> errors = shaderEditor.getErrors();
		new ErrorListModal(errors, this::navigateToLine).show(getParentFragmentManager(),
				ErrorListModal.TAG);
	}

	public void navigateToLine(int lineNumber) {
		shaderEditor.navigateToLine(lineNumber);
	}

	public boolean isModified() {
		return shaderEditor.isModified();
	}

	public String getText() {
		return shaderEditor.getCleanText();
	}

	public void setText(String text) {
		clearError();
		undoRedo.clearHistory();
		undoRedo.stopListeningForChanges();
		shaderEditor.setTextHighlighted(text);
		undoRedo.listenForChanges();
	}

	public void insert(@NonNull CharSequence text) {
		shaderEditor.insert(text);
	}

	public void addUniform(String name) {
		shaderEditor.addUniform(name);
	}

	public boolean isCodeVisible() {
		return editorContainer.getVisibility() == View.VISIBLE;
	}

	public boolean toggleCode() {
		boolean visible = isCodeVisible();
		editorContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
		if (visible) {
			SoftKeyboard.hide(getActivity(), shaderEditor);
		}
		return visible;
	}

	private void updateToPreferences() {
		Preferences preferences = ShaderEditorApp.preferences;
		shaderEditor.setUpdateDelay(preferences.getUpdateDelay());
		shaderEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, preferences.getTextSize());
		Typeface font = preferences.getFont();
		shaderEditor.setTypeface(font);
		String features = shaderEditor.getFontFeatureSettings();
		boolean isMono = font == Typeface.MONOSPACE;
		// Don't touch font features for the default MONOSPACE font as
		// this can impact performance.
		if (!isMono || features != null) {
			shaderEditor.setFontFeatureSettings(isMono ? null : preferences.useLigatures() ?
					"normal" : "calt off");
		}
	}

	public void setShowLineNumbers(boolean showLineNumbers) {
		shaderEditor.setShowLineNumbers(showLineNumbers);
	}
}