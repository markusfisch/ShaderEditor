package de.markusfisch.android.shadereditor.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.markusfisch.android.shadereditor.R;

public class SamplesAdapter extends BaseAdapter {
	public static final class Sample {
		public final int resId;
		public final int thumbId;
		public final float quality;
		public final String name;
		public final String rationale;

		private Sample(
				int resId,
				int thumbId,
				String name,
				String rationale) {
			this(resId, thumbId, name, rationale, 1f);
		}

		private Sample(
				int resId,
				int thumbId,
				String name,
				String rationale,
				float quality) {
			this.resId = resId;
			this.thumbId = thumbId;
			this.name = name;
			this.rationale = rationale;
			this.quality = quality;
		}
	}

	private final Sample[] samples;

	public SamplesAdapter(Context context) {
		samples = new Sample[]{
				new Sample(R.raw.sample_battery,
						R.drawable.thumbnail_battery,
						context.getString(R.string.sample_battery_name),
						context.getString(R.string.sample_battery_rationale)),
				new Sample(R.raw.sample_camera_back,
						R.drawable.thumbnail_camera_back,
						context.getString(R.string.sample_camera_back_name),
						context.getString(R.string.sample_camera_back_rationale)),
				new Sample(R.raw.sample_circles,
						R.drawable.thumbnail_circles,
						context.getString(R.string.sample_circles_name),
						context.getString(R.string.sample_circles_rationale)),
				new Sample(R.raw.sample_cloudy_conway,
						R.drawable.thumbnail_cloudy_conway,
						context.getString(R.string.sample_cloudy_conway_name),
						context.getString(R.string.sample_cloudy_conway_rationale),
						.125f),
				new Sample(R.raw.sample_game_of_life,
						R.drawable.thumbnail_game_of_life,
						context.getString(R.string.sample_game_of_life_name),
						context.getString(R.string.sample_game_of_life_rationale),
						.125f),
				new Sample(R.raw.sample_gles_300,
						R.drawable.thumbnail_default,
						context.getString(R.string.sample_gles_300_name),
						context.getString(R.string.sample_gles_300_rationale)),
				new Sample(R.raw.sample_gravity,
						R.drawable.thumbnail_gravity,
						context.getString(R.string.sample_gravity_name),
						context.getString(R.string.sample_gravity_rationale)),
				new Sample(R.raw.sample_orientation,
						R.drawable.thumbnail_orientation,
						context.getString(R.string.sample_orientation_name),
						context.getString(R.string.sample_orientation_rationale)),
				new Sample(R.raw.sample_swirl,
						R.drawable.thumbnail_swirl,
						context.getString(R.string.sample_swirl_name),
						context.getString(R.string.sample_swirl_rationale)),
				new Sample(R.raw.sample_texture,
						R.drawable.thumbnail_texture,
						context.getString(R.string.sample_texture_name),
						context.getString(R.string.sample_texture_rationale)),
				new Sample(R.raw.sample_touch,
						R.drawable.thumbnail_touch,
						context.getString(R.string.sample_touch_name),
						context.getString(R.string.sample_touch_rationale)),
		};
	}

	@Override
	public int getCount() {
		return samples.length;
	}

	@Override
	public Sample getItem(int position) {
		return samples[position];
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater
					.from(parent.getContext())
					.inflate(R.layout.row_sample, parent, false);
		}

		ViewHolder holder = getViewHolder(convertView);
		Sample sample = samples[position];

		holder.thumbnail.setImageResource(sample.thumbId);
		holder.name.setText(sample.name);
		holder.rationale.setText(sample.rationale);

		return convertView;
	}

	private ViewHolder getViewHolder(View view) {
		ViewHolder holder;
		if ((holder = (ViewHolder) view.getTag()) == null) {
			holder = new ViewHolder();
			holder.thumbnail = view.findViewById(R.id.thumbnail);
			holder.name = view.findViewById(R.id.name);
			holder.rationale = view.findViewById(R.id.rationale);
			view.setTag(holder);
		}

		return holder;
	}

	private static final class ViewHolder {
		private ImageView thumbnail;
		private TextView name;
		private TextView rationale;
	}
}
