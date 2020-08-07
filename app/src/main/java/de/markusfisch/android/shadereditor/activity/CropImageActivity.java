package de.markusfisch.android.shadereditor.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.fragment.CropImageFragment;
import de.markusfisch.android.shadereditor.view.SystemBarMetrics;
import de.markusfisch.android.shadereditor.widget.CropImageView;

public class CropImageActivity
		extends AbstractSubsequentActivity
		implements CropImageFragment.CropImageViewProvider {
	private CropImageView cropImageView;

	public static Intent getIntentForImage(Context context, Uri imageUri) {
		Intent intent = new Intent(context, CropImageActivity.class);
		intent.putExtra(CropImageFragment.IMAGE_URI, imageUri);
		return intent;
	}

	@Override
	public CropImageView getCropImageView() {
		return cropImageView;
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_crop_image);

		cropImageView = (CropImageView) findViewById(R.id.crop_image_view);

		SystemBarMetrics.initSystemBars(this, cropImageView.insets);
		AbstractSubsequentActivity.initToolbar(this);

		if (state == null) {
			setFragmentForIntent(new CropImageFragment(), getIntent());
		}
	}
}
