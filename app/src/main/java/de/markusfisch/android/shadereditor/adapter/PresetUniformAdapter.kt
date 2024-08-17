package de.markusfisch.android.shadereditor.adapter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.opengl.ShaderRenderer
import java.util.Locale

class PresetUniformAdapter(context: Context) : BaseAdapter(), Filterable {

    data class Uniform(
        val type: String, val name: String, val rationale: String, val minSdk: Int = 0
    ) {
        val isAvailable: Boolean get() = minSdk <= Build.VERSION.SDK_INT
        val isSampler: Boolean get() = type.startsWith("sampler")
    }

    private val uniformFormat: String = context.getString(R.string.uniform_format)
    private val uniforms: List<Uniform> = listOf(
        Uniform(
            "sampler2D",
            ShaderRenderer.UNIFORM_BACKBUFFER,
            context.getString(R.string.previous_frame)
        ), Uniform(
            "float", ShaderRenderer.UNIFORM_BATTERY, context.getString(R.string.battery_level)
        ), Uniform(
            "vec2",
            ShaderRenderer.UNIFORM_CAMERA_ADDENT,
            context.getString(R.string.camera_addent),
            Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
        ), Uniform(
            "samplerExternalOES",
            ShaderRenderer.UNIFORM_CAMERA_BACK,
            context.getString(R.string.camera_back),
            Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
        ), Uniform(
            "samplerExternalOES",
            ShaderRenderer.UNIFORM_CAMERA_FRONT,
            context.getString(R.string.camera_front),
            Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
        ), Uniform(
            "mat2",
            ShaderRenderer.UNIFORM_CAMERA_ORIENTATION,
            context.getString(R.string.camera_orientation),
            Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
        ), Uniform(
            "vec4", ShaderRenderer.UNIFORM_DATE, context.getString(R.string.date_time)
        ), Uniform(
            "vec3", ShaderRenderer.UNIFORM_DAYTIME, context.getString(R.string.daytime)
        ), Uniform(
            "int", ShaderRenderer.UNIFORM_FRAME_NUMBER, context.getString(R.string.frame_number)
        ), Uniform(
            "float", ShaderRenderer.UNIFORM_FTIME, context.getString(R.string.time_in_cycle)
        ), Uniform(
            "vec3", ShaderRenderer.UNIFORM_GRAVITY, context.getString(R.string.gravity_vector)
        ), Uniform(
            "vec3", ShaderRenderer.UNIFORM_GYROSCOPE, context.getString(R.string.gyroscope)
        ), Uniform(
            "float",
            ShaderRenderer.UNIFORM_LAST_NOTIFICATION_TIME,
            context.getString(R.string.last_notification_time)
        ), Uniform(
            "float",
            ShaderRenderer.UNIFORM_INCLINATION,
            context.getString(R.string.device_inclination)
        ), Uniform(
            "mat3",
            ShaderRenderer.UNIFORM_INCLINATION_MATRIX,
            context.getString(R.string.device_inclination_matrix)
        ), Uniform(
            "float", ShaderRenderer.UNIFORM_LIGHT, context.getString(R.string.light)
        ), Uniform(
            "vec3",
            ShaderRenderer.UNIFORM_LINEAR,
            context.getString(R.string.linear_acceleration_vector)
        ), Uniform(
            "vec3", ShaderRenderer.UNIFORM_MAGNETIC, context.getString(R.string.magnetic_field)
        ), Uniform(
            "int", ShaderRenderer.UNIFORM_NIGHT_MODE, context.getString(R.string.night_mode)
        ), Uniform(
            "int",
            ShaderRenderer.UNIFORM_NOTIFICATION_COUNT,
            context.getString(R.string.notification_count)
        ), Uniform(
            "vec2", ShaderRenderer.UNIFORM_OFFSET, context.getString(R.string.wallpaper_offset)
        ), Uniform(
            "vec3",
            ShaderRenderer.UNIFORM_ORIENTATION,
            context.getString(R.string.device_orientation)
        ), Uniform(
            "vec3",
            ShaderRenderer.UNIFORM_POINTERS + "[10]",
            context.getString(R.string.positions_of_touches)
        ), Uniform(
            "int",
            ShaderRenderer.UNIFORM_POINTER_COUNT,
            context.getString(R.string.number_of_touches)
        ), Uniform(
            "int",
            ShaderRenderer.UNIFORM_POWER_CONNECTED,
            context.getString(R.string.power_connected)
        ), Uniform(
            "float", ShaderRenderer.UNIFORM_PRESSURE, context.getString(R.string.pressure)
        ), Uniform(
            "float", ShaderRenderer.UNIFORM_PROXIMITY, context.getString(R.string.proximity)
        ), Uniform(
            "vec2",
            ShaderRenderer.UNIFORM_RESOLUTION,
            context.getString(R.string.resolution_in_pixels)
        ), Uniform(
            "mat3",
            ShaderRenderer.UNIFORM_ROTATION_MATRIX,
            context.getString(R.string.device_rotation_matrix)
        ), Uniform(
            "vec3",
            ShaderRenderer.UNIFORM_ROTATION_VECTOR,
            context.getString(R.string.device_rotation_vector)
        ), Uniform(
            "int", ShaderRenderer.UNIFORM_SECOND, context.getString(R.string.int_seconds_since_load)
        ), Uniform(
            "float", ShaderRenderer.UNIFORM_START_RANDOM, context.getString(R.string.start_random)
        ), Uniform(
            "float",
            ShaderRenderer.UNIFORM_SUB_SECOND,
            context.getString(R.string.fractional_part_of_seconds_since_load)
        ), Uniform(
            "float",
            ShaderRenderer.UNIFORM_TIME,
            context.getString(R.string.time_in_seconds_since_load)
        ), Uniform(
            "float",
            ShaderRenderer.UNIFORM_MEDIA_VOLUME,
            context.getString(R.string.media_volume_level)
        ), Uniform(
            "vec2",
            ShaderRenderer.UNIFORM_TOUCH,
            context.getString(R.string.touch_position_in_pixels)
        ), Uniform(
            "vec2",
            ShaderRenderer.UNIFORM_TOUCH_START,
            context.getString(R.string.touch_start_position_in_pixels)
        )

    )
    private var filteredUniforms: List<Uniform> = uniforms
    private val filter = UniformFilter()

