package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import java.util.Collections;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.editor.AccessibleText;
import de.markusfisch.android.shadereditor.editor.Document;
import de.markusfisch.android.shadereditor.editor.Motion;
import de.markusfisch.android.shadereditor.editor.Text;

// What a CodeView tells accessibility services, as the platform's EditText
// does: one editable multi-line node whose text is the lines AccessibleText
// reads for the selection, and whose state is the caret's line. The framework
// calls it only while a service is enabled, and the view tells it of changes
// only then.
final class CodeAccessibility extends View.AccessibilityDelegate {
	private static final int GRANULARITIES =
			AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER |
					AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD |
					AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE |
					AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH |
					AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE;
	private static final int SAID_CHARS = 200;

	private final CodeView view;
	private final Motion motion = new Motion();
	// The text of the last window, while its text is the document's.
	private Text windowOf;
	private AccessibleText.Window window;
	private String windowText;
	// The window of the last selection event, in the text it was of. A
	// service says what a move passed over from the text of the selection
	// event, between the index of the event before and the new one, so the
	// window stays the same while it holds the selection.
	private Text toldOf;
	private AccessibleText.Window toldWindow;
	private String toldText;
	private Document.Selection reported;
	CodeAccessibility(CodeView view) {
		this.view = view;
	}

