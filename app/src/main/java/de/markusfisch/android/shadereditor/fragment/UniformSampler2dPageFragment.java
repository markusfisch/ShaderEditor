package de.markusfisch.android.shadereditor.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.concurrent.Executors;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.adapter.TextureAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

public class UniformSampler2dPageFragment extends AddUniformPageFragment {
	@Nullable
	protected String searchQuery;

	@Nullable
	private ListView listView;
	@Nullable
	private TextureAdapter texturesAdapter;
	@Nullable
	private View progressBar;
	@Nullable
	private View noTexturesMessage;
	private String samplerType = AbstractSamplerPropertiesFragment.SAMPLER_2D;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
	}

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

		listView = view.findViewById(R.id.textures);
		initListView(view);

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

	private void loadTexturesAsync(@Nullable final Context context) {
		if (context == null) {
			return;
		}
		Handler handler = new Handler(Looper.getMainLooper());
		Executors.newSingleThreadExecutor().execute(() -> {
			//noinspection resource
			Cursor cursor = loadTextures();
			handler.post(() -> {
				if (!isAdded() || cursor == null) {
					return;
				}
				updateAdapter(context, cursor);
			});
		});
	}

	protected Cursor loadTextures() {
		return ShaderEditorApp.db.getTextures(searchQuery);
	}

	private void updateAdapter(@NonNull Context context, @NonNull Cursor cursor) {
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

	private void initListView(@NonNull View view) {
		listView.setEmptyView(view.findViewById(R.id.no_textures));
		listView.setOnItemClickListener(
				(parent, view1, position, id) -> showTexture(id));
	}

	protected void onSearch(@Nullable String query) {
		searchQuery = query == null
				? null
				: query.toLowerCase(Locale.getDefault());

		loadTexturesAsync(getActivity());
	}
}
