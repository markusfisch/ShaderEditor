package de.markusfisch.android.shadereditor.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.database.Database

open class ShaderAdapter(
    context: Context, cursor: Cursor
) : CursorAdapter(context, cursor, false) {

    private val idIndex = cursor.getColumnIndex(Database.SHADERS_ID)
    private val thumbIndex = cursor.getColumnIndex(Database.SHADERS_THUMB)
    private val nameIndex = cursor.getColumnIndex(Database.SHADERS_NAME)
    private val modifiedIndex = cursor.getColumnIndex(Database.SHADERS_MODIFIED)
    private val textColorSelected = ContextCompat.getColor(context, R.color.accent)
    private val textColorUnselected =
        ContextCompat.getColor(context, R.color.drawer_text_unselected)
    private var selectedShaderId: Long = -1L

    fun setSelectedId(id: Long) {
        selectedShaderId = id
    }

    fun getName(position: Int): String? {
        val cursor = getItem(position) as? Cursor
        return cursor?.getString(nameIndex)
    }

    fun getTitle(cursor: Cursor): String {
        val title = cursor.getString(nameIndex)
        return if (!title.isNullOrEmpty()) title else cursor.getString(modifiedIndex)
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(parent.context)
        return inflater.inflate(R.layout.row_shader, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val holder = getViewHolder(view)
        setData(holder, cursor)
        holder.title.setTextColor(
            if (cursor.getLong(idIndex) == selectedShaderId) textColorSelected else textColorUnselected
        )
    }

    fun getViewHolder(view: View): ViewHolder {
        return (view.tag as? ViewHolder) ?: ViewHolder(view).apply { view.tag = this }
    }

    fun setData(holder: ViewHolder, cursor: Cursor) {
        val bytes = cursor.getBlob(thumbIndex)
        val bitmap: Bitmap? = if (bytes != null && bytes.isNotEmpty()) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            null
        }
        holder.icon.setImageBitmap(bitmap)
        holder.title.text = getTitle(cursor)
    }

    class ViewHolder(view: View) {
        val icon: ImageView = view.findViewById(R.id.shader_icon)
        val title: TextView = view.findViewById(R.id.shader_title)
    }
}
