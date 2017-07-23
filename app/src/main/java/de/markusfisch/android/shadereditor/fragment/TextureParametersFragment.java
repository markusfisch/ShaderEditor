package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.widget.TextureParametersView;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class TextureParametersFragment extends Fragment {
	private static final String TYPE = "type";
	private static final String NAME = "name";

	private TextureParametersView textureParameterView;
	private String samplerType;
	private String textureName;

	public static Fragment newInstance(String type, String name) {
		Bundle args = new Bundle();
		args.putString(TYPE, type);
		args.putString(NAME, name);

		TextureParametersFragment fragment =
				new TextureParametersFragment();
		fragment.setArguments(args);

		return fragment;
	}

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
		Activity activity = getActivity();
		activity.setTitle(R.string.texture_parameters);

		Bundle args = getArguments();
		if (args == null ||
				(samplerType = args.getString(TYPE)) == null ||
				(textureName = args.getString(NAME)) == null) {
			throw new IllegalArgumentException(
					"Missing type and name arguments");
		}

		View view = inflater.inflate(
				R.layout.fragment_texture_parameters,
				container,
				false);

		textureParameterView = (TextureParametersView) view.findViewById(
				R.id.texture_parameters);

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_texture_parameters, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.insert_code:
				insertUniform();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void insertUniform() {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		AddUniformActivity.setAddUniformResult(activity,
				"uniform " + samplerType + " " + textureName + ";" +
						textureParameterView.getTextureParams());

		activity.finish();
	}
}
