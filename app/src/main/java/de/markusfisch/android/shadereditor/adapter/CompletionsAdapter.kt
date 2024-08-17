package de.markusfisch.android.shadereditor.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.markusfisch.android.shadereditor.R

class CompletionsAdapter(private val onInsertListener: (CharSequence) -> Unit) :
    ListAdapter<String, CompletionsAdapter.ViewHolder>(StringDiffer()) {

    private var positionInCompletion: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.extra_key_btn, parent, false)
        return ViewHolder(view as Button)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.update(item)
    }

    fun setPositionInCompletion(position: Int) {
        this.positionInCompletion = position
    }

    inner class ViewHolder(itemView: Button) : RecyclerView.ViewHolder(itemView) {
        private val btn: Button = itemView

        init {
            btn.setOnClickListener {
                val text = btn.text
                onInsertListener(text.subSequence(positionInCompletion, text.length))
            }
        }

        fun update(item: String) {
            btn.text = item
        }
    }
}