	@Override
	public void onInitializeAccessibilityNodeInfo(@NonNull View host,
			@NonNull AccessibilityNodeInfo info) {
		super.onInitializeAccessibilityNodeInfo(host, info);
		Document document = view.getDocument();
		info.setEditable(true);
		info.setMultiLine(true);
		info.setInputType(view.inputType());
		info.setClickable(true);
		info.addAction(AccessibilityAction.ACTION_CLICK);
		info.setLongClickable(true);
		info.addAction(AccessibilityAction.ACTION_LONG_CLICK);
		addScrollActions(host, info);
		if (document == null) {
			return;
		}
		info.addAction(AccessibilityAction.ACTION_SET_TEXT);
		info.setContentInvalid(view.caretLineHasError());
		if (host.isFocused() &&
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			info.addAction(AccessibilityAction.ACTION_IME_ENTER);
		}
		Document.Selection selection = document.selection();
		Text all = document.text();
		AccessibleText.Window window = reading(all, selection);
		String text = text(all, window);
		info.setText(text.isEmpty() ? null : text);
		AccessibilityNodeInfoCompat.wrap(info).setStateDescription(
				host.getContext().getString(R.string.a11y_code_line,
						all.lineOf(selection.head()) + 1, all.lineCount()));
		if (!text.isEmpty()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				info.setAvailableExtraData(Collections.singletonList(
						AccessibilityNodeInfo
								.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY));
			}
			info.setTextSelection(window.index(selection.anchor()),
					window.index(selection.head()));
			info.addAction(AccessibilityAction.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
			info.addAction(
					AccessibilityAction.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY);
			info.setMovementGranularities(GRANULARITIES);
			info.addAction(AccessibilityAction.ACTION_SET_SELECTION);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				info.setTextSelectable(true);
			}
		}
		if (host.isFocused()) {
			// In the order of TextView's.
			int[] offered = view.offered();
			if (offers(offered, android.R.id.copy)) {
				info.addAction(AccessibilityAction.ACTION_COPY);
			}
			if (offers(offered, android.R.id.paste)) {
				info.addAction(AccessibilityAction.ACTION_PASTE);
			}
			if (offers(offered, android.R.id.cut)) {
				info.addAction(AccessibilityAction.ACTION_CUT);
			}
		}
	}

	// The framework calls the view's addExtraDataToAccessibilityNodeInfo, not
	// the delegate's.
	@RequiresApi(Build.VERSION_CODES.O)
	void addExtraData(AccessibilityNodeInfo info, String key,
			Bundle arguments) {
		Document document = view.getDocument();
		if (document == null || arguments == null || !AccessibilityNodeInfo
				.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY.equals(key)) {
			return;
		}
		AccessibleText.Window window = reading(document.text(),
				document.selection());
		int start = arguments.getInt(AccessibilityNodeInfo
				.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, -1);
		int length = arguments.getInt(AccessibilityNodeInfo
				.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, -1);
		if (length <= 0 || start < 0 || start >= window.length()) {
			return;
		}
		RectF[] bounds = new RectF[Math.min(length, window.length() - start)];
		view.characterBounds(window.start() + start, bounds);
		info.getExtras().putParcelableArray(key, bounds);
	}

	@Override
	public void onInitializeAccessibilityEvent(@NonNull View host,
			@NonNull AccessibilityEvent event) {
		super.onInitializeAccessibilityEvent(host, event);
		Document document = view.getDocument();
		if (document == null) {
			return;
		}
		switch (event.getEventType()) {
			case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
				Document.Selection selection = document.selection();
				AccessibleText.Window window = told(document.text(),
						selection.start(), selection.end());
				if (window.length() > 0) {
					event.setFromIndex(window.index(selection.anchor()));
					event.setToIndex(window.index(selection.head()));
					event.setItemCount(window.length());
				}
			}
			case AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
				event.setScrollable(host.canScrollVertically(-1) ||
						host.canScrollVertically(1) ||
						host.canScrollHorizontally(-1) ||
						host.canScrollHorizontally(1));
				event.setScrollX(host.getScrollX());
				event.setScrollY(host.getScrollY());
				event.setMaxScrollX(Math.max(0,
						view.computeHorizontalScrollRange() - host.getWidth()));
				event.setMaxScrollY(Math.max(0,
						view.computeVerticalScrollRange() - host.getHeight()));
			}
			default -> {
			}
		}
	}

	@Override
	public void sendAccessibilityEvent(@NonNull View host, int eventType) {
		if (eventType ==
				AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED) {
			view.forgetExplored();
		}
		super.sendAccessibilityEvent(host, eventType);
	}

	@Override
	public void onPopulateAccessibilityEvent(@NonNull View host,
			@NonNull AccessibilityEvent event) {
		super.onPopulateAccessibilityEvent(host, event);
		Document document = view.getDocument();
		if (document == null) {
			return;
		}
		Document.Selection selection = document.selection();
		Text all = document.text();
		String text = event.getEventType() ==
				AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
				? toldText(all, told(all, selection.start(), selection.end()))
				: text(all, reading(all, selection));
		if (!text.isEmpty()) {
			event.getText().add(text);
		}
	}

	// As TextView does when its text is set.
	void documentChanged() {
		Document document = view.getDocument();
		reported = document != null ? document.selection() : null;
		ViewParent parent = view.getParent();
		if (parent != null && isEnabled()) {
			parent.notifySubtreeAccessibilityStateChanged(view, view,
					AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT);
		}
	}

	// As TextView does while it has focus.
	void changed(Document.Change change, Text before) {
		ViewParent parent = view.getParent();
		Document document = view.getDocument();
		if (parent == null || document == null || !view.isFocused() ||
				!view.isShown() || !isEnabled()) {
			return;
		}
		Text after = document.text();
		AccessibleText.Edit edit = AccessibleText.edit(before, after, change);
		AccessibilityEvent event = AccessibilityEvent.obtain(
				AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
		view.onInitializeAccessibilityEvent(event);
		event.setBeforeText(before.substring(edit.before().start(),
				edit.before().end()));
		String text = text(after, edit.after());
		if (!text.isEmpty()) {
			event.getText().add(text);
		}
		event.setFromIndex(edit.from());
		event.setRemovedCount(edit.removed());
		event.setAddedCount(edit.added());
		parent.requestSendAccessibilityEvent(view, event);
	}

	void updated() {
		Document document = view.getDocument();
		if (document == null || !isEnabled()) {
			return;
		}
		Document.Selection selection = document.selection();
		if (!selection.equals(reported)) {
			reported = selection;
			view.sendAccessibilityEvent(
					AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED);
		}
	}

	@Override
	public boolean performAccessibilityAction(@NonNull View host, int action,
			Bundle arguments) {
		Document document = view.getDocument();
		if (document == null) {
			return super.performAccessibilityAction(host, action, arguments);
		}
		switch (action) {
			case AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY:
			case AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY:
				return arguments != null && traverse(document, action,
						arguments.getInt(AccessibilityNodeInfo
								.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT),
						arguments.getBoolean(AccessibilityNodeInfo
								.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN));
			case AccessibilityNodeInfo.ACTION_SET_SELECTION:
				return setSelection(document, arguments);
			case AccessibilityNodeInfo.ACTION_COPY:
				return textAction(host, android.R.id.copy);
			case AccessibilityNodeInfo.ACTION_PASTE:
				return textAction(host, android.R.id.paste);
			case AccessibilityNodeInfo.ACTION_CUT:
				return textAction(host, android.R.id.cut);
			case AccessibilityNodeInfo.ACTION_SET_TEXT:
				return view.replaceAll(arguments != null
						? arguments.getCharSequence(AccessibilityNodeInfo
						.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE)
						: null);
			case AccessibilityNodeInfo.ACTION_CLICK:
				return view.clickForAccessibility();
			case AccessibilityNodeInfo.ACTION_LONG_CLICK:
				return view.longClickForAccessibility();
			case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:
				return view.scrollByPage(0, 1);
			case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:
				return view.scrollByPage(0, -1);
			default:
				break;
		}
		if (action == android.R.id.accessibilityActionScrollDown) {
			return view.scrollByPage(0, 1);
		} else if (action == android.R.id.accessibilityActionScrollUp) {
			return view.scrollByPage(0, -1);
		} else if (action == android.R.id.accessibilityActionScrollRight) {
			return view.scrollByPage(1, 0);
		} else if (action == android.R.id.accessibilityActionScrollLeft) {
			return view.scrollByPage(-1, 0);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
				action == android.R.id.accessibilityActionImeEnter) {
			if (host.isFocused()) {
				view.enter();
			}
			return true;
		}
		return super.performAccessibilityAction(host, action, arguments);
	}

	// The window of the last selection event if it holds from to to in text,
	// else the window of a change there.
	private AccessibleText.Window told(Text text, int from, int to) {
		if (text != toldOf || from < toldWindow.start() ||
				to > toldWindow.end()) {
			toldOf = text;
			toldWindow = AccessibleText.window(text, from, to);
			toldText = null;
		}
		return toldWindow;
	}

	private String toldText(Text text, AccessibleText.Window window) {
		if (toldText == null) {
			toldText = text.substring(window.start(), window.end());
		}
		return toldText;
	}

	private static AccessibleText.Window reading(Text text,
			Document.Selection selection) {
		return AccessibleText.reading(text, selection.start(),
				selection.end());
	}

	private String text(Text text, AccessibleText.Window window) {
		if (text != windowOf || !window.equals(this.window)) {
			windowOf = text;
			this.window = window;
			windowText = text.substring(window.start(), window.end());
		}
		return windowText;
	}

	// Moves the selection's head over the next or previous segment of
	// granularity, as View.traverseAtGranularity does.
	private boolean traverse(Document document, int action,
			int granularity, boolean extend) {
		AccessibleText.Unit unit = unit(granularity);
		Text text = document.text();
		if (unit == null || text.length() == 0) {
			return false;
		}
		Document.Selection selection = document.selection();
		boolean forward =
				action == AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY;
		AccessibleText.Segment segment = forward
				? AccessibleText.following(motion, text, unit,
				selection.head(), view.pageLines())
				: AccessibleText.preceding(motion, text, unit,
				selection.head(), view.pageLines());
		if (segment == null) {
			return false;
		}
		int head = forward ? segment.end() : segment.start();
		view.selectForAccessibility(extend ? selection.anchor() : head, head,
				false);
		sendTraversed(text, segment, action, granularity);
		return true;
	}

	private void sendTraversed(Text text, AccessibleText.Segment segment,
			int action, int granularity) {
		ViewParent parent = view.getParent();
		if (parent == null || !isEnabled()) {
			return;
		}
		// A service reads it against the node it gets for it.
		AccessibleText.Window window = reading(text,
				view.getDocument().selection());
		AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent
				.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY);
		view.onInitializeAccessibilityEvent(event);
		event.getText().add(text(text, window));
		event.setFromIndex(window.index(segment.start()));
		event.setToIndex(window.index(segment.end()));
		event.setAction(action);
		event.setMovementGranularity(granularity);
		parent.requestSendAccessibilityEvent(view, event);
	}

	// Selects as TextView does: in the window of the node, start before
	// end, the selection shown with its toolbar.
	private boolean setSelection(Document document, Bundle arguments) {
		int start = arguments != null ? arguments.getInt(
				AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
				-1) : -1;
		int end = arguments != null ? arguments.getInt(
				AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
				-1) : -1;
		Document.Selection selection = document.selection();
		AccessibleText.Window window = reading(document.text(), selection);
		if (start == window.index(selection.anchor()) &&
				end == window.index(selection.head())) {
			return false;
		}
		if (start == -1 && end == -1) {
			// TextView removes the selection; an editor keeps its caret.
			return true;
		}
		if (start < 0 || start > end || end > window.length()) {
			return false;
		}
		view.selectForAccessibility(window.start() + start,
				window.start() + end, true);
		return true;
	}

	private boolean textAction(View host, int id) {
		return host.isFocused() && offers(view.offered(), id) &&
				view.toolbarAction(id);
	}

	private boolean isEnabled() {
		AccessibilityManager manager = manager();
		return manager != null && manager.isEnabled();
	}

	// A service tells of a selection it made itself, not of one made by its
	// long press.
	void selectedByTouch() {
		Document document = view.getDocument();
		if (document == null) {
			return;
		}
		Document.Selection selection = document.selection();
		int end = Math.min(selection.end(), selection.start() + SAID_CHARS);
		view.announceForAccessibility(view.getContext().getString(
				R.string.a11y_selected, document.text().substring(
						selection.start(), end)));
	}

	boolean exploresByTouch() {
		AccessibilityManager manager = manager();
		return manager != null && manager.isTouchExplorationEnabled();
	}

	private AccessibilityManager manager() {
		return (AccessibilityManager) view.getContext().getSystemService(
				Context.ACCESSIBILITY_SERVICE);
	}

	private static void addScrollActions(View host,
			AccessibilityNodeInfo info) {
		boolean up = host.canScrollVertically(-1);
		boolean down = host.canScrollVertically(1);
		boolean left = host.canScrollHorizontally(-1);
		boolean right = host.canScrollHorizontally(1);
		info.setScrollable(up || down || left || right);
		if (up) {
			info.addAction(AccessibilityAction.ACTION_SCROLL_BACKWARD);
			info.addAction(AccessibilityAction.ACTION_SCROLL_UP);
		}
		if (down) {
			info.addAction(AccessibilityAction.ACTION_SCROLL_FORWARD);
			info.addAction(AccessibilityAction.ACTION_SCROLL_DOWN);
		}
		if (left) {
			info.addAction(AccessibilityAction.ACTION_SCROLL_LEFT);
		}
		if (right) {
			info.addAction(AccessibilityAction.ACTION_SCROLL_RIGHT);
		}
	}

	private static boolean offers(int[] offered, int id) {
		for (int o : offered) {
			if (o == id) {
				return true;
			}
		}
		return false;
	}

	private static AccessibleText.Unit unit(int granularity) {
		return switch (granularity) {
			case AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER ->
					AccessibleText.Unit.CHARACTER;
			case AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD ->
					AccessibleText.Unit.WORD;
			case AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE ->
					AccessibleText.Unit.LINE;
			case AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH ->
					AccessibleText.Unit.PARAGRAPH;
			case AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE ->
					AccessibleText.Unit.PAGE;
			default -> null;
		};
	}
}
