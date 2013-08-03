package de.markusfisch.android.shadereditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity
	extends Activity
	implements
		ShaderRenderer.ErrorListener,
		ShaderRenderer.FpsListener
{
	private static final String STATE_SOURCE = "source";
	private static final String STATE_SHADER = "shader";

	private ShaderDataSource dataSource;
	private ShaderAdapter adapter;
	private ShaderView shaderView;
	private Spinner shaderSpinner;
	private ImageButton saveButton;
	private ImageButton sourceButton;
	private LockableScrollView scrollView;
	private ShaderEditor shaderEditor;
	private PopupWindow errorPopup;
	private TextView errorMessage;
	private boolean errorPopupVisible = false;
	private boolean compileOnChange = true;
	private boolean showSource = true;
	private boolean zoomPinch = false;
	private float minimumTextSize;
	private float maximumTextSize;
	private float textSize;
	private float zoomPinchFactor;

	@Override
	public void onCreate( Bundle state )
	{
		super.onCreate( state );

		this.requestWindowFeature( Window.FEATURE_NO_TITLE );
		setContentView( R.layout.main_activity );

		shaderView = (ShaderView)findViewById( R.id.preview );
		shaderSpinner = (Spinner)findViewById( R.id.shader );
		saveButton = (ImageButton)findViewById( R.id.save );
		sourceButton = (ImageButton)findViewById( R.id.source );
		scrollView = (LockableScrollView)findViewById( R.id.scroll );
		shaderEditor = (ShaderEditor)findViewById( R.id.editor );

		saveButton.setOnLongClickListener(
			new View.OnLongClickListener()
			{
				@Override
				public boolean onLongClick( View v )
				{
					compileOnChange ^= true;
					setSaveButton();
					savePreferences();

					return true;
				}
			} );

		shaderEditor.onTextChangedListener =
			new ShaderEditor.OnTextChangedListener()
			{
				@Override
				public void onTextChanged( String text )
				{
					if( compileOnChange )
						loadShader( text );
				}
			};

		shaderView.renderer.errorListener = this;
		shaderView.renderer.fpsListener = this;

		// create error message popup
		{
			View v = ((LayoutInflater)getSystemService(
				LAYOUT_INFLATER_SERVICE )).inflate(
					R.layout.error_popup,
					null );

			errorMessage = (TextView)v.findViewById( R.id.message );
			errorPopup = new PopupWindow(
				v,
				LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT );
		}

		float density = getResources().getDisplayMetrics().scaledDensity;
		textSize = shaderEditor.getTextSize()/density;
		minimumTextSize = 9f;
		maximumTextSize = 22f;

		scrollView.setOnTouchListener(
			new View.OnTouchListener()
			{
				@Override
				public boolean onTouch( View v, MotionEvent ev )
				{
					return pinchZoom( ev );
				}
			} );

		dataSource = new ShaderDataSource( this );
		dataSource.open();

		adapter = new ShaderAdapter(
			this,
			dataSource.queryAll() );

		shaderSpinner.setAdapter( adapter );
		shaderSpinner.setOnItemSelectedListener(
			new Spinner.OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(
					AdapterView<?> parent,
					View view,
					int pos,
					long id )
				{
					Cursor c = (Cursor)parent.getItemAtPosition( pos );

					resetFps();

					if( c != null )
					{
						String src = c.getString( c.getColumnIndex(
							ShaderDataSource.COLUMN_SHADER ) );

						shaderEditor.setTextHighlighted( src );

						if( !compileOnChange )
							loadShader( src );
					}
				}

				@Override
				public void onNothingSelected(
					AdapterView<?> parent )
				{
					resetFps();
				}
			} );

		registerForContextMenu( shaderSpinner );

		// show/hide source
		if( state != null &&
			!state.getBoolean( STATE_SOURCE, showSource ) )
			onToggleSource( null );

		// set selection
		{
			int p = 0;

			if( state != null )
				p = state.getInt( STATE_SHADER );

			if( shaderSpinner.getCount() <= p )
				p = 0;

			shaderSpinner.setSelection( p );
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		shaderView.renderer.errorListener = null;
		shaderView.renderer.fpsListener = null;
	}

	@Override
	public void onSaveInstanceState( Bundle state )
	{
		if( state != null )
		{
			state.putBoolean( STATE_SOURCE, showSource );
			state.putInt(
				STATE_SHADER,
				shaderSpinner.getSelectedItemPosition() );
		}

		super.onSaveInstanceState( state );
	}

	@Override
	public void onResume()
	{
		super.onResume();

		shaderView.onResume();
		dataSource.open();

		loadPreferences();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		if( shaderEditor.dirty )
		{
			saveShader();
			shaderEditor.dirty = false;
		}

		shaderView.onPause();
		dataSource.close();
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		inflateShaderMenu( menu );

		return true;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		if( shaderMenuItemSelected( item ) )
			return true;

		return super.onOptionsItemSelected( item );
	}

	@Override
	public void onCreateContextMenu(
		ContextMenu menu,
		View v,
		ContextMenuInfo menuInfo )
	{
		super.onCreateContextMenu( menu, v, menuInfo );

		inflateShaderMenu( menu );
	}

	@Override
	public boolean onContextItemSelected( MenuItem item )
	{
		if( shaderMenuItemSelected( item ) )
			return true;

		return super.onContextItemSelected( item );
	}

	@Override
	public void onShaderError( final String error )
	{
		// this call comes from the GL thread
		runOnUiThread(
			new Runnable()
			{
				public void run()
				{
					showError( error );
				}
			} );
	}

	@Override
	public void onShaderFramesPerSecond( final int fps )
	{
		// this call comes from the GL thread
		runOnUiThread(
			new Runnable()
			{
				public void run()
				{
					updateFps( fps );
				}
			} );
	}

	public void onInsertTab( View v )
	{
		int start = shaderEditor.getSelectionStart();
		int end = shaderEditor.getSelectionEnd();

		shaderEditor.getText().replace(
			Math.min( start, end ),
			Math.max( start, end ),
			"\t",
			0,
			1 );
	}

	public void onSaveShader( View v )
	{
		saveShader();
	}

	public void onToggleSource( View v )
	{
		if( shaderEditor.errorLine > 0 )
		{
			if( errorPopupVisible )
				errorPopup.dismiss();
			else
				errorPopup.showAsDropDown(
					sourceButton,
					getWindow().getDecorView().getWidth(),
					0 );

			errorPopupVisible ^= true;
			return;
		}

		scrollView.setVisibility( showSource ?
			View.GONE :
			View.VISIBLE );

		if( showSource )
		{
			InputMethodManager m =
				(InputMethodManager)getSystemService(
					Context.INPUT_METHOD_SERVICE );

			m.hideSoftInputFromWindow(
				shaderEditor.getWindowToken(),
				0 );
		}

		showSource ^= true;
		setSourceButtonDefault();
	}

	private void inflateShaderMenu( Menu menu )
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.shader_options, menu );
	}

	private boolean shaderMenuItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case R.id.add_shader:
				addShader();
				return true;
			case R.id.duplicate_shader:
				duplicateShader();
				return true;
			case R.id.delete_shader:
				deleteShader();
				return true;
			case R.id.save_shader:
				saveShader();
				return true;
			case R.id.share_shader:
				shareShader();
				return true;
			case R.id.preferences:
				showPreferences();
				return true;
		}

		return false;
	}

	private void setSaveButton()
	{
		saveButton.setImageResource( compileOnChange ?
			R.drawable.ic_save :
			R.drawable.ic_save_play );
	}

	private void setSourceButtonError()
	{
		sourceButton.setImageResource(
			R.drawable.ic_show_error );
	}

	private void setSourceButtonDefault()
	{
		sourceButton.setImageResource( showSource ?
			R.drawable.ic_hide_source :
			R.drawable.ic_show_source );
	}

	private void addShader()
	{
		selectNewShader( dataSource.insert() );
	}

	private void duplicateShader()
	{
		selectNewShader( dataSource.insert(
			shaderEditor.getText().toString(),
			shaderView.renderer.getThumbnail() ) );
	}

	private void deleteShader()
	{
		if( shaderSpinner.getCount() < 2 )
			return;

		DialogInterface.OnClickListener listener =
			new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(
					DialogInterface dialog,
					int which )
				{
					switch( which )
					{
						case DialogInterface.BUTTON_POSITIVE:
							dataSource.remove(
								shaderSpinner.getSelectedItemId() );

							updateAdapter();
							break;
					}
				}
			};

		new AlertDialog.Builder( this )
			.setMessage( R.string.are_you_sure )
			.setPositiveButton( R.string.yes, listener )
			.setNegativeButton( R.string.no, listener )
			.show();
	}

	private void saveShader()
	{
		String src = shaderEditor.getText().toString();

		if( !compileOnChange )
		{
			loadShader( src );
			shaderEditor.refresh();
		}

		dataSource.update(
			shaderSpinner.getSelectedItemId(),
			src,
			shaderView.renderer.getThumbnail() );

		updateAdapter();
	}

	private void shareShader()
	{
		Intent i = new Intent();

		i.setAction( Intent.ACTION_SEND );
		i.putExtra(
			Intent.EXTRA_TEXT,
			shaderEditor.getText().toString() );
		i.setType( "text/plain" );

		startActivity( Intent.createChooser(
			i,
			getResources().getText( R.string.share_shader ) ) );
	}

	private void showPreferences()
	{
		startActivity( new Intent(
			this,
			ShaderPreferenceActivity.class ) );
	}

	private void updateAdapter()
	{
		adapter.changeCursor( dataSource.queryAll() );
		resetFps();
	}

	private void selectNewShader( long id )
	{
		if( id < 1 )
			return;

		updateAdapter();
		setSpinnerItemById( shaderSpinner, id );
	}

	private static void setSpinnerItemById( Spinner spinner, long id )
	{
		for( int n = 0, l = spinner.getCount();
			n < l;
			++n )
			if( spinner.getItemIdAtPosition( n ) == id )
			{
				spinner.setSelection( n );
				return;
			}
	}

	private SharedPreferences getSharedPreferences()
	{
		return getSharedPreferences(
			ShaderPreferenceActivity.SHARED_PREFERENCES_NAME,
			0 );
	}

	private void loadPreferences()
	{
		SharedPreferences p = getSharedPreferences();

		compileOnChange = p.getBoolean(
			ShaderPreferenceActivity.COMPILE_ON_CHANGE,
			true );
		setSaveButton();

		shaderView.renderer.showFpsGauge = p.getBoolean(
			ShaderPreferenceActivity.SHOW_FPS_GAUGE,
			false );

		try
		{
			shaderEditor.updateDelay = Integer.parseInt( p.getString(
				ShaderPreferenceActivity.UPDATE_DELAY,
				"1000" ) );

			if( shaderEditor.updateDelay < 500 )
				shaderEditor.updateDelay = 500;
		}
		catch( Exception ex )
		{
			shaderEditor.updateDelay = 1000;

			SharedPreferences.Editor e = p.edit();

			e.putString(
				ShaderPreferenceActivity.UPDATE_DELAY,
				String.valueOf( shaderEditor.updateDelay ) );
			e.commit();
		}
	}

	private void savePreferences()
	{
		SharedPreferences.Editor e = getSharedPreferences().edit();

		e.putBoolean(
			ShaderPreferenceActivity.COMPILE_ON_CHANGE,
			compileOnChange );
		e.commit();
	}

	private void loadShader( String source )
	{
		setSourceButtonDefault();
		errorPopup.dismiss();
		errorPopupVisible = false;
		shaderEditor.errorLine = 0;

		shaderView.onPause();
		shaderView.renderer.fragmentShader = source;
		shaderView.onResume();
	}

	private void showError( String message )
	{
		if( message == null )
			return;

		int f;

		if( (f = message.indexOf( "ERROR: 0:" )) > -1 )
			f += 9;
		else if( (f = message.indexOf( "0:" )) > -1 )
			f += 2;

		if( f > -1 )
		{
			int t;

			if( (t = message.indexOf( ":", f )) > -1 )
			{
				try
				{
					shaderEditor.errorLine = Integer.valueOf(
						message.substring( f, t ).trim() );
				}
				catch( Exception e )
				{
					e.printStackTrace();
				}

				f = ++t;
			}

			if( (t = message.indexOf( "\n", f )) < 0 )
				t = message.length();

			message = message.substring( f, t ).trim();
		}

		if( message != null &&
			message.length() > 0 )
		{
			errorMessage.setText( message );
			setSourceButtonError();
		}

		shaderEditor.refresh();
	}

	private void resetFps()
	{
		shaderView.renderer.resetFps();
	}

	private void updateFps( int fps )
	{
		// It's better to not save the view returned for R.id.fps
		// because it's impossible to know when Spinner does change
		// the selected view; especially when the adapter changes
		// which may update the spinner not immediately.
		// Anyway, getSelectedView() and findViewById() are just
		// lookups and this method is only called every
		// ShaderRenderer.FPS_UPDATE_FREQUENCY milliseconds and if
		// fps did actually change so that shoudln't be problem
		View v = shaderSpinner.getSelectedView();
		TextView tv;

		if( v == null ||
			(tv = (TextView)v.findViewById( R.id.fps )) == null )
			return;

		tv.setText( String.valueOf( fps )+" fps" );
	}

	private static float getDistanceBetweenTouches( MotionEvent ev )
	{
		float xx = ev.getX( 1 )-ev.getX( 0 );
		float yy = ev.getY( 1 )-ev.getY( 0 );

		return (float)Math.sqrt( xx*xx+yy*yy );
	}

	private boolean pinchZoom( MotionEvent ev )
	{
		switch( ev.getAction() )
		{
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				shaderEditor.setOnLongClickListener( null );
				scrollView.setScrollingEnabled( true );
				zoomPinch = false;
				break;
			case MotionEvent.ACTION_MOVE:
				if( ev.getPointerCount() == 2 )
				{
					float d = getDistanceBetweenTouches( ev );

					if( !zoomPinch )
					{
						shaderEditor.setOnLongClickListener(
							new View.OnLongClickListener()
							{
								@Override
								public boolean onLongClick( View v )
								{
									return true;
								}
							} );
						scrollView.setScrollingEnabled( false );

						zoomPinchFactor = textSize/d;
						zoomPinch = true;
						break;
					}

					textSize = zoomPinchFactor*d;

					if( textSize < minimumTextSize )
						textSize = minimumTextSize;
					else if( textSize > maximumTextSize )
						textSize = maximumTextSize;

					shaderEditor.setTextSize(
						android.util.TypedValue.COMPLEX_UNIT_SP,
						textSize );
				}
				break;
		}

		return zoomPinch;
	}
}
