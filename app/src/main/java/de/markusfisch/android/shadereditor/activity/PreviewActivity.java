package de.markusfisch.android.shadereditor.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;
import de.markusfisch.android.shadereditor.view.SystemBarMetrics;
import de.markusfisch.android.shadereditor.widget.ShaderView;

public class PreviewActivity extends AppCompatActivity {
	public static class RenderStatus {
		volatile int fps;
		volatile String infoLog;
		byte[] thumbnail;

		RenderStatus() {
			reset();
		}

		void reset() {
			fps = 0;
			infoLog = null;
			thumbnail = null;
		}
	}

	public static final String FRAGMENT_SHADER = "fragment_shader";
	public static final String QUALITY = "quality";
	public static final RenderStatus renderStatus = new RenderStatus();

	private final Runnable finishRunnable = this::finish;
	private final Runnable thumbnailRunnable = new Runnable() {
		@Override
		public void run() {
			if (shaderView == null) {
				return;
			}

			renderStatus.thumbnail = shaderView.getRenderer().getThumbnail();
		}
	};

	private ShaderView shaderView;

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		renderStatus.reset();
		shaderView = new ShaderView(this);

		if (!setShaderFromIntent(getIntent())) {
			finish();
			return;
		}

		shaderView.getRenderer().setOnRendererListener(
				new ShaderRenderer.OnRendererListener() {
					@Override
					public void onFramesPerSecond(int fps) {
						// Invoked from the GL thread.
						renderStatus.fps = fps;
					}

					@Override
					public void onInfoLog(String infoLog) {
						// Invoked from the GL thread.
						renderStatus.infoLog = infoLog;
						runOnUiThread(finishRunnable);
					}
				});

		setContentView(shaderView);
		SystemBarMetrics.hideNavigation(getWindow());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (!setShaderFromIntent(intent)) {
			finish();
		}
	}

	@Override
	protected void onStart() {
		// Don't use onResume()/onPause() because in multi window mode
		// an activity may be paused but should still show animations.
		super.onStart();

		shaderView.onResume();
		renderStatus.reset();
		shaderView.postDelayed(thumbnailRunnable, 500);
	}

	@Override
	protected void onStop() {
		super.onStop();

		shaderView.onPause();
	}

	private boolean setShaderFromIntent(Intent intent) {
		String fragmentShader;

		if (intent == null ||
				shaderView == null ||
				(fragmentShader = intent.getStringExtra(
						FRAGMENT_SHADER)) == null) {
			return false;
		}

		shaderView.setFragmentShader(
				fragmentShader,
				intent.getFloatExtra(QUALITY, 1f));

		return true;
	}
}
