package de.markusfisch.android.shadereditor.activity.managers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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
	private static final long NO_SHADER = 0;

	private final AppCompatActivity activity;
	private final EditorFragment editorFragment;
	private final ShaderViewManager shaderViewManager;
	private final ShaderListManager shaderListManager;
	private final UIManager uiManager;
	private final DataSource dataSource;

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
						long newId = dataSource.shader.insertShaderFromResource(activity,
								Objects.requireNonNull(result.getData().getStringExtra(
										LoadSampleActivity.NAME)),
								result.getData().getIntExtra(LoadSampleActivity.RESOURCE_ID,
										R.raw.new_shader),
								result.getData().getIntExtra(LoadSampleActivity.THUMBNAIL_ID,
										R.drawable.thumbnail_new_shader),
								result.getData().getFloatExtra(LoadSampleActivity.QUALITY, 1f));
						selectShader(newId);
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
						saveShader();
					}
				});
	}

	public long getSelectedShaderId() {
		return selectedShaderId;
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
	}

	public void restoreState(@NonNull Bundle savedInstanceState) {
		selectedShaderId = savedInstanceState.getLong(
				SELECTED_SHADER_ID, NO_SHADER);
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

	public void saveShader() {
		saveShader(false);
	}

	public void saveShader(boolean force) {
		if (!force && !isModified()) {
			return;
		}

		String src = editorFragment.getText();
		if (src.trim().isEmpty() && selectedShaderId <= 0) {
			return;
		}

		byte[] thumbnail = getThumbnail();
		if (selectedShaderId > 0) {
			dataSource.shader.updateShader(
					selectedShaderId, src, thumbnail, quality);
		} else {
			selectedShaderId = dataSource.shader.insertShader(
					src, null, thumbnail, quality);
			shaderListManager.setSelectedShaderId(selectedShaderId);
		}

		currentProjectSession = loadSavedProjectSession(selectedShaderId, src);
		updatePendingCrashShaderId();
		setModified(false);
		shaderListManager.loadShadersAsync();
		Toast.makeText(activity, R.string.shader_saved,
				Toast.LENGTH_SHORT).show();
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
		if (uri == null) {
			return;
		}

		try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
			if (in == null) {
				return;
			}
			byte[] buffer = new byte[4096];
			int len;
			var sb = new StringBuilder();
			while ((len = in.read(buffer)) != -1) {
				sb.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
			}
			ShaderProjectSession projectSession =
					new LooseShaderFileProjectSource(uri, sb.toString(), 1f)
							.openSession();
			PreviewActivity.renderStatus.reset();
			intent.setAction(null);
			applyProjectSession(NO_SHADER,
					projectSession,
					projectSession.getProject().getTitle());
			setModified(true);
		} catch (IOException e) {
			Toast.makeText(activity, R.string.unsuitable_text,
					Toast.LENGTH_SHORT).show();
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
