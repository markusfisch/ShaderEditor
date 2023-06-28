package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity;
import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.adapter.PresetUniformAdapter;
import de.markusfisch.android.shadereditor.widget.SearchMenu;

public class UniformPresetPageFragment extends Fragment {
	private PresetUniformAdapter uniformsAdapter;
	private ListView listView;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		View view = inflater.inflate(
				R.layout.fragment_uniform_preset_page,
				container,
				false);

		Activity activity = getActivity();

		listView = view.findViewById(R.id.uniforms);
		initListView(activity);

		return view;
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		SearchMenu.addSearchMenu(menu, inflater, this::filterUniforms);
	}

	private void filterUniforms(String query) {
		uniformsAdapter.getFilter().filter(query);
		uniformsAdapter.notifyDataSetChanged();
	}

	private void initListView(Context context) {
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
					getParentFragmentManager(),
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
