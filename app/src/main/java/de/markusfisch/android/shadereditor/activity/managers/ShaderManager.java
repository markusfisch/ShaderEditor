package de.markusfisch.android.shadereditor.activity.managers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.activity.LoadSampleActivity;
import de.markusfisch.android.shadereditor.activity.PreviewActivity;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.fragment.EditorFragment;
import de.markusfisch.android.shadereditor.project.LegacyShaderProjectSource;
import de.markusfisch.android.shadereditor.project.LooseShaderFileProjectSource;
import de.markusfisch.android.shadereditor.project.ShaderProjectSession;

public class ShaderManager {
	public final ActivityResultLauncher<Intent> addUniformLauncher;
	public final ActivityResultLauncher<Intent> loadSampleLauncher;
	public final ActivityResultLauncher<Intent> previewShaderLauncher;

	private static final String SELECTED_SHADER_ID = "selected_shader_id";
	private static final String PROJECT_SESSION = "project_session";
	private static final String MODIFIED = "modified";
	private static final long NO_SHADER = 0;

	private final AppCompatActivity activity;
	private final EditorFragment editorFragment;
	private final ShaderViewManager shaderViewManager;
	private final ShaderListManager shaderListManager;
	private final UIManager uiManager;
	private final DataSource dataSource;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final Handler handler = new Handler(Looper.getMainLooper());

	private long selectedShaderId = NO_SHADER;
	private float quality = 1f;
	private boolean isModified = false;
	@Nullable
	private ShaderProjectSession currentProjectSession;

	public ShaderManager(@NonNull AppCompatActivity activity,
			EditorFragment editorFragment,
			ShaderViewManager shaderViewManager,
			ShaderListManager shaderListManager,
			UIManager uiManager,
			DataSource dataSource,
			ShaderViewManager.Listener shaderViewManagerListener) {
		this.activity = activity;
		this.editorFragment = editorFragment;
		this.shaderViewManager = shaderViewManager;
		this.shaderListManager = shaderListManager;
		this.uiManager = uiManager;
		this.dataSource = dataSource;

		addUniformLauncher = activity.registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(), result -> {
					if (result.getResultCode() == Activity.RESULT_OK &&
							result.getData() != null) {
						editorFragment.addUniform(
								result.getData().getStringExtra(
										AddUniformActivity.STATEMENT));
					}
				});

