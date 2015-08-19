package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.opengl.ShaderError;
import de.markusfisch.android.shadereditor.widget.ShaderEditor;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class EditorFragment extends Fragment
{
	public static final String TAG = "EditorFragment";

	private ScrollView scrollView;
	private ShaderEditor shaderEditor;
	private InputMethodManager imm;
	private TextView errorView;

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

		try
		{
			shaderEditor.setOnTextChangedListener(
				(ShaderEditor.OnTextChangedListener)activity );
		}
		catch( ClassCastException e )
		{
			throw new ClassCastException(
				activity.toString()+
				" must implement "+
				"ShaderEditor.OnTextChangedListener" );
		}

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

	public void hideError()
	{
		errorView.setVisibility( View.GONE );
		shaderEditor.setErrorLine( 0 );
	}

	public void setError( String glError )
	{
		ShaderError.parseError( glError );

		errorView.setText( ShaderError.getMessage() );
		errorView.setVisibility( View.VISIBLE );

		shaderEditor.setErrorLine( ShaderError.getErrorLine() );
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

	public boolean isCodeVisible()
	{
		return scrollView.getVisibility() == View.VISIBLE;
	}

	public boolean toggleCode()
	{
		boolean visible = isCodeVisible();

		scrollView.setVisibility( visible ?
			View.GONE :
			View.VISIBLE );

		if( visible )
			imm.hideSoftInputFromWindow(
				shaderEditor.getWindowToken(),
				0 );

		return visible;
	}

	private void initErrorView( View view )
	{
		errorView = (TextView)view.findViewById(
			R.id.error );
	}
}
