package de.markusfisch.android.shadereditor.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.adapter.TextureAdapter;
import de.markusfisch.android.shadereditor.database.DataRecords;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.database.Database;

public class UniformSampler2dPageFragment extends AddUniformPageFragment {
	protected String searchQuery;

	private ListView listView;
	private TextureAdapter texturesAdapter;
	private View progressBar;
	private View noTexturesMessage;
	private String samplerType = AbstractSamplerPropertiesFragment.SAMPLER_2D;

	@Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		View view = inflater.inflate(
				R.layout.fragment_uniform_sampler_2d_page,
				container,
				false);

		View fab = view.findViewById(R.id.add_texture);
		fab.setOnClickListener(v -> addTexture());

		progressBar = view.findViewById(R.id.progress_bar);
		noTexturesMessage = view.findViewById(R.id.no_textures_message);

		listView = view.findViewById(R.id.textures);
		texturesAdapter = new TextureAdapter(requireActivity());
		listView.setAdapter(texturesAdapter);
		initListView(view);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (texturesAdapter.getCount() == 0) {
			progressBar.setVisibility(View.VISIBLE);
			noTexturesMessage.setVisibility(View.GONE);
		}
		loadTexturesAsync(getActivity());
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (texturesAdapter != null) {
			texturesAdapter.setData(null);
			texturesAdapter = null;
		}
		listView = null;
		progressBar = null;
		noTexturesMessage = null;
	}

	public void setSamplerType(String type) {
		samplerType = type;
	}

	protected void addTexture() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		if (getActivity() instanceof AddUniformActivity activity) {
			activity.startPickImage();
		}
	}

	private void showTexture(long id) {
		if ((getActivity() instanceof AddUniformActivity activity)) {
			activity.startPickTexture(id, samplerType);
		}
	}

	private void loadTexturesAsync(final Context context) {
		if (context == null) {
			return;
		}
		Handler handler = new Handler(Looper.getMainLooper());
		Executors.newSingleThreadExecutor().execute(() -> {
			DataSource dataSource = Database.getInstance(context).getDataSource();
			// Call the new overridable method to get the textures.
			final List<DataRecords.TextureInfo> textures = getTextures(dataSource, searchQuery);

			handler.post(() -> {
				if (isAdded()) {
					updateAdapter(textures);
				}
			});
		});
	}

	/**
	 * This method can be overridden by subclasses to load different
	 * types of textures (e.g., samplerCube).
	 */
	protected List<DataRecords.TextureInfo> getTextures(DataSource dataSource, String query) {
		return dataSource.getTextures(query);
	}

	private void updateAdapter(List<DataRecords.TextureInfo> textures) {
		progressBar.setVisibility(View.GONE);
		if (texturesAdapter != null) {
			texturesAdapter.setData(textures);
		}
	}

	private void initListView(@NonNull View view) {
		listView.setEmptyView(view.findViewById(R.id.no_textures));
		listView.setOnItemClickListener(
				(parent, view1, position, id) -> showTexture(id));
	}

	protected void onSearch(@Nullable String query) {
		searchQuery = query == null
				? null
				: query.toLowerCase(Locale.getDefault());

		progressBar.setVisibility(View.VISIBLE);
		noTexturesMessage.setVisibility(View.GONE);
		loadTexturesAsync(getActivity());
	}
}
