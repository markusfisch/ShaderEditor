package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.adapter.ShaderAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.fragment.EditorFragment;
import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;
import de.markusfisch.android.shadereditor.view.SoftKeyboard;
import de.markusfisch.android.shadereditor.view.SystemBarMetrics;
import de.markusfisch.android.shadereditor.widget.TouchThruDrawerLayout;
import de.markusfisch.android.shadereditor.widget.ShaderEditor;
import de.markusfisch.android.shadereditor.widget.ShaderView;
import de.markusfisch.android.shadereditor.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity
		extends AppCompatActivity
		implements ShaderEditor.OnTextChangedListener {
	private static final String SELECTED_SHADER = "selected_shader";
	private static final int PREVIEW_SHADER = 1;
	private static final int ADD_UNIFORM = 2;
	private static final int FIRST_SHADER = -1;
	private static final int NO_SHADER = 0;

	private final Runnable updateFpsRunnable = new Runnable() {
		@Override
		public void run() {
			// "fps" should be the same in all languages
			toolbar.setSubtitle(fps + " fps");
		}
	};

	private EditorFragment editorFragment;
	private Toolbar toolbar;
	private Spinner qualitySpinner;
	private TouchThruDrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private View menuFrame;
	private ListView listView;
	private ShaderAdapter shaderAdapter;
	private ShaderView shaderView;
	private long selectedShaderId = FIRST_SHADER;
	private volatile int fps;
	private float qualityValues[];
	private float quality = 1f;

	public static void initSystemBars(AppCompatActivity activity) {
		View view;

		if (activity != null &&
				(view = activity.findViewById(R.id.main_layout)) != null &&
				setSystemBarColor(
						activity.getWindow(),
						ShaderEditorApplication
								.preferences
								.getSystemBarColor(),
						true)) {
			view.setPadding(
					0,
					SystemBarMetrics.getStatusBarHeight(
							activity.getResources()),
					0,
					0);
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static boolean setSystemBarColor(
			Window window,
			int color,
			boolean expand) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return false;
		}

		if (expand) {
			window.getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
							View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
							View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}

		window.setStatusBarColor(color);
		window.setNavigationBarColor(color);

		return true;
	}

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
		menu.findItem(R.id.insert_tab).setVisible(
				ShaderEditorApplication
						.preferences
						.doesShowInsertTab());

		menu.findItem(R.id.run_code).setVisible(
				!ShaderEditorApplication
						.preferences
						.doesRunOnChange());

		menu.findItem(R.id.toggle_code).setVisible(
				ShaderEditorApplication
						.preferences
						.doesRunInBackground());

		menu.findItem(R.id.update_wallpaper).setTitle(
				ShaderEditorApplication
						.preferences
						.getWallpaperShader() == selectedShaderId ?
								R.string.update_wallpaper :
								R.string.set_as_wallpaper);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.insert_tab:
				insertTab();
				return true;
			case R.id.run_code:
				runShader();
				return true;
			case R.id.save_shader:
				saveShader(selectedShaderId);
				return true;
			case R.id.toggle_code:
				toggleCode();
				return true;
			case R.id.add_shader:
				addShader();
				return true;
			case R.id.duplicate_shader:
				duplicateShader(selectedShaderId);
				return true;
			case R.id.delete_shader:
				deleteShader(selectedShaderId);
				return true;
			case R.id.share_shader:
				shareShader();
				return true;
			case R.id.update_wallpaper:
				updateWallpaper(selectedShaderId);
				return true;
			case R.id.add_uniform:
				addUniform();
				return true;
			case R.id.settings:
				showSettings();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onTextChanged(String text) {
		if (!ShaderEditorApplication.preferences.doesRunOnChange()) {
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

		// add uniform statement
		if (editorFragment != null &&
				requestCode == ADD_UNIFORM &&
				resultCode == RESULT_OK &&
				data != null) {
			editorFragment.addUniform(data.getStringExtra(
					AddUniformActivity.STATEMENT));
		}

		// update fps, info log and thumbnail after shader ran
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
					ShaderEditorApplication.preferences.doesSaveOnRun()) {
				saveShader(selectedShaderId);
			}
		}
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_main);

		initSystemBars(this);
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
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);

		selectedShaderId = state != null ?
				state.getLong(SELECTED_SHADER) :
				FIRST_SHADER;
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		if (state != null) {
			state.putLong(SELECTED_SHADER, selectedShaderId);
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
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// close last cursor
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
		if (drawerLayout == null) {
			return;
		}

		drawerLayout.closeDrawer(menuFrame);
	}

	private void openDrawer() {
		if (drawerLayout == null) {
			return;
		}

		drawerLayout.openDrawer(menuFrame);
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
					ShaderEditorApplication.dataSource.updateShaderQuality(
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

		String qualityStringValues[] = getResources().getStringArray(
				R.array.quality_values);
		int len = qualityStringValues.length;
		qualityValues = new float[len];

		for (int i = 0; i < len; ++i) {
			qualityValues[i] = Float.valueOf(qualityStringValues[i]);
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
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(
					AdapterView<?> parent,
					View view,
					int position,
					long id) {
				selectShaderAndUpdate(id);
				closeDrawer();
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(
					AdapterView<?> parent,
					View view,
					int position,
					long id) {
				if (shaderAdapter != null) {
					editShaderName(id, shaderAdapter.getName(position));
					return true;
				}
				return false;
			}
		});
	}

	private void initShaderView() {
		shaderView = (ShaderView) findViewById(R.id.preview);
		shaderView.getRenderer().setOnRendererListener(
				new ShaderRenderer.OnRendererListener() {
			@Override
			public void onFramesPerSecond(int fps) {
				// invoked from the GL thread
				postUpdateFps(fps);
			}

			@Override
			public void onInfoLog(String infoLog) {
				// invoked from the GL thread
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

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (editorFragment != null) {
					editorFragment.showError(infoLog);
				}
			}
		});
	}

	private void updateUiToPreferences() {
		if (ShaderEditorApplication
				.preferences
				.doesRunInBackground()) {
			shaderView.setVisibility(View.VISIBLE);
			shaderView.onResume();
		} else {
			shaderView.setVisibility(View.GONE);

			if (editorFragment != null &&
					!editorFragment.isCodeVisible()) {
				toggleCode();
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			invalidateOptionsMenu();
		}
	}

	private void getShadersAsync() {
		if (!ShaderEditorApplication.dataSource.isOpen()) {
			listView.postDelayed(new Runnable() {
				@Override
				public void run() {
					getShadersAsync();
				}
			}, 500);

			return;
		}

		new AsyncTask<Void, Void, Cursor>() {
			@Override
			protected Cursor doInBackground(Void... nothings) {
				return ShaderEditorApplication.dataSource.getShaders();
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
		String type = intent.getType();
		String text;

		if (!Intent.ACTION_SEND.equals(action) ||
				type == null ||
				!"text/plain".equals(type) ||
				(text = intent.getStringExtra(Intent.EXTRA_TEXT)) == null) {
			return;
		}

		// don't use an old thumbnail
		PreviewActivity.renderStatus.reset();

		// consume this intent; this is necessary because
		// a orientation change will start a new activity
		// with the exact same intent
		intent.setAction(null);

		int len = text.length();
		if (len < 1 || len > 65536) {
			Toast.makeText(
					this,
					R.string.unsuitable_text,
					Toast.LENGTH_SHORT).show();

			return;
		}

		if (ShaderEditorApplication
				.preferences
				.doesRunInBackground()) {
			setFragmentShader(text.replaceAll("\\p{C}", ""));
		}

		selectedShaderId = NO_SHADER;
		editorFragment.setText(text);
		setDefaultToolbarTitle();
	}

	private void insertTab() {
		editorFragment.insertTab();
	}

	private void runShader() {
		String src = editorFragment.getText();

		editorFragment.clearError();

		if (ShaderEditorApplication
				.preferences
				.doesSaveOnRun()) {
			// don't save the old thumbnail;
			// onActivityResult() will add an
			// updated one
			PreviewActivity.renderStatus.reset();

			saveShader(selectedShaderId);
		}

		if (ShaderEditorApplication
				.preferences
				.doesRunInBackground()) {
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
		byte thumbnail[] = ShaderEditorApplication
				.preferences
				.doesRunInBackground() ?
						shaderView.getRenderer().getThumbnail() :
						PreviewActivity.renderStatus.thumbnail;

		if (id > 0) {
			ShaderEditorApplication.dataSource.updateShader(
					id,
					fragmentShader,
					thumbnail,
					quality);
		} else {
			setToolbarTitle(ShaderEditorApplication.dataSource.insertShader(
					fragmentShader,
					thumbnail,
					quality));
		}

		// update thumbnails
		getShadersAsync();
	}

	private void toggleCode() {
		if (editorFragment == null) {
			return;
		}

		drawerLayout.setTouchThru(editorFragment.toggleCode());
	}

	private void addShader() {
		selectShaderAndUpdate(ShaderEditorApplication
				.dataSource
				.insertNewShader(this));
	}

	private void duplicateShader(long id) {
		if (editorFragment == null || id < 1) {
			return;
		}

		if (editorFragment.isModified()) {
			saveShader(id);
		}

		selectShaderAndUpdate(ShaderEditorApplication.dataSource.insertShader(
				editorFragment.getText(),
				ShaderEditorApplication.dataSource.getThumbnail(id),
				quality));

		// update thumbnails
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
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(
									DialogInterface dialog,
									int which) {
								ShaderEditorApplication
										.dataSource
										.removeShader(id);

								selectShaderAndUpdate(ShaderEditorApplication
										.dataSource
										.getFirstShaderId());
							}
						})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void shareShader() {
		if (editorFragment == null) {
			return;
		}

		Intent intent = new Intent();
		intent.setType("text/plain");
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT, editorFragment.getText());

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

		// the onSharedPreferenceChanged() listener in WallpaperService
		// is only triggered if the value has changed so force this
		ShaderEditorApplication.preferences.setWallpaperShader(0);
		ShaderEditorApplication.preferences.setWallpaperShader(id);
	}

	private void addUniform() {
		startActivityForResult(
				new Intent(this, AddUniformActivity.class),
				ADD_UNIFORM);
	}

	private void showSettings() {
		startActivity(new Intent(
				MainActivity.this,
				PreferencesActivity.class));
	}

	@SuppressLint("InflateParams")
	private void editShaderName(final long id, String name) {
		View view = getLayoutInflater().inflate(
				R.layout.dialog_rename_shader,
				// a dialog does not have a parent view group
				// so InflateParams must be suppressed
				null);
		final EditText nameView = (EditText) view.findViewById(R.id.name);
		if (name != null) {
			nameView.setText(name);
		}

		new AlertDialog.Builder(this)
				.setMessage(R.string.rename_shader)
				.setView(view)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(
									DialogInterface dialog,
									int which) {
								ShaderEditorApplication
										.dataSource
										.updateShaderName(
												id,
												nameView.getText().toString());
								getShadersAsync();
								SoftKeyboard.hide(
										MainActivity.this,
										nameView);
							}
						})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void selectShaderAndUpdate(long id) {
		selectShader(id);
		getShadersAsync();
	}

	private void selectShader(long id) {
		// remove thumbnail from previous shader
		PreviewActivity.renderStatus.reset();

		if ((selectedShaderId = loadShader(id)) < 1) {
			if (editorFragment != null) {
				editorFragment.setText(null);
			}

			setFragmentShader(null);
			setDefaultToolbarTitle();
		}

		shaderAdapter.setSelectedId(selectedShaderId);
	}

	private long loadShader(long id) {
		Cursor cursor = ShaderEditorApplication.dataSource.getShader(id);

		if (DataSource.closeIfEmpty(cursor)) {
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

		String fragmentShader = cursor.getString(
				cursor.getColumnIndex(DataSource.SHADERS_FRAGMENT_SHADER));

		if (editorFragment != null) {
			// runs setFragmentShader() in onTextChanged()
			editorFragment.setText(fragmentShader);
		} else if (ShaderEditorApplication
				.preferences
				.doesRunInBackground()) {
			setFragmentShader(fragmentShader);
		}
	}

	private void setDefaultToolbarTitle() {
		setToolbarTitle(getString(R.string.app_name));
	}

	private void setToolbarTitle(long id) {
		Cursor cursor = ShaderEditorApplication.dataSource.getShader(id);

		if (DataSource.closeIfEmpty(cursor)) {
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
		float q = cursor.getFloat(cursor.getColumnIndex(
				DataSource.SHADERS_QUALITY));

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

		if (ShaderEditorApplication.preferences.doesRunInNewTask() &&
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
					Intent.FLAG_ACTIVITY_NEW_TASK);

			startActivity(intent);
		} else {
			startActivityForResult(intent, PREVIEW_SHADER);
		}
	}
}
