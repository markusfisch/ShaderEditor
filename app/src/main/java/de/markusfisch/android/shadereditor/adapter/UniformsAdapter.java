package de.markusfisch.android.shadereditor.adapter;

import de.markusfisch.android.shadereditor.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class UniformsAdapter extends BaseAdapter
{
	private final Uniform uniforms[];

	public UniformsAdapter( Context context )
	{
		uniforms = new Uniform[]{
			new Uniform(
				"uniform vec2 resolution",
				context.getString( R.string.resolution_in_pixels ) ),
			new Uniform(
				"uniform float time",
				context.getString( R.string.time_in_seconds_since_load ) ),
			new Uniform(
				"uniform vec2 touch",
				context.getString( R.string.touch_position_in_pixels ) ),
			new Uniform(
				"uniform int pointerCount",
				context.getString( R.string.number_of_touches ) ),
			new Uniform(
				"uniform vec3 pointers[10]",
				context.getString( R.string.positions_of_touches ) ),
			new Uniform(
				"uniform vec3 linear",
				context.getString( R.string.linear_acceleration_vector ) ),
			new Uniform(
				"uniform vec3 gravity",
				context.getString( R.string.gravity_vector ) ),
			new Uniform(
				"uniform vec3 rotation",
				context.getString( R.string.device_rotation ) ),
			new Uniform(
				"uniform vec3 magnetic",
				context.getString( R.string.magnetic_field ) ),
			new Uniform(
				"uniform float light",
				context.getString( R.string.light ) ),
			new Uniform(
				"uniform float pressure",
				context.getString( R.string.pressure ) ),
			new Uniform(
				"uniform float proximity",
				context.getString( R.string.proximity ) ),
			new Uniform(
				"uniform float battery",
				context.getString( R.string.battery_level ) ),
			new Uniform(
				"uniform vec2 offset",
				context.getString( R.string.wallpaper_offset ) ),
			new Uniform(
				"uniform vec4 date",
				context.getString( R.string.date_time ) ),
			new Uniform(
				"uniform float startRandom",
				context.getString( R.string.start_random ) ),
			new Uniform(
				"uniform sampler2D backbuffer",
				context.getString( R.string.previous_frame ) ),
		};
	}

	@Override
	public int getCount()
	{
		return uniforms.length;
	}

	@Override
	public String getItem( int position )
	{
		return uniforms[position].name;
	}

	@Override
	public long getItemId( int position )
	{
		return 0;
	}

	@Override
	public View getView(
		int position,
		View convertView,
		ViewGroup parent )
	{
		if( convertView == null )
			convertView = LayoutInflater
				.from( parent.getContext() )
				.inflate(
					android.R.layout.simple_list_item_2,
					parent,
					false );

		ViewHolder holder = getViewHolder( convertView );
		Uniform uniform = uniforms[position];

		holder.name.setText( uniform.name );
		holder.rationale.setText( uniform.rationale );

		return convertView;
	}

	protected ViewHolder getViewHolder( View view )
	{
		ViewHolder holder;

		if( (holder = (ViewHolder)view.getTag()) == null )
		{
			holder = new ViewHolder();
			holder.name = (TextView)view.findViewById(
				android.R.id.text1 );
			holder.rationale = (TextView)view.findViewById(
				android.R.id.text2 );
		}

		return holder;
	}

	private static final class Uniform
	{
		private String name;
		private String rationale;

		public Uniform( String name, String rationale )
		{
			this.name = name;
			this.rationale = rationale;
		}
	}

	private static final class ViewHolder
	{
		public TextView name;
		public TextView rationale;
	}
}
