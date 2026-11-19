package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.opengl.ShaderError;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.view.SoftKeyboard;
import de.markusfisch.android.shadereditor.view.UndoRedo;
import de.markusfisch.android.shadereditor.widget.CodeEditor;
import de.markusfisch.android.shadereditor.widget.CodeView;
import de.markusfisch.android.shadereditor.widget.ErrorListModal;
import de.markusfisch.android.shadereditor.widget.ShaderEditor;

public class EditorFragment extends Fragment {
	public static final String TAG = "EditorFragment";

	private static final String DRAFT_KEY = "draft_key";
	private static final String SELECTION_START = "selection_start";
	private static final String SELECTION_END = "selection_end";

	public static class Draft extends ViewModel {
		private String key;
		private String text;
		private Object history;
	}

	private View editorContainer;
	private ShaderEditor shaderEditor;
	private UndoRedo undoRedo;
	private EditorDraft editorDraft;
	private boolean textRestored;
	private boolean hasDraftFile;
	// The new editor's, else null.
	@Nullable
	private CodeEditor codeEditor;
	private boolean newEditor;

	@Nullable
	private ShaderEditor.OnEditPausedListener editPausedListener;
	@Nullable
	private ShaderEditor.OnTextModifiedListener textModifiedListener;
	@Nullable
	private ShaderEditor.CodeCompletionListener codeCompletionListener;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		newEditor = ShaderEditorApp.preferences.useNewEditor();
		if (newEditor) {
			return createCodeEditor(inflater, container, state);
		}
		View view = inflater.inflate(R.layout.fragment_editor, container, false);

		getChildFragmentManager().setFragmentResultListener(ErrorListModal.RESULT,
				getViewLifecycleOwner(), (key, result) ->
						navigateToLine(result.getInt(ErrorListModal.LINE)));

		editorContainer = view.findViewById(R.id.editor_container);
		shaderEditor = view.findViewById(R.id.editor);
		shaderEditor.setOnEditPausedListener((text) -> {
			if (editPausedListener != null) {
				editPausedListener.onEditPaused(text);
			}
		});
		shaderEditor.setOnTextModifiedListener(() -> {
			if (textModifiedListener != null) {
				textModifiedListener.onTextModified();
			}
		});
		shaderEditor.setOnCompletionsListener((completions, position) -> {
			if (codeCompletionListener != null) {
				codeCompletionListener.onCodeCompletions(completions, position);
			}
		});
		setShowLineNumbers(ShaderEditorApp.preferences.showLineNumbers());
		undoRedo = new UndoRedo(shaderEditor, ShaderEditorApp.editHistory);
		shaderEditor.setUndoRedo(undoRedo);
		editorDraft = new EditorDraft(requireContext().getNoBackupFilesDir());
		if (state != null) {
			restoreText(state);
		}

