package de.markusfisch.android.shadereditor;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.EditText;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ShaderEditor extends EditText
{
	public interface OnTextChangedListener
	{
		public void onTextChanged( String text );
	}

	public OnTextChangedListener onTextChangedListener = null;
	public int errorLine = 0;
	public boolean dirty = false;

	private static final Pattern line = Pattern.compile(
		".*\\n" );
	private static final Pattern numbers = Pattern.compile(
		"\\b(\\d*[.]?\\d+)\\b" );
	private static final Pattern keywords = Pattern.compile(
		"\\b(attribute|const|uniform|varying|break|continue|"+
		"do|for|while|if|else|in|out|inout|float|int|void|bool|true|false|"+
		"lowp|mediump|highp|precision|invariant|discard|return|mat2|mat3|"+
		"mat4|vec2|vec3|vec4|ivec2|ivec3|ivec4|bvec2|bvec3|bvec4|sampler2D|"+
		"samplerCube|struct|gl_Vertex|gl_FragCoord|gl_FragColor)\\b" );
	private static final Pattern builtins = Pattern.compile(
		"\\b(radians|degrees|sin|cos|tan|asin|acos|atan|pow|"+
		"exp|log|exp2|log2|sqrt|inversesqrt|abs|sign|floor|ceil|fract|mod|"+
		"min|max|clamp|mix|step|smoothstep|length|distance|dot|cross|"+
		"normalize|faceforward|reflect|refract|matrixCompMult|lessThan|"+
		"lessThanEqual|greaterThan|greaterThanEqual|equal|notEqual|any|all|"+
		"not|dFdx|dFdy|fwidth|texture2D|texture2DProj|texture2DLod|"+
		"texture2DProjLod|textureCube|textureCubeLod)\\b" );
	private static final Pattern comments = Pattern.compile(
		"/\\*(?:.|[\\n\\r])*?\\*/|//.*" );

	private final Handler updateHandler = new Handler();
	private final Runnable updateRunnable =
		new Runnable()
		{
			@Override
			public void run()
			{
				Editable e = getText();

				if( onTextChangedListener != null )
					onTextChangedListener.onTextChanged( e.toString() );

				replaceTextKeepCursor( e );
			}
		};
	private boolean modified = true;

	public ShaderEditor( Context context )
	{
		super( context );
		init();
	}

	public ShaderEditor( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		init();
	}

	public void setTextHighlighted( CharSequence text )
	{
		cancelUpdate();

		errorLine = 0;
		dirty = false;

		replaceText( new SpannableStringBuilder( text ) );

		if( onTextChangedListener != null )
			onTextChangedListener.onTextChanged( text.toString() );
	}

	public void refresh()
	{
		replaceTextKeepCursor( getText() );
	}

	private void init()
	{
		setHorizontallyScrolling( true );

		addTextChangedListener(
			new TextWatcher()
			{
				@Override
				public void onTextChanged(
					CharSequence s,
					int start,
					int before,
					int count )
				{
				}

				@Override
				public void beforeTextChanged(
					CharSequence s,
					int start,
					int count,
					int after )
				{
				}

				@Override
				public void afterTextChanged( Editable e )
				{
					cancelUpdate();

					if( !modified )
						return;

					dirty = true;
					updateHandler.postDelayed(
						updateRunnable,
						1000 );
				}
			} );
	}

	private void cancelUpdate()
	{
		updateHandler.removeCallbacks( updateRunnable );
	}

	private void replaceText( Editable e )
	{
		modified = false;
		setText( highlight( e ) );
		modified = true;
	}

	private void replaceTextKeepCursor( Editable e )
	{
		int p = getSelectionStart();

		replaceText( e );

		if( p > -1 )
			setSelection( p );
	}

	private Editable highlight( Editable editable )
	{
		try
		{
			editable.clearSpans();

			if( editable.length() == 0 )
				return editable;

			if( errorLine > 0 )
			{
				Matcher m = line.matcher( editable );

				for( int n = errorLine;
					n-- > 0 && m.find(); );

				editable.setSpan(
					new BackgroundColorSpan( 0x80ff0000 ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
			}

			for( Matcher m = numbers.matcher( editable );
				m.find(); )
				editable.setSpan(
					new ForegroundColorSpan( 0xff7ba212 ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );

			for( Matcher m = keywords.matcher( editable );
				m.find(); )
				editable.setSpan(
					new ForegroundColorSpan( 0xff399ed7 ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );

			for( Matcher m = builtins.matcher( editable );
				m.find(); )
				editable.setSpan(
					new ForegroundColorSpan( 0xffd79e39 ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );

			for( Matcher m = comments.matcher( editable );
				m.find(); )
				editable.setSpan(
					new ForegroundColorSpan( 0xff808080 ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
		catch( Exception e )
		{
		}

		return editable;
	}
}
