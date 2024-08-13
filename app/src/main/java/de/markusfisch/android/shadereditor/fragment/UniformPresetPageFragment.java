package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity;
import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.adapter.PresetUniformAdapter;

public class UniformPresetPageFragment extends AddUniformPageFragment {
	private PresetUniformAdapter uniformsAdapter;
	private ListView listView;

	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		View view = inflater.inflate(
				R.layout.fragment_uniform_preset_page,
				container,
				false);

		Activity activity = requireActivity();
		listView = view.findViewById(R.id.uniforms);
		initListView(activity);

		return view;
	}

	@Override
	protected void onSearch(@Nullable String query) {
		uniformsAdapter.getFilter().filter(query,
				count -> uniformsAdapter.notifyDataSetChanged());
	}

	private void initListView(@NonNull Context context) {
		uniformsAdapter = new PresetUniformAdapter(context);

		listView.setAdapter(uniformsAdapter);
		listView.setOnItemClickListener((parent, view, position, id) -> {
			if (view.isEnabled()) {
				addUniform(uniformsAdapter.getItem(position));
			}
		});
	}

	private void addUniform(PresetUniformAdapter.Uniform uniform) {
		if (uniform.isSampler()) {
			AbstractSubsequentActivity.addFragment(
					getParentFragment().getFragmentManager(),
					TextureParametersFragment.newInstance(
							uniform.type,
							uniform.name));
		} else {
			Activity activity = getActivity();
			if (activity != null) {
				AddUniformActivity.setAddUniformResult(activity,
						"uniform " + uniform.type + " " +
								uniform.name + ";");
				activity.finish();
			}
		}
	}
}
