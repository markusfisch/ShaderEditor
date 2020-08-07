package de.markusfisch.android.shadereditor.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Required to get touches through DrawerLayout
 */
public class TouchThruDrawerLayout extends DrawerLayout {
	private boolean touchThru = false;

	public TouchThruDrawerLayout(Context context) {
		super(context);
	}

	public TouchThruDrawerLayout(Context context, AttributeSet attr) {
		super(context, attr);
	}

	public void setTouchThru(boolean touchThru) {
		this.touchThru = touchThru;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		return !touchThru && super.onInterceptTouchEvent(event);
	}

	// click is handled in super class
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return !touchThru && super.onTouchEvent(event);
	}
}
