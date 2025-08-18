package de.markusfisch.android.shadereditor.activity.managers;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

public class MainMenuManager {
	public interface EditorActions {
		void onUndo();

		void onRedo();

		boolean canUndo();

		boolean canRedo();
	}

	public interface ShaderActions {
		void onAddShader();

		void onSaveShader();

		void onDuplicateShader();

		void onDeleteShader();

		void onShareShader();

		void onUpdateWallpaper();

		void onToggleExtraKeys();

		long getSelectedShaderId();
	}

	public interface NavigationActions {
		void onAddUniform();

		void onLoadSample();

		void onShowSettings();

		void onShowFaq();
	}

	@NonNull
	private final PopupWindow popupWindow;
	@NonNull
	private final EditorActions editorActions;
	@NonNull
	private final ShaderActions shaderActions;
	@NonNull
	private final Activity activity;

	public MainMenuManager(@NonNull Activity activity,
			@NonNull EditorActions editorActions,
			@NonNull ShaderActions shaderActions,
			@NonNull NavigationActions navigationActions) {
		this.activity = activity;
		this.editorActions = editorActions;
		this.shaderActions = shaderActions;
		ViewGroup root = activity.findViewById(android.R.id.content);
		View view = activity.getLayoutInflater().inflate(
				R.layout.main_menu, root, false);
		view.setClipToOutline(true);

		popupWindow = new PopupWindow(view,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				true);
		popupWindow.setOutsideTouchable(true);
		popupWindow.setFocusable(true);
		popupWindow.setElevation(16f);
		popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);

		// Wire up listeners
		setClickListener(R.id.undo, () -> {
			editorActions.onUndo();
			updateUndoRedoMenu(popupWindow.getContentView());
		}, false);
		setClickListener(R.id.redo, () -> {
			editorActions.onRedo();
			updateUndoRedoMenu(popupWindow.getContentView());
		}, false);
		setClickListener(R.id.add_shader, shaderActions::onAddShader);
		setClickListener(R.id.save_shader, shaderActions::onSaveShader);
		setClickListener(R.id.duplicate_shader, shaderActions::onDuplicateShader);
		setClickListener(R.id.delete_shader, shaderActions::onDeleteShader);
		setClickListener(R.id.share_shader, shaderActions::onShareShader);
		setClickListener(R.id.update_wallpaper, shaderActions::onUpdateWallpaper);
		setClickListener(R.id.add_uniform, navigationActions::onAddUniform);
		setClickListener(R.id.load_sample, navigationActions::onLoadSample);
		setClickListener(R.id.settings, navigationActions::onShowSettings);
		setClickListener(R.id.faq, navigationActions::onShowFaq);

		CompoundButton extraKeysToggle = view.findViewById(
				R.id.show_extra_keys_box);
		extraKeysToggle.setOnClickListener(v -> {
			shaderActions.onToggleExtraKeys();
			updateExtraKeysToggle(extraKeysToggle,
					ShaderEditorApp.preferences.showExtraKeys());
		});
	}

	public void show(@NonNull View anchor) {
		if (popupWindow.isShowing()) {
			popupWindow.dismiss();
			return;
		}
		prepareMenu();
		resizeAndShow(anchor);
	}

	private void updateUndoRedoMenu(@NonNull View menuView) {
		View undo = menuView.findViewById(R.id.undo);
		undo.setEnabled(editorActions.canUndo());
		View redo = menuView.findViewById(R.id.redo);
		redo.setEnabled(editorActions.canRedo());
	}

	private void prepareMenu() {
		View menuView = popupWindow.getContentView();
		updateUndoRedoMenu(menuView);

		long selectedShaderId = shaderActions.getSelectedShaderId();
		((Button) menuView.findViewById(R.id.update_wallpaper)).setText(
				ShaderEditorApp.preferences.getWallpaperShader() == selectedShaderId
						? R.string.update_wallpaper
						: R.string.set_as_wallpaper);

		updateExtraKeysToggle(menuView.findViewById(R.id.show_extra_keys_box),
				ShaderEditorApp.preferences.showExtraKeys());
	}

	public void updateExtraKeysToggle(@NonNull CompoundButton extraKeysToggle,
			boolean visible) {
		extraKeysToggle.setChecked(visible);
		Drawable drawable = ContextCompat.getDrawable(activity, visible
				? R.drawable.ic_bottom_panel_close
				: R.drawable.ic_bottom_panel_open);
		Drawable[] drawables = extraKeysToggle.getCompoundDrawablesRelative();
		extraKeysToggle.setCompoundDrawablesRelativeWithIntrinsicBounds(
				drawable, drawables[1], drawables[2], drawables[3]);
	}

	private void resizeAndShow(@NonNull View anchor) {
		Rect screenRect = new Rect();
		anchor.getWindowVisibleDisplayFrame(screenRect);
		int screenHeight = screenRect.height();

		DisplayMetrics displayMetrics =
				activity.getResources().getDisplayMetrics();
		int padding = (int) (100 * displayMetrics.density);
		int maxHeight = screenHeight - padding;

		View contentView = popupWindow.getContentView();
		contentView.measure(View.MeasureSpec.UNSPECIFIED,
				View.MeasureSpec.UNSPECIFIED);
		popupWindow.setHeight(Math.min(maxHeight,
				contentView.getMeasuredHeight()));
		popupWindow.showAsDropDown(anchor,
				anchor.getWidth() - contentView.getMeasuredWidth(),
				-anchor.getHeight());
	}

	private void setClickListener(int id, @NonNull Runnable action,
			boolean dismiss) {
		popupWindow.getContentView().findViewById(id).setOnClickListener(v -> {
			action.run();
			if (dismiss) {
				popupWindow.dismiss();
			}
		});
	}

	private void setClickListener(int id, @NonNull Runnable action) {
		setClickListener(id, action, true);
	}
}
