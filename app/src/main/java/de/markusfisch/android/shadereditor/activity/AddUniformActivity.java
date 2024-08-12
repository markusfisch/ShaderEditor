package de.markusfisch.android.shadereditor.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.fragment.TextureViewFragment;
import de.markusfisch.android.shadereditor.fragment.UniformPagesFragment;

public class AddUniformActivity extends AbstractContentActivity {
	public static final String STATEMENT = "statement";

	private ActivityResultLauncher<Intent> pickImageLauncher;
	private ActivityResultLauncher<Intent> cropImageLauncher;
	private ActivityResultLauncher<Intent> pickTextureLauncher;

	public static void setAddUniformResult(@NonNull Activity activity, String name) {
		Bundle bundle = new Bundle();
		bundle.putString(STATEMENT, name);

		Intent data = new Intent();
		data.putExtras(bundle);

		activity.setResult(RESULT_OK, data);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Register the ActivityResultLaunchers
		pickImageLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> {
					if (result.getResultCode() == RESULT_OK && result.getData() != null) {
						Uri imageUri = result.getData().getData();
						if (imageUri != null) {
							Intent cropIntent = CropImageActivity.getIntentForImage(this,
									imageUri);
							cropImageLauncher.launch(cropIntent);
						}
					}
				});

		cropImageLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> {
					if (result.getResultCode() == RESULT_OK && result.getData() != null) {
						setResult(RESULT_OK, result.getData());
						finish();
					}
				});

		pickTextureLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> {
					if (result.getResultCode() == RESULT_OK && result.getData() != null) {
						setResult(RESULT_OK, result.getData());
						finish();
					}
				});

		// Handle any intents passed to this activity
		startActivityForIntent(getIntent());
	}

	@Override
	protected Fragment defaultFragment() {
		return new UniformPagesFragment();
	}

	private void startActivityForIntent(Intent intent) {
		if (intent == null) {
			return;
		}

		String type;
		if (!Intent.ACTION_SEND.equals(intent.getAction()) ||
				(type = intent.getType()) == null ||
				!type.startsWith("image/")) {
			return;
		}

		Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
		if (imageUri == null) {
			return;
		}

		Intent cropIntent = CropImageActivity.getIntentForImage(this, imageUri);
		cropImageLauncher.launch(cropIntent);
	}

	public void startPickImage() {
		Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
		pickImageIntent.setType("image/*");
		pickImageLauncher.launch(Intent.createChooser(pickImageIntent,
				getString(R.string.choose_image)));
	}

	public void startPickTexture(long id, @NonNull String samplerType) {
		Intent pickTextureIntent = new Intent(this, TextureViewActivity.class);
		pickTextureIntent.putExtra(TextureViewFragment.TEXTURE_ID, id);
		pickTextureIntent.putExtra(TextureViewFragment.SAMPLER_TYPE, samplerType);
		pickTextureLauncher.launch(pickTextureIntent);
	}
}