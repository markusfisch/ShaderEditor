package de.markusfisch.android.shadereditor.view;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

public class SystemBarMetrics {
	public static void initSystemBars(AppCompatActivity activity) {
		initSystemBars(activity, null);
	}

	public static void initSystemBars(AppCompatActivity activity,
			Rect insets) {
		View mainLayout;
		if (activity == null ||
				(mainLayout = activity.findViewById(R.id.main_layout)) == null) {
			return;
		}
		setSystemBarColor(
				activity.getWindow(),
				ShaderEditorApp.preferences.getSystemBarColor(),
				true);
		setWindowInsets(mainLayout, insets);
	}

	public static void setSystemBarColor(
			Window window,
			int color,
			boolean expand) {
		if (expand) {
			window.getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
							View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
							View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
		// System bars no longer have a background from SDK35+.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			window.setStatusBarColor(color);
			window.setNavigationBarColor(color);
		}
	}

	public static void hideNavigation(Window window) {
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
		ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
			Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			int left = systemBarsInsets.left;
			int top = systemBarsInsets.top;
			int right = systemBarsInsets.right;
			int bottom = systemBarsInsets.bottom;
			mainLayout.setPadding(left, top, right, bottom);

			if (windowInsets != null) {
				windowInsets.set(left, top, right, bottom);
			}

			return insets;
		});
	}
}