package de.markusfisch.android.shadereditor.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.view.SystemBarMetrics;

public abstract class AbstractSubsequentActivity extends AppCompatActivity {
	private FragmentManager fm;

	public static void addFragment(FragmentManager fm, Fragment fragment) {
		getReplaceFragmentTransaction(fm, fragment)
				.addToBackStack(null)
				.commit();
	}

	public static void setFragment(FragmentManager fm, Fragment fragment) {
		getReplaceFragmentTransaction(fm, fragment).commit();
	}

	public static void initToolbar(AppCompatActivity activity) {
		Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
		activity.setSupportActionBar(toolbar);

		ActionBar actionBar = activity.getSupportActionBar();
		if (actionBar == null) {
			return;
		}
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	public static void initSystemBars(AppCompatActivity activity) {
		SystemBarMetrics.setSystemBarColor(
				activity.getWindow(),
				ShaderEditorApp.preferences.getSystemBarColor(),
				false);
	}

	@Override
	public boolean onSupportNavigateUp() {
		if (fm.getBackStackEntryCount() > 0) {
			fm.popBackStack();
		} else {
			finish();
		}

		return true;
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		fm = getSupportFragmentManager();
	}

	protected void setFragmentForIntent(Fragment fragment, Intent intent) {
		if (fragment == null || intent == null) {
			finish();
			return;
		}

		fragment.setArguments(intent.getExtras());
		setFragment(getSupportFragmentManager(), fragment);
	}

	// Lint is missing the commit() but this method should return an
	// uncommitted transaction so it can be extended
	@SuppressLint("CommitTransaction")
	private static FragmentTransaction getReplaceFragmentTransaction(
			FragmentManager fm,
			Fragment fragment) {
		return fm.beginTransaction().replace(R.id.content_frame, fragment);
	}
}
