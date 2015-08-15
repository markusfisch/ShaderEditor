package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

/** Required to get touches through DrawerLayout */
public class TouchThruDrawerlayout extends DrawerLayout
{
	private boolean touchThru = false;

	public TouchThruDrawerlayout( Context context )
	{
		super( context );
	}

	public TouchThruDrawerlayout( Context context, AttributeSet attr )
	{
		super( context, attr );
	}

	public void setTouchThru( boolean touchThru )
	{
		this.touchThru = touchThru;
	}

	@Override
	public boolean onInterceptTouchEvent( MotionEvent ev )
	{
		if( ev.getActionMasked() == MotionEvent.ACTION_DOWN &&
			touchThru )
			return false;

		return super.onInterceptTouchEvent( ev );
	}

	@Override
	public boolean onTouchEvent( MotionEvent ev )
	{
		if( touchThru )
			return false;

		return super.onTouchEvent( ev );
	}
}
