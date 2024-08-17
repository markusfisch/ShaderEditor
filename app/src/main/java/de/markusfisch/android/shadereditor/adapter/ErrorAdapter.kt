package de.markusfisch.android.shadereditor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.opengl.ShaderError
import java.util.Locale

class ErrorAdapter(private val listener: OnItemClickListener) :
    ListAdapter<ShaderError, ErrorAdapter.ViewHolder>(DIFF_CALLBACK) {
    fun interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.error_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val error = getItem(position)
        error?.let {
            holder.update(it, listener)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val errorLine: TextView = itemView.findViewById(R.id.error_line)
        private val errorMessage: TextView = itemView.findViewById(R.id.error_message)

        fun update(error: ShaderError, listener: OnItemClickListener) {
            if (error.hasLine()) {
                errorLine.text = String.format(Locale.getDefault(), "%d: ", error.line)
                errorLine.visibility = View.VISIBLE
            } else {
                errorLine.visibility = View.GONE
            }

            errorMessage.text = error.message

            itemView.setOnClickListener {
                listener.onItemClick(error.line)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShaderError>() {
            override fun areItemsTheSame(oldItem: ShaderError, newItem: ShaderError): Boolean {
                // Check if the two items are the same
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ShaderError, newItem: ShaderError): Boolean {
                // Check if the content of the two items are the same
                return oldItem == newItem
            }
        }
    }
}
