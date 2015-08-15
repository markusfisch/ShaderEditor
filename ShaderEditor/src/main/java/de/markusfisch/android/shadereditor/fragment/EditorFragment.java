package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.opengl.ShaderError;
import de.markusfisch.android.shadereditor.widget.ShaderEditor;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class EditorFragment extends Fragment
{
	public static final String TAG = "EditorFragment";

	public interface OnEditorListener
	{
		public void onRunCode( String code );
		public void onCodeHidden( boolean hidden );
	}

	private OnEditorListener onEditorListener;
	private ScrollView scrollView;
	private ShaderEditor shaderEditor;
	private InputMethodManager imm;
	private TextView errorView;

	@Override
	public void onAttach( Activity activity )
	{
		super.onAttach( activity );

		try
		{
			onEditorListener =
				(OnEditorListener)activity;
		}
		catch( ClassCastException e )
		{
			throw new ClassCastException(
				activity.toString()+
				" must implement "+
				"OnEditorListener" );
		}
	}

	@Override
	public void onCreate( Bundle state )
	{
		super.onCreate( state );

		setHasOptionsMenu( true );
	}

	@Override
	public View onCreateView(
		LayoutInflater inflater,
		ViewGroup container,
		Bundle state )
	{
		Activity activity;
		View view;

		if( (activity = getActivity()) == null )
			return null;

		if( (view = inflater.inflate(
				R.layout.fragment_editor,
				container,
				false )) == null ||
			(shaderEditor = (ShaderEditor)view.findViewById(
				R.id.editor )) == null ||
			(scrollView = (ScrollView)view.findViewById(
				R.id.scroll )) == null )
		{
			activity.finish();
			return null;
		}

		imm = (InputMethodManager)activity.getSystemService(
			Context.INPUT_METHOD_SERVICE );

		shaderEditor.setOnTextChangedListener(
			new ShaderEditor.OnTextChangedListener()
			{
				@Override
				public void onTextChanged( String text )
				{
					if( ShaderEditorApplication
							.preferences
							.doesCompileOnChange() )
						runText( text );
				}
			} );

		initErrorView( view );

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();

		shaderEditor.setTextSize(
			android.util.TypedValue.COMPLEX_UNIT_SP,
			ShaderEditorApplication
				.preferences
				.getTextSize() );
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate( R.menu.fragment_editor, menu );

		boolean doesCompileOnChange =
			ShaderEditorApplication
				.preferences
				.doesCompileOnChange();

		menu.findItem( R.id.run_code ).setVisible(
			doesCompileOnChange ^ true );
		menu.findItem( R.id.save_shader ).setVisible(
			doesCompileOnChange );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case R.id.toggle_code:
				toggleCode();
				return true;
			case R.id.run_code:
				runText( shaderEditor.getText().toString() );
				return true;
			case R.id.share_shader:
				shareShader();
				return true;
		}

		return super.onOptionsItemSelected( item );
	}

	public boolean isModified()
	{
		return shaderEditor.isModified();
	}

	public String getText()
	{
		return shaderEditor.getCleanText();
	}

	public void setText( String text )
	{
		hideError();

		shaderEditor.setTextHighlighted( text );
	}

	public void setError( String glError )
	{
		ShaderError.parseError( glError );

		errorView.setText( ShaderError.getMessage() );
		errorView.setVisibility( View.VISIBLE );

		shaderEditor.setErrorLine( ShaderError.getErrorLine() );
	}

	private void initErrorView( View view )
	{
		errorView = (TextView)view.findViewById(
			R.id.error );
	}

	private void toggleCode()
	{
		boolean hidden = scrollView.getVisibility() == View.VISIBLE;

		scrollView.setVisibility( hidden ?
			View.GONE :
			View.VISIBLE );

		if( hidden )
			imm.hideSoftInputFromWindow(
				shaderEditor.getWindowToken(),
				0 );

		if( onEditorListener != null )
			onEditorListener.onCodeHidden( hidden );
	}

	private void runText( String text )
	{
		hideError();

		if( text != null &&
			onEditorListener != null )
			onEditorListener.onRunCode( text );
	}

	private void hideError()
	{
		errorView.setVisibility( View.GONE );
		shaderEditor.setErrorLine( 0 );
	}

	private void shareShader()
	{
		Intent intent = new Intent();

		intent.setAction( Intent.ACTION_SEND );
		intent.putExtra(
			Intent.EXTRA_TEXT,
			shaderEditor.getText().toString() );
		intent.setType( "text/plain" );

		startActivity( Intent.createChooser(
			intent,
			getResources().getText( R.string.share_shader ) ) );
	}
}
