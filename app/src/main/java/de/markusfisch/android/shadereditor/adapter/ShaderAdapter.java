package de.markusfisch.android.shadereditor.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.Database;

public class ShaderAdapter extends CursorAdapter {
	private int idIndex;
	private int thumbIndex;
	private int nameIndex;
	private int modifiedIndex;
	private int textColorSelected;
	private int textColorUnselected;
	private long selectedShaderId;

	public ShaderAdapter(Context context, Cursor cursor) {
		super(context, cursor, false);

		indexColumns(cursor);
		initTextColors(context);
	}

	public void setSelectedId(long id) {
		selectedShaderId = id;
	}

	public String getName(int position) {
		Cursor cursor = (Cursor) getItem(position);
		return cursor != null ? cursor.getString(nameIndex) : null;
	}

	public String getTitle(Cursor cursor) {
		String title = cursor.getString(nameIndex);
		return title != null && !title.isEmpty()
				? title
				: cursor.getString(modifiedIndex);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		return inflater.inflate(
				R.layout.row_shader,
				parent,
				false);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = getViewHolder(view);
		setData(holder, cursor);

		holder.title.setTextColor(
				cursor.getLong(idIndex) == selectedShaderId
						? textColorSelected
						: textColorUnselected);
	}

	ViewHolder getViewHolder(View view) {
		ViewHolder holder;

		if ((holder = (ViewHolder) view.getTag()) == null) {
			holder = new ViewHolder();
			holder.icon = view.findViewById(R.id.shader_icon);
			holder.title = view.findViewById(R.id.shader_title);
			view.setTag(holder);
		}

		return holder;
	}

	void setData(ViewHolder holder, Cursor cursor) {
		byte[] bytes = cursor.getBlob(thumbIndex);
		Bitmap bitmap = null;

		if (bytes != null && bytes.length > 0) {
			bitmap = BitmapFactory.decodeByteArray(
					bytes,
					0,
					bytes.length);
		}

		holder.icon.setImageBitmap(bitmap);
		holder.title.setText(getTitle(cursor));
	}

	private void indexColumns(Cursor cursor) {
		idIndex = cursor.getColumnIndex(
				Database.SHADERS_ID);
		thumbIndex = cursor.getColumnIndex(
				Database.SHADERS_THUMB);
		nameIndex = cursor.getColumnIndex(
				Database.SHADERS_NAME);
		modifiedIndex = cursor.getColumnIndex(
				Database.SHADERS_MODIFIED);
	}

	private static final class ViewHolder {
		private ImageView icon;
		private TextView title;
	}

	private void initTextColors(Context context) {
		textColorSelected = ContextCompat.getColor(
				context,
				R.color.accent);
		textColorUnselected = ContextCompat.getColor(
				context,
				R.color.drawer_text_unselected);
	}
}
