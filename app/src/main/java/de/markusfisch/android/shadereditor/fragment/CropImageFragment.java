package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
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

import java.util.concurrent.Executors;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity;
import de.markusfisch.android.shadereditor.graphics.BitmapEditor;
import de.markusfisch.android.shadereditor.widget.CropImageView;

public class CropImageFragment extends Fragment {
	public interface CropImageViewProvider {
		CropImageView getCropImageView();
	}

	public static final String IMAGE_URI = "image_uri";
	private static boolean inProgress = false;
	private CropImageView cropImageView;
	private View progressView;
	private Uri imageUri;
	private Bitmap bitmap;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
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
		activity.setTitle(R.string.crop_image);

		try {
			cropImageView = ((CropImageViewProvider) activity)
					.getCropImageView();
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() +
					" must implement " +
					"CropImageFragment.CropImageViewProvider");
		}

		Bundle args = getArguments();
		if (args == null ||
				(imageUri = args.getParcelable(IMAGE_URI)) == null) {
			abort(activity);
			return null;
		}

		View view = inflater.inflate(
				R.layout.fragment_crop_image,
				container,
				false);
		progressView = view.findViewById(R.id.progress_view);

		// Make cropImageView in activity visible (again).
		cropImageView.setVisibility(View.VISIBLE);

		view.findViewById(R.id.crop).setOnClickListener(v -> cropImage());

		requireActivity().addMenuProvider(new MenuProvider() {
			@Override
			public void onCreateMenu(@NonNull android.view.Menu menu,
					@NonNull MenuInflater menuInflater) {
				menuInflater.inflate(R.menu.fragment_crop_image, menu);
			}

			@Override
			public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
				if (menuItem.getItemId() == R.id.rotate_clockwise) {
					rotateClockwise();
					return true;
				}
				return false;
			}
		}, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		loadBitmapAsync();
	}

	private void loadBitmapAsync() {
		final Activity activity = getActivity();
		if (activity == null || inProgress) {
			return;
		}

		inProgress = true;
		progressView.setVisibility(View.VISIBLE);
		Handler handler = new Handler(Looper.getMainLooper());
		Executors.newSingleThreadExecutor().execute(() -> {
			Bitmap b = BitmapEditor.getBitmapFromUri(
					activity,
					imageUri,
					1024);
			handler.post(() -> {
				inProgress = false;
				progressView.setVisibility(View.GONE);

				if (b == null) {
					abort(activity);
					return;
				}

				if (isAdded()) {
					if (bitmap != null) {
						bitmap.recycle();
					}
					// `b` is non-premultiplied. We need a premultiplied one for display.
					if (b.hasAlpha() && !b.isPremultiplied()) {
						// Create a premultiplied copy for display.
						Bitmap premultipliedBitmap = b.copy(b.getConfig(), true);
						premultipliedBitmap.setPremultiplied(true);
						b.recycle();
						bitmap = premultipliedBitmap;
					} else {
						bitmap = b;
					}
					cropImageView.setImageBitmap(bitmap);
				}
			});
		});
	}

	private void abort(Activity activity) {
		Toast.makeText(
				activity,
				R.string.cannot_pick_image,
				Toast.LENGTH_SHORT).show();
		activity.finish();
	}

	private void cropImage() {
		AbstractSubsequentActivity.addFragment(
				getParentFragmentManager(),
				Sampler2dPropertiesFragment.newInstance(
						imageUri,
						cropImageView.getNormalizedRectInBounds(),
						cropImageView.getImageRotation()));

		bitmap.recycle();
		bitmap = null;

		cropImageView.setImageBitmap(null);
		cropImageView.setVisibility(View.GONE);
	}

	private void rotateClockwise() {
		cropImageView.setImageRotation(
				(cropImageView.getImageRotation() + 90) % 360);
	}
}
