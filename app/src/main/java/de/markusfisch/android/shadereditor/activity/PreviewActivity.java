package de.markusfisch.android.shadereditor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import de.markusfisch.android.shadereditor.opengl.ShaderError;
import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;
import de.markusfisch.android.shadereditor.widget.ShaderView;

public class PreviewActivity extends AppCompatActivity {
	public static class RenderStatus {
		private volatile int fps;
		private volatile List<ShaderError> infoLog;
		private byte[] thumbnail;

		RenderStatus() {
			reset();
		}

		public void reset() {
			fps = 0;
			infoLog = null;
			thumbnail = null;
		}

		public int getFps() {
			return fps;
		}

		public List<ShaderError> getInfoLog() {
			return infoLog;
		}

		public byte[] getThumbnail() {
			return thumbnail;
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
					public void onInfoLog(@NonNull List<ShaderError> infoLog) {
						// Invoked from the GL thread.
						renderStatus.infoLog = infoLog;
						if (!infoLog.isEmpty()) {
							runOnUiThread(finishRunnable);
						}
					}
				});

		setContentView(shaderView);

		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
						View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
						View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_FULLSCREEN);
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
