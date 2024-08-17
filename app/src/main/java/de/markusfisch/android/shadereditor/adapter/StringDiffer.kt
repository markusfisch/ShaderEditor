package de.markusfisch.android.shadereditor.adapter

import androidx.recyclerview.widget.DiffUtil

class StringDiffer : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        // Update the condition according to your unique identifier
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        // Return true if the contents of the items have not changed
        return oldItem == newItem
    }
}
