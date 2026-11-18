package de.markusfisch.android.shadereditor.widget;

import android.animation.ValueAnimator;
import android.os.Build;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Magnifier;

import androidx.annotation.RequiresApi;

// The platform's magnifier over a dragged end of a selection, as its text views
// show it: with their defaults, sliding for 100 ms when it jumps to another
// line, and not at all for text taller than what it shows.
@RequiresApi(Build.VERSION_CODES.P)
final class DragMagnifier {
	private static final long JUMP = 100;
	// The defaults of the platform before Android 10 has the getters:
	// 100 dp by 48 dp at a zoom of 1.25.
	private static final float WIDTH_DP = 100 / 1.25f;
	private static final float HEIGHT_DP = 48 / 1.25f;

	private final Magnifier magnifier;
	private final ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
	private final float sourceWidth;
	private final float sourceHeight;
	private boolean showing;
	private float fromX;
	private float fromY;
	private float atX;
	private float atY;
	private float toX;
	private float toY;

	// The deprecated constructor has the defaults of the text views.
	@SuppressWarnings("deprecation")
	DragMagnifier(View view) {
		magnifier = new Magnifier(view);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			sourceWidth = magnifier.getWidth() / magnifier.getZoom();
			sourceHeight = magnifier.getHeight() / magnifier.getZoom();
		} else {
			float density = view.getResources().getDisplayMetrics().density;
			sourceWidth = WIDTH_DP * density;
			sourceHeight = HEIGHT_DP * density;
		}
		animator.setDuration(JUMP);
		animator.setInterpolator(new LinearInterpolator());
		animator.addUpdateListener(animation -> {
			float f = animation.getAnimatedFraction();
			atX = fromX + (toX - fromX) * f;
			atY = fromY + (toY - fromY) * f;
			magnifier.show(atX, atY);
		});
	}

	// In pixels.
	float halfWidth() {
		return sourceWidth / 2;
	}

	// height in pixels.
	boolean fits(float height) {
		return height <= sourceHeight;
	}

	void show(float x, float y) {
		boolean jump = showing && y != toY;
		if (jump) {
			boolean running = animator.isRunning();
			animator.cancel();
			fromX = running ? atX : toX;
			fromY = running ? atY : toY;
		}
		toX = x;
		toY = y;
		if (jump) {
			animator.start();
		} else if (!animator.isRunning()) {
			magnifier.show(x, y);
		}
		showing = true;
	}

	void dismiss() {
		animator.cancel();
		magnifier.dismiss();
		showing = false;
	}
}
