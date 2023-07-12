package de.markusfisch.android.shadereditor.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.activity.TextureViewActivity;
import de.markusfisch.android.shadereditor.adapter.TextureAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.widget.SearchMenu;

public class UniformSampler2dPageFragment extends Fragment {
	protected String searchQuery;

	private ListView listView;
	private TextureAdapter texturesAdapter;
	private EditText searchBar;
	private View progressBar;
	private View noTexturesMessage;
	private String samplerType = AbstractSamplerPropertiesFragment.SAMPLER_2D;

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
				R.layout.fragment_uniform_sampler_2d_page,
				container,
				false);

		View fab = view.findViewById(R.id.add_texture);
		fab.setOnClickListener(v -> addTexture());

		listView = view.findViewById(R.id.textures);
		initListView(view);

		//searchBar = getActivity().findViewById(R.id.search_bar);
		//initSearchBar();

		progressBar = view.findViewById(R.id.progress_bar);
		noTexturesMessage = view.findViewById(R.id.no_textures_message);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		loadTexturesAsync(getActivity());
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		if (texturesAdapter != null) {
			texturesAdapter.changeCursor(null);
			texturesAdapter = null;
		}

		listView = null;
		progressBar = null;
		noTexturesMessage = null;
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
		SearchMenu.addSearchMenu(menu, inflater, this::filterTextures);
	}

	public void setSamplerType(String type) {
		samplerType = type;
	}

	protected void addTexture() {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");

		// Use Activity.startActivityForResult() to keep
		// requestCode. Fragment.startActivityForResult()
		// will modify the requestCode.
		activity.startActivityForResult(
				Intent.createChooser(
						intent,
						getString(R.string.choose_image)),
				AddUniformActivity.PICK_IMAGE);
	}

	protected Cursor loadTextures() {
		return ShaderEditorApp.db.getTextures(searchQuery);
	}

	private void showTexture(long id) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		Intent intent = new Intent(activity, TextureViewActivity.class);
		intent.putExtra(TextureViewFragment.TEXTURE_ID, id);
		intent.putExtra(TextureViewFragment.SAMPLER_TYPE, samplerType);

		// Use Activity.startActivityForResult() to keep
		// requestCode. Fragment.startActivityForResult()
		// will modify the requestCode.
		activity.startActivityForResult(
				intent,
				AddUniformActivity.PICK_TEXTURE);
	}

	// This AsyncTask is running for a short and finite time only
	// and it's perfectly okay to delay garbage collection of the
	// parent instance until this task has ended.
	@SuppressLint("StaticFieldLeak")
	private void loadTexturesAsync(final Context context) {
		if (context == null) {
			return;
		}

		new AsyncTask<Void, Void, Cursor>() {
			@Override
			protected Cursor doInBackground(Void... nothings) {
				return loadTextures();
			}

			@Override
			protected void onPostExecute(Cursor cursor) {
				if (!isAdded() || cursor == null) {
					return;
				}

				updateAdapter(context, cursor);
			}
		}.execute();
	}

	private void updateAdapter(Context context, Cursor cursor) {
		if (texturesAdapter != null) {
			texturesAdapter.changeCursor(cursor);
			texturesAdapter.notifyDataSetChanged();
		} else {
			texturesAdapter = new TextureAdapter(context, cursor);
			listView.setAdapter(texturesAdapter);
		}

		if (cursor.getCount() < 1) {
			progressBar.setVisibility(View.GONE);
			noTexturesMessage.setVisibility(View.VISIBLE);
		}
	}

	private void initListView(View view) {
		listView.setEmptyView(view.findViewById(R.id.no_textures));
		listView.setOnItemClickListener(
				(parent, view1, position, id) -> showTexture(id));
	}

	private void filterTextures(String query) {
		searchQuery = query == null
				? null
				: query.toLowerCase();

		loadTexturesAsync(getActivity());
	}
}
