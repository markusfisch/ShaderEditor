package de.markusfisch.android.shadereditor.activity;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.managers.ExtraKeysManager;
import de.markusfisch.android.shadereditor.activity.managers.MainMenuManager;
import de.markusfisch.android.shadereditor.activity.managers.ShaderListManager;
import de.markusfisch.android.shadereditor.activity.managers.ShaderManager;
import de.markusfisch.android.shadereditor.activity.managers.ShaderViewManager;
import de.markusfisch.android.shadereditor.activity.managers.UIManager;
import de.markusfisch.android.shadereditor.activity.util.NavigationManager;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.fragment.EditorFragment;
import de.markusfisch.android.shadereditor.opengl.ShaderError;
import de.markusfisch.android.shadereditor.service.ShaderWallpaperService;
import de.markusfisch.android.shadereditor.view.SystemBarMetrics;

public class MainActivity extends AppCompatActivity implements ShaderManager.Provider {

	private static final String CODE_VISIBLE = "code_visible";

	private EditorFragment editorFragment;
	private UIManager uiManager;
	private ShaderManager shaderManager;
	private ShaderListManager shaderListManager;
	private ShaderViewManager shaderViewManager;
	private NavigationManager navigationManager;
	private DataSource dataSource;

	@Override
	public ShaderManager getShaderManager() {
		return shaderManager;
	}

	@Override
	protected void onCreate(@Nullable Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_main);

		dataSource = Database.getInstance(this).getDataSource();
		SystemBarMetrics.initSystemBars(this);

		editorFragment = state == null
				? new EditorFragment()
				: (EditorFragment) getSupportFragmentManager().findFragmentByTag(EditorFragment.TAG);

