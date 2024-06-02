package de.markusfisch.android.shadereditor.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import de.markusfisch.android.shadereditor.R;

public class CompletionsAdapter extends ListAdapter<String, CompletionsAdapter.ViewHolder> {
	public interface OnInsertListener {
		void onInsert(CharSequence sequence);
	}

	private static final DiffUtil.ItemCallback<String> DIFF_CALLBACK =
			new DiffUtil.ItemCallback<String>() {
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
			};

	@NonNull
	private final LayoutInflater inflater;
	private final OnInsertListener onInsertListener;

	private int position = 0;

	public CompletionsAdapter(Context context, OnInsertListener onInsertListener) {
		super(DIFF_CALLBACK);
		this.inflater = LayoutInflater.from(context);
		this.onInsertListener = onInsertListener;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = inflater.inflate(R.layout.extra_key_btn, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		String item = getItem(position);
		holder.update(item);
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		private final Button btn;

		public ViewHolder(@NonNull View itemView) {
			super(itemView);
			btn = itemView.findViewById(R.id.btn);
			btn.setOnClickListener((v) -> {
				CharSequence text = btn.getText();
				onInsertListener.onInsert(text.subSequence(position, text.length()));
			});
			itemView.setOnTouchListener(new View.OnTouchListener() {
				@SuppressLint("ClickableViewAccessibility")
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return btn.onTouchEvent(event); // The FrameLayout always forwards
				}
			});
		}

		public void update(String item) {
			btn.setText(item);
		}
	}
}
