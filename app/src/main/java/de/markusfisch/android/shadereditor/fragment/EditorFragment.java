package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.opengl.InfoLog;
import de.markusfisch.android.shadereditor.widget.ShaderEditor;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Toast;

public class EditorFragment extends Fragment
{
	public static final String TAG = "EditorFragment";

	private InputMethodManager imm;
	private ScrollView scrollView;
	private ShaderEditor shaderEditor;
	private int yOffset;

	@Override
	public View onCreateView(
		LayoutInflater inflater,
		ViewGroup container,
		Bundle state )
	{
		Activity activity;

		if( (activity = getActivity()) == null )
			return null;

		View view;

		if( (imm = (InputMethodManager)activity.getSystemService(
				Context.INPUT_METHOD_SERVICE )) == null ||
			(view = inflater.inflate(
				R.layout.fragment_editor,
				container,
				false )) == null ||
			(scrollView = (ScrollView)view.findViewById(
				R.id.scroll_view )) == null ||
			(shaderEditor = (ShaderEditor)view.findViewById(
				R.id.editor )) == null )
		{
			activity.finish();
			return null;
		}

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
		shaderEditor.setErrorLine( 0 );
	}

	public void showError( String infoLog )
	{
		Activity activity = getActivity();

		if( activity == null )
			return;

		InfoLog.parse( infoLog );
		shaderEditor.setErrorLine( InfoLog.getErrorLine() );

		Toast errorToast = Toast.makeText(
			activity,
			InfoLog.getMessage(),
			Toast.LENGTH_SHORT );

		errorToast.setGravity(
			Gravity.TOP | Gravity.CENTER_HORIZONTAL,
			0,
			getYOffset( activity ) );
		errorToast.show();
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

	public void insertTab()
	{
		shaderEditor.insertTab();
	}

	public void addUniform( String name )
	{
		shaderEditor.addUniform( name );
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

	private int getYOffset( Activity activity )
	{
		if( yOffset == 0 )
		{
			float dp = getResources().getDisplayMetrics().density;

			try
			{
				yOffset = ((AppCompatActivity)activity)
					.getSupportActionBar()
					.getHeight();
			}
			catch( ClassCastException e )
			{
				yOffset = Math.round( 48f*dp );
			}

			yOffset += Math.round( 16f*dp );
		}

		return yOffset;
	}
}
