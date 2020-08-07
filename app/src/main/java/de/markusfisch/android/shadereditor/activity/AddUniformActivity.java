package de.markusfisch.android.shadereditor.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import de.markusfisch.android.shadereditor.fragment.UniformPagesFragment;

public class AddUniformActivity extends AbstractContentActivity {
	public static final String STATEMENT = "statement";
	public static final int PICK_IMAGE = 1;
	public static final int CROP_IMAGE = 2;
	public static final int PICK_TEXTURE = 3;

	public static void setAddUniformResult(Activity activity, String name) {
		Bundle bundle = new Bundle();
		bundle.putString(STATEMENT, name);

		Intent data = new Intent();
		data.putExtras(bundle);

		activity.setResult(RESULT_OK, data);
	}

	@Override
	public void onActivityResult(
			int requestCode,
			int resultCode,
			Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK) {
			return;
		}

		Uri imageUri;

		if (requestCode == PICK_IMAGE &&
				data != null &&
				(imageUri = data.getData()) != null) {
			startActivityForResult(
					CropImageActivity.getIntentForImage(
							this,
							imageUri),
					CROP_IMAGE);
		} else if (requestCode == CROP_IMAGE ||
				requestCode == PICK_TEXTURE) {
			setResult(RESULT_OK, data);
			finish();
		}
	}

	@Override
	protected Fragment defaultFragment() {
		startActivityForIntent(getIntent());
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

		startActivity(CropImageActivity.getIntentForImage(this, imageUri));
	}
}
