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
	public boolean onInterceptTouchEvent( MotionEvent event )
	{
		if( touchThru )
			return false;

		return super.onInterceptTouchEvent( event );
	}

	@Override
	public boolean onTouchEvent( MotionEvent event )
	{
		if( touchThru )
			return false;

		return super.onTouchEvent( event );
	}
}
