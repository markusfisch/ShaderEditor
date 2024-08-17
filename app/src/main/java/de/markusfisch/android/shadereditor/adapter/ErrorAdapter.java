package de.markusfisch.android.shadereditor.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.opengl.ShaderError;

public class ErrorAdapter extends ListAdapter<ShaderError, ErrorAdapter.ViewHolder> {
	@FunctionalInterface
	public interface OnItemClickListener {
		void onItemClick(int lineNumber);
	}

	@NonNull
	private final OnItemClickListener listener;

	public ErrorAdapter(@NonNull OnItemClickListener listener) {
		super(DIFF_CALLBACK);
		this.listener = listener;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new ViewHolder(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.error_item, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		ShaderError error = getItem(position);
		if (error != null) {
			holder.update(error, listener);
		}
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		TextView errorLine;
		TextView errorMessage;

		public ViewHolder(@NonNull View itemView) {
			super(itemView);
			errorLine = itemView.findViewById(R.id.error_line);
			errorMessage = itemView.findViewById(R.id.error_message);
		}

		public void update(@NonNull ShaderError error, @NonNull ErrorAdapter.OnItemClickListener listener) {
			if (error.hasLine()) {
				errorLine.setText(String.format(Locale.getDefault(), "%d: ", error.getLine()));
			} else {
				errorLine.setVisibility(View.GONE);
			}

			errorMessage.setText(error.getMessage());

			itemView.setOnClickListener(v -> listener.onItemClick(error.getLine()));
		}
	}

	private static final DiffUtil.ItemCallback<ShaderError> DIFF_CALLBACK =
			new DiffUtil.ItemCallback<ShaderError>() {
				@Override
				public boolean areItemsTheSame(@NonNull ShaderError oldItem,
						@NonNull ShaderError newItem) {
					// Implement logic to check if items are the same
					return oldItem.equals(newItem);
				}

				@Override
				public boolean areContentsTheSame(@NonNull ShaderError oldItem,
						@NonNull ShaderError newItem) {
					// Implement logic to check if item contents are the same
					return oldItem.equals(newItem);
				}
			};
}