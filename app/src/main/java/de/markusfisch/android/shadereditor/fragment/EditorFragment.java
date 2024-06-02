package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.opengl.InfoLog;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.view.SoftKeyboard;
import de.markusfisch.android.shadereditor.view.UndoRedo;
import de.markusfisch.android.shadereditor.widget.ShaderEditor;

public class EditorFragment extends Fragment {
	public static final String TAG = "EditorFragment";

	private View editorContainer;
	private ShaderEditor shaderEditor;
	private UndoRedo undoRedo;

	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		View view = inflater.inflate(
				R.layout.fragment_editor,
				container,
				false);

		editorContainer = view.findViewById(R.id.editor_container);
		shaderEditor = view.findViewById(R.id.editor);
		setShowLineNumbers(ShaderEditorApp.preferences.showLineNumbers());
		undoRedo = new UndoRedo(shaderEditor, ShaderEditorApp.editHistory);

		Activity activity = requireActivity();
		if (activity instanceof ShaderEditor.OnTextChangedListener) {
			shaderEditor.setOnTextChangedListener(
					(ShaderEditor.OnTextChangedListener) activity);
			shaderEditor.setOnCompletionsListener(
					(ShaderEditor.CodeCompletionListener) activity);
		} else {
			throw new ClassCastException(activity +
					" must implement " +
					"ShaderEditor.OnTextChangedListener");
		}

		return view;
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

	public boolean hasErrorLine() {
		return shaderEditor.hasErrorLine();
	}

	public void clearError() {
		shaderEditor.setErrorLine(0);
	}

	public void updateHighlighting() {
		shaderEditor.updateHighlighting();
	}

	public void highlightError() {
		shaderEditor.updateErrorHighlighting();
	}

	public void showError(String infoLog) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		InfoLog.parse(infoLog);
		shaderEditor.setErrorLine(InfoLog.getErrorLine());
		highlightError();

		Toast.makeText(
				activity,
				InfoLog.getMessage(),
				Toast.LENGTH_SHORT).show();
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
		shaderEditor.setTextSize(
				TypedValue.COMPLEX_UNIT_SP,
				preferences.getTextSize());
		Typeface font = preferences.getFont();
		shaderEditor.setTypeface(font);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			String features = shaderEditor.getFontFeatureSettings();
			boolean isMono = font == Typeface.MONOSPACE;
			// Don't touch font features for the default MONOSPACE font as
			// this can impact performance.
			if (!isMono || features != null) {
				shaderEditor.setFontFeatureSettings(isMono
						? null
						: preferences.useLigatures() ? "normal" : "calt off");
			}
		}
	}

	public void setShowLineNumbers(boolean showLineNumbers) {
		shaderEditor.setShowLineNumbers(showLineNumbers);
	}
}
