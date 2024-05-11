package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.concurrent.Executors;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
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
	public void onCreate(Bundle state) {
		super.onCreate(state);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		Activity activity = getActivity();
		if (activity == null) {
			return null;
		}

		try {
			imageView = ((ScalingImageViewProvider) activity)
					.getScalingImageView();
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() +
					" must implement " +
					"TextureViewFragment.ScalingImageViewProvider");
		}

		Bundle args;
		Cursor cursor = null;

		if (imageView == null ||
				(args = getArguments()) == null ||
				(textureId = args.getLong(TEXTURE_ID)) < 1 ||
				(samplerType = args.getString(SAMPLER_TYPE)) == null ||
				(cursor = ShaderEditorApp.db.getTexture(textureId)) == null ||
				Database.closeIfEmpty(cursor)) {
			if (cursor != null && textureId > 0) {
				// Automatically remove defect textures.
				ShaderEditorApp.db.removeTexture(textureId);
				Toast.makeText(activity, R.string.removed_invalid_texture,
						Toast.LENGTH_LONG).show();
			}
			activity.finish();
			return null;
		}

		imageView.setVisibility(View.VISIBLE);

		try {
			textureName = Database.getString(cursor,
					Database.TEXTURES_NAME);
			imageView.setImageBitmap(
					ShaderEditorApp.db.getTextureBitmap(cursor));
		} catch (IllegalStateException e) {
			if (textureName == null) {
				textureName = getString(R.string.image_too_big);
			}
		}

		activity.setTitle(textureName);
		cursor.close();

		View view = inflater.inflate(
				R.layout.fragment_view_texture,
				container,
				false);
		view.findViewById(R.id.insert_code).setOnClickListener(
				v -> insertUniformSamplerStatement());

		return view;
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_view_texture, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.remove_texture) {
			askToRemoveTexture(textureId);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void askToRemoveTexture(final long id) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		new AlertDialog.Builder(activity)
				.setTitle(R.string.remove_texture)
				.setMessage(R.string.sure_remove_texture)
				.setPositiveButton(
						android.R.string.ok,
						(dialog, whichButton) -> removeTextureAsync(id))
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void removeTextureAsync(final long id) {
		Handler handler = new Handler(Looper.getMainLooper());
		Executors.newSingleThreadExecutor().execute(() -> {
			ShaderEditorApp.db.removeTexture(id);
			handler.post(() -> {
				Activity activity = getActivity();
				if (activity == null) {
					return;
				}
				activity.finish();
			});
		});
	}

	private void insertUniformSamplerStatement() {
		imageView.setVisibility(View.GONE);
		AbstractSubsequentActivity.addFragment(
				getFragmentManager(),
				TextureParametersFragment.newInstance(
						samplerType,
						textureName));
	}
}
