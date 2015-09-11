package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.adapter.ShaderAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.fragment.EditorFragment;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
	public static long selectedShaderId = 0;

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
	private volatile int fps;

	@Override
	public void onCreate( Bundle state )
	{
		super.onCreate( state );
		setContentView( R.layout.activity_main );

		initStatusBar();
		initToolbar();
		initDrawer();
		initListView();
		initShaderView();

		if( state == null ||
			(editorFragment = (EditorFragment)
				getSupportFragmentManager()
					.findFragmentByTag(
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
	public void onRestoreInstanceState( Bundle state )
	{
		super.onRestoreInstanceState( state );

		selectedShaderId = state != null ?
			state.getLong( SELECTED_SHADER ) :
			0;
	}

	@Override
	public void onSaveInstanceState( Bundle state )
	{
		if( state != null )
			state.putLong(
				SELECTED_SHADER,
				selectedShaderId );

		super.onSaveInstanceState( state );
	}

	@Override
	public void onPostCreate( Bundle state )
	{
		super.onPostCreate( state );

		drawerToggle.syncState();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		updateUiToPrefrences();
		queryShadersAsync();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		saveShader( selectedShaderId );
		shaderView.onPause();
	}

	@Override
	public void onDestroy()
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

		editorFragment.hideError();
		setFragmentShader( text );
	}

	@TargetApi( 22 )
	private void initStatusBar()
	{
		// below status bar settings are available
		// from Lollipop on only
		if( Build.VERSION.SDK_INT <
			Build.VERSION_CODES.LOLLIPOP )
			return;

		Window window = getWindow();

		window.getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
			View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
		window.setStatusBarColor( 0x88000000 );

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
		LayoutInflater inflater = getLayoutInflater();

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
					editorFragment.setErrorMessage( infoLog );
				}
			} );
	}

	private void updateUiToPrefrences()
	{
		if( ShaderEditorApplication
				.preferences
				.doesRunInBackground() )
			shaderView.onResume();
		else
		{
			if( !editorFragment.isCodeVisible() )
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

		if( selectedShaderId < 1 &&
			shaderAdapter.getCount() > 0 )
			selectedShaderId = shaderAdapter.getItemId( 0 );

		selectShader( selectedShaderId );

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
		String code = editorFragment.getText();

		editorFragment.hideError();

		if( ShaderEditorApplication
				.preferences
				.doesRunInBackground() )
			setFragmentShader( code );
		else
			showPreview( code );
	}

	private void saveShader( long id )
	{
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
				.update(
					id,
					fragmentShader,
					thumbnail );
		else
			ShaderEditorApplication
				.dataSource
				.insert(
					fragmentShader,
					thumbnail );

		// update thumbnails
		queryShadersAsync();
	}

	private void toggleCode()
	{
		drawerLayout.setTouchThru(
			editorFragment.toggleCode() );
	}

	private void addShader()
	{
		selectShader(
			ShaderEditorApplication
				.dataSource
				.insert() );
	}

	private void duplicateShader( long id )
	{
		if( id < 1 )
			return;

		if( editorFragment.isModified() )
			saveShader( id );

		selectShader(
			ShaderEditorApplication
				.dataSource
				.insert(
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

		DialogInterface.OnClickListener listener =
			new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(
					DialogInterface dialog,
					int which )
				{
					if( which != DialogInterface.BUTTON_POSITIVE )
						return;

					ShaderEditorApplication
						.dataSource
						.remove( id );

					selectShader(
						ShaderEditorApplication
							.dataSource
							.getFirstShaderId() );
				}
			};

		new AlertDialog.Builder( this )
			.setMessage( R.string.are_you_sure )
			.setPositiveButton( android.R.string.yes, listener )
			.setNegativeButton( android.R.string.no, listener )
			.show();
	}

	private void shareShader()
	{
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
		if( id < 1 )
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

	private void selectShader( long id )
	{
		if( (selectedShaderId = loadShader( id )) < 1 )
		{
			editorFragment.setText( null );
			setFragmentShader( null );
			setTitle( R.string.app_name );
			toolbar.setSubtitle( null );
		}

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
			cursor.getColumnIndex( DataSource.SHADERS_SHADER ) );
		String modified = cursor.getString( cursor.getColumnIndex(
			DataSource.SHADERS_MODIFIED ) );

		toolbar.setTitle( modified );
		editorFragment.setText( fragmentShader );

		if( ShaderEditorApplication
				.preferences
				.doesRunInBackground() )
			setFragmentShader( fragmentShader );
	}

	private void setFragmentShader( String code )
	{
		shaderView.setFragmentShader( code );
	}

	private void showPreview( String code )
	{
		Intent intent = new Intent(
			this,
			PreviewActivity.class );

		intent.putExtra(
			PreviewActivity.FRAGMENT_SHADER,
			code );

		startActivity( intent );
	}
}
