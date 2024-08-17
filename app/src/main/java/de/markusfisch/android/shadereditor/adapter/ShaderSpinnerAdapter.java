package de.markusfisch.android.shadereditor.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

import androidx.annotation.NonNull;

import de.markusfisch.android.shadereditor.R;

public class ShaderSpinnerAdapter
		extends ShaderAdapter
		implements SpinnerAdapter {
	public ShaderSpinnerAdapter(@NonNull Context context, @NonNull Cursor cursor) {
		super(context, cursor);
	}

	@Override
	public View newDropDownView(
			Context context,
			Cursor cursor,
			@NonNull ViewGroup parent) {
		return LayoutInflater
				.from(parent.getContext())
				.inflate(R.layout.row_shader, parent, false);
	}

	@Override
	public void bindView(@NonNull View view, Context context, @NonNull Cursor cursor) {
		setData(getViewHolder(view), cursor);
	}
}
