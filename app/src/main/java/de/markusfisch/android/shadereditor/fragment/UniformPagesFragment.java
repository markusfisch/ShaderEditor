package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.adapter.UniformPageAdapter;
import de.markusfisch.android.shadereditor.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class UniformPagesFragment extends Fragment {
	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		View view = inflater.inflate(
				R.layout.fragment_uniform_pages,
				container,
				false);

		ViewPager viewPager = (ViewPager) view.findViewById(R.id.pager);
		viewPager.setAdapter(new UniformPageAdapter(
				getActivity(),
				getChildFragmentManager()));

		return view;
	}
}
