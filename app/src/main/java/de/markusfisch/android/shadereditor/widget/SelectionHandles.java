package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

// The platform's selection handles as a CodeView shows them. A handle points at
// a place at the bottom of a line and hangs below it; places are in the view's
// content, scrolling included.
final class SelectionHandles {
	static final int START = 0;
	static final int END = 1;

	private final Drawable left;
	private final Drawable right;
	private final Drawable middle;
	private final float minSize;
	private final Drawable[] shown = new Drawable[2];
	private final float[] x = new float[2];
	private final float[] y = new float[2];
	private final Rect bounds = new Rect();

	SelectionHandles(Context context, int color, float minSize) {
		TypedArray a = context.obtainStyledAttributes(new int[]{
				android.R.attr.textSelectHandleLeft,
				android.R.attr.textSelectHandleRight,
				android.R.attr.textSelectHandle});
		try {
			left = tinted(a.getDrawable(0), color);
			right = tinted(a.getDrawable(1), color);
			middle = tinted(a.getDrawable(2), color);
		} finally {
			a.recycle();
		}
		this.minSize = minSize;
	}

	void clear() {
		shown[START] = shown[END] = null;
	}

	void caret(float x, float y) {
		place(START, middle, x, y);
	}

	void end(int which, float x, float y) {
		place(which, which == START ? left : right, x, y);
	}

	int height() {
		return middle.getIntrinsicHeight();
	}

	float x(int which) {
		return x[which];
	}

	float y(int which) {
		return y[which];
	}

	// The nearer of two, at least minSize wide and tall; -1 for none.
	int at(float x, float y) {
		int found = -1;
		float nearest = Float.MAX_VALUE;
		for (int i = START; i <= END; ++i) {
			if (shown[i] == null) {
				continue;
			}
			bounds(i);
			float grow = Math.max(0, minSize - bounds.width()) / 2;
			float bottom = Math.max(bounds.bottom, bounds.top + minSize);
			if (x >= bounds.left - grow && x < bounds.right + grow &&
					y >= bounds.top && y < bottom) {
				float d = Math.abs(x - this.x[i]);
				if (d < nearest) {
					nearest = d;
					found = i;
				}
			}
		}
		return found;
	}

	void draw(Canvas canvas) {
		for (int i = START; i <= END; ++i) {
			if (shown[i] != null) {
				bounds(i);
				shown[i].setBounds(bounds);
				shown[i].draw(canvas);
			}
		}
	}

	private void place(int which, Drawable drawable, float x, float y) {
		shown[which] = drawable;
		this.x[which] = x;
		this.y[which] = y;
	}

	// Puts the bounds of handle which into bounds, its point where the
	// platform has it: a quarter in from the side it points to.
	private void bounds(int which) {
		Drawable drawable = shown[which];
		int width = drawable.getIntrinsicWidth();
		int hotspot = drawable == left
				? width * 3 / 4
				: drawable == right ? width / 4 : width / 2;
		int l = Math.round(x[which]) - hotspot;
		int t = Math.round(y[which]);
		bounds.set(l, t, l + width, t + drawable.getIntrinsicHeight());
	}

	private static Drawable tinted(Drawable drawable, int color) {
		drawable = drawable.mutate();
		drawable.setTint(color);
		return drawable;
	}
}
