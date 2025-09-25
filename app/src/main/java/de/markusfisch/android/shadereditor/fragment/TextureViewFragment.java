package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.Executors;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity;
import de.markusfisch.android.shadereditor.database.DataRecords.TextureInfo;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.widget.ScalingImageView;

public class TextureViewFragment extends Fragment {
	public interface ScalingImageViewProvider {
		ScalingImageView getScalingImageView();
	}

	public static final String TEXTURE_ID = "texture_id";
	public static final String SAMPLER_TYPE = "sampler_type";

	private ScalingImageView imageView;
	private long textureId;
	private String textureName;
	private String samplerType;

	@Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		Activity activity = requireActivity();
		DataSource dataSource = Database.getInstance(activity).getDataSource();

		try {
			imageView = ((ScalingImageViewProvider) activity)
					.getScalingImageView();
		} catch (ClassCastException e) {
			throw new ClassCastException(activity +
					" must implement " +
					"TextureViewFragment.ScalingImageViewProvider");
		}

		Bundle args = getArguments();
		if (imageView == null || args == null) {
			activity.finish();
			return null;
		}

		textureId = args.getLong(TEXTURE_ID);
		samplerType = args.getString(SAMPLER_TYPE);
		if (textureId < 1 || samplerType == null) {
			activity.finish();
			return null;
		}

		// Fetch texture info and bitmap using the modern DataSource.
		Bitmap nonPremultipliedBitmap = dataSource.texture.getTextureBitmap(textureId);
		TextureInfo textureInfo = dataSource.texture.getTextureInfo(textureId);

		if (nonPremultipliedBitmap == null || textureInfo == null) {
			// Automatically remove defective textures.
			dataSource.texture.removeTexture(textureId);
			Toast.makeText(activity, R.string.removed_invalid_texture,
					Toast.LENGTH_LONG).show();
			activity.finish();
			return null;
		}

		Bitmap textureBitmap;
		if (nonPremultipliedBitmap.hasAlpha() && !nonPremultipliedBitmap.isPremultiplied()) {
			textureBitmap = nonPremultipliedBitmap.copy(nonPremultipliedBitmap.getConfig(), true);
			textureBitmap.setPremultiplied(true);
			nonPremultipliedBitmap.recycle();
		} else {
			textureBitmap = nonPremultipliedBitmap;
		}


		textureName = textureInfo.name();
		activity.setTitle(textureName);

		imageView.setVisibility(View.VISIBLE);
		imageView.setImageBitmap(textureBitmap);

		View view = inflater.inflate(
				R.layout.fragment_view_texture,
				container,
				false);
		view.findViewById(R.id.insert_code).setOnClickListener(
				v -> insertUniformSamplerStatement());

		addMenuProvider();
		return view;
	}

	private void addMenuProvider() {
		requireActivity().addMenuProvider(new MenuProvider() {
			@Override
			public void onCreateMenu(@NonNull android.view.Menu menu,
					@NonNull MenuInflater menuInflater) {
				menuInflater.inflate(R.menu.fragment_view_texture, menu);
			}

			@Override
			public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
				if (menuItem.getItemId() == R.id.remove_texture) {
					askToRemoveTexture(textureId);
					return true;
				}
				return false;
			}
		}, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
	}

	private void askToRemoveTexture(final long id) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		new MaterialAlertDialogBuilder(activity)
				.setTitle(R.string.remove_texture)
				.setMessage(R.string.sure_remove_texture)
				.setPositiveButton(
						android.R.string.ok,
						(dialog, whichButton) -> removeTextureAsync(id))
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void removeTextureAsync(final long id) {
		Context context = getContext();
		if (context == null) {
			return;
		}
		// Use application context for safety in background tasks.
		Context appContext = context.getApplicationContext();
		Handler handler = new Handler(Looper.getMainLooper());

		Executors.newSingleThreadExecutor().execute(() -> {
			Database.getInstance(appContext).getDataSource().texture.removeTexture(id);
			handler.post(() -> {
				Activity activity = getActivity();
				if (activity != null) {
					activity.finish();
				}
			});
		});
	}

	private void insertUniformSamplerStatement() {
		imageView.setVisibility(View.GONE);
		AbstractSubsequentActivity.addFragment(
				getParentFragmentManager(),
				TextureParametersFragment.newInstance(
						samplerType,
						textureName));
	}
}
