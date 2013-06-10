package de.markusfisch.android.shadereditor;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.io.ByteArrayInputStream;

public class ShaderAdapter
	extends CursorAdapter
	implements SpinnerAdapter
{
	private LayoutInflater inflater = null;
	private int layout = 0;

	public ShaderAdapter( Context context, Cursor cursor )
	{
		super( context, cursor, false );
		init( context );
	}

	public ShaderAdapter(
		Context context,
		Cursor cursor,
		int layout )
	{
		super( context, cursor, false );
		init( context );

		this.layout = layout;
	}

	@Override
	public View newView(
		Context context,
		Cursor cursor,
		ViewGroup parent )
	{
		LayoutInflater i = LayoutInflater.from(
			parent.getContext() );

		return i.inflate(
			layout > 0 ? layout : R.layout.shader_spinner,
			parent,
			false );
	}

	@Override
	public View newDropDownView(
		Context context,
		Cursor cursor,
		ViewGroup parent )
	{
		LayoutInflater i = LayoutInflater.from(
			parent.getContext() );

		return i.inflate(
			R.layout.shader_spinner_dropdown,
			parent,
			false );
	}

	@Override
	public void bindView( View view, Context context, Cursor cursor )
	{
		final long id = cursor.getLong( cursor.getColumnIndex(
			ShaderDataSource.COLUMN_ID ) );

		// set icon
		{
			ImageView v = (ImageView)view.findViewById(
				R.id.icon );
			byte bytes[] = cursor.getBlob( cursor.getColumnIndex(
				ShaderDataSource.COLUMN_THUMB ) );

			if( v != null &&
				bytes != null &&
				bytes.length > 0 )
			{
				ByteArrayInputStream in =
					new ByteArrayInputStream( bytes );
				Bitmap b = BitmapFactory.decodeStream( in );

				if( b != null )
					v.setImageBitmap( b );
			}
		}

		// set title
		{
			TextView v = (TextView)view.findViewById(
				R.id.title );

			if( v != null )
				v.setText( cursor.getString( cursor.getColumnIndex(
					ShaderDataSource.COLUMN_MODIFIED ) ) );
		}
	}

	private void init( Context context )
	{
		inflater = LayoutInflater.from( context );
	}
}
