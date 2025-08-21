package de.markusfisch.android.shadereditor.activity;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.view.SystemBarMetrics;

public abstract class AbstractContentActivity
		extends AbstractSubsequentActivity {
	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_subsequent);

		SystemBarMetrics.initMainLayout(this, null);
		initToolbar(this);

		if (state == null) {
			AbstractSubsequentActivity.setFragment(
					getSupportFragmentManager(),
					defaultFragment());
		}
	}

	protected abstract Fragment defaultFragment();
}
