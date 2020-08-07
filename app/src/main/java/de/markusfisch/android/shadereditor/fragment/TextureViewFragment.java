package de.markusfisch.android.shadereditor.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.widget.ScalingImageView;

public class TextureViewFragment extends Fragment {
	public static final String TEXTURE_ID = "texture_id";
	public static final String SAMPLER_TYPE = "sampler_type";

	public interface ScalingImageViewProvider {
		ScalingImageView getScalingImageView();
	}

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
			LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		Activity activity = getActivity();

		try {
			imageView = ((ScalingImageViewProvider) activity)
					.getScalingImageView();
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() +
					" must implement " +
					"TextureViewFragment.ScalingImageViewProvider");
		}

		Bundle args;
		Cursor cursor;

		if (imageView == null ||
				(args = getArguments()) == null ||
				(textureId = args.getLong(TEXTURE_ID)) < 1 ||
				(samplerType = args.getString(SAMPLER_TYPE)) == null ||
				(cursor = ShaderEditorApp.db.getTexture(textureId)) == null ||
				Database.closeIfEmpty(cursor)) {
			activity.finish();
			return null;
		}

		imageView.setVisibility(View.VISIBLE);

		try {
			textureName = cursor.getString(cursor.getColumnIndex(
					Database.TEXTURES_NAME));
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
		view.findViewById(R.id.insert_code).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				insertUniformSamplerStatement();
			}
		});

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_view_texture, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.remove_texture:
				askToRemoveTexture(textureId);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
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
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(
									DialogInterface dialog,
									int whichButton) {
								removeTextureAsync(id);
							}
						})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	// this AsyncTask is running for a short and finite time only
	// and it's perfectly okay to delay garbage collection of the
	// parent instance until this task has ended
	@SuppressLint("StaticFieldLeak")
	private void removeTextureAsync(final long id) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... nothings) {
				ShaderEditorApp.db.removeTexture(id);
				return null;
			}

			@Override
			protected void onPostExecute(Void nothing) {
				Activity activity = getActivity();
				if (activity == null) {
					return;
				}

				activity.finish();
			}
		}.execute();
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