		if (state == null) {
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.content_frame, editorFragment, EditorFragment.TAG)
					.commit();
		}

		navigationManager = new NavigationManager(this);
		shaderViewManager = new ShaderViewManager(this, findViewById(R.id.preview),
				findViewById(R.id.quality), createShaderViewListener());
		ExtraKeysManager extraKeysManager = new ExtraKeysManager(this,
				findViewById(android.R.id.content), editorFragment::insert);
		uiManager = new UIManager(this, editorFragment, extraKeysManager, shaderViewManager);
		shaderListManager = new ShaderListManager(this, findViewById(R.id.shaders),
				dataSource, createShaderListListener());
		shaderManager = new ShaderManager(this, editorFragment, shaderViewManager,
				shaderListManager, uiManager, dataSource, createShaderViewListener());

		MainMenuManager mainMenuManager = new MainMenuManager(this, createEditorActions(),
				createShaderActions(extraKeysManager), createNavigationActions());

		uiManager.setupToolbar(mainMenuManager::show, v -> this.runShader(),
				v -> uiManager.toggleCodeVisibility(), v -> editorFragment.showErrors());


		shaderManager.handleSendText(getIntent());

		editorFragment.setOnTextChangedListener(text -> {
			shaderManager.setModified(true);
			if (ShaderEditorApp.preferences.doesRunOnChange()) {
				if (editorFragment.hasErrors()) {
					editorFragment.clearError();
					editorFragment.highlightErrors();
				}
				shaderViewManager.setFragmentShader(text);
			}
		});
		editorFragment.setCodeCompletionListener(extraKeysManager::setCompletions);
	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle state) {
		super.onRestoreInstanceState(state);
		shaderManager.restoreState(state);
		if (!state.getBoolean(CODE_VISIBLE, true)) {
			uiManager.toggleCodeVisibility();
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle state) {
		shaderManager.saveState(state);
		state.putBoolean(CODE_VISIBLE, editorFragment.isCodeVisible());
		super.onSaveInstanceState(state);
	}

	@Override
	protected void onResume() {
		super.onResume();
		uiManager.updateUiToPreferences();
		shaderListManager.loadShadersAsync();
		shaderViewManager.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		shaderViewManager.onPause();
		if (ShaderEditorApp.preferences.autoSave()) {
			shaderManager.saveShader();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		shaderManager.handleSendText(intent);
	}

	@Override
	protected void onPostCreate(Bundle state) {
		super.onPostCreate(state);
		if (uiManager.drawerToggle != null) {
			uiManager.drawerToggle.syncState();
		}
	}

	private ShaderListManager.Listener createShaderListListener() {
		return new ShaderListManager.Listener() {
			@Override
			public void onShaderSelected(long id) {
				if (ShaderEditorApp.preferences.autoSave()) {
					shaderManager.saveShader();
				}
				uiManager.closeDrawers();
				shaderManager.selectShader(id);
			}

			@Override
			public void onShaderRenamed(long id, @NonNull String name) {
				if (id == shaderManager.getSelectedShaderId()) {
					uiManager.setToolbarTitle(name);
				}
				shaderListManager.loadShadersAsync();
			}

			@Override
			public void onAllShadersDeleted() {
				shaderManager.selectShader(0);
			}
		};
	}

	private ShaderViewManager.Listener createShaderViewListener() {
		return new ShaderViewManager.Listener() {
			@Override
			public void onFramesPerSecond(int fps) {
				if (fps > 0) {
					uiManager.setToolbarSubtitle(fps + " fps");
				}
			}

			@Override
			public void onInfoLog(@NonNull List<ShaderError> infoLog) {
				runOnUiThread(() -> {
					editorFragment.setErrors(infoLog);
					findViewById(R.id.show_errors).setVisibility(editorFragment.hasErrors() ?
							View.VISIBLE : View.GONE);
					if (editorFragment.hasErrors()) {
						Snackbar.make(findViewById(R.id.main_coordinator),
										infoLog.get(0).toString(), Snackbar.LENGTH_LONG)
								.setAction(R.string.details, v -> editorFragment.showErrors())
								.setAnchorView(R.id.extra_keys)
								.show();
					}
				});
			}

			@Override
			public void onQualityChanged(float quality) {
				shaderManager.setQuality(quality);
				if (shaderManager.getSelectedShaderId() > 0) {
					dataSource.updateShader(shaderManager.getSelectedShaderId(), null,
							null, quality);
				}
			}
		};
	}

	private MainMenuManager.EditorActions createEditorActions() {
		return new MainMenuManager.EditorActions() {
			@Override
			public void onUndo() {
				editorFragment.undo();
			}

			@Override
			public void onRedo() {
				editorFragment.redo();
			}

			@Override
			public boolean canUndo() {
				return editorFragment != null && editorFragment.canUndo();
			}

			@Override
			public boolean canRedo() {
				return editorFragment != null && editorFragment.canRedo();
			}
		};
	}

	private MainMenuManager.ShaderActions createShaderActions(ExtraKeysManager extraKeysManager) {
		return new MainMenuManager.ShaderActions() {
			@Override
			public void onAddShader() {
				long defaultId = ShaderEditorApp.preferences.getDefaultNewShader();
				if (defaultId > 0 && dataSource.getShader(defaultId) != null) {
					duplicateShader(defaultId);
				} else {
					shaderManager.selectShader(dataSource.insertNewShader());
				}
			}

			@Override
			public void onSaveShader() {
				shaderManager.saveShader();
			}

			@Override
			public void onDuplicateShader() {
				if (editorFragment == null || shaderManager.getSelectedShaderId() < 1) return;
				if (shaderManager.isModified()) shaderManager.saveShader();
				duplicateShader(shaderManager.getSelectedShaderId());
			}

			@Override
			public void onDeleteShader() {
				if (shaderManager.getSelectedShaderId() < 1) return;
				new MaterialAlertDialogBuilder(MainActivity.this)
						.setMessage(R.string.sure_remove_shader)
						.setPositiveButton(android.R.string.ok, (dialog, which) -> {
							shaderManager.deleteShader(shaderManager.getSelectedShaderId());
							shaderManager.selectShader(dataSource.getFirstShaderId());
						})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
			}

			@Override
			public void onShareShader() {
				navigationManager.shareShader(editorFragment.getText());
			}

			@Override
			public void onUpdateWallpaper() {
				updateWallpaper();
			}

			@Override
			public void onToggleExtraKeys() {
				extraKeysManager.setVisible(ShaderEditorApp.preferences.toggleShowExtraKeys());
			}


			@Override
			public long getSelectedShaderId() {
				return shaderManager.getSelectedShaderId();
			}
		};
	}

	private MainMenuManager.NavigationActions createNavigationActions() {
		return new MainMenuManager.NavigationActions() {
			@Override
			public void onAddUniform() {
				navigationManager.goToAddUniform(shaderManager.addUniformLauncher);
			}

			@Override
			public void onLoadSample() {
				navigationManager.goToLoadSample(shaderManager.loadSampleLauncher);
			}

			@Override
			public void onShowSettings() {
				navigationManager.goToPreferences();
			}

			@Override
			public void onShowFaq() {
				navigationManager.goToFaq();
			}
		};
	}

	private void duplicateShader(long id) {
		// The ShaderManager is now responsible for getting the shader details.
		// We just need to pass the ID.
		long newId = shaderManager.duplicateShader(id);
		if (newId > 0) {
			shaderManager.selectShader(newId);
		}
	}

	private void updateWallpaper() {
		if (shaderManager.getSelectedShaderId() < 1) return;
		if (shaderManager.isModified()) shaderManager.saveShader();

		ShaderEditorApp.preferences.setWallpaperShader(0); // Force change
		ShaderEditorApp.preferences.setWallpaperShader(shaderManager.getSelectedShaderId());

		int messageId = R.string.wallpaper_set;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
				!WallpaperManager.getInstance(this).isWallpaperSupported()) {
			messageId = R.string.cannot_set_wallpaper;
		} else if (!ShaderWallpaperService.isRunning()) {
			Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
					.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
							new ComponentName(this, ShaderWallpaperService.class));
			try {
				startActivity(intent);
				return;
			} catch (Exception e) {
				messageId = R.string.pick_live_wallpaper_manually;
			}
		}
		Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
	}

	private void runShader() {
		String src = editorFragment.getText();
		editorFragment.clearError();
		if (ShaderEditorApp.preferences.doesSaveOnRun()) {
			PreviewActivity.renderStatus.reset();
			shaderManager.saveShader();
		}
		if (ShaderEditorApp.preferences.doesRunInBackground()) {
			shaderViewManager.setFragmentShader(src);
		} else {
			navigationManager.showPreview(src, shaderManager.getQuality(),
					shaderManager.previewShaderLauncher);
		}
	}
}