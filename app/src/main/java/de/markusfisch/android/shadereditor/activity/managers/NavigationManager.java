package de.markusfisch.android.shadereditor.activity.managers;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.activity.LoadSampleActivity;
import de.markusfisch.android.shadereditor.activity.PreferencesActivity;
import de.markusfisch.android.shadereditor.activity.PreviewActivity;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

public class NavigationManager {
	@NonNull
	private final Activity activity;

	public NavigationManager(@NonNull Activity activity) {
		this.activity = activity;
	}

	public void goToAddUniform(@NonNull ActivityResultLauncher<Intent> launcher) {
		launcher.launch(new Intent(activity, AddUniformActivity.class));
	}

	public void goToLoadSample(@NonNull ActivityResultLauncher<Intent> launcher) {
		launcher.launch(new Intent(activity, LoadSampleActivity.class));
	}

	public void goToPreferences() {
		activity.startActivity(new Intent(activity, PreferencesActivity.class));
	}

	public void goToFaq() {
		tryStartActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github" + ".com" +
				"/markusfisch/ShaderEditor/blob/master/FAQ.md")));
	}

	public void showPreview(String src, float quality, ActivityResultLauncher<Intent> launcher) {
		Intent intent = new Intent(activity, PreviewActivity.class);
		intent.putExtra(PreviewActivity.QUALITY, quality);
		intent.putExtra(PreviewActivity.FRAGMENT_SHADER, src);

		if (ShaderEditorApp.preferences.doesRunInNewTask() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
		} else {
			launcher.launch(intent);
		}
	}

	public void shareShader(String shader) {
		final var prefs = ShaderEditorApp.preferences;
		if (!prefs.exportTabs() && shader.contains("\t")) {
			String spaces = " ".repeat(prefs.getTabWidth());
			shader = shader.replace("\t", spaces);
		}
		Intent intent = new Intent();
		intent.setType("text/plain");
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT, shader);
		activity.startActivity(Intent.createChooser(intent,
				activity.getString(R.string.share_shader)));
	}

	private void tryStartActivity(Intent intent) {
		try {
			activity.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(activity, R.string.cannot_open_content, Toast.LENGTH_SHORT).show();
		}
	}
}