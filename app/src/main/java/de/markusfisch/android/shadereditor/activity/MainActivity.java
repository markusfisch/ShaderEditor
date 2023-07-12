package de.markusfisch.android.shadereditor.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.ShaderAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.fragment.EditorFragment;
import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;
import de.markusfisch.android.shadereditor.service.ShaderWallpaperService;
import de.markusfisch.android.shadereditor.view.SoftKeyboard;
import de.markusfisch.android.shadereditor.view.SystemBarMetrics;
import de.markusfisch.android.shadereditor.widget.ShaderEditor;
import de.markusfisch.android.shadereditor.widget.ShaderView;
import de.markusfisch.android.shadereditor.widget.TouchThruDrawerLayout;

public class MainActivity
		extends AppCompatActivity
		implements ShaderEditor.OnTextChangedListener {
	private static final String SELECTED_SHADER = "selected_shader";
	private static final String CODE_VISIBLE = "code_visible";
	private static final int PREVIEW_SHADER = 1;
	private static final int ADD_UNIFORM = 2;
	private static final int LOAD_SAMPLE = 3;
	private static final int FIRST_SHADER = -1;
	private static final int NO_SHADER = 0;

	private final Runnable updateFpsRunnable = new Runnable() {
		@Override
		public void run() {
			// "fps" should be the same in all languages.
			toolbar.setSubtitle(fps + " fps");
		}
	};

	private EditorFragment editorFragment;
	private Toolbar toolbar;
	private Spinner qualitySpinner;
	private MenuItem insertTabMenuItem;
	private TouchThruDrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private View menuFrame;
	private ListView listView;
	private ShaderAdapter shaderAdapter;
	private ShaderView shaderView;
	private long selectedShaderId = FIRST_SHADER;
	private volatile int fps;
	private float[] qualityValues;
	private float quality = 1f;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent e) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (drawerLayout.isDrawerOpen(menuFrame)) {
				closeDrawer();
			} else {
				openDrawer();
			}
			return true;
		}
		return super.onKeyDown(keyCode, e);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		insertTabMenuItem = menu.findItem(R.id.insert_tab);
		insertTabMenuItem.setVisible(
				ShaderEditorApp.preferences.doesShowInsertTab());
		menu.findItem(R.id.run_code).setVisible(
				!ShaderEditorApp.preferences.doesRunOnChange());
		menu.findItem(R.id.toggle_code).setVisible(
				ShaderEditorApp.preferences.doesRunInBackground());
		menu.findItem(R.id.update_wallpaper).setTitle(
				ShaderEditorApp.preferences.getWallpaperShader() ==
						selectedShaderId
						? R.string.update_wallpaper
						: R.string.set_as_wallpaper);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.insert_tab) {
			insertTab();
			return true;
		} else if (itemId == R.id.run_code) {
			runShader();
			return true;
		} else if (itemId == R.id.undo) {
			editorFragment.undo();
			return true;
		} else if (itemId == R.id.redo) {
			editorFragment.redo();
			return true;
		} else if (itemId == R.id.save_shader) {
			saveShader(selectedShaderId);
			return true;
		} else if (itemId == R.id.toggle_code) {
			toggleCode();
			return true;
		} else if (itemId == R.id.add_shader) {
			addShader();
			return true;
		} else if (itemId == R.id.duplicate_shader) {
			duplicateSelectedShader();
			return true;
		} else if (itemId == R.id.delete_shader) {
			deleteShader(selectedShaderId);
			return true;
		} else if (itemId == R.id.share_shader) {
			shareShader();
			return true;
		} else if (itemId == R.id.update_wallpaper) {
			updateWallpaper(selectedShaderId);
			return true;
		} else if (itemId == R.id.add_uniform) {
			addUniform();
			return true;
		} else if (itemId == R.id.load_sample) {
			loadSample();
			return true;
		} else if (itemId == R.id.faq) {
			showFaq();
			return true;
		} else if (itemId == R.id.settings) {
			showSettings();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onTextChanged(String text) {
		if (!ShaderEditorApp.preferences.doesRunOnChange()) {
			return;
		}

		if (editorFragment.hasErrorLine()) {
			editorFragment.clearError();
			editorFragment.updateHighlighting();
		}

		setFragmentShader(text);
	}

	@Override
	protected void onActivityResult(
			int requestCode,
			int resultCode,
			Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Add uniform statement.
		if (editorFragment != null &&
				requestCode == ADD_UNIFORM &&
				resultCode == RESULT_OK &&
				data != null) {
			editorFragment.addUniform(data.getStringExtra(
					AddUniformActivity.STATEMENT));
		}

		// Load sample.
		if (editorFragment != null &&
				requestCode == LOAD_SAMPLE &&
				resultCode == RESULT_OK &&
				data != null) {
			if (selectedShaderId != FIRST_SHADER) {
				saveShader(selectedShaderId);
			}
			selectShaderAndUpdate(ShaderEditorApp.db.insertShaderFromResource(
					this,
					data.getStringExtra(LoadSampleActivity.NAME),
					data.getIntExtra(LoadSampleActivity.RESOURCE_ID,
							R.raw.new_shader),
					data.getIntExtra(LoadSampleActivity.THUMBNAIL_ID,
							R.drawable.thumbnail_new_shader),
					data.getFloatExtra(LoadSampleActivity.QUALITY,
							1f)));
		}

		// Update fps, info log and thumbnail after shader ran.
		if (requestCode == PREVIEW_SHADER) {
			PreviewActivity.RenderStatus status =
					PreviewActivity.renderStatus;

			if (status.fps > 0) {
				postUpdateFps(status.fps);
			}

			if (status.infoLog != null) {
				postInfoLog(status.infoLog);
			}

			if (selectedShaderId > 0 &&
					status.thumbnail != null &&
					ShaderEditorApp.preferences.doesSaveOnRun()) {
				saveShader(selectedShaderId);
			}
		}
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_main);

		SystemBarMetrics.initSystemBars(this);
		initToolbar();
		initQualitySpinner();
		initDrawer();
		initListView();
		initShaderView();

		if (state == null || (editorFragment =
				(EditorFragment) getSupportFragmentManager()
						.findFragmentByTag(EditorFragment.TAG)) == null) {
			editorFragment = new EditorFragment();
			getSupportFragmentManager()
					.beginTransaction()
					.replace(
							R.id.content_frame,
							editorFragment,
							EditorFragment.TAG)
					.commit();
		}
	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle state) {
		super.onRestoreInstanceState(state);

		selectedShaderId = state.getLong(SELECTED_SHADER);
		if (!state.getBoolean(CODE_VISIBLE)) {
			toggleCode();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		if (state != null) {
			state.putLong(SELECTED_SHADER, selectedShaderId);
			state.putBoolean(CODE_VISIBLE, editorFragment.isCodeVisible());
		}
		super.onSaveInstanceState(state);
	}

	@Override
	protected void onPostCreate(Bundle state) {
		super.onPostCreate(state);
		drawerToggle.syncState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateUiToPreferences();
		getShadersAsync();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (shaderView.getVisibility() == View.VISIBLE) {
			shaderView.onPause();
		}
		autoSave();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Close last cursor.
		if (shaderAdapter != null) {
			shaderAdapter.changeCursor(null);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleSendText(intent);
	}

	private void closeDrawer() {
		if (drawerLayout != null) {
			drawerLayout.closeDrawer(menuFrame);
		}
	}

	private void openDrawer() {
		if (drawerLayout != null) {
			drawerLayout.openDrawer(menuFrame);
		}
	}

	private void initToolbar() {
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
	}

	private void initQualitySpinner() {
		setQualityValues();

		qualitySpinner = (Spinner) findViewById(R.id.quality);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this,
				R.array.quality_names,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		qualitySpinner.setAdapter(adapter);
		qualitySpinner.setOnItemSelectedListener(
				new Spinner.OnItemSelectedListener() {
					@Override
					public void onItemSelected(
							AdapterView<?> parent,
							View view,
							int position,
							long id) {
						float q = qualityValues[position];

						if (q == quality) {
							return;
						}

						quality = q;

						if (selectedShaderId > 0) {
							ShaderEditorApp.db.updateShaderQuality(
									selectedShaderId,
									quality);
						}

						if (shaderView.getVisibility() != View.VISIBLE) {
							return;
						}

						shaderView.getRenderer().setQuality(quality);
						shaderView.onPause();
						shaderView.onResume();
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});
	}

	private void setQualityValues() {
		if (qualityValues != null) {
			return;
		}

		String[] qualityStringValues = getResources().getStringArray(
				R.array.quality_values);
		int len = qualityStringValues.length;
		qualityValues = new float[len];

		for (int i = 0; i < len; ++i) {
			qualityValues[i] = Float.parseFloat(qualityStringValues[i]);
		}
	}

	private void initDrawer() {
		drawerLayout = (TouchThruDrawerLayout) findViewById(
				R.id.drawer_layout);

		menuFrame = findViewById(R.id.menu_frame);

		drawerToggle = new ActionBarDrawerToggle(
				this,
				drawerLayout,
				toolbar,
				R.string.drawer_open,
				R.string.drawer_close) {
			@Override
			public void onDrawerClosed(View view) {
				supportInvalidateOptionsMenu();
			}

			@Override
			public void onDrawerOpened(View view) {
				supportInvalidateOptionsMenu();
			}
		};

		drawerToggle.setDrawerIndicatorEnabled(true);
		drawerLayout.addDrawerListener(drawerToggle);
	}

	private void initListView() {
		listView = (ListView) findViewById(R.id.shaders);
		listView.setEmptyView(findViewById(R.id.no_shaders));
		listView.setOnItemClickListener((parent, view, position, id) -> {
			selectShaderAndUpdate(id);
			closeDrawer();
		});
		listView.setOnItemLongClickListener((parent, view, position, id) -> {
			if (shaderAdapter != null) {
				editShaderName(id, shaderAdapter.getName(position));
				return true;
			}
			return false;
		});
	}

	private void initShaderView() {
		shaderView = (ShaderView) findViewById(R.id.preview);
		shaderView.getRenderer().setOnRendererListener(
				new ShaderRenderer.OnRendererListener() {
					@Override
					public void onFramesPerSecond(int fps) {
						// Invoked from the GL thread.
						postUpdateFps(fps);
					}

					@Override
					public void onInfoLog(String infoLog) {
						// Invoked from the GL thread.
						postInfoLog(infoLog);
					}
				});
	}

	private void postUpdateFps(int fps) {
		if (fps < 1) {
			return;
		}

		this.fps = fps;
		toolbar.post(updateFpsRunnable);
	}

	private void postInfoLog(final String infoLog) {
		if (infoLog == null) {
			return;
		}

		runOnUiThread(() -> {
			if (editorFragment != null) {
				editorFragment.showError(infoLog);
			}
		});
	}

	private void updateUiToPreferences() {
		if (ShaderEditorApp.preferences.doesRunInBackground()) {
			shaderView.setVisibility(View.VISIBLE);
			shaderView.onResume();
		} else {
			shaderView.setVisibility(View.GONE);

			if (editorFragment != null &&
					!editorFragment.isCodeVisible()) {
				toggleCode();
			}
		}

		if (editorFragment != null) {
			editorFragment.updateHighlighting();
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			invalidateOptionsMenu();
		}
	}

	private void autoSave() {
		if (ShaderEditorApp.preferences.autoSave() &&
				selectedShaderId > 0) {
			saveShader(selectedShaderId);
		}
	}

	// This AsyncTask is running for a short and finite time only
	// and it's perfectly okay to delay garbage collection of the
	// parent instance until this task has ended.
	@SuppressLint("StaticFieldLeak")
	private void getShadersAsync() {
		if (!ShaderEditorApp.db.isOpen()) {
			listView.postDelayed(this::getShadersAsync, 500);
			return;
		}

		new AsyncTask<Void, Void, Cursor>() {
			@Override
			protected Cursor doInBackground(Void... nothings) {
				return ShaderEditorApp.db.getShaders();
			}

			@Override
			protected void onPostExecute(Cursor cursor) {
				updateShaderAdapter(cursor);
			}
		}.execute();
	}

	private void updateShaderAdapter(Cursor cursor) {
		handleSendText(getIntent());

		if (cursor == null || cursor.getCount() < 1) {
			if (cursor != null) {
				cursor.close();
			}
			showNoShadersAvailable();
			return;
		}

		if (shaderAdapter != null) {
			shaderAdapter.setSelectedId(selectedShaderId);
			shaderAdapter.changeCursor(cursor);
			return;
		}

		shaderAdapter = new ShaderAdapter(MainActivity.this, cursor);

		if (selectedShaderId < 0 &&
				shaderAdapter.getCount() > 0) {
			selectShader(shaderAdapter.getItemId(0));
		} else if (selectedShaderId > 0) {
			shaderAdapter.setSelectedId(selectedShaderId);
			setToolbarTitle(selectedShaderId);
		}

		listView.setAdapter(shaderAdapter);
	}

	private void showNoShadersAvailable() {
		View progressView = findViewById(R.id.progress_bar);
		progressView.setVisibility(View.GONE);

		View textView = findViewById(R.id.no_shaders_message);
		textView.setVisibility(View.VISIBLE);

		if (shaderAdapter != null) {
			shaderAdapter.changeCursor(null);
		}
	}

	private void handleSendText(Intent intent) {
		if (intent == null) {
			return;
		}

		String action = intent.getAction();
		if (!Intent.ACTION_SEND.equals(action) &&
				!Intent.ACTION_VIEW.equals(action)) {
			return;
		}

		String type = intent.getType();
		String text;
		if ("text/plain".equals(type)) {
			text = intent.getStringExtra(Intent.EXTRA_TEXT);
		} else if ("application/octet-stream".equals(type) ||
				"application/glsl".equals(type)) {
			text = getTextFromUri(getContentResolver(), intent.getData());
		} else {
			return;
		}

		if (text == null) {
			return;
		}

		// Don't use an old thumbnail.
		PreviewActivity.renderStatus.reset();

		// Consume this intent. This is necessary because
		// a orientation change will start a new activity
		// with the exact same intent.
		intent.setAction(null);

		int len = text.length();
		if (len < 1 || len > 65536) {
			Toast.makeText(
					this,
					R.string.unsuitable_text,
					Toast.LENGTH_SHORT).show();
			return;
		}

		selectedShaderId = NO_SHADER;
		editorFragment.setText(text);
		setQualitySpinner(.5f);
		setDefaultToolbarTitle();
	}

	private static String getTextFromUri(ContentResolver resolver, Uri uri) {
		try {
			InputStream in = resolver.openInputStream(uri);
			if (in == null) {
				return null;
			}
			StringBuilder sb = new StringBuilder();
			byte[] buffer = new byte[2048];
			for (int len; (len = in.read(buffer)) > 0; ) {
				// StandardCharsets.UTF_8 would require API level 19.
				sb.append(new String(buffer, 0, len, "UTF-8"));
			}
			in.close();
			return sb.toString();
		} catch (IOException e) {
			return null;
		}
	}

	private void insertTab() {
		editorFragment.insertTab();
	}

	private void runShader() {
		String src = editorFragment.getText();
		editorFragment.clearError();

		if (ShaderEditorApp.preferences.doesSaveOnRun()) {
			// Don't save the old thumbnail.
			// onActivityResult() will add an
			// updated one.
			PreviewActivity.renderStatus.reset();

			saveShader(selectedShaderId);
		}

		if (ShaderEditorApp.preferences.doesRunInBackground()) {
			setFragmentShader(src);
		} else {
			showPreview(src);
		}
	}

	private void saveShader(long id) {
		if (editorFragment == null) {
			return;
		}

		String fragmentShader = editorFragment.getText();
		byte[] thumbnail = ShaderEditorApp.preferences.doesRunInBackground()
				? shaderView.getRenderer().getThumbnail()
				: PreviewActivity.renderStatus.thumbnail;

		if (id > 0) {
			ShaderEditorApp.db.updateShader(
					id,
					fragmentShader,
					thumbnail,
					quality);
		} else {
			setToolbarTitle(ShaderEditorApp.db.insertShader(
					fragmentShader,
					thumbnail,
					quality));
		}

		// Update thumbnails.
		getShadersAsync();
	}

	private void toggleCode() {
		if (editorFragment != null) {
			boolean isVisible = editorFragment.toggleCode();
			drawerLayout.setTouchThru(isVisible);
			insertTabMenuItem.setEnabled(!isVisible);
		}
	}

	private void addShader() {
		long id = ShaderEditorApp.preferences.getDefaultNewShader();
		if (id > 0) {
			if (!ShaderEditorApp.db.isShaderAvailable(id)) {
				ShaderEditorApp.preferences.setDefaultNewShader(0);
			} else {
				duplicateShader(id);
				return;
			}
		}
		selectShaderAndUpdate(ShaderEditorApp.db.insertNewShader(this));
	}

	private void duplicateShader(long id) {
		Cursor cursor = ShaderEditorApp.db.getShader(id);
		if (Database.closeIfEmpty(cursor)) {
			return;
		}
		selectShaderAndUpdate(ShaderEditorApp.db.insertShader(
				Database.getString(cursor, Database.SHADERS_FRAGMENT_SHADER),
				ShaderEditorApp.db.getThumbnail(id),
				Database.getFloat(cursor, Database.SHADERS_QUALITY)));
		cursor.close();
	}

	private void duplicateSelectedShader() {
		if (editorFragment == null || selectedShaderId < 1) {
			return;
		}

		if (editorFragment.isModified()) {
			saveShader(selectedShaderId);
		}

		selectShaderAndUpdate(ShaderEditorApp.db.insertShader(
				editorFragment.getText(),
				ShaderEditorApp.db.getThumbnail(selectedShaderId),
				quality));

		// Update thumbnails.
		getShadersAsync();
	}

	private void deleteShader(final long id) {
		if (id < 1) {
			return;
		}
		new AlertDialog.Builder(this)
				.setMessage(R.string.sure_remove_shader)
				.setPositiveButton(
						android.R.string.ok,
						(dialog, which) -> {
							ShaderEditorApp.db.removeShader(id);
							selectShaderAndUpdate(ShaderEditorApp
									.db.getFirstShaderId());
						})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void shareShader() {
		if (editorFragment == null) {
			return;
		}

		String text = editorFragment.getText();
		if (!ShaderEditorApp.preferences.exportTabs() &&
				text.contains("\t")) {
			StringBuilder sb = new StringBuilder();
			for (int i = ShaderEditorApp.preferences.getTabWidth();
					i-- > 0; ) {
				sb.append(" ");
			}
			text = text.replaceAll("\t", sb.toString());
		}

		Intent intent = new Intent();
		intent.setType("text/plain");
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT, text);

		startActivity(Intent.createChooser(
				intent,
				getString(R.string.share_shader)));
	}

	private void updateWallpaper(long id) {
		if (id < 1) {
			return;
		}

		if (editorFragment != null && editorFragment.isModified()) {
			saveShader(id);
		}

		// The onSharedPreferenceChanged() listener in WallpaperService
		// is only triggered if the value has changed so force this.
		ShaderEditorApp.preferences.setWallpaperShader(0);
		ShaderEditorApp.preferences.setWallpaperShader(id);

		int message;
		if (ShaderWallpaperService.isRunning()) {
			message = R.string.wallpaper_set;
		} else if (startLiveWallpaperPicker()) {
			message = R.string.pick_live_wallpaper;
		} else {
			message = R.string.pick_live_wallpaper_manually;
		}
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private boolean startLiveWallpaperPicker() {
		Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
		intent.setClassName(
				"com.android.wallpaper.livepicker",
				"com.android.wallpaper.livepicker.LiveWallpaperActivity");
		return startActivity(this, intent);
	}

	private void addUniform() {
		startActivityForResult(
				new Intent(this, AddUniformActivity.class),
				ADD_UNIFORM);
	}

	private void loadSample() {
		startActivityForResult(
				new Intent(this, LoadSampleActivity.class),
				LOAD_SAMPLE);
	}

	private void showFaq() {
		Uri uri = Uri.parse("https://github.com/markusfisch/ShaderEditor/blob/master/FAQ.md");
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		if (!startActivity(this, intent)) {
			Toast.makeText(
					this,
					R.string.cannot_open_content,
					Toast.LENGTH_SHORT).show();
		}
	}

	private void showSettings() {
		startActivity(new Intent(this, PreferencesActivity.class));
	}

	@SuppressLint("InflateParams")
	private void editShaderName(final long id, String name) {
		View view = getLayoutInflater().inflate(
				R.layout.dialog_rename_shader,
				// A dialog does not have a parent view group
				// so InflateParams must be suppressed.
				null);
		final EditText nameView = view.findViewById(R.id.name);
		if (name != null) {
			nameView.setText(name);
		}
		new AlertDialog.Builder(this)
				.setMessage(R.string.rename_shader)
				.setView(view)
				.setPositiveButton(android.R.string.ok,
						(dialog, which) -> {
							String name1 = nameView.getText().toString();
							ShaderEditorApp.db.updateShaderName(id, name1);
							if (id == selectedShaderId) {
								setToolbarTitle(name1);
							}
							getShadersAsync();
							SoftKeyboard.hide(MainActivity.this, nameView);
						})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void selectShaderAndUpdate(long id) {
		autoSave();
		selectShader(id);
		getShadersAsync();
	}

	private void selectShader(long id) {
		// Remove thumbnail from previous shader.
		PreviewActivity.renderStatus.reset();

		if ((selectedShaderId = loadShader(id)) < 1) {
			if (editorFragment != null) {
				editorFragment.setText(null);
			}

			setFragmentShader(null);
			setDefaultToolbarTitle();
		}

		if (shaderAdapter != null) {
			shaderAdapter.setSelectedId(selectedShaderId);
		}
	}

	private long loadShader(long id) {
		Cursor cursor = ShaderEditorApp.db.getShader(id);

		if (Database.closeIfEmpty(cursor)) {
			return 0;
		}

		setQualitySpinner(cursor);
		loadShader(cursor);
		cursor.close();

		return id;
	}

	private void loadShader(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		setToolbarTitle(cursor);

		String fragmentShader = Database.getString(
				cursor, Database.SHADERS_FRAGMENT_SHADER);

		if (editorFragment != null) {
			// Runs setFragmentShader() in onTextChanged().
			editorFragment.setText(fragmentShader);
		} else if (ShaderEditorApp.preferences.doesRunInBackground()) {
			setFragmentShader(fragmentShader);
		}
	}

	private void setDefaultToolbarTitle() {
		setToolbarTitle(getString(R.string.app_name));
	}

	private void setToolbarTitle(long id) {
		Cursor cursor = ShaderEditorApp.db.getShader(id);
		if (Database.closeIfEmpty(cursor)) {
			return;
		}
		setQualitySpinner(cursor);
		setToolbarTitle(cursor);
		cursor.close();
	}

	private void setToolbarTitle(Cursor cursor) {
		if (cursor != null && shaderAdapter != null) {
			setToolbarTitle(shaderAdapter.getTitle(cursor));
		}
	}

	private void setToolbarTitle(String name) {
		toolbar.setTitle(name);
		toolbar.setSubtitle(null);
	}

	private void setQualitySpinner(Cursor cursor) {
		setQualitySpinner(Database.getFloat(cursor,
				Database.SHADERS_QUALITY));
	}

	private void setQualitySpinner(float q) {
		for (int i = 0, l = qualityValues.length; i < l; ++i) {
			if (qualityValues[i] == q) {
				qualitySpinner.setSelection(i);
				quality = q;
				return;
			}
		}
	}

	private void setFragmentShader(String src) {
		shaderView.setFragmentShader(src, quality);
	}

	@TargetApi(Build.VERSION_CODES.N)
	private void showPreview(String src) {
		toolbar.setSubtitle(null);

		Intent intent = new Intent(this, PreviewActivity.class);
		intent.putExtra(PreviewActivity.QUALITY, quality);
		intent.putExtra(PreviewActivity.FRAGMENT_SHADER, src);

		if (ShaderEditorApp.preferences.doesRunInNewTask() &&
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
					Intent.FLAG_ACTIVITY_NEW_TASK);

			startActivity(intent);
		} else {
			startActivityForResult(intent, PREVIEW_SHADER);
		}
	}

	private static boolean startActivity(Context context, Intent intent) {
		try {
			// Avoid using `intent.resolveActivity()` at API level 30+ due
			// to the new package visibility restrictions. In order for
			// `resolveActivity()` to "see" another package, we would need
			// to list that package/intent in a `<queries>` block in the
			// Manifest. But since we used `resolveActivity()` only to avoid
			// an exception if the Intent cannot be resolved, it's much easier
			// and more robust to just try and catch that exception if
			// necessary.
			context.startActivity(intent);
			return true;
		} catch (ActivityNotFoundException e) {
			return false;
		}
	}
}
