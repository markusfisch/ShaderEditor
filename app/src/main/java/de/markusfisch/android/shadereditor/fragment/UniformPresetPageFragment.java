package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity;
import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.adapter.PresetUniformAdapter;

public class UniformPresetPageFragment extends Fragment {
	private PresetUniformAdapter uniformsAdapter;
	private ListView listView;
	private EditText searchBar;

	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		View view = inflater.inflate(
				R.layout.fragment_uniform_preset_page,
				container,
				false);

		listView = view.findViewById(R.id.uniforms);
		initListView(getActivity());

		searchBar = getActivity().findViewById(R.id.search_bar);
		initSearchBar();

		return view;
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

	private void initSearchBar() {
		searchBar.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				uniformsAdapter.getFilter().filter(s);
				uniformsAdapter.notifyDataSetChanged();
			}

			@Override
			public void afterTextChanged(Editable s) {

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
