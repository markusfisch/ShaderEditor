package de.markusfisch.android.shadereditor.widget;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.RequiresApi;

// The platform's floating toolbar over the selection or the caret. It hides
// while the host moves what it points at and comes back a moment after. Back
// closes it, as the user's dismissal.
final class SelectionToolbar extends ActionMode.Callback2 {
	private final CodeView host;
	private final Runnable reveal = this::reveal;
	private ActionMode mode;
	private boolean closing;
	// From Android 13 on, while the toolbar shows: the dispatcher and the
	// callback that closes it. Back reaches the callback only while
	// predictive back is on for the app; else it comes as a key.
	private Object backDispatcher;
	private Object backCallback;

	SelectionToolbar(CodeView host) {
		this.host = host;
	}

	// As the platform's text views, which offer few enough actions.
	@SuppressLint("AlwaysShowAction")
	private static void fill(Menu menu, int[] actions) {
		menu.clear();
		for (int i = 0; i < actions.length; ++i) {
			int id = actions[i];
			MenuItem item;
			if (id == android.R.id.cut) {
				item = menu.add(Menu.NONE, id, i, android.R.string.cut)
						.setAlphabeticShortcut('x');
			} else if (id == android.R.id.copy) {
				item = menu.add(Menu.NONE, id, i, android.R.string.copy)
						.setAlphabeticShortcut('c');
			} else if (id == android.R.id.paste) {
				item = menu.add(Menu.NONE, id, i, android.R.string.paste)
						.setAlphabeticShortcut('v');
			} else {
				item = menu.add(Menu.NONE, id, i, android.R.string.selectAll)
						.setAlphabeticShortcut('a');
			}
			// Select all goes to the overflow first.
			item.setShowAsAction(id == android.R.id.selectAll
					? MenuItem.SHOW_AS_ACTION_IF_ROOM
					: MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
	}

	boolean isShown() {
		return mode != null;
	}

	// A moment after the last touch if it was hidden.
	void show() {
		if (mode == null) {
			mode = host.startActionMode(this, ActionMode.TYPE_FLOATING);
			if (mode != null &&
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				takeBack();
			}
			return;
		}
		mode.invalidate();
		mode.invalidateContentRect();
		host.removeCallbacks(reveal);
		host.postDelayed(reveal, ViewConfiguration.getDoubleTapTimeout());
	}

	// For as long as a drag goes on.
	void hide() {
		if (mode != null) {
			host.removeCallbacks(reveal);
			mode.hide(ActionMode.DEFAULT_HIDE_DURATION);
		}
	}

	void moved() {
		if (mode != null) {
			mode.invalidateContentRect();
		}
	}

	// As the user does with Back.
	void dismiss() {
		if (mode != null) {
			mode.finish();
		}
	}

	void close() {
		if (mode != null) {
			closing = true;
			try {
				mode.finish();
			} finally {
				closing = false;
			}
		}
	}

	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	private void takeBack() {
		OnBackInvokedDispatcher dispatcher =
				host.findOnBackInvokedDispatcher();
		if (dispatcher != null) {
			OnBackInvokedCallback callback = this::dismiss;
			dispatcher.registerOnBackInvokedCallback(
					OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
			backDispatcher = dispatcher;
			backCallback = callback;
		}
	}

	private void releaseBack() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
				backCallback != null) {
			((OnBackInvokedDispatcher) backDispatcher)
					.unregisterOnBackInvokedCallback(
							(OnBackInvokedCallback) backCallback);
			backDispatcher = backCallback = null;
		}
	}

	private void reveal() {
		if (mode != null) {
			mode.hide(0);
		}
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		return onPrepareActionMode(mode, menu);
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		fill(menu, host.offered());
		return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (host.toolbarAction(item.getItemId())) {
			close();
		}
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		this.mode = null;
		host.removeCallbacks(reveal);
		releaseBack();
		host.toolbarClosed(!closing);
	}

	@Override
	public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
		host.selectionRect(outRect);
	}
}
