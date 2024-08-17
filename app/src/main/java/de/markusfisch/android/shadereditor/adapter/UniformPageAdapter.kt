package de.markusfisch.android.shadereditor.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.fragment.UniformPresetPageFragment
import de.markusfisch.android.shadereditor.fragment.UniformSampler2dPageFragment
import de.markusfisch.android.shadereditor.fragment.UniformSamplerCubePageFragment

class UniformPageAdapter(
    fragmentManager: FragmentManager, lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle), TabConfigurationStrategy {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> UniformSampler2dPageFragment()
            2 -> UniformSamplerCubePageFragment()
            else -> UniformPresetPageFragment()
        }
    }

    override fun getItemCount(): Int = 3

    override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
        tab.setText(
            when (position) {
                1 -> R.string.sampler_2d
                2 -> R.string.sampler_cube
                else -> R.string.preset
            }
        )
    }
}
