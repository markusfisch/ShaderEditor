package de.markusfisch.android.shadereditor.adapter;

import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;
import de.markusfisch.android.shadereditor.R;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Locale;

public class PresetUniformAdapter extends BaseAdapter {
	public static final class Uniform {
		public final String type;
		public final String name;

		private final String rationale;
		private final int minSdk;

		private boolean isAvailable() {
			return minSdk <= Build.VERSION.SDK_INT;
		}

		public boolean isSampler() {
			return type.startsWith("sampler");
		}

		private Uniform(String type, String name, String rationale) {
			this(type, name, rationale, 0);
		}

		private Uniform(
				String type,
				String name,
				String rationale,
				int minSdk) {
			this.type = type;
			this.name = name;
			this.rationale = rationale;
			this.minSdk = minSdk;
		}
	}

	private final String uniformFormat;
	private final Uniform uniforms[];

	public PresetUniformAdapter(Context context) {
		uniformFormat = context.getString(R.string.uniform_format);
		uniforms = new Uniform[]{
				new Uniform(
						"sampler2D",
						ShaderRenderer.UNIFORM_BACKBUFFER,
						context.getString(R.string.previous_frame)),
				new Uniform(
						"float",
						ShaderRenderer.UNIFORM_BATTERY,
						context.getString(R.string.battery_level)),
				new Uniform(
						"vec2",
						ShaderRenderer.UNIFORM_CAMERA_ADDENT,
						context.getString(R.string.camera_addent),
						Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1),
				new Uniform(
						"samplerExternalOES",
						ShaderRenderer.UNIFORM_CAMERA_BACK,
						context.getString(R.string.camera_back),
						Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1),
				new Uniform(
						"samplerExternalOES",
						ShaderRenderer.UNIFORM_CAMERA_FRONT,
						context.getString(R.string.camera_front),
						Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1),
				new Uniform(
						"mat2",
						ShaderRenderer.UNIFORM_CAMERA_ORIENTATION,
						context.getString(R.string.camera_orientation),
						Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1),
				new Uniform(
						"vec4",
						ShaderRenderer.UNIFORM_DATE,
						context.getString(R.string.date_time)),
				new Uniform(
						"int",
						ShaderRenderer.UNIFORM_FRAME_NUMBER,
						context.getString(R.string.frame_number)),
				new Uniform(
						"float",
						ShaderRenderer.UNIFORM_FTIME,
						context.getString(R.string.time_in_cycle)),
				new Uniform(
						"vec3",
						ShaderRenderer.UNIFORM_GRAVITY,
						context.getString(R.string.gravity_vector)),
				new Uniform(
						"vec3",
						ShaderRenderer.UNIFORM_GYROSCOPE,
						context.getString(R.string.gyroscope)),
				new Uniform(
						"float",
						ShaderRenderer.UNIFORM_INCLINATION,
						context.getString(R.string.device_inclination)),
				new Uniform(
						"mat3",
						ShaderRenderer.UNIFORM_INCLINATION_MATRIX,
						context.getString(R.string.device_inclination_matrix)),
				new Uniform(
						"float",
						ShaderRenderer.UNIFORM_LIGHT,
						context.getString(R.string.light)),
				new Uniform(
						"vec3",
						ShaderRenderer.UNIFORM_LINEAR,
						context.getString(R.string.linear_acceleration_vector)),
				new Uniform(
						"vec3",
						ShaderRenderer.UNIFORM_MAGNETIC,
						context.getString(R.string.magnetic_field)),
				new Uniform(
						"vec2",
						ShaderRenderer.UNIFORM_OFFSET,
						context.getString(R.string.wallpaper_offset)),
				new Uniform(
						"vec3",
						ShaderRenderer.UNIFORM_ORIENTATION,
						context.getString(R.string.device_orientation)),
				new Uniform(
						"vec3",
						ShaderRenderer.UNIFORM_POINTERS + "[10]",
						context.getString(R.string.positions_of_touches)),
				new Uniform(
						"int",
						ShaderRenderer.UNIFORM_POINTER_COUNT,
						context.getString(R.string.number_of_touches)),
				new Uniform(
						"float",
						ShaderRenderer.UNIFORM_PRESSURE,
						context.getString(R.string.pressure)),
				new Uniform(
						"float",
						ShaderRenderer.UNIFORM_PROXIMITY,
						context.getString(R.string.proximity)),
				new Uniform(
						"vec2",
						ShaderRenderer.UNIFORM_RESOLUTION,
						context.getString(R.string.resolution_in_pixels)),
				new Uniform(
						"mat3",
						ShaderRenderer.UNIFORM_ROTATION_MATRIX,
						context.getString(R.string.device_rotation_matrix)),
				new Uniform(
						"vec3",
						ShaderRenderer.UNIFORM_ROTATION_VECTOR,
						context.getString(R.string.device_rotation_vector)),
				new Uniform(
						"int",
						ShaderRenderer.UNIFORM_SECOND,
						context.getString(R.string.int_seconds_since_load)),
				new Uniform(
						"float",
						ShaderRenderer.UNIFORM_START_RANDOM,
						context.getString(R.string.start_random)),
				new Uniform(
						"float",
						ShaderRenderer.UNIFORM_SUB_SECOND,
						context.getString(R.string.fractional_part_of_seconds_since_load)),
				new Uniform(
						"float",
						ShaderRenderer.UNIFORM_TIME,
						context.getString(R.string.time_in_seconds_since_load)),
				new Uniform(
						"vec2",
						ShaderRenderer.UNIFORM_TOUCH,
						context.getString(R.string.touch_position_in_pixels)),
		};
	}

	@Override
	public int getCount() {
		return uniforms.length;
	}

	@Override
	public Uniform getItem(int position) {
		return uniforms[position];
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
					.inflate(R.layout.row_preset, parent, false);
		}

		ViewHolder holder = getViewHolder(convertView);
		Uniform uniform = uniforms[position];
		boolean enabled = uniform.isAvailable();

		convertView.setEnabled(enabled);

		holder.name.setTextColor(ContextCompat.getColor(
				parent.getContext(),
				enabled ?
						android.R.color.primary_text_dark :
						R.color.disabled_text));
		holder.name.setText(String.format(
				Locale.US,
				uniformFormat,
				uniform.name,
				uniform.type));
		holder.rationale.setText(uniform.rationale);

		return convertView;
	}

	private ViewHolder getViewHolder(View view) {
		ViewHolder holder;
		if ((holder = (ViewHolder) view.getTag()) == null) {
			holder = new ViewHolder();
			holder.name = view.findViewById(R.id.name);
			holder.rationale = view.findViewById(R.id.rationale);
			view.setTag(holder);
		}

		return holder;
	}

	private static final class ViewHolder {
		private TextView name;
		private TextView rationale;
	}
}
