package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.UniformPageAdapter;

public class UniformPagesFragment extends Fragment {
	@Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		Activity activity = getActivity();
		if (activity != null) {
			// Reset title if we're coming back from another fragment.
			activity.setTitle(R.string.add_uniform);
		}

		View view = inflater.inflate(
				R.layout.fragment_uniform_pages,
				container,
				false);

		ViewPager viewPager = view.findViewById(R.id.pager);
		viewPager.setAdapter(new UniformPageAdapter(
				getActivity(),
				getChildFragmentManager()));

		// Workaround to make sure PagerTabStrip is visible in a release
		// build. Without this, it will only be visible in a debug build.
		// See: https://code.google.com/p/android/issues/detail?id=213359
		PagerTabStrip pagerTabStrip = view.findViewById(R.id.pagerTabStrip);
		((ViewPager.LayoutParams) pagerTabStrip.getLayoutParams())
				.isDecor = true;

		return view;
	}
}
