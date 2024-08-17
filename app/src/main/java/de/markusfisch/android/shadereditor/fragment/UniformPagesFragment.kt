package de.markusfisch.android.shadereditor.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.adapter.UniformPageAdapter

class UniformPagesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        requireActivity().title = getString(R.string.add_uniform)

        val view = inflater.inflate(R.layout.fragment_uniform_pages, container, false)

        val viewPager: ViewPager2 = view.findViewById(R.id.pager)
        val adapter = UniformPageAdapter(childFragmentManager, lifecycle)
        viewPager.adapter = adapter

        TabLayoutMediator(view.findViewById(R.id.tab_layout), viewPager, adapter).attach()

        return view
    }
}