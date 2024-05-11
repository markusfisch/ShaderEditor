package de.markusfisch.android.shadereditor.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.fragment.UniformPresetPageFragment;
import de.markusfisch.android.shadereditor.fragment.UniformSampler2dPageFragment;
import de.markusfisch.android.shadereditor.fragment.UniformSamplerCubePageFragment;

public class UniformPageAdapter extends FragmentStatePagerAdapter {
	private final Context context;

	public UniformPageAdapter(Context context, FragmentManager fm) {
		super(fm);
		this.context = context;
	}

	@Override
	public int getCount() {
		return 3;
	}

	@NonNull
	@Override
	public Fragment getItem(int position) {
		switch (position) {
			default:
			case 0:
				return new UniformPresetPageFragment();
			case 1:
				return new UniformSampler2dPageFragment();
			case 2:
				return new UniformSamplerCubePageFragment();
		}
	}

	@Override
	public CharSequence getPageTitle(int position) {
		int id;
		switch (position) {
			default:
			case 0:
				id = R.string.preset;
				break;
			case 1:
				id = R.string.sampler_2d;
				break;
			case 2:
				id = R.string.sampler_cube;
				break;
		}

		return context.getString(id);
	}
}
