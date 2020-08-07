package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.opengl.InfoLog;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.view.SoftKeyboard;
import de.markusfisch.android.shadereditor.view.UndoRedo;
import de.markusfisch.android.shadereditor.widget.ShaderEditor;

public class EditorFragment extends Fragment {
	public static final String TAG = "EditorFragment";

	private ScrollView scrollView;
	private ShaderEditor shaderEditor;
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
		shaderEditor = view.findViewById(R.id.editor);
		undoRedo = new UndoRedo(shaderEditor, ShaderEditorApp.editHistory);

		Activity activity = getActivity();
		try {
			shaderEditor.setOnTextChangedListener(
					(ShaderEditor.OnTextChangedListener) activity);
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() +
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
		// only start listening after EditText restored its content
		// to make sure the initial change is not recorded
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

	private void updateToPreferences() {
		Preferences preferences = ShaderEditorApp.preferences;
		shaderEditor.setUpdateDelay(preferences.getUpdateDelay());
		shaderEditor.setTextSize(
				android.util.TypedValue.COMPLEX_UNIT_SP,
				preferences.getTextSize());
		shaderEditor.setTabWidth(preferences.getTabWidth());
	}

	private int getYOffset(Activity activity) {
		if (yOffset == 0) {
			float dp = getResources().getDisplayMetrics().density;

			try {
				ActionBar actionBar = ((AppCompatActivity) activity)
						.getSupportActionBar();

				if (actionBar != null) {
					yOffset = actionBar.getHeight();
				}
			} catch (ClassCastException e) {
				yOffset = Math.round(48f * dp);
			}

			yOffset += Math.round(16f * dp);
		}

		return yOffset;
	}
}
