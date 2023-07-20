package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.opengl.InfoLog;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.view.SoftKeyboard;
import de.markusfisch.android.shadereditor.view.UndoRedo;
import de.markusfisch.android.shadereditor.widget.LineNumbers;
import de.markusfisch.android.shadereditor.widget.ShaderEditor;
import de.markusfisch.android.shadereditor.widget.SyntaxView;

public class EditorFragment extends Fragment {
	public static final String TAG = "EditorFragment";

	private ScrollView scrollView;
	private ShaderEditor shaderEditor;
	private LineNumbers lineNumbers;
	private ViewGroup lineNumbersContainer;
	private SyntaxView syntaxView;
	private UndoRedo undoRedo;
	private int yOffset;

	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		View view = inflater.inflate(
				R.layout.fragment_editor,
				container,
				false);

		scrollView = view.findViewById(R.id.scroll_view);
		lineNumbers = view.findViewById(R.id.line_numbers);
		lineNumbersContainer = view.findViewById(R.id.line_numbers_container);
		shaderEditor = view.findViewById(R.id.editor);
		syntaxView = view.findViewById(R.id.syntax);
		ViewGroup editorContainer = view.findViewById(R.id.editor_container);
		editorContainer.setOnClickListener((v) -> {
			shaderEditor.requestFocus();
			InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(shaderEditor, InputMethodManager.SHOW_FORCED);
		});
		// lineNumbersContainer = view.findViewById(R.id.line_numbers_container);
		setShowLineNumbers(ShaderEditorApp.preferences.showLineNumbers());
		shaderEditor.setTabSupplier(ShaderEditorApp.preferences::getTabWidth);
		shaderEditor.setSizeProvider(new ShaderEditor.SizeProvider() {
			@Override
			public int getWidth() {
				return syntaxView.getMaxX();
			}

			@Override
			public int getHeight() {
				return syntaxView.getMaxY();
			}
		});
		syntaxView.setTabSupplier(ShaderEditorApp.preferences::getTabWidth);
		lineNumbers.setSource(shaderEditor);
		syntaxView.setSource(shaderEditor);
		undoRedo = new UndoRedo(shaderEditor, ShaderEditorApp.editHistory);

		Activity activity = requireActivity();
		if (activity instanceof ShaderEditor.OnTextChangedListener) {
			shaderEditor.setOnTextChangedListener(
					(ShaderEditor.OnTextChangedListener) activity);
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

	public void redo() {
		undoRedo.redo();
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

	public void showError(String infoLog) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		InfoLog.parse(infoLog);
		shaderEditor.setErrorLine(InfoLog.getErrorLine());
		updateHighlighting();

		Toast errorToast = Toast.makeText(
				activity,
				InfoLog.getMessage(),
				Toast.LENGTH_SHORT);
		errorToast.setGravity(
				Gravity.TOP | Gravity.CENTER_HORIZONTAL,
				0,
				getYOffset(activity));
		errorToast.show();
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

	public void insertTab() {
		shaderEditor.insertTab();
	}

	public void addUniform(String name) {
		shaderEditor.addUniform(name);
	}

	public boolean isCodeVisible() {
		return scrollView.getVisibility() == View.VISIBLE;
	}

	public boolean toggleCode() {
		boolean visible = isCodeVisible();
		scrollView.setVisibility(visible ? View.GONE : View.VISIBLE);
		if (visible) {
			SoftKeyboard.hide(getActivity(), shaderEditor);
		}
		return visible;
	}

	public void highlightError() {
		shaderEditor.updateErrorHighlighting();
	}

	public void setShowLineNumbers(boolean showLineNumbers) {
		lineNumbersContainer.setVisibility(showLineNumbers ? View.VISIBLE : View.GONE);
	}

	private void updateToPreferences() {
		Preferences preferences = ShaderEditorApp.preferences;
		shaderEditor.setUpdateDelay(preferences.getUpdateDelay());
		shaderEditor.setTextSize(
				TypedValue.COMPLEX_UNIT_SP,
				preferences.getTextSize());
		lineNumbers.setTextSize(TypedValue.COMPLEX_UNIT_SP,
				preferences.getTextSize());
		shaderEditor.setTypeface(preferences.getFont());
		lineNumbers.setTypeface(preferences.getFont());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			shaderEditor.setFontFeatureSettings(preferences.useLigatures() ? "normal" : "calt off");
		}
	}

	private int getYOffset(Activity activity) {
		if (yOffset == 0) {
			float dp = getResources().getDisplayMetrics().density;
			if (activity instanceof AppCompatActivity) {
				ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
				if (actionBar != null) {
					yOffset = Math.round(48f * dp);
				}
			} else {
				yOffset = Math.round(48f * dp);
			}
			yOffset += Math.round(16f * dp);
		}

		return yOffset;
	}
}
