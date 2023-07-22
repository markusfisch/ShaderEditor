package de.markusfisch.android.shadereditor.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		// It's important _not_ to inflate a layout file here
		// because that would happen after the app is fully
		// initialized what is too late.

		startActivity(new Intent(this, MainActivity.class));
		finish();
	}
}
