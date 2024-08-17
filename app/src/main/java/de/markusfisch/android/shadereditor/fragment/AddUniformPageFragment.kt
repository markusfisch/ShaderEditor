package de.markusfisch.android.shadereditor.fragment

import androidx.fragment.app.Fragment
import de.markusfisch.android.shadereditor.activity.AddUniformActivity

abstract class AddUniformPageFragment : Fragment() {

    override fun onResume() {
        super.onResume()
        (activity as? AddUniformActivity)?.let { addUniformActivity ->
            addUniformActivity.setSearchListener(::onSearch)
            addUniformActivity.currentSearchQuery?.let { query ->
                onSearch(query)
            }
        }
    }

    protected abstract fun onSearch(query: String?)
}