package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity;
import de.markusfisch.android.shadereditor.widget.CubeMapView;

public class CubeMapFragment extends Fragment {
	public interface CubeMapViewProvider {
		CubeMapView getCubeMapView();
	}

	private CubeMapView cubeMapView;
	private ActivityResultLauncher<Intent> pickImageLauncher;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);

		// Register the ActivityResultLauncher for picking an image
		pickImageLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> {
					if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
						Uri imageUri = result.getData().getData();
						if (imageUri != null) {
							cubeMapView.setSelectedFaceImage(imageUri);
						}
					}
				});
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

		// Use the ActivityResultLauncher to launch the image picker
		pickImageLauncher.launch(
				Intent.createChooser(
						intent,
						getString(R.string.choose_image)));
	}
}
