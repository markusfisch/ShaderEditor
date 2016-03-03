package de.markusfisch.android.shadereditor.adapter;

import de.markusfisch.android.shadereditor.R;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

public class ShaderSpinnerAdapter
	extends ShaderAdapter
	implements SpinnerAdapter
{
	public ShaderSpinnerAdapter(
		Context context,
		Cursor cursor )
	{
		super( context, cursor );
	}

	@Override
	public View newDropDownView(
		Context context,
		Cursor cursor,
		ViewGroup parent )
	{
		return LayoutInflater
			.from( parent.getContext() )
			.inflate(
				R.layout.row_shader,
				parent,
				false );
	}

	@Override
	public void bindView(
		View view,
		Context context,
		Cursor cursor )
	{
		setData( getViewHolder( view ), cursor );
	}
}
