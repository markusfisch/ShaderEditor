package de.markusfisch.android.shadereditor.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.util.TypedValue;

public class SystemBarMetrics {
	public static int getStatusAndToolBarHeight(Context context) {
		return getStatusBarHeight(context.getResources()) +
				getToolBarHeight(context);
	}

	public static int getStatusBarHeight(Resources res) {
		return getIdentifierDimen(res, "status_bar_height");
	}

	public static int getToolBarHeight(Context context) {
		TypedValue tv = new TypedValue();
		return context.getTheme().resolveAttribute(
				android.R.attr.actionBarSize,
				tv,
				true) ?
			TypedValue.complexToDimensionPixelSize(
					tv.data,
					context.getResources().getDisplayMetrics()) :
			0;
	}

	public static Point getNavigationBarSize(Resources res) {
		Point size = new Point(0, 0);
		if (!getIdentifierBoolean(res, "config_showNavigationBar")) {
			return size;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			Configuration conf = res.getConfiguration();
			if (conf.orientation == Configuration.ORIENTATION_LANDSCAPE &&
					// according to https://developer.android.com/training/multiscreen/screensizes.html#TaskUseSWQuali
					// only a screen < 600 dp is considered to be a phone
					// and can move its navigation bar to the side
					conf.smallestScreenWidthDp < 600) {
				size.x = getIdentifierDimen(
						res,
						"navigation_bar_height_landscape");
				return size;
			}
		}

		size.y = getIdentifierDimen(res, "navigation_bar_height");
		return size;
	}

	private static boolean getIdentifierBoolean(Resources res, String name) {
		int id = res.getIdentifier(
				name,
				"bool",
				"android");
		return id > 0 && res.getBoolean(id);
	}

	private static int getIdentifierDimen(Resources res, String name) {
		int id = res.getIdentifier(
				name,
				"dimen",
				"android");
		return id > 0 ? res.getDimensionPixelSize(id) : 0;
	}
}
