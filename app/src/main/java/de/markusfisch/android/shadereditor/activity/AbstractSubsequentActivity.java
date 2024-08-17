package de.markusfisch.android.shadereditor.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.view.SystemBarMetrics;

public abstract class AbstractSubsequentActivity extends AppCompatActivity {
	private FragmentManager fm;

	public static void addFragment(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
		getReplaceFragmentTransaction(fm, fragment)
				.addToBackStack(null)
				.commit();
	}

	public static void setFragment(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
		getReplaceFragmentTransaction(fm, fragment).commit();
	}

	public static void initToolbar(@NonNull AppCompatActivity activity) {
		Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
		activity.setSupportActionBar(toolbar);

		ActionBar actionBar = activity.getSupportActionBar();
		if (actionBar == null) {
			return;
		}
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	public static void initSystemBars(@NonNull AppCompatActivity activity) {
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

	protected void setFragmentForIntent(@Nullable Fragment fragment, @Nullable Intent intent) {
		if (fragment == null || intent == null) {
			finish();
			return;
		}

		fragment.setArguments(intent.getExtras());
		setFragment(getSupportFragmentManager(), fragment);
	}

	// Lint is missing the commit() but this method should return an
	// uncommitted transaction so it can be extended.
	@NonNull
	@SuppressLint("CommitTransaction")
	private static FragmentTransaction getReplaceFragmentTransaction(
			@NonNull FragmentManager fm,
			@NonNull Fragment fragment) {
		return fm.beginTransaction().replace(R.id.content_frame, fragment);
	}
}
