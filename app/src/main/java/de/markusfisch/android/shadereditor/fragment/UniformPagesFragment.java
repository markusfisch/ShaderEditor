package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

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

		ViewPager2 viewPager = view.findViewById(R.id.pager);
		var adapter = new UniformPageAdapter(getChildFragmentManager(), getLifecycle());
		viewPager.setAdapter(adapter);
		new TabLayoutMediator(view.findViewById(R.id.tab_layout), viewPager, adapter).attach();

		return view;
	}
}
