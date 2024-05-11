package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity;
import de.markusfisch.android.shadereditor.widget.CubeMapView;

public class CubeMapFragment extends Fragment {
	private static final int PICK_IMAGE = 1;

	public interface CubeMapViewProvider {
		CubeMapView getCubeMapView();
	}

	private CubeMapView cubeMapView;

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
		activity.setTitle(R.string.compose_sampler_cube);

		try {
			cubeMapView = ((CubeMapViewProvider) activity).getCubeMapView();
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() +
					" must implement " +
					"CubeMapFragment.CubeMapViewProvider");
		}

		View view = inflater.inflate(
				R.layout.fragment_cube_map,
				container,
				false);

		view.findViewById(R.id.add_texture).setOnClickListener(v -> addTexture());
		view.findViewById(R.id.crop).setOnClickListener(v -> composeMap());

		// Make cubeMapView in activity visible (again).
		cubeMapView.setVisibility(View.VISIBLE);

		return view;
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

	@Override
	public void onActivityResult(
			int requestCode,
			int resultCode,
			Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Uri imageUri;
		if (requestCode == PICK_IMAGE &&
				resultCode == Activity.RESULT_OK &&
				(imageUri = data.getData()) != null) {
			cubeMapView.setSelectedFaceImage(imageUri);
		}
	}

	private void composeMap() {
		CubeMapView.Face[] faces = cubeMapView.getFaces();

		for (int i = faces.length; i-- > 0; ) {
			if (faces[i].getUri() == null) {
				Activity activity = getActivity();
				if (activity == null) {
					return;
				}
				Toast.makeText(
						activity,
						R.string.not_enough_faces,
						Toast.LENGTH_SHORT).show();
				return;
			}
		}

		AbstractSubsequentActivity.addFragment(
				getFragmentManager(),
				SamplerCubePropertiesFragment.newInstance(faces));

		cubeMapView.setVisibility(View.GONE);
	}

	private void rotateClockwise() {
		cubeMapView.setImageRotation(
				(cubeMapView.getImageRotation() + 90) % 360);
	}

	private void addTexture() {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");

		// Use Activity.startActivityForResult() to keep
		// requestCode. Fragment.startActivityForResult()
		// will modify the requestCode.
		startActivityForResult(
				Intent.createChooser(
						intent,
						getString(R.string.choose_image)),
				CubeMapFragment.PICK_IMAGE);
	}
}
