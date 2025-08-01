package de.markusfisch.android.shadereditor.activity.managers;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.CompletionsAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

public class ExtraKeysManager implements ViewTreeObserver.OnGlobalLayoutListener {
	@NonNull
	private final View extraKeysView;
	@NonNull
	private final CompletionsAdapter completionsAdapter;

	public ExtraKeysManager(@NonNull Activity activity, @NonNull View rootView,
			@NonNull Editor editor) {
		this.extraKeysView = rootView.findViewById(R.id.extra_keys);

		extraKeysView.findViewById(R.id.insert_tab).setOnClickListener(v -> editor.insert("\t"));
		RecyclerView completions = extraKeysView.findViewById(R.id.completions);
		completions.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.HORIZONTAL,
				false));
		completionsAdapter = new CompletionsAdapter(editor::insert);
		completions.setAdapter(completionsAdapter);

		DividerItemDecoration divider = new DividerItemDecoration(completions.getContext(),
				DividerItemDecoration.HORIZONTAL);
		divider.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(activity,
				R.drawable.divider_with_padding_vertical)));
		completions.addItemDecoration(divider);
		completions.getViewTreeObserver().addOnGlobalLayoutListener(this);

		updateVisibility();
	}

	public void setCompletions(List<String> completions, int position) {
		completionsAdapter.setPosition(position);
		completionsAdapter.submitList(completions);
	}

	public void updateVisibility() {
		extraKeysView.setVisibility(ShaderEditorApp.preferences.showExtraKeys() ? View.VISIBLE :
				View.GONE);
		extraKeysView.findViewById(R.id.insert_tab).setVisibility(ShaderEditorApp.preferences.doesShowInsertTab() ? View.VISIBLE : View.GONE);
	}

	public void setVisible(boolean visible) {
		extraKeysView.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	@Override
	public void onGlobalLayout() {
		if (!ShaderEditorApp.preferences.autoHideExtraKeys()) return;

		Rect r = new Rect();
		extraKeysView.getWindowVisibleDisplayFrame(r);
		int screenHeight = extraKeysView.getRootView().getHeight();
		int keypadHeight = screenHeight - r.bottom;

		if (keypadHeight > screenHeight * 0.15) {
			// Keyboard is showing
			if (ShaderEditorApp.preferences.showExtraKeys()) {
				extraKeysView.setVisibility(View.VISIBLE);
			}
		} else {
			// Keyboard is hidden
			extraKeysView.setVisibility(View.GONE);
		}
	}

	public interface Editor {
		void insert(@NonNull CharSequence text);
	}
}