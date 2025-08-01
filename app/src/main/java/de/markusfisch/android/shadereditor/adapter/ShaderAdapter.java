package de.markusfisch.android.shadereditor.adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.DataRecords.ShaderInfo;

public class ShaderAdapter extends BaseAdapter {
	private final int textColorSelected;
	private final int textColorUnselected;
	private List<ShaderInfo> shaders = new ArrayList<>();
	private long selectedShaderId = 0;

	public ShaderAdapter(Context context) {
		this.textColorSelected = ContextCompat.getColor(context, R.color.accent);
		this.textColorUnselected = ContextCompat.getColor(context, R.color.drawer_text_unselected);
	}

	@Override
	public int getCount() {
		return shaders.size();
	}

	@Override
	public Object getItem(int position) {
		return shaders.get(position);
	}

	@Override
	public long getItemId(int position) {
		return shaders.get(position).id();
	}

	public String getName(int position) {
		return shaders.get(position).name();
	}

	public String getTitle(int position) {
		ShaderInfo shader = shaders.get(position);
		String title = shader.name();
		return title != null && !title.isEmpty()
				? title
				: shader.modified();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(parent.getContext()).inflate(
					R.layout.row_shader,
					parent,
					false);
			holder = new ViewHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.bindTo(shaders.get(position));
		return convertView;
	}

	public void setSelectedId(long id) {
		selectedShaderId = id;
		notifyDataSetChanged();
	}

	public void setData(List<ShaderInfo> newShaders) {
		this.shaders.clear();
		if (newShaders != null) {
			this.shaders.addAll(newShaders);
		}
		notifyDataSetChanged();
	}

	private class ViewHolder {
		private final ImageView icon;
		private final TextView title;

		ViewHolder(View itemView) {
			icon = itemView.findViewById(R.id.shader_icon);
			title = itemView.findViewById(R.id.shader_title);
		}

		void bindTo(final ShaderInfo shader) {
			byte[] bytes = shader.thumb();
			if (bytes != null && bytes.length > 0) {
				icon.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
			} else {
				icon.setImageResource(R.drawable.thumbnail_new_shader);
			}

			String name = shader.name();
			title.setText(name != null && !name.isEmpty() ? name : shader.modified());
			title.setTextColor(shader.id() == selectedShaderId ? textColorSelected :
                    textColorUnselected);
		}
	}
}