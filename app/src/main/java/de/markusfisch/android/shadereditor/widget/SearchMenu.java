package de.markusfisch.android.shadereditor.widget;

import android.view.Menu;
import android.view.MenuInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;

import java.util.Objects;

import de.markusfisch.android.shadereditor.R;

public class SearchMenu {
	public interface OnSearchListener {
		void filter(@Nullable String value);
	}

	public static void addSearchMenu(@NonNull Menu menu,
			@NonNull MenuInflater inflater,
			@NonNull OnSearchListener onSearchListener) {
		inflater.inflate(R.menu.search, menu);
		SearchView searchView =
				(SearchView) Objects.requireNonNull(menu.findItem(R.id.search).getActionView());
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String newText) {
				onSearchListener.filter(newText);
				return false;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				onSearchListener.filter(query);
				return true;
			}
		});
		searchView.setOnCloseListener(() -> {
			onSearchListener.filter(null);
			return false;
		});
	}
}
