package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
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
import androidx.fragment.app.Fragment;

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

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		loadBitmapAsync();
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_crop_image, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.rotate_clockwise) {
			rotateClockwise();
			return true;
		}
		return super.onOptionsItemSelected(item);
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
					bitmap = b;
					cropImageView.setImageBitmap(b);
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
				getFragmentManager(),
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
