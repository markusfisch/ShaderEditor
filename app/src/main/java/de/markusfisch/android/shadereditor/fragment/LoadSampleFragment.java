package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.LoadSampleActivity;
import de.markusfisch.android.shadereditor.adapter.SamplesAdapter;

public class LoadSampleFragment extends Fragment {
	private ListView listView;

	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		View view = inflater.inflate(
				R.layout.fragment_samples,
				container,
				false);

		listView = view.findViewById(R.id.samples);
		initListView(getActivity());

		return view;
	}

	private void initListView(Context context) {
		final SamplesAdapter samplesAdapter = new SamplesAdapter(context);

		listView.setAdapter(samplesAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(
					AdapterView<?> parent,
					View view,
					int position,
					long id) {
				loadSample(samplesAdapter.getItem(position));
			}
		});
	}

	private void loadSample(SamplesAdapter.Sample sample) {
		Activity activity = getActivity();
		if (activity != null) {
			LoadSampleActivity.setSampleResult(
					activity,
					sample.name,
					sample.resId,
					sample.thumbId,
					sample.quality);
			activity.finish();
		}
	}
}