		loadSampleLauncher = activity.registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(), result -> {
					if (result.getResultCode() == Activity.RESULT_OK &&
							result.getData() != null) {
						if (isModified()) {
							saveShader();
						}
						createShaderFromResource(
								Objects.requireNonNull(result.getData().getStringExtra(
										LoadSampleActivity.NAME)),
								result.getData().getIntExtra(LoadSampleActivity.RESOURCE_ID,
										R.raw.new_shader),
								result.getData().getIntExtra(LoadSampleActivity.THUMBNAIL_ID,
										R.drawable.thumbnail_new_shader),
								result.getData().getFloatExtra(LoadSampleActivity.QUALITY, 1f));
					}
				});

		previewShaderLauncher =
				activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
					if (result.getResultCode() != Activity.RESULT_OK) {
						return;
					}
					PreviewActivity.RenderStatus status =
							PreviewActivity.renderStatus;
					if (status.getFps() > 0) {
						shaderViewManagerListener.onFramesPerSecond(
								status.getFps());
					}
					if (status.getInfoLog() != null) {
						shaderViewManagerListener.onInfoLog(status.getInfoLog());
					}
					if (getSelectedShaderId() > 0 &&
							status.getThumbnail() != null &&
							ShaderEditorApp.preferences.doesSaveOnRun()) {
						saveShader(true);
					}
				});
	}

	public long getSelectedShaderId() {
		return selectedShaderId;
	}

	public void destroy() {
		executor.shutdownNow();
		handler.removeCallbacksAndMessages(null);
	}

	public void createShaderFromResource(@Nullable String name,
			int sourceId, int thumbId, float quality) {
		executor.execute(() -> {
			long newId = dataSource.shader.insertShaderFromResource(
					activity.getApplicationContext(), name, sourceId, thumbId, quality);
			handler.post(() -> {
				if (activity.isFinishing() || activity.isDestroyed()) {
					return;
				}
				selectShader(newId);
				shaderListManager.loadShadersAsync();
			});
		});
	}

	public float getQuality() {
		return quality;
	}

	public void setQuality(float quality) {
		this.quality = quality;
		if (currentProjectSession != null) {
			currentProjectSession = currentProjectSession.withQuality(quality);
		}
		setModified(true);
	}

	public boolean isModified() {
		return isModified;
	}

	public void setModified(boolean modified) {
		this.isModified = modified;
		// The EditorFragment's modified state is managed internally by its
		// UndoRedo helper. It cannot and should not be set from the outside.
		// When a shader is loaded via `selectShader`,
		// `editorFragment.setText()` is called, which clears the history
		// and resets the modified state.
	}

	public void saveState(@NonNull Bundle outState) {
		outState.putLong(SELECTED_SHADER_ID, selectedShaderId);
		outState.putSerializable(PROJECT_SESSION, getEditedProjectSession());
		outState.putBoolean(MODIFIED, isModified);
	}

	@SuppressWarnings("deprecation")
	public void restoreState(@NonNull Bundle savedInstanceState) {
		selectedShaderId = savedInstanceState.getLong(
				SELECTED_SHADER_ID, NO_SHADER);
		if (savedInstanceState.getSerializable(PROJECT_SESSION) instanceof
				ShaderProjectSession projectSession) {
			currentProjectSession = projectSession;
			quality = projectSession.getQuality();
			uiManager.setToolbarTitle(projectSession.getProject().getTitle());
			shaderListManager.setSelectedShaderId(selectedShaderId);
			shaderViewManager.setProjectSession(projectSession);
			updatePendingCrashShaderId();
		}
		setModified(savedInstanceState.getBoolean(MODIFIED));
	}

	@NonNull
	public ShaderProjectSession getEditedProjectSession() {
		return getEditedProjectSession(editorFragment.getText());
	}

	@NonNull
	public ShaderProjectSession getEditedProjectSession(
			@NonNull String entryPointSource) {
		return getCurrentProjectSession()
				.withEntryPointSource(entryPointSource)
				.withQuality(quality);
	}

	public void selectShader(long id) {
		if (isModified()) {
			saveShader();
		}

		PreviewActivity.renderStatus.reset();
		var shader = dataSource.shader.getShader(id);
		if (shader == null) {
			applyProjectSession(
					NO_SHADER,
					createScratchProjectSession(
							activity.getString(R.string.new_shader_template),
							1f),
					activity.getString(R.string.add_shader));
			return;
		}

		ShaderProjectSession projectSession =
				new LegacyShaderProjectSource(shader).openSession();
		ShaderEditorApp.preferences.setLastOpenedShader(id);
		applyProjectSession(id,
				projectSession,
				projectSession.getProject().getTitle());
	}

	public boolean saveShader() {
		return saveShader(false);
	}

	public boolean saveShader(boolean force) {
		if (!force && !isModified()) {
			return true;
		}

		String src = editorFragment.getText();
		if (src.trim().isEmpty() && selectedShaderId <= 0) {
			return true;
		}

		byte[] thumbnail = getThumbnail();
		if (selectedShaderId > 0) {
			dataSource.shader.updateShader(
					selectedShaderId, src, thumbnail, quality);
		} else {
			long newId = dataSource.shader.insertShader(
					src, null, thumbnail, quality);
			if (newId <= 0) {
				Toast.makeText(activity, R.string.storage_not_writeable,
						Toast.LENGTH_LONG).show();
				return false;
			}
			selectedShaderId = newId;
			shaderListManager.setSelectedShaderId(selectedShaderId);
		}

		currentProjectSession = loadSavedProjectSession(selectedShaderId, src);
		updatePendingCrashShaderId();
		setModified(false);
		shaderListManager.loadShadersAsync();
		Toast.makeText(activity, R.string.shader_saved,
				Toast.LENGTH_SHORT).show();
		return true;
	}

	private byte[] getThumbnail() {
		return ShaderEditorApp.preferences.doesRunInBackground()
				? shaderViewManager.getThumbnail()
				: PreviewActivity.renderStatus.getThumbnail();
	}

	public void handleSendText(@Nullable Intent intent) {
		if (intent == null || intent.getAction() == null) {
			return;
		}
		if (!Intent.ACTION_SEND.equals(intent.getAction()) &&
				!Intent.ACTION_VIEW.equals(intent.getAction())) {
			return;
		}

		Uri uri = intent.getData();
		if (uri == null && Intent.ACTION_SEND.equals(intent.getAction())) {
			uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class);
		}

		try {
			String source;
			if (uri != null) {
				source = readShaderSource(uri);
			} else if (Intent.ACTION_SEND.equals(intent.getAction())) {
				CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
				source = text != null ? text.toString() : null;
			} else {
				source = null;
			}
			if (source == null) {
				return;
			}
			ShaderProjectSession projectSession =
					new LooseShaderFileProjectSource(uri, source, 1f)
							.openSession();
			if (isModified() && !saveShader()) {
				return;
			}
			PreviewActivity.renderStatus.reset();
			intent.setAction(null);
			applyProjectSession(NO_SHADER,
					projectSession,
					projectSession.getProject().getTitle());
			setModified(true);
		} catch (IOException | SecurityException e) {
			Toast.makeText(activity, R.string.unsuitable_text,
					Toast.LENGTH_SHORT).show();
		}
	}

	@Nullable
	private String readShaderSource(@NonNull Uri uri) throws IOException {
		try (var in = activity.getContentResolver().openInputStream(uri)) {
			if (in == null) {
				return null;
			}
			try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				char[] buffer = new char[4096];
				int len;
				var source = new StringBuilder();
				while ((len = reader.read(buffer)) != -1) {
					source.append(buffer, 0, len);
				}
				return source.toString();
			}
		}
	}

	public long duplicateShader(long shaderId) {
		var shader = dataSource.shader.getShader(shaderId);
		if (shader == null) {
			return NO_SHADER;
		}
		var thumbnail = dataSource.shader.getThumbnail(shaderId);
		return dataSource.shader.insertShader(
				shader.fragmentShader(),
				null,
				thumbnail,
				shader.quality());
	}

	public void deleteShader(long shaderId) {
		dataSource.shader.removeShader(shaderId);
		if (shaderId == selectedShaderId) {
			selectedShaderId = NO_SHADER;
			currentProjectSession = null;
			setModified(false);
			updatePendingCrashShaderId();
		}
	}

	@NonNull
	private ShaderProjectSession getCurrentProjectSession() {
		return currentProjectSession != null
				? currentProjectSession
				: createScratchProjectSession(editorFragment.getText(), quality);
	}

	@NonNull
	private ShaderProjectSession createScratchProjectSession(
			@NonNull String source,
			float quality) {
		return new LooseShaderFileProjectSource(null, source, quality)
				.openSession();
	}

	@NonNull
	private ShaderProjectSession loadSavedProjectSession(long shaderId,
			@NonNull String fallbackSource) {
		var shader = dataSource.shader.getShader(shaderId);
		return shader != null
				? new LegacyShaderProjectSource(shader).openSession()
				: createScratchProjectSession(fallbackSource, quality);
	}

	private void applyProjectSession(long shaderId,
			@NonNull ShaderProjectSession projectSession,
			@NonNull String toolbarTitle) {
		selectedShaderId = shaderId;
		currentProjectSession = projectSession;
		quality = projectSession.getQuality();
		editorFragment.setText(projectSession.getEntryPointSource());
		uiManager.setToolbarTitle(toolbarTitle);
		shaderListManager.setSelectedShaderId(selectedShaderId);
		shaderViewManager.setProjectSession(projectSession);
		updatePendingCrashShaderId();
		setModified(false);
	}

	private void updatePendingCrashShaderId() {
		ShaderEditorApp.preferences.setPendingCrashShaderId(
				ShaderEditorApp.preferences.doesRunInBackground() &&
						selectedShaderId > 0
						? selectedShaderId
						: 0);
	}
}
