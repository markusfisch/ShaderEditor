package de.markusfisch.android.shadereditor.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

	public static void initSystemBars(@Nullable AppCompatActivity activity,
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

	public static boolean setSystemBarColor(
			@NonNull Window window,
			int color,
			boolean expand) {
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

	public static void hideNavigation(@NonNull Window window) {
		window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
						View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
						View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_FULLSCREEN);
	}

	public static int getToolBarHeight(@NonNull Context context) {
		TypedValue tv = new TypedValue();
		return context.getTheme().resolveAttribute(
				android.R.attr.actionBarSize,
				tv,
				true) ? TypedValue.complexToDimensionPixelSize(
				tv.data,
				context.getResources().getDisplayMetrics()) : 0;
	}

	private static void setWindowInsets(@NonNull final View mainLayout,
			@Nullable final Rect windowInsets) {
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