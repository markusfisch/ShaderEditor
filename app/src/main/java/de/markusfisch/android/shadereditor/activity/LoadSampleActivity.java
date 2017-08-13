package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.fragment.LoadSampleFragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class LoadSampleActivity extends AbstractContentActivity {
	public static final String NAME = "name";
	public static final String RESOURCE_ID = "resource_id";
	public static final String THUMBNAIL_ID = "thumbnail_id";

	public static void setSampleResult(
			Activity activity,
			String name,
			int resId,
			int thumbId) {
		Bundle bundle = new Bundle();
		bundle.putString(NAME, name);
		bundle.putInt(RESOURCE_ID, resId);
		bundle.putInt(THUMBNAIL_ID, thumbId);

		Intent data = new Intent();
		data.putExtras(bundle);

		activity.setResult(RESULT_OK, data);
	}

	@Override
	protected Fragment defaultFragment() {
		return new LoadSampleFragment();
	}
}
