package de.markusfisch.android.shadereditor.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Required to get touches through DrawerLayout
 */
public class TouchThruDrawerLayout extends DrawerLayout {
	private boolean touchThru = false;

	public TouchThruDrawerLayout(@NonNull Context context) {
		super(context);
	}

	public TouchThruDrawerLayout(@NonNull Context context, AttributeSet attr) {
		super(context, attr);
	}

	public void setTouchThru(boolean touchThru) {
		this.touchThru = touchThru;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		return !touchThru && super.onInterceptTouchEvent(event);
	}

	// Click is handled in super class.
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return !touchThru && super.onTouchEvent(event);
	}
}
