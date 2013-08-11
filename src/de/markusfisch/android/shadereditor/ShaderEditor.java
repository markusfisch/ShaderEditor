package de.markusfisch.android.shadereditor;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
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
	public int updateDelay = 1000;
	public int errorLine = 0;
	public boolean dirty = false;

	private static final int COLOR_ERROR = 0x80ff0000;
	private static final int COLOR_NUMBER = 0xff7ba212;
	private static final int COLOR_KEYWORD = 0xff399ed7;
	private static final int COLOR_BUILTIN = 0xffd79e39;
	private static final int COLOR_COMMENT = 0xff808080;

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

				highlightWithoutChange( e );
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

		modified = false;
		setText( highlight( new SpannableStringBuilder( text ) ) );
		modified = true;

		if( onTextChangedListener != null )
			onTextChangedListener.onTextChanged( text.toString() );
	}

	public void refresh()
	{
		highlightWithoutChange( getText() );
	}

	private void init()
	{
		setHorizontallyScrolling( true );

		setFilters( new InputFilter[]{
			new InputFilter()
			{
				@Override
				public CharSequence filter(
					CharSequence source,
					int start,
					int end,
					Spanned dest,
					int dstart,
					int dend )
				{
					if( modified )
						return autoIndent(
							source,
							start,
							end,
							dest,
							dstart,
							dend );

					return source;
				}
			} } );

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
						updateDelay );
				}
			} );
	}

	private void cancelUpdate()
	{
		updateHandler.removeCallbacks( updateRunnable );
	}

	private void highlightWithoutChange( Editable e )
	{
		modified = false;
		highlight( e );
		modified = true;
	}

	private Editable highlight( Editable e )
	{
		try
		{
			// don't use e.clearSpans() because it will remove
			// too much
			clearSpans( e );

			if( e.length() == 0 )
				return e;

			if( errorLine > 0 )
			{
				Matcher m = line.matcher( e );

				for( int n = errorLine;
					n-- > 0 && m.find(); );

				e.setSpan(
					new BackgroundColorSpan( COLOR_ERROR ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
			}

			for( Matcher m = numbers.matcher( e );
				m.find(); )
				e.setSpan(
					new ForegroundColorSpan( COLOR_NUMBER ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );

			for( Matcher m = keywords.matcher( e );
				m.find(); )
				e.setSpan(
					new ForegroundColorSpan( COLOR_KEYWORD ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );

			for( Matcher m = builtins.matcher( e );
				m.find(); )
				e.setSpan(
					new ForegroundColorSpan( COLOR_BUILTIN ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );

			for( Matcher m = comments.matcher( e );
				m.find(); )
				e.setSpan(
					new ForegroundColorSpan( COLOR_COMMENT ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
		catch( Exception ex )
		{
		}

		return e;
	}

	private void clearSpans( Editable e )
	{
		// remove foreground color spans
		{
			ForegroundColorSpan spans[] = e.getSpans(
				0,
				e.length(),
				ForegroundColorSpan.class );

			for( int n = spans.length; n-- > 0; )
				e.removeSpan( spans[n] );
		}

		// remove background color spans
		{
			BackgroundColorSpan spans[] = e.getSpans(
				0,
				e.length(),
				BackgroundColorSpan.class );

			for( int n = spans.length; n-- > 0; )
				e.removeSpan( spans[n] );
		}
	}

	private CharSequence autoIndent(
		CharSequence source,
		int start,
		int end,
		Spanned dest,
		int dstart,
		int dend )
	{
		if( end-start != 1 ||
			start >= source.length() ||
			source.charAt( start ) != '\n' ||
			dstart >= dest.length() )
			return source;

		int istart = dstart;
		int iend = -1;
		String indent = "";

		// skip end of line if cursor is at the end of a line
		if( dest.charAt( istart ) == '\n' )
			--istart;

		// indent next line if this one isn't terminated
		if( istart > -1 )
		{
			// skip white space
			for( ; istart > -1; --istart )
			{
				char c = dest.charAt( istart );

				if( c != ' ' &&
					c != '\t' )
					break;
			}

			if( istart > -1 )
			{
				char c = dest.charAt( istart );

				if( c != ';' &&
					c != '}' &&
					c != '\n' )
					indent = "\t";
			}
		}

		// find start of previous line
		for( ; istart > -1; --istart )
			if( dest.charAt( istart ) == '\n' )
				break;

		// cursor is in the first line
		if( istart < 0 )
			return source;

		// span over previous indent
		for( iend = ++istart;
			iend < dend;
			++iend )
		{
			char c = dest.charAt( iend );

			if( c != ' ' &&
				c != '\t' )
				break;
		}

		// copy white space of previous lines and append new indent
		return "\n"+dest.subSequence(
			istart,
			iend )+indent;
	}
}
