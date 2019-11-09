package de.markusfisch.android.shadereditor.view;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class SoftKeyboard {
	public static void hide(Context context, View view) {
		if (context != null && view != null) {
			InputMethodManager imm = ((InputMethodManager) context.getSystemService(
					Context.INPUT_METHOD_SERVICE));
			if (imm != null) {
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			}
		}
	}
}
