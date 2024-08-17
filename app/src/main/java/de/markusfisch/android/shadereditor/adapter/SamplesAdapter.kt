package de.markusfisch.android.shadereditor.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import de.markusfisch.android.shadereditor.R

class SamplesAdapter(context: Context) : BaseAdapter() {

    data class Sample(
        val resId: Int,
        val thumbId: Int,
        val name: String,
        val rationale: String,
        val quality: Float = 1f
    )

    private val samples: Array<Sample> = arrayOf(
        Sample(
            R.raw.sample_battery,
            R.drawable.thumbnail_battery,
            context.getString(R.string.sample_battery_name),
            context.getString(R.string.sample_battery_rationale)
        ), Sample(
            R.raw.sample_camera_back,
            R.drawable.thumbnail_camera_back,
            context.getString(R.string.sample_camera_back_name),
            context.getString(R.string.sample_camera_back_rationale)
        ), Sample(
            R.raw.sample_circles,
            R.drawable.thumbnail_circles,
            context.getString(R.string.sample_circles_name),
            context.getString(R.string.sample_circles_rationale)
        ), Sample(
            R.raw.sample_cloudy_conway,
            R.drawable.thumbnail_cloudy_conway,
            context.getString(R.string.sample_cloudy_conway_name),
            context.getString(R.string.sample_cloudy_conway_rationale),
            0.125f
        ), Sample(
            R.raw.sample_electric_fade,
            R.drawable.thumbnail_electric_fade,
            context.getString(R.string.sample_electric_fade_name),
            context.getString(R.string.sample_electric_fade_rationale),
            0.125f
        ), Sample(
            R.raw.sample_game_of_life,
            R.drawable.thumbnail_game_of_life,
            context.getString(R.string.sample_game_of_life_name),
            context.getString(R.string.sample_game_of_life_rationale),
            0.125f
        ), Sample(
            R.raw.sample_gles_300,
            R.drawable.thumbnail_default,
            context.getString(R.string.sample_gles_300_name),
            context.getString(R.string.sample_gles_300_rationale)
        ), Sample(
            R.raw.sample_gravity,
            R.drawable.thumbnail_gravity,
            context.getString(R.string.sample_gravity_name),
            context.getString(R.string.sample_gravity_rationale)
        ), Sample(
            R.raw.sample_orientation,
            R.drawable.thumbnail_orientation,
            context.getString(R.string.sample_orientation_name),
            context.getString(R.string.sample_orientation_rationale)
        ), Sample(
            R.raw.sample_swirl,
            R.drawable.thumbnail_swirl,
            context.getString(R.string.sample_swirl_name),
            context.getString(R.string.sample_swirl_rationale)
        ), Sample(
            R.raw.sample_texture,
            R.drawable.thumbnail_texture,
            context.getString(R.string.sample_texture_name),
            context.getString(R.string.sample_texture_rationale)
        ), Sample(
            R.raw.sample_touch,
            R.drawable.thumbnail_touch,
            context.getString(R.string.sample_touch_name),
            context.getString(R.string.sample_touch_rationale)
        )
    )

    override fun getCount(): Int = samples.size

    override fun getItem(position: Int): Sample = samples[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.row_sample, parent, false)
        val holder = (view.tag as? ViewHolder) ?: ViewHolder(view).also { view.tag = it }

        val sample = getItem(position)

        holder.thumbnail.setImageResource(sample.thumbId)
        holder.name.text = sample.name
        holder.rationale.text = sample.rationale

        return view
    }

    private class ViewHolder(view: View) {
        val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        val name: TextView = view.findViewById(R.id.name)
        val rationale: TextView = view.findViewById(R.id.rationale)
    }
}
