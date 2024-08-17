package de.markusfisch.android.shadereditor.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.database.Database

open class TextureAdapter(
    context: Context, cursor: Cursor
) : CursorAdapter(context, cursor, false) {

    private val sizeFormat: String = context.getString(R.string.texture_size_format)

    private val nameIndex: Int = cursor.getColumnIndex(Database.TEXTURES_NAME)
    private val widthIndex: Int = cursor.getColumnIndex(Database.TEXTURES_WIDTH)
    private val heightIndex: Int = cursor.getColumnIndex(Database.TEXTURES_HEIGHT)
    private val thumbIndex: Int = cursor.getColumnIndex(Database.TEXTURES_THUMB)

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.row_texture, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val holder = getViewHolder(view)
        setData(holder, cursor)
    }

    fun getViewHolder(view: View): ViewHolder {
        return (view.tag as? ViewHolder) ?: ViewHolder(view).apply {
            view.tag = this
        }
    }

    fun setData(holder: ViewHolder, cursor: Cursor) {
        val bytes = cursor.getBlob(thumbIndex)
        val bitmap = bytes?.let {
            if (it.isNotEmpty()) {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            } else null
        }
        holder.preview.setImageBitmap(bitmap)
        holder.name.text = cursor.getString(nameIndex)
        holder.size.text =
            String.format(sizeFormat, cursor.getInt(widthIndex), cursor.getInt(heightIndex))
    }

    class ViewHolder(view: View) {
        val preview: ImageView = view.findViewById(R.id.texture_preview)
        val name: TextView = view.findViewById(R.id.texture_name)
        val size: TextView = view.findViewById(R.id.texture_size)
    }
}
