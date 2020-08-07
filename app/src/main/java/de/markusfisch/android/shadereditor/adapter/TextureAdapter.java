package de.markusfisch.android.shadereditor.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.Database;

public class TextureAdapter extends CursorAdapter {
	private final String sizeFormat;

	private int nameIndex;
	private int widthIndex;
	private int heightIndex;
	private int thumbIndex;

	public TextureAdapter(Context context, Cursor cursor) {
		super(context, cursor, false);

		indexColumns(cursor);
		sizeFormat = context.getString(R.string.texture_size_format);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		return inflater.inflate(R.layout.row_texture, parent, false);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = getViewHolder(view);
		setData(holder, cursor);
	}

	ViewHolder getViewHolder(View view) {
		ViewHolder holder;
		if ((holder = (ViewHolder) view.getTag()) == null) {
			holder = new ViewHolder();
			holder.preview = view.findViewById(R.id.texture_preview);
			holder.name = view.findViewById(R.id.texture_name);
			holder.size = view.findViewById(R.id.texture_size);
			view.setTag(holder);
		}

		return holder;
	}

	void setData(ViewHolder holder, Cursor cursor) {
		byte[] bytes = cursor.getBlob(thumbIndex);

		if (bytes != null && bytes.length > 0) {
			holder.preview.setImageBitmap(BitmapFactory.decodeByteArray(
					bytes,
					0,
					bytes.length));
		}

		holder.name.setText(cursor.getString(nameIndex));
		holder.size.setText(String.format(
				sizeFormat,
				cursor.getInt(widthIndex),
				cursor.getInt(heightIndex)));
	}

	private void indexColumns(Cursor cursor) {
		nameIndex = cursor.getColumnIndex(
				Database.TEXTURES_NAME);
		widthIndex = cursor.getColumnIndex(
				Database.TEXTURES_WIDTH);
		heightIndex = cursor.getColumnIndex(
				Database.TEXTURES_HEIGHT);
		thumbIndex = cursor.getColumnIndex(
				Database.TEXTURES_THUMB);
	}

	private static final class ViewHolder {
		private ImageView preview;
		private TextView name;
		private TextView size;
	}
}
