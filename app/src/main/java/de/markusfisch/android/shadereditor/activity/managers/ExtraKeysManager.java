package de.markusfisch.android.shadereditor.activity.managers;

import android.app.Activity;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.CompletionsAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

public class ExtraKeysManager implements ViewTreeObserver.OnGlobalLayoutListener {
	public interface Editor {
		void insert(@NonNull CharSequence text);
	}

	@NonNull
	private final View extraKeysView;
	@NonNull
	private final View insertTabView;
	@NonNull
	private final CompletionsAdapter completionsAdapter;

	public ExtraKeysManager(@NonNull Activity activity,
			@NonNull View rootView, @NonNull Editor editor) {
		this.extraKeysView = rootView.findViewById(R.id.extra_keys);

		insertTabView = extraKeysView.findViewById(R.id.insert_tab);
		insertTabView.setOnClickListener(v -> editor.insert("\t"));

		RecyclerView completions = extraKeysView.findViewById(R.id.completions);
		completions.setLayoutManager(new LinearLayoutManager(
				activity, RecyclerView.HORIZONTAL, false));
		completionsAdapter = new CompletionsAdapter(editor::insert);
		completions.setAdapter(completionsAdapter);

		var divider = new DividerItemDecoration(
				completions.getContext(), DividerItemDecoration.HORIZONTAL);
		divider.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(
				activity, R.drawable.divider_with_padding_vertical)));
		completions.addItemDecoration(divider);
		completions.getViewTreeObserver().addOnGlobalLayoutListener(this);

		updateVisibility();
	}

	public void setCompletions(List<String> completions, int position) {
		completionsAdapter.submitCompletions(completions, position);
	}

	public void updateVisibility() {
		var preferences = ShaderEditorApp.preferences;
		extraKeysView.setVisibility(
				getVisibility(preferences.showExtraKeys()));
		insertTabView.setVisibility(
				getVisibility(preferences.doesShowInsertTab()));
	}

	public void setVisible(boolean visible) {
		extraKeysView.setVisibility(getVisibility(visible));
	}

	@Override
	public void onGlobalLayout() {
		var preferences = ShaderEditorApp.preferences;
		if (!preferences.autoHideExtraKeys()) {
			return;
		}
		if (!preferences.showExtraKeys()) {
			extraKeysView.setVisibility(View.GONE);
			return;
		}

		WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(extraKeysView);
		extraKeysView.setVisibility(getVisibility(insets != null &&
				insets.isVisible(WindowInsetsCompat.Type.ime())));
	}

	private static int getVisibility(boolean isVisible) {
		return isVisible ? View.VISIBLE : View.GONE;
	}
}
