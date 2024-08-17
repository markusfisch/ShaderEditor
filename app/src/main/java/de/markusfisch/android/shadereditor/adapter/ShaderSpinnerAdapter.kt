package de.markusfisch.android.shadereditor.adapter

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SpinnerAdapter
import de.markusfisch.android.shadereditor.R

class ShaderSpinnerAdapter(
    context: Context, cursor: Cursor
) : ShaderAdapter(context, cursor), SpinnerAdapter {

    override fun newDropDownView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(R.layout.row_shader, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        setData(getViewHolder(view), cursor)
    }
}
