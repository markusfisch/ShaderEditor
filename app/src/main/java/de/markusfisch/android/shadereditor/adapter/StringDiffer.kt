package de.markusfisch.android.shadereditor.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class StringDiffer extends DiffUtil.ItemCallback<String> {
	@Override
	public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
		// Update the condition according to your unique identifier
		return oldItem.equals(newItem);
	}

	@Override
	public boolean areContentsTheSame(@NonNull String oldItem,
			@NonNull String newItem) {
		// Return true if the contents of the items have not changed
		return oldItem.equals(newItem);
	}
}
