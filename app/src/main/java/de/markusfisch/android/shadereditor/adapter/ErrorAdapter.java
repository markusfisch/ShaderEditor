package de.markusfisch.android.shadereditor.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.opengl.ShaderError;

public class ErrorAdapter extends ArrayAdapter<ShaderError> {
	@FunctionalInterface
	public interface OnItemClickListener {
		void onItemClick(int lineNumber);
	}

	@NonNull
	private final OnItemClickListener listener;

	public ErrorAdapter(@NonNull Context context, @NonNull List<ShaderError> errors,
			@NonNull OnItemClickListener listener) {
		super(context, 0, errors);
		this.listener = listener;
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.error_item, parent,
					false);
		}

		ShaderError error = getItem(position);

		if (error != null) {
			TextView errorLine = convertView.findViewById(R.id.error_line);
			errorLine.setText(String.format(Locale.getDefault(), "%d: ", error.getErrorLine()));
			TextView errorMessage = convertView.findViewById(R.id.error_message);
			errorMessage.setText(error.getMessage());

			convertView.setOnClickListener(v -> {
				int lineNumber = error.getErrorLine();
				listener.onItemClick(lineNumber);
			});
		}

		return convertView;
	}
}