    override fun getFilter(): Filter = filter

    override fun getCount(): Int = filteredUniforms.size

    override fun getItem(position: Int): Uniform = filteredUniforms[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.row_preset, parent, false).also {
                it.tag = ViewHolder(it)
            }
        val holder = view.tag as ViewHolder

        val uniform = filteredUniforms[position]
        holder.update(uniform)
        holder.name.text = String.format(Locale.US, uniformFormat, uniform.name, uniform.type)
        holder.rationale.text = uniform.rationale

        return view
    }


    private inner class ViewHolder(val view: View) {
        val name: TextView = view.findViewById(R.id.name)
        val rationale: TextView = view.findViewById(R.id.rationale)

        fun update(uniform: Uniform) {
            view.isEnabled = uniform.isAvailable
            val colorAttr = if (uniform.isAvailable) {
                com.google.android.material.R.attr.colorOnSurface
            } else {
                com.google.android.material.R.attr.colorOnSurfaceVariant
            }
            name.apply {
                setTextColor(MaterialColors.getColor(name, colorAttr))
                text = String.format(Locale.US, uniformFormat, uniform.name, uniform.type)
            }
            rationale.text = uniform.rationale

        }
    }

    private inner class UniformFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterString = constraint?.toString()?.lowercase(Locale.getDefault()) ?: ""
            val filtered = uniforms.filter { it.name.contains(filterString) }

            return FilterResults().apply {
                values = filtered
                count = filtered.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            filteredUniforms = results.values as List<Uniform>
            notifyDataSetChanged()
        }

    }
}
