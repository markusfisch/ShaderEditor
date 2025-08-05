package de.markusfisch.android.shadereditor.fragment;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.markusfisch.android.shadereditor.activity.AddUniformActivity;

public abstract class AddUniformPageFragment extends Fragment {
	@Override
	public void onResume() {
		super.onResume();
		if (requireActivity() instanceof AddUniformActivity addUniformActivity) {
			addUniformActivity.setSearchListener(this::onSearch);
			var currentSearchQuery = addUniformActivity.getCurrentSearchQuery();
			if (currentSearchQuery != null) {
				onSearch(currentSearchQuery);
			}
		}
	}

	protected abstract void onSearch(@Nullable String query);
}
