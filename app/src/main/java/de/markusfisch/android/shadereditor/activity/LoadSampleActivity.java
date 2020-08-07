package de.markusfisch.android.shadereditor.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import de.markusfisch.android.shadereditor.fragment.LoadSampleFragment;

public class LoadSampleActivity extends AbstractContentActivity {
	public static final String NAME = "name";
	public static final String RESOURCE_ID = "resource_id";
	public static final String THUMBNAIL_ID = "thumbnail_id";
	public static final String QUALITY = "quality";

	public static void setSampleResult(
			Activity activity,
			String name,
			int resId,
			int thumbId,
			float quality) {
		Bundle bundle = new Bundle();
		bundle.putString(NAME, name);
		bundle.putInt(RESOURCE_ID, resId);
		bundle.putInt(THUMBNAIL_ID, thumbId);
		bundle.putFloat(QUALITY, quality);

		Intent data = new Intent();
		data.putExtras(bundle);

		activity.setResult(RESULT_OK, data);
	}

	@Override
	protected Fragment defaultFragment() {
		return new LoadSampleFragment();
	}
}
