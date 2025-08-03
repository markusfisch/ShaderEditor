package de.markusfisch.android.shadereditor.adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.DataRecords.TextureInfo;

public class TextureAdapter extends BaseAdapter {
	private final String sizeFormat;
	private final List<TextureInfo> textures = new ArrayList<>();

	public TextureAdapter(@NonNull Context context) {
		sizeFormat = context.getString(R.string.texture_size_format);
	}

	@Override
	public int getCount() {
		return textures.size();
	}

	@Override
	public TextureInfo getItem(int position) {
		return textures.get(position);
	}

	@Override
	public long getItemId(int position) {
		return textures.get(position).id();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(parent.getContext()).inflate(
					R.layout.row_texture, parent, false);
			holder = new ViewHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.bindTo(textures.get(position));
		return convertView;
	}

	public void setData(List<TextureInfo> newTextures) {
		this.textures.clear();
		if (newTextures != null) {
			this.textures.addAll(newTextures);
		}
		notifyDataSetChanged();
	}

	private class ViewHolder {
		private final ImageView preview;
		private final TextView name;
		private final TextView size;

		ViewHolder(View itemView) {
			preview = itemView.findViewById(R.id.texture_preview);
			name = itemView.findViewById(R.id.texture_name);
			size = itemView.findViewById(R.id.texture_size);
		}

		void bindTo(TextureInfo texture) {
			byte[] bytes = texture.thumb();
			if (bytes != null && bytes.length > 0) {
				preview.setImageBitmap(BitmapFactory.decodeByteArray(
						bytes, 0, bytes.length));
			} else {
				preview.setImageBitmap(null);
			}

			name.setText(texture.name());
			size.setText(String.format(sizeFormat, texture.width(), texture.height()));
		}
	}
}