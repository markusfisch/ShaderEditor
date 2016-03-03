package de.markusfisch.android.shadereditor.adapter;

import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.R;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ShaderAdapter extends CursorAdapter
{
	private int idIndex;
	private int thumbIndex;
	private int modifiedIndex;
	private int textColorSelected;
	private int textColorUnselected;
	private long selectedShaderId;

	public ShaderAdapter(
		Context context,
		Cursor cursor )
	{
		super( context, cursor, false );

		indexColumns( cursor );
		initTextColors( context );
	}

	public void setSelectedId( long id )
	{
		selectedShaderId = id;
	}

	@Override
	public View newView(
		Context context,
		Cursor cursor,
		ViewGroup parent )
	{
		LayoutInflater inflater = LayoutInflater.from(
			parent.getContext() );

		return inflater.inflate(
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
		ViewHolder holder = getViewHolder( view );

		setData( holder, cursor );

		holder.title.setTextColor(
			cursor.getLong( idIndex ) == selectedShaderId ?
				textColorSelected :
				textColorUnselected );
	}

	protected ViewHolder getViewHolder( View view )
	{
		ViewHolder holder;

		if( (holder = (ViewHolder)view.getTag()) == null )
		{
			holder = new ViewHolder();
			holder.icon = (ImageView)view.findViewById(
				R.id.shader_icon );
			holder.title = (TextView)view.findViewById(
				R.id.shader_title );
		}

		return holder;
	}

	protected void setData( ViewHolder holder, Cursor cursor )
	{
		byte bytes[] = cursor.getBlob( thumbIndex );

		if( bytes != null &&
			bytes.length > 0 )
			holder.icon.setImageBitmap(
				BitmapFactory.decodeByteArray(
					bytes,
					0,
					bytes.length ) );

		holder.title.setText( cursor.getString( modifiedIndex ) );
	}

	private void indexColumns( Cursor cursor )
	{
		idIndex = cursor.getColumnIndex(
			DataSource.SHADERS_ID );
		thumbIndex = cursor.getColumnIndex(
			DataSource.SHADERS_THUMB );
		modifiedIndex = cursor.getColumnIndex(
			DataSource.SHADERS_MODIFIED );
	}

	private static final class ViewHolder
	{
		public ImageView icon;
		public TextView title;
	}

	private void initTextColors( Context context )
	{
		textColorSelected = ContextCompat.getColor(
			context,
			R.color.accent );
		textColorUnselected = ContextCompat.getColor(
			context,
			R.color.drawer_text_unselected );
	}
}
