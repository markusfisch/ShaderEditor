package de.markusfisch.android.shadereditor.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.fragment.UniformPresetPageFragment;
import de.markusfisch.android.shadereditor.fragment.UniformSampler2dPageFragment;
import de.markusfisch.android.shadereditor.fragment.UniformSamplerCubePageFragment;

public class UniformPageAdapter extends FragmentStateAdapter implements TabConfigurationStrategy {


	public UniformPageAdapter(@NonNull FragmentManager fragmentManager,
			@NonNull Lifecycle lifecycle) {
		super(fragmentManager, lifecycle);
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		return switch (position) {
			default -> new UniformPresetPageFragment();
			case 1 -> new UniformSampler2dPageFragment();
			case 2 -> new UniformSamplerCubePageFragment();
		};
	}

	@Override
	public int getItemCount() {
		return 3;
	}

	@Override
	public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
		tab.setText(switch (position) {
			default -> R.string.preset;
			case 1 -> R.string.sampler_2d;
			case 2 -> R.string.sampler_cube;
		});
	}
}
