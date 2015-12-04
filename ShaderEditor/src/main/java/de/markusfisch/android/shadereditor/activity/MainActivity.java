package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.adapter.ShaderAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.fragment.EditorFragment;
import de.markusfisch.android.shadereditor.fragment.CropImageFragment;
import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;
import de.markusfisch.android.shadereditor.widget.TouchThruDrawerlayout;
import de.markusfisch.android.shadereditor.widget.ShaderEditor;
import de.markusfisch.android.shadereditor.widget.ShaderView;
import de.markusfisch.android.shadereditor.R;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
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
import android.widget.ListView;

public class MainActivity
	extends AppCompatActivity
	implements ShaderEditor.OnTextChangedListener
{
	private static final String SELECTED_SHADER = "selected_shader";

	private static EditorFragment editorFragment;

	private final Runnable updateFpsRunnable =
		new Runnable()
		{
			@Override
			public void run()
			{
				toolbar.setSubtitle(
					// fps should be the same in
					// all languages
					fps+" fps" );
			}
		};

	private Toolbar toolbar;
	private TouchThruDrawerlayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private View menuFrame;
	private ListView listView;
	private ShaderAdapter shaderAdapter;
	private ShaderView shaderView;
	private long selectedShaderId = 0;
	private volatile int fps;

	@Override
	protected void onCreate( Bundle state )
	{
		super.onCreate( state );
		setContentView( R.layout.activity_main );

		initSystemBars();
		initToolbar();
		initDrawer();
		initListView();
		initShaderView();

		if( state == null ||
			(editorFragment = (EditorFragment)
				getSupportFragmentManager().findFragmentByTag(
					EditorFragment.TAG )) == null )
		{
			editorFragment = new EditorFragment();

			getSupportFragmentManager()
				.beginTransaction()
				.replace(
					R.id.content_frame,
					editorFragment,
					EditorFragment.TAG )
				.commit();
		}
	}

	@Override
	protected void onRestoreInstanceState( Bundle state )
	{
		super.onRestoreInstanceState( state );

		selectedShaderId = state != null ?
			state.getLong( SELECTED_SHADER ) :
			0;
	}

	@Override
	protected void onSaveInstanceState( Bundle state )
	{
		if( state != null )
			state.putLong(
				SELECTED_SHADER,
				selectedShaderId );

		super.onSaveInstanceState( state );
	}

	@Override
	protected void onPostCreate( Bundle state )
	{
		super.onPostCreate( state );

		drawerToggle.syncState();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		updateUiToPreferences();
		queryShadersAsync();
		CropImageFragment.recycle();
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		shaderView.onPause();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		// close last cursor
		if( shaderAdapter != null )
			shaderAdapter.changeCursor( null );
	}

	@Override
	public void onConfigurationChanged( Configuration newConfig )
	{
		super.onConfigurationChanged( newConfig );

		drawerToggle.onConfigurationChanged( newConfig );
	}

	@Override
	public boolean onKeyDown( int keyCode, KeyEvent e )
	{
		if( keyCode == KeyEvent.KEYCODE_MENU )
		{
			if( drawerLayout.isDrawerOpen( menuFrame ) )
				closeDrawer();
			else
				openDrawer();

			return true;
		}

		return super.onKeyDown( keyCode, e );
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.main, menu );

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu( Menu menu )
	{
		menu.findItem( R.id.run_code ).setVisible(
			ShaderEditorApplication
				.preferences
				.doesRunOnChange() ^ true );

		menu.findItem( R.id.toggle_code ).setVisible(
			ShaderEditorApplication
				.preferences
				.doesRunInBackground() );

		return true;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case R.id.run_code:
				runShader();
				return true;
			case R.id.save_shader:
				saveShader( selectedShaderId );
				return true;
			case R.id.toggle_code:
				toggleCode();
				return true;
			case R.id.add_shader:
				addShader();
				return true;
			case R.id.duplicate_shader:
				duplicateShader( selectedShaderId );
				return true;
			case R.id.delete_shader:
				deleteShader( selectedShaderId );
				return true;
			case R.id.share_shader:
				shareShader();
				return true;
			case R.id.update_wallpaper:
				updateWallpaper( selectedShaderId );
				return true;
			case R.id.textures:
				showTextures();
				return true;
		}

		return super.onOptionsItemSelected( item );
	}

	@Override
	public void onTextChanged( String text )
	{
		if( !ShaderEditorApplication
				.preferences
				.doesRunOnChange() )
			return;

		if( editorFragment != null )
			editorFragment.hideError();

		setFragmentShader( text );
	}

	@TargetApi( 22 )
	public static void setSystemBarColor( Window window, int color )
	{
		window.getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
			View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
			View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
		window.setStatusBarColor( color );
		window.setNavigationBarColor( color );
	}

	@TargetApi( 22 )
	private void initSystemBars()
	{
		// below system bar settings are available
		// from Lollipop on only
		if( Build.VERSION.SDK_INT <
			Build.VERSION_CODES.LOLLIPOP )
			return;

		setSystemBarColor(
			getWindow(),
			ContextCompat.getColor(
				this,
				R.color.primary_dark_translucent ) );

		findViewById( R.id.main_layout ).setPadding(
			0,
			getStatusBarHeight( getResources() ),
			0,
			0 );
	}

	private static int getStatusBarHeight( Resources res )
	{
		int id = res.getIdentifier(
			"status_bar_height",
			"dimen",
			"android" );

		return id > 0 ?
			res.getDimensionPixelSize( id ) :
			0;
	}

	private void initToolbar()
	{
		toolbar = (Toolbar)findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );
	}

	private void initDrawer()
	{
		drawerLayout = (TouchThruDrawerlayout)findViewById(
			R.id.drawer_layout );

		menuFrame = findViewById( R.id.menu_frame );

		drawerToggle =
			new ActionBarDrawerToggle(
				this,
				drawerLayout,
				toolbar,
				R.string.drawer_open,
				R.string.drawer_close )
			{
				public void onDrawerClosed( View view )
				{
					supportInvalidateOptionsMenu();
				}

				public void onDrawerOpened( View view )
				{
					supportInvalidateOptionsMenu();
				}
			};

		drawerToggle.setDrawerIndicatorEnabled( true );
		drawerLayout.setDrawerListener( drawerToggle );

		initSettingsButton();
	}

	private void initSettingsButton()
	{
		View view = findViewById( R.id.settings );

		if( view == null )
			return;

		view.setOnClickListener(
			new View.OnClickListener()
			{
				@Override
				public void onClick( View v )
				{
					startActivity( new Intent(
						MainActivity.this,
						PreferencesActivity.class ) );

					closeDrawer();
				}
			} );
	}

	private void initListView()
	{
		listView = (ListView)findViewById( R.id.shaders );
		listView.setEmptyView( findViewById( R.id.no_shaders ) );
		listView.setOnItemClickListener(
			new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(
					AdapterView<?> parent,
					View view,
					int position,
					long id )
				{
					selectShader( id );
					closeDrawer();
				}
			} );
	}

	private void initShaderView()
	{
		shaderView = (ShaderView)findViewById( R.id.preview );

		shaderView.getRenderer().setOnRendererListener(
			new ShaderRenderer.OnRendererListener()
			{
				@Override
				public void onFramesPerSecond( int fps )
				{
					// this is running in the GL thread
					postUpdateFps( fps );
				}

				@Override
				public void onInfoLog( String infoLog )
				{
					// this is running in the GL thread
					postInfoLog( infoLog );
				}
			} );
	}

	private void postUpdateFps( int fps )
	{
		this.fps = fps;
		toolbar.post( updateFpsRunnable );
	}

	private void postInfoLog( final String infoLog )
	{
		shaderView.post(
			new Runnable()
			{
				@Override
				public void run()
				{
					if( editorFragment != null )
						editorFragment.showError( infoLog );
				}
			} );
	}

	private void updateUiToPreferences()
	{
		if( ShaderEditorApplication
				.preferences
				.doesRunInBackground() )
		{
			shaderView.onResume();
		}
		else
		{
			if( editorFragment != null &&
				!editorFragment.isCodeVisible() )
				toggleCode();

			toolbar.setSubtitle( null );
		}
	}

	private void queryShadersAsync()
	{
		if( !ShaderEditorApplication.dataSource.isOpen() )
		{
			listView.postDelayed(
				new Runnable()
				{
					@Override
					public void run()
					{
						queryShadersAsync();
					}
				},
				500 );

			return;
		}

		new AsyncTask<Void, Void, Cursor>()
		{
			@Override
			protected Cursor doInBackground( Void... nothings )
			{
				return ShaderEditorApplication
					.dataSource
					.queryShaders();
			}

			@Override
			protected void onPostExecute( Cursor cursor )
			{
				updateShaderAdapter( cursor );
			}
		}.execute();
	}

	private void updateShaderAdapter( Cursor cursor )
	{
		if( cursor == null ||
			cursor.getCount() < 1 )
		{
			if( cursor != null )
				cursor.close();

			showNoShadersAvailable();
			return;
		}

		if( shaderAdapter != null )
		{
			shaderAdapter.changeCursor( cursor );
			return;
		}

		shaderAdapter = new ShaderAdapter(
			this,
			cursor );

		if( shaderAdapter.getCount() > 0 )
		{
			if( selectedShaderId < 1 )
			{
				selectedShaderId = shaderAdapter.getItemId( 0 );
				selectShader( selectedShaderId );
			}
			else
				shaderAdapter.setSelectedId( selectedShaderId );
		}

		listView.setAdapter( shaderAdapter );
	}

	private void showNoShadersAvailable()
	{
		View progressView;
		View textView;

		if( (progressView = findViewById(
				R.id.progress_bar )) == null ||
			(textView = findViewById(
				R.id.no_shaders_message )) == null )
			return;

		progressView.setVisibility( View.GONE );
		textView.setVisibility( View.VISIBLE );

		if( shaderAdapter != null )
			shaderAdapter.changeCursor( null );
	}

	private void closeDrawer()
	{
		if( drawerLayout == null )
			return;

		drawerLayout.closeDrawer( menuFrame );
	}

	private void openDrawer()
	{
		if( drawerLayout == null )
			return;

		drawerLayout.openDrawer( menuFrame );
	}

	private void runShader()
	{
		String src = editorFragment.getText();

		editorFragment.hideError();

		if( ShaderEditorApplication
				.preferences
				.doesRunInBackground() )
			setFragmentShader( src );
		else
			showPreview( src );
	}

	private void saveShader( long id )
	{
		if( editorFragment == null )
			return;

		String fragmentShader = editorFragment.getText();
		byte thumbnail[] =
			ShaderEditorApplication
				.preferences
				.doesRunInBackground() ?
					shaderView.getRenderer().getThumbnail() :
					PreviewActivity.thumbnail;

		if( id > 0 )
			ShaderEditorApplication
				.dataSource
				.updateShader(
					id,
					fragmentShader,
					thumbnail );
		else
			ShaderEditorApplication
				.dataSource
				.insertShader(
					fragmentShader,
					thumbnail );

		// update thumbnails
		queryShadersAsync();
	}

	private void toggleCode()
	{
		if( editorFragment == null )
			return;

		drawerLayout.setTouchThru(
			editorFragment.toggleCode() );
	}

	private void addShader()
	{
		selectShader(
			ShaderEditorApplication
				.dataSource
				.insertShader() );
	}

	private void duplicateShader( long id )
	{
		if( editorFragment == null ||
			id < 1 )
			return;

		if( editorFragment.isModified() )
			saveShader( id );

		selectShader(
			ShaderEditorApplication
				.dataSource
				.insertShader(
					editorFragment.getText(),
					ShaderEditorApplication
						.dataSource
						.getThumbnail( id ) ) );

		// update thumbnails
		queryShadersAsync();
	}

	private void deleteShader( final long id )
	{
		if( id < 1 )
			return;

		new AlertDialog.Builder( this )
			.setMessage( R.string.are_you_sure )
			.setPositiveButton(
				android.R.string.yes,
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(
						DialogInterface dialog,
						int which )
					{
						ShaderEditorApplication
							.dataSource
							.removeShader( id );

						selectShader(
							ShaderEditorApplication
								.dataSource
								.getFirstShaderId() );
					}
				} )
			.setNegativeButton(
				android.R.string.no,
				null )
			.show();
	}

	private void shareShader()
	{
		if( editorFragment == null )
			return;

		Intent intent = new Intent();

		intent.setAction( Intent.ACTION_SEND );
		intent.putExtra(
			Intent.EXTRA_TEXT,
			editorFragment.getText() );
		intent.setType( "text/plain" );

		startActivity( Intent.createChooser(
			intent,
			getString( R.string.share_shader ) ) );
	}

	private void updateWallpaper( long id )
	{
		if( editorFragment == null ||
			id < 1 )
			return;

		if( editorFragment.isModified() )
			saveShader( id );

		// the onSharedPreferenceChanged() listener
		// in WallpaperService is only triggered if
		// the value would change
		ShaderEditorApplication
			.preferences
			.setWallpaperShader( 0 );

		ShaderEditorApplication
			.preferences
			.setWallpaperShader( id );
	}

	private void showTextures()
	{
		startActivity( new Intent(
			this,
			TexturesActivity.class ) );
	}

	private void selectShader( long id )
	{
		if( (selectedShaderId = loadShader( id )) < 1 )
		{
			if( editorFragment != null )
				editorFragment.setText( null );

			setFragmentShader( null );
			setTitle( R.string.app_name );
			toolbar.setSubtitle( null );
		}

		shaderAdapter.setSelectedId( id );
		queryShadersAsync();
	}

	private long loadShader( long id )
	{
		if( id < 1 )
			return 0;

		Cursor cursor = ShaderEditorApplication
			.dataSource
			.getShader( id );

		if( DataSource.closeIfEmpty( cursor ) )
			return 0;

		loadShader( cursor );
		cursor.close();

		return id;
	}

	private void loadShader( Cursor cursor )
	{
		if( cursor == null )
			return;

		String fragmentShader = cursor.getString(
			cursor.getColumnIndex( DataSource.SHADERS_FRAGMENT_SHADER ) );
		String modified = cursor.getString( cursor.getColumnIndex(
			DataSource.SHADERS_MODIFIED ) );

		toolbar.setTitle( modified );

		if( editorFragment != null )
			editorFragment.setText( fragmentShader );

		if( ShaderEditorApplication
				.preferences
				.doesRunInBackground() )
			setFragmentShader( fragmentShader );
	}

	private void setFragmentShader( String src )
	{
		shaderView.setFragmentShader( src );
	}

	private void showPreview( String src )
	{
		Intent intent = new Intent(
			this,
			PreviewActivity.class );

		intent.putExtra(
			PreviewActivity.FRAGMENT_SHADER,
			src );

		startActivity( intent );
	}
}