		return view;
	}

	private View createCodeEditor(LayoutInflater inflater, ViewGroup container,
			Bundle state) {
		View view = inflater.inflate(R.layout.fragment_editor_code, container,
				false);
		getChildFragmentManager().setFragmentResultListener(ErrorListModal.RESULT,
				getViewLifecycleOwner(), (key, result) ->
						navigateToLine(result.getInt(ErrorListModal.LINE)));
		editorContainer = view;
		codeEditor = new CodeEditor((CodeView) view);
		codeEditor.setOnEditPausedListener((text) -> {
			if (editPausedListener != null) {
				editPausedListener.onEditPaused(text);
			}
		});
		codeEditor.setOnTextModifiedListener(() -> {
			if (textModifiedListener != null) {
				textModifiedListener.onTextModified();
			}
		});
		codeEditor.setOnCompletionsListener((completions, position) -> {
			if (codeCompletionListener != null) {
				codeCompletionListener.onCodeCompletions(completions, position);
			}
		});
		setShowLineNumbers(ShaderEditorApp.preferences.showLineNumbers());
		editorDraft = new EditorDraft(requireContext().getNoBackupFilesDir());
		if (state != null) {
			restoreText(state);
		}
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (codeEditor != null) {
			saveCodeEditor(outState);
			return;
		}
		Draft draft = new ViewModelProvider(this).get(Draft.class);
		String key = UUID.randomUUID().toString();
		String text = String.valueOf(shaderEditor.getText());
		draft.key = key;
		draft.text = text;
		// A configuration change keeps the view model. Otherwise write
		// now: the process may be killed as soon as this returns.
		if (!requireActivity().isChangingConfigurations()) {
			editorDraft.write(key, text);
			hasDraftFile = true;
		}
		outState.putString(DRAFT_KEY, key);
		outState.putInt(SELECTION_START, shaderEditor.getSelectionStart());
		outState.putInt(SELECTION_END, shaderEditor.getSelectionEnd());
	}

	private void saveCodeEditor(@NonNull Bundle outState) {
		Draft draft = new ViewModelProvider(this).get(Draft.class);
		String key = UUID.randomUUID().toString();
		String text = codeEditor.getText();
		draft.key = key;
		draft.text = text;
		draft.history = codeEditor.getHistory();
		if (!requireActivity().isChangingConfigurations()) {
			editorDraft.write(key, text);
			hasDraftFile = true;
		}
		outState.putString(DRAFT_KEY, key);
		outState.putInt(SELECTION_START, codeEditor.getSelectionStart());
		outState.putInt(SELECTION_END, codeEditor.getSelectionEnd());
	}

	public boolean isTextRestored() {
		return textRestored;
	}

	private void restoreText(@NonNull Bundle state) {
		String key = state.getString(DRAFT_KEY);
		if (key == null) {
			return;
		}
		Draft draft = new ViewModelProvider(this).get(Draft.class);
		String text = draft.text;
		if (!key.equals(draft.key)) {
			text = editorDraft.read(key);
			hasDraftFile = true;
		}
		if (text == null) {
			return;
		}
		if (codeEditor != null) {
			codeEditor.restore(text, key.equals(draft.key)
							? draft.history
							: null, state.getInt(SELECTION_START),
					state.getInt(SELECTION_END));
			textRestored = true;
			return;
		}
		shaderEditor.setTextHighlighted(text);
		int start = state.getInt(SELECTION_START);
		int end = state.getInt(SELECTION_END);
		if (start >= 0 && end >= 0 && start <= text.length() &&
				end <= text.length()) {
			shaderEditor.setSelection(start, end);
		}
		textRestored = true;
	}

	public void setOnTextModifiedListener(@Nullable ShaderEditor.OnTextModifiedListener listener) {
		textModifiedListener = listener;
	}

	public void setOnEditPausedListener(@Nullable ShaderEditor.OnEditPausedListener listener) {
		editPausedListener = listener;
	}

	public void setCodeCompletionListener(@Nullable ShaderEditor.CodeCompletionListener listener) {
		codeCompletionListener = listener;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (codeEditor != null) {
			codeEditor.destroy();
			return;
		}
		undoRedo.detachListener();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (newEditor != ShaderEditorApp.preferences.useNewEditor()) {
			// The draft keeps the text while the other editor is made. Not
			// from within onResume: the picture then stays black on
			// Android 6.
			requireView().post(() -> {
				Activity activity = getActivity();
				if (activity != null) {
					activity.recreate();
				}
			});
			return;
		}
		if (codeEditor != null) {
			codeEditor.applyPreferences(ShaderEditorApp.preferences);
			if (hasDraftFile) {
				editorDraft.delete();
				hasDraftFile = false;
			}
			return;
		}
		updateToPreferences();
		// Only start listening after EditText restored its content
		// to make sure the initial change is not recorded.
		undoRedo.listenForChanges();
		if (hasDraftFile) {
			editorDraft.delete();
			hasDraftFile = false;
		}
	}

	public void undo() {
		if (codeEditor != null) {
			codeEditor.undo();
			return;
		}
		undoRedo.undo();
	}

	public boolean canUndo() {
		if (codeEditor != null) {
			return codeEditor.canUndo();
		}
		return undoRedo.canUndo();
	}

	public void redo() {
		if (codeEditor != null) {
			codeEditor.redo();
			return;
		}
		undoRedo.redo();
	}

	public boolean canRedo() {
		if (codeEditor != null) {
			return codeEditor.canRedo();
		}
		return undoRedo.canRedo();
	}

	public boolean hasErrors() {
		if (codeEditor != null) {
			return codeEditor.hasErrors();
		}
		return shaderEditor.hasErrors();
	}

	public void clearError() {
		if (codeEditor != null) {
			codeEditor.setErrors(Collections.emptyList());
			return;
		}
		shaderEditor.setErrors(Collections.emptyList());
	}

	public void updateHighlighting() {
		if (codeEditor != null) {
			return;
		}
		shaderEditor.updateHighlighting();
	}

	public void highlightErrors() {
		if (codeEditor != null) {
			codeEditor.markErrors();
			return;
		}
		shaderEditor.updateErrorHighlighting();
	}

	public void setErrors(@NonNull List<ShaderError> errors) {
		if (codeEditor != null) {
			codeEditor.setErrors(errors);
			codeEditor.markErrors();
			return;
		}
		shaderEditor.setErrors(errors);
		highlightErrors();
	}

	public void showErrors() {
		if (codeEditor != null) {
			ErrorListModal.newInstance(codeEditor.getErrors()).show(
					getChildFragmentManager(), ErrorListModal.TAG);
			return;
		}
		List<ShaderError> errors = shaderEditor.getErrors();
		ErrorListModal.newInstance(errors).show(getChildFragmentManager(),
				ErrorListModal.TAG);
	}

	public void navigateToLine(int lineNumber) {
		if (codeEditor != null) {
			codeEditor.navigateToLine(lineNumber);
			return;
		}
		shaderEditor.navigateToLine(lineNumber);
	}


	public String getText() {
		if (codeEditor != null) {
			return codeEditor.getCleanText();
		}
		return shaderEditor.getCleanText();
	}

	public void setText(String text) {
		if (codeEditor != null) {
			codeEditor.setText(text == null ? "" : text);
			return;
		}
		clearError();
		undoRedo.clearHistory();
		undoRedo.stopListeningForChanges();
		shaderEditor.setTextHighlighted(text);
		undoRedo.listenForChanges();
	}

	public void insert(@NonNull CharSequence text) {
		if (codeEditor != null) {
			codeEditor.insert(text);
			return;
		}
		shaderEditor.insert(text);
	}

	public void addUniform(String name) {
		if (codeEditor != null) {
			if (name != null) {
				codeEditor.addUniform(name);
			}
			return;
		}
		shaderEditor.addUniform(name);
	}

	public boolean isCodeVisible() {
		return editorContainer.getVisibility() == View.VISIBLE;
	}

	public boolean toggleCode() {
		boolean visible = isCodeVisible();
		editorContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
		if (visible && codeEditor != null) {
			SoftKeyboard.hide(getActivity(), editorContainer);
			return visible;
		}
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
		if (codeEditor != null) {
			codeEditor.setShowLineNumbers(showLineNumbers);
			return;
		}
		shaderEditor.setShowLineNumbers(showLineNumbers);
	}
}