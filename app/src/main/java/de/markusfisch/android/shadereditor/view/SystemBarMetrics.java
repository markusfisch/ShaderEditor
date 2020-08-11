package de.markusfisch.android.shadereditor.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

public class SystemBarMetrics {
	public static void initSystemBars(AppCompatActivity activity) {
		initSystemBars(activity, null);
	}

	public static void initSystemBars(AppCompatActivity activity,
			Rect insets) {
		View mainLayout;
		if (activity != null &&
				(mainLayout = activity.findViewById(R.id.main_layout)) != null &&
				setSystemBarColor(
						activity.getWindow(),
						ShaderEditorApp.preferences.getSystemBarColor(),
						true)) {
			setWindowInsets(mainLayout, insets);
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static boolean setSystemBarColor(
			Window window,
			int color,
			boolean expand) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return false;
		}
		if (expand) {
			window.getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
							View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
							View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
		window.setStatusBarColor(color);
		window.setNavigationBarColor(color);
		return true;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static void hideNavigation(Window window) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return;
		}
		window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
						View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
						View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_FULLSCREEN);
	}

	public static int getToolBarHeight(Context context) {
		TypedValue tv = new TypedValue();
		return context.getTheme().resolveAttribute(
				android.R.attr.actionBarSize,
				tv,
				true) ? TypedValue.complexToDimensionPixelSize(
				tv.data,
				context.getResources().getDisplayMetrics()) : 0;
	}

	private static void setWindowInsets(final View mainLayout,
			final Rect windowInsets) {
		ViewCompat.setOnApplyWindowInsetsListener(mainLayout, new OnApplyWindowInsetsListener() {
			@Override
			public WindowInsetsCompat onApplyWindowInsets(View v,
					WindowInsetsCompat insets) {
				if (insets.hasSystemWindowInsets()) {
					int left = insets.getSystemWindowInsetLeft();
					int top = insets.getSystemWindowInsetTop();
					int right = insets.getSystemWindowInsetRight();
					int bottom = insets.getSystemWindowInsetBottom();
					mainLayout.setPadding(left, top, right, bottom);
					if (windowInsets != null) {
						windowInsets.set(left, top, right, bottom);
					}
				}
				return insets.consumeSystemWindowInsets();
			}
		});
	}
}
