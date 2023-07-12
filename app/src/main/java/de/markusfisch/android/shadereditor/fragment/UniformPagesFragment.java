package de.markusfisch.android.shadereditor.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.UniformPageAdapter;

public class UniformPagesFragment extends Fragment {
	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		// Reset title if we're coming back from another fragment.
		getActivity().setTitle(R.string.add_uniform);

		View view = inflater.inflate(
				R.layout.fragment_uniform_pages,
				container,
				false);

		ViewPager viewPager = view.findViewById(R.id.pager);
		viewPager.setAdapter(new UniformPageAdapter(
				getActivity(),
				getChildFragmentManager()));

		return view;
	}
}
