package de.markusfisch.android.shadereditor.widget;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EdgeEffect;
import android.widget.OverScroller;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.editor.Document;
import de.markusfisch.android.shadereditor.editor.EditCommands;
import de.markusfisch.android.shadereditor.editor.KeyCommands;
import de.markusfisch.android.shadereditor.editor.LineStyles;
import de.markusfisch.android.shadereditor.editor.LineStylesScheduler;
import de.markusfisch.android.shadereditor.editor.Text;
import de.markusfisch.android.shadereditor.editor.TextInput;

// Shows a Document in the colours of its LineStyles; it owns no text. Only the
// lines on screen are laid out, and from Android 10 on a line's drawing is
// recorded once and replayed until its string or colours change. The app runs
// from Android 6 on, so what later versions add is used behind version checks.
public class CodeView extends View {
	// As the old editor with native suggestions hidden: TYPE_NULL tells a
	// keyboard the field is no text editor, so Gboard sends raw key events.
	public static final int INPUT_TYPE_NO_SUGGESTIONS = InputType.TYPE_NULL;
	// Not NO_SUGGESTIONS, as the old editor keeps: the AOSP keyboard and
	// Gboard then offer none.
	public static final int INPUT_TYPE_TEXT = InputType.TYPE_CLASS_TEXT |
			InputType.TYPE_TEXT_FLAG_MULTI_LINE;

	// One thread that ends when it has nothing to do.
	private static final Executor STYLING = new ThreadPoolExecutor(0, 1, 1,
			TimeUnit.SECONDS, new LinkedBlockingQueue<>(), task ->
			new Thread(() -> {
				Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
				task.run();
			}, "LineStyles"));
	private static final long BLINK = 500;
	// What the clipboard takes: a clip goes to the system, and from it
	// to every app that reads it, in binder transactions of at most
	// 1 MB, at up to two bytes a character.
	private static final int MAX_CLIP = 400_000;
	private static final int HORIZONTAL = 1;
	private static final int VERTICAL = 2;

	private final CodeAppearance appearance;
	private final LineGeometry lines = new LineGeometry();
	private final Paint fill = new Paint();
	private final LineCache cache;
	private final char[] digits = new char[10];
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Document.Listener listener = new Document.Listener() {
		@Override
		public void onChange(Document.Change change, Text before) {
			invalidate();
		}

		@Override
		public void onUpdate() {
			restartBlink();
			scrollInside(getScrollX(), getScrollY());
			invalidate();
		}
	};
	private final Runnable blink = this::blink;
	private final KeyCommands.Clipboard clipboard =
			new KeyCommands.Clipboard() {
				@Override
				public Text read() {
					ClipData.Item item = clipItem();
					try {
						CharSequence clip = item != null
								? item.coerceToText(getContext())
								: null;
						return clip != null ? Text.of(clip) : null;
					} catch (RuntimeException e) {
						return null;
					}
				}

				@Override
				public boolean set(Text text, int start, int end) {
					ClipboardManager manager = clipboardManager();
					try {
						if (end - start <= MAX_CLIP) {
							manager.setPrimaryClip(ClipData.newPlainText(null,
									text.substring(start, end)));
							return true;
						}
					} catch (RuntimeException e) {
						// Too large after all.
					}
					Toast.makeText(getContext(), R.string.copy_failed,
							Toast.LENGTH_SHORT).show();
					return false;
				}
			};
	private final OverScroller scroller;
	private final GestureDetector gestures;
	private final EdgeEffect edgeTop;
	private final EdgeEffect edgeBottom;
	private final EdgeEffect edgeLeft;
	private final EdgeEffect edgeRight;
	private boolean caretIntoViewWhenLaidOut;
	private int axis;
	private Document document;
	private EditCommands commands;
	private KeyCommands keys;
	private int deadChar;
	private int inputType = INPUT_TYPE_NO_SUGGESTIONS;
	// The keyboard's open connections. It may go on using one after it
	// was given a newer one.
	private final ArrayList<CodeInputConnection> connections =
			new ArrayList<>();
	// The connection the keyboard used last; only its updates reach it.
	private CodeInputConnection active;
	private LineStyles styles;
	private LineStylesScheduler scheduler;
	private boolean tabIndents = true;
	private boolean showLineNumbers = true;
	private int composingStart = -1;
	private int composingEnd = -1;
	private boolean caretOn;
	private boolean caretShown;
	private float caretX;
	private float caretTop;
	private float caretClipLeft;
	// From Android 10 on, the drawing of everything but the caret,
	// replayed when only the caret blinked. Typed Object, as gutter, so
	// only code behind a version check names RenderNode.
	private Object content;
	private boolean caretOnly;
	// From Android 10 on, whole blocks of line numbers, recorded until a line
	// of another block comes into view.
	private Object gutter;
	private int gutterFirst;
	private int gutterLast = -1;
	private int gutterDigits;
	private LineCache.Line[] shown = new LineCache.Line[0];
	private int shownFirst;
	private int shownCount;
	private float shownWidest;

	public CodeView(Context context) {
		this(context, null);
	}

	public CodeView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs, 0, R.style.CodeView);
		appearance = new CodeAppearance(context);
		lines.setLineHeight(appearance.lineHeight);
		cache = new LineCache(appearance);
		setFocusable(true);
		setFocusableInTouchMode(true);
		scroller = new OverScroller(context);
		edgeTop = new EdgeEffect(context);
		edgeBottom = new EdgeEffect(context);
		edgeLeft = new EdgeEffect(context);
		edgeRight = new EdgeEffect(context);
		gestures = new GestureDetector(context,
				new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onDown(@NonNull MotionEvent e) {
						scroller.forceFinished(true);
						axis = 0;
						return true;
					}

					@Override
					public boolean onSingleTapUp(@NonNull MotionEvent e) {
						placeCaret(e.getX(), e.getY());
						return true;
					}

					@Override
					public boolean onScroll(@Nullable MotionEvent down,
							@NonNull MotionEvent e, float dx, float dy) {
						if (axis == 0) {
							axis = down != null
									? dominant(e.getX() - down.getX(),
											e.getY() - down.getY())
									: dominant(dx, dy);
						}
						drag(axis == HORIZONTAL ? dx : 0,
								axis == VERTICAL ? dy : 0, e);
						return true;
					}

					@Override
					public boolean onFling(@Nullable MotionEvent down,
							@NonNull MotionEvent e, float vx, float vy) {
						fling(axis == HORIZONTAL ? (int) -vx : 0,
								axis == VERTICAL ? (int) -vy : 0);
						return true;
					}
				});
	}

	public void setDocument(@NonNull Document document,
			@NonNull LineStyles styles) {
		disconnect();
		closeInput();
		this.document = document;
		this.styles = styles;
		commands = new EditCommands(document);
		commands.setTabWidth(appearance.tabWidth);
		commands.setIndentation(styles.syntax().indentation());
		keys = new KeyCommands(document, commands, clipboard);
		deadChar = 0;
		composingStart = composingEnd = -1;
		forgetLines();
		scrollTo(0, 0);
		if (isAttachedToWindow()) {
			connect();
			restartInput();
		}
		invalidate();
	}

	public void setInputType(int inputType) {
		this.inputType = inputType;
		restartInput();
	}

	@Nullable
	public Document getDocument() {
		return document;
	}

	// Underlines what composes; -1 for none.
	private void showComposing(int start, int end) {
		composingStart = start;
		composingEnd = end;
		invalidate();
	}

	public void setTypeface(@NonNull Typeface typeface) {
		appearance.paint.setTypeface(typeface);
		appearanceChanged();
	}

	public void setTextSize(int unit, float size) {
		appearance.paint.setTextSize(TypedValue.applyDimension(unit, size,
				getResources().getDisplayMetrics()));
		appearanceChanged();
	}

	public void setFontFeatureSettings(@Nullable String settings) {
		appearance.paint.setFontFeatureSettings(settings);
		appearanceChanged();
	}

	public void setTabWidth(int tabWidth) {
		appearance.tabWidth = tabWidth;
		if (commands != null) {
			commands.setTabWidth(tabWidth);
		}
		appearanceChanged();
	}

	public void setShowLineNumbers(boolean showLineNumbers) {
		this.showLineNumbers = showLineNumbers;
		invalidate();
	}

	// True by default; false leaves Tab to move the focus, as the platform
	// does.
	public void setTabIndents(boolean tabIndents) {
		this.tabIndents = tabIndents;
	}

	public void bringCaretIntoView() {
		if (document == null) {
			return;
		}
		if (getWidth() == 0 || getHeight() == 0) {
			caretIntoViewWhenLaidOut = true;
			return;
		}
		caretIntoViewWhenLaidOut = false;
		Text text = document.text();
		int head = document.selection().head();
		int n = text.lineOf(head);
		float x = textLeft(text.lineCount()) +
				layoutOf(n).xOf(head - text.lineStart(n));
		float margin = appearance.charWidth * 4;
		int scrollX = getScrollX();
		int scrollY = getScrollY();
		float visibleLeft = scrollX + textLeft(text.lineCount());
		float visibleRight = scrollX + getWidth();
		if (x - margin < visibleLeft) {
			scrollX -= (int) Math.ceil(visibleLeft - x + margin);
		} else if (x + margin > visibleRight) {
			scrollX += (int) Math.ceil(x + margin - visibleRight);
		}
		scrollY = revealLine(scrollY, n);
		scroller.forceFinished(true);
		scrollInside(scrollX, scrollY);
	}

	// In pixels per second.
	private void fling(int velocityX, int velocityY) {
		scroller.fling(getScrollX(), getScrollY(), velocityX, velocityY,
				0, keptMaxScrollX(), 0, maxScrollY());
		postInvalidateOnAnimation();
	}

	// Kept inside the range of the text.
	@Override
	public void scrollTo(int x, int y) {
		caretIntoViewWhenLaidOut = false;
		scrollInside(x, y);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (caretIntoViewWhenLaidOut || (h < oldh && isFocused())) {
			bringCaretIntoView();
		} else {
			scrollInside(getScrollX(), getScrollY());
		}
	}

	@Override
	public void computeScroll() {
		if (!scroller.computeScrollOffset()) {
			return;
		}
		int y = scroller.getCurrY();
		int velocity = (int) scroller.getCurrVelocity();
		if (y <= 0 && getScrollY() > 0) {
			edgeTop.onAbsorb(velocity);
		} else if (y >= maxScrollY() && getScrollY() < maxScrollY()) {
			edgeBottom.onAbsorb(velocity);
		}
		caretIntoViewWhenLaidOut = false;
		scrollInside(scroller.getCurrX(), y, true);
		postInvalidateOnAnimation();
	}

	// The gesture detector calls performClick for a tap.
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return touch(event);
	}

	private boolean touch(MotionEvent event) {
		int action = event.getActionMasked();
		boolean handled = gestures.onTouchEvent(event);
		if (action == MotionEvent.ACTION_UP ||
				action == MotionEvent.ACTION_CANCEL) {
			edgeTop.onRelease();
			edgeBottom.onRelease();
			edgeLeft.onRelease();
			edgeRight.onRelease();
			postInvalidateOnAnimation();
		}
		return handled || super.onTouchEvent(event);
	}

	@Override
	public boolean onCheckIsTextEditor() {
		return true;
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		if (document == null) {
			return null;
		}
		outAttrs.inputType = inputType;
		outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI |
				EditorInfo.IME_FLAG_NO_FULLSCREEN |
				EditorInfo.IME_FLAG_NO_ENTER_ACTION;
		Document.Selection selection = document.selection();
		outAttrs.initialSelStart = selection.anchor();
		outAttrs.initialSelEnd = selection.head();
		outAttrs.initialCapsMode = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
				selection.end() - selection.start() <= TextInput.MAX_READ) {
			Text text = document.text();
			int from = Math.max(0, selection.start() - TextInput.MAX_READ / 4);
			outAttrs.setInitialSurroundingSubText(text.substring(from,
					Math.min(text.length(),
							selection.end() + TextInput.MAX_READ / 4)), from);
		}
		CodeInputConnection connection = new CodeInputConnection(this,
				document);
		connections.add(connection);
		if (active == null) {
			active = connection;
		}
		return connection;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keys == null ||
				(keyCode == KeyEvent.KEYCODE_TAB && !tabIndents)) {
			return super.onKeyDown(keyCode, event);
		}
		int meta = event.getMetaState();
		int c = event.getUnicodeChar(meta);
		if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0 &&
				KeyCommands.types(meta)) {
			deadChar = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
			return true;
		}
		String typed = c != 0 ? new String(Character.toChars(c)) : null;
		if (deadChar != 0 && KeyCommands.command(keyCode, meta) != null) {
			deadChar = 0;
		} else if (deadChar != 0 && c != 0) {
			// An accent that does not combine stays in front, as on Android.
			int combined = KeyEvent.getDeadChar(deadChar, c);
			typed = combined != 0
					? new String(Character.toChars(combined))
					: new String(Character.toChars(deadChar)) + typed;
			deadChar = 0;
		}
		return key(keyCode, meta, typed) || super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		String characters = event.getCharacters();
		return keyCode == KeyEvent.KEYCODE_UNKNOWN && characters != null &&
				keys != null && key(keyCode, 0, characters) ||
				super.onKeyMultiple(keyCode, count, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		int meta = event.getMetaState();
		return keys != null &&
				(keyCode != KeyEvent.KEYCODE_TAB || tabIndents) &&
				(KeyCommands.command(keyCode, meta) != null ||
				KeyCommands.types(meta) && event.getUnicodeChar(meta) != 0) ||
				super.onKeyUp(keyCode, event);
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		super.draw(canvas);
		int w = getWidth();
		int h = getHeight();
		boolean more = drawEdge(canvas, edgeTop, 0, 0, 0, w, h);
		more |= drawEdge(canvas, edgeBottom, w, h, 180, w, h);
		more |= drawEdge(canvas, edgeLeft, 0, h, 270, h, w);
		more |= drawEdge(canvas, edgeRight, w, 0, 90, h, w);
		if (more) {
			postInvalidateOnAnimation();
		}
	}

	@Override
	protected int computeVerticalScrollRange() {
		return document == null ? 0 : maxScrollY() + getHeight();
	}

	@Override
	protected int computeHorizontalScrollRange() {
		return document == null ? 0 : maxScrollX() + getWidth();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (document != null) {
			connect();
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		disconnect();
		closeInput();
		super.onDetachedFromWindow();
	}

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction,
			@Nullable Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		deadChar = 0;
		restartBlink();
		invalidate();
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		restartBlink();
		invalidate();
	}

	@Override
	public void invalidate() {
		caretOnly = false;
		super.invalidate();
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		if (document == null) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
				canvas.isHardwareAccelerated()) {
			drawRecorded(canvas);
		} else {
			drawText(canvas);
		}
		drawCaret(canvas);
	}

	@RequiresApi(Build.VERSION_CODES.Q)
	private void drawRecorded(Canvas canvas) {
		if (content == null) {
			content = new RenderNode("text");
		}
		RenderNode node = (RenderNode) content;
		int scrollX = getScrollX();
		int scrollY = getScrollY();
		if (!caretOnly || !node.hasDisplayList() ||
				node.getLeft() != scrollX || node.getTop() != scrollY ||
				node.getWidth() != getWidth() ||
				node.getHeight() != getHeight()) {
			node.setPosition(scrollX, scrollY, scrollX + getWidth(),
					scrollY + getHeight());
			RecordingCanvas recording = node.beginRecording();
			try {
				recording.translate(-scrollX, -scrollY);
				drawText(recording);
			} finally {
				node.endRecording();
			}
		}
		canvas.drawRenderNode(node);
	}

	private void drawCaret(Canvas canvas) {
		if (!caretOn || !caretShown) {
			return;
		}
		CodeAppearance a = appearance;
		int scrollY = getScrollY();
		int save = canvas.save();
		canvas.clipRect(caretClipLeft, scrollY, getScrollX() + getWidth(),
				scrollY + getHeight());
		fill.setColor(a.caretColor);
		canvas.drawRect(caretX - a.caretWidth / 2, caretTop,
				caretX + a.caretWidth / 2, caretTop + a.lineHeight, fill);
		canvas.restoreToCount(save);
	}

	private void drawText(Canvas canvas) {
		cache.nextFrame();
		CodeAppearance a = appearance;
		Text text = document.text();
		int lineCount = text.lineCount();
		int lineHeight = a.lineHeight;
		int scrollX = getScrollX();
		int scrollY = getScrollY();
		int right = scrollX + getWidth();
		float textTop = textTop();
		float textLeft = textLeft(lineCount);
		float clipLeft = showLineNumbers
				? scrollX + textLeft - a.padding / 2
				: scrollX;
		int first = lineAt(scrollY);
		int last = lineAt(scrollY + getHeight());
		if (styles != null) {
			styles.show(first, last + 1);
		}
		show(first, last + 1);
		Document.Selection selection = document.selection();
		int selStart = selection.start();
		int selEnd = selection.end();
		int save = canvas.save();
		canvas.clipRect(clipLeft, scrollY, right, scrollY + getHeight());
		caretShown = false;
		caretClipLeft = clipLeft;
		int lineStart = text.lineStart(first);
		// A frame records a third of the new lines and draws the others
		// directly, which costs less than recording them.
		int recordable = (last - first + 3) / 3;
		boolean unrecorded = false;
		shownWidest = 0;
		for (int n = first; n <= last; ++n) {
			String s = text.line(n);
			LineCache.Line line = cache.get(s,
					styles != null ? styles.runs(n) : null, true);
			shown[n - first] = line;
			LineLayout layout = line.layout();
			shownWidest = Math.max(shownWidest, layout.width());
			float top = textTop + lines.top(n);
			int lineEnd = lineStart + s.length();
			canvas.translate(textLeft, top);
			if (selStart < selEnd && selStart <= lineEnd &&
					selEnd > lineStart) {
				fill.setColor(a.selectionColor);
				int from = Math.max(selStart, lineStart) - lineStart;
				int to = Math.min(selEnd, lineEnd) - lineStart;
				if (from < to) {
					layout.drawSelection(canvas, from, to, fill);
				}
				if (selEnd > lineEnd) {
					// The line break.
					float x = layout.xOf(s.length());
					canvas.drawRect(x, 0, x + a.charWidth, lineHeight, fill);
				}
			}
			if (!s.isEmpty()) {
				boolean record = recordable > 0;
				if (line.needsRecording(canvas)) {
					if (record) {
						--recordable;
					} else {
						unrecorded = true;
					}
				}
				line.draw(canvas, clipLeft - textLeft, right - textLeft,
						lineHeight, record);
			}
			if (composingStart < composingEnd &&
					composingStart <= lineEnd && composingEnd > lineStart) {
				fill.setColor(a.textColor);
				canvas.drawRect(
						layout.xOf(Math.max(composingStart, lineStart) -
								lineStart),
						lineHeight - a.underline,
						layout.xOf(Math.min(composingEnd, lineEnd) -
								lineStart),
						lineHeight, fill);
			}
			if (selStart == selEnd && selStart >= lineStart &&
					selStart <= lineEnd) {
				caretShown = true;
				caretX = textLeft + layout.xOf(selStart - lineStart);
				caretTop = top;
			}
			canvas.translate(-textLeft, -top);
			lineStart = lineEnd + 1;
		}
		canvas.restoreToCount(save);
		if (showLineNumbers) {
			drawLineNumbers(canvas, first, last, scrollX + gutterLeft(),
					textTop, lineCount);
		}
		if (unrecorded) {
			// Record the lines drawn directly in the frames to come.
			postInvalidateOnAnimation();
		}
	}

	float textTop() {
		return getPaddingTop();
	}

	// Without scrolling.
	float lineTop(int n) {
		return textTop() + lines.top(n);
	}

	// Without scrolling.
	int lineAt(float y) {
		return lines.lineAt(y - textTop(), document.text().lineCount());
	}

	float gutterLeft() {
		return getPaddingLeft();
	}

	// Without scrolling.
	float textLeft(int lineCount) {
		return gutterLeft() + (showLineNumbers
				? CodeGeometry.gutterWidth(lineCount, appearance.digitWidth,
						appearance.padding)
				: 0);
	}

	// A line on screen may be wider than its characters, for a tab or a
	// wide character; only those count, so the width shrinks with the text.
	float textWidth() {
		return Math.max(shownWidest,
				document.text().longestLineLength() * appearance.charWidth);
	}

	LineLayout layoutOf(int n) {
		return cache.get(document.text().line(n),
				styles != null ? styles.runs(n) : null, false).layout();
	}

	private int maxScrollY() {
		return lines.maxScroll(document.text().lineCount(), textTop(),
				getPaddingBottom(), getHeight());
	}

	private int maxScrollX() {
		float right = textLeft(document.text().lineCount()) + textWidth() +
				appearance.charWidth + getPaddingRight();
		return Math.max(0, (int) Math.ceil(right - getWidth()));
	}

	// For a scroll of the user: the lines on screen, whose tabs and wide
	// characters set the width, change as it moves up and down, and that
	// must not move it sideways.
	private int keptMaxScrollX() {
		return Math.max(getScrollX(), maxScrollX());
	}

	private void drag(float dx, float dy, MotionEvent e) {
		float x = getScrollX() + dx;
		float y = getScrollY() + dy;
		int maxX = keptMaxScrollX();
		int maxY = maxScrollY();
		float w = getWidth();
		float h = getHeight();
		if (y < 0) {
			edgeTop.onPull(-y / h, e.getX() / w);
		} else if (y > maxY) {
			edgeBottom.onPull((y - maxY) / h, 1 - e.getX() / w);
		}
		if (x < 0) {
			edgeLeft.onPull(-x / w, 1 - e.getY() / h);
		} else if (x > maxX) {
			edgeRight.onPull((x - maxX) / w, e.getY() / h);
		}
		caretIntoViewWhenLaidOut = false;
		scrollInside(Math.round(x), Math.round(y), true);
		postInvalidateOnAnimation();
	}

	// For a line break the keyboard commits.
	void enter() {
		if (keys != null) {
			commands.enter();
			bringCaretIntoView();
		}
	}

	public void tab() {
		if (keys != null) {
			key(KeyEvent.KEYCODE_TAB, 0, null);
		}
	}

	boolean contextMenuAction(int id) {
		KeyCommands.Command command = KeyCommands.contextMenuCommand(id);
		if (command != null) {
			menuCommand(command);
		}
		return command != null;
	}

	private boolean menuCommand(KeyCommands.Command command) {
		if (keys == null) {
			return false;
		}
		boolean done = keys.run(command, false, 0);
		bringCaretIntoView();
		return done;
	}

	int pageLines() {
		return Math.max(1, getHeight() / appearance.lineHeight - 1);
	}

	private void restartInput() {
		InputMethodManager imm = inputMethodManager();
		if (imm != null) {
			imm.restartInput(this);
		}
	}

	// The keyboard does not go back to the older ones, so they are closed.
	void use(CodeInputConnection connection) {
		if (active != connection) {
			active = connection;
			for (int i = connections.indexOf(connection); i-- > 0; ) {
				connections.get(i).closeConnection();
			}
			showComposing(connection.composingStart(),
					connection.composingEnd());
		}
	}

	void updateSelection(CodeInputConnection connection, int selStart,
			int selEnd, int composingStart, int composingEnd) {
		InputMethodManager imm = inputMethodManager();
		if (connection == active && imm != null) {
			showComposing(composingStart, composingEnd);
			imm.updateSelection(this, selStart, selEnd, composingStart,
					composingEnd);
		}
	}

	void updateExtractedText(CodeInputConnection connection, int token,
			TextInput.Window window) {
		InputMethodManager imm = inputMethodManager();
		if (connection == active && imm != null) {
			imm.updateExtractedText(this, token,
					CodeInputConnection.extracted(window));
		}
	}

	void closed(CodeInputConnection connection) {
		connections.remove(connection);
		if (connection == active) {
			active = connections.isEmpty()
					? null
					: connections.get(connections.size() - 1);
			showComposing(-1, -1);
		}
	}

	private void closeInput() {
		for (int i = connections.size(); i-- > 0; ) {
			connections.get(i).closeConnection();
		}
	}

	private InputMethodManager inputMethodManager() {
		return (InputMethodManager) getContext().getSystemService(
				Context.INPUT_METHOD_SERVICE);
	}

	private boolean key(int keyCode, int meta, String typed) {
		if (!keys.keyDown(keyCode, meta, typed, pageLines())) {
			return false;
		}
		bringCaretIntoView();
		return true;
	}

	// Null if there is none or it cannot be read.
	private ClipData.Item clipItem() {
		try {
			ClipboardManager manager = clipboardManager();
			ClipData clip = manager != null ? manager.getPrimaryClip() : null;
			return clip != null && clip.getItemCount() > 0
					? clip.getItemAt(0)
					: null;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private ClipboardManager clipboardManager() {
		return (ClipboardManager) getContext().getSystemService(
				Context.CLIPBOARD_SERVICE);
	}

	private boolean drawEdge(Canvas canvas, EdgeEffect edge, int x, int y,
			int degrees, int width, int height) {
		if (edge.isFinished()) {
			return false;
		}
		int save = canvas.save();
		canvas.translate(getScrollX() + x, getScrollY() + y);
		canvas.rotate(degrees);
		edge.setSize(width, height);
		boolean more = edge.draw(canvas);
		canvas.restoreToCount(save);
		return more;
	}

	private void placeCaret(float x, float y) {
		if (document == null) {
			return;
		}
		int offset = offsetAt(x, y);
		document.select(offset, offset);
		requestFocus();
		InputMethodManager imm = inputMethodManager();
		if (imm != null) {
			imm.showSoftInput(this, 0);
		}
		performClick();
	}

	private int offsetAt(float x, float y) {
		Text text = document.text();
		int n = lineAt(y + getScrollY());
		int offset = layoutOf(n).offsetAt(x + getScrollX() -
				textLeft(text.lineCount()));
		return text.lineStart(n) + Math.max(0, Math.min(offset,
				text.line(n).length()));
	}

	private void scrollInside(int x, int y) {
		scrollInside(x, y, false);
	}

	// kept for a scroll of the user, see keptMaxScrollX.
	private void scrollInside(int x, int y, boolean kept) {
		super.scrollTo(document == null ? 0 : clamp(x, kept
						? keptMaxScrollX()
						: maxScrollX()),
				document == null ? 0 : clamp(y, maxScrollY()));
	}

	private static int dominant(float dx, float dy) {
		return Math.abs(dx) > Math.abs(dy) ? HORIZONTAL : VERTICAL;
	}

	private static int clamp(int value, int max) {
		return Math.max(0, Math.min(value, max));
	}

	private void drawLineNumbers(Canvas canvas, int first, int last,
			float left, float textTop, int lineCount) {
		int digitCount = CodeGeometry.digits(lineCount);
		boolean recorded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
				canvas.isHardwareAccelerated();
		if (recorded) {
			first = CodeGeometry.numbersFrom(first);
			last = CodeGeometry.numbersTo(last, lineCount);
		}
		float top = textTop + lines.top(first);
		int save = canvas.save();
		if (recorded) {
			// The lines of the blocks that are out of view.
			int scrollX = getScrollX();
			int scrollY = getScrollY();
			canvas.clipRect(scrollX, scrollY, scrollX + getWidth(),
					scrollY + getHeight());
		}
		canvas.translate(left, top);
		if (recorded) {
			drawRecordedNumbers(canvas, first, last, digitCount);
		} else {
			drawNumbers(canvas, first, last, digitCount);
		}
		canvas.restoreToCount(save);
	}

	@RequiresApi(Build.VERSION_CODES.Q)
	private void drawRecordedNumbers(Canvas canvas, int first, int last,
			int digitCount) {
		if (gutter == null) {
			gutter = new RenderNode("gutter");
		}
		RenderNode node = (RenderNode) gutter;
		if (first != gutterFirst || last != gutterLast ||
				digitCount != gutterDigits ||
				!node.hasDisplayList()) {
			node.setPosition(0, 0,
					(int) Math.ceil(digitCount * appearance.digitWidth) + 1,
					(int) Math.ceil(lines.top(last) - lines.top(first)) +
							appearance.lineHeight);
			node.setClipToBounds(false);
			RecordingCanvas recording = node.beginRecording();
			try {
				drawNumbers(recording, first, last, digitCount);
			} finally {
				node.endRecording();
			}
			gutterFirst = first;
			gutterLast = last;
			gutterDigits = digitCount;
		}
		canvas.drawRenderNode(node);
	}

	private void drawNumbers(Canvas canvas, int first, int last,
			int digitCount) {
		CodeAppearance a = appearance;
		Paint paint = a.paint;
		float right = digitCount * a.digitWidth;
		float firstTop = lines.top(first);
		paint.setColor(a.lineNumberColor);
		for (int n = first; n <= last; ++n) {
			int start = digits.length;
			for (int number = n + 1; number > 0; number /= 10) {
				digits[--start] = (char) ('0' + number % 10);
			}
			int count = digits.length - start;
			canvas.drawText(digits, start, count,
					right - count * a.digitWidth,
					lines.top(n) - firstTop + a.baseline, paint);
		}
	}

	private int revealLine(int scrollY, int n) {
		float textTop = textTop();
		return lines.reveal(scrollY, n, -textTop,
				getHeight() - textTop);
	}

	// Returns the caret's line if any of it is on screen, else the first
	// line on screen.
	private int anchorLine() {
		int caret = document.text().lineOf(document.selection().head());
		float top = lineTop(caret) - getScrollY();
		if (top + appearance.lineHeight > 0 && top < getHeight()) {
			return caret;
		}
		return lineAt(getScrollY());
	}

	private void connect() {
		document.addListener(listener);
		scheduler = new LineStylesScheduler(document, styles, STYLING,
				handler::post, this::restyled);
		restartBlink();
	}

	private void disconnect() {
		handler.removeCallbacks(blink);
		if (scheduler != null) {
			document.removeListener(listener);
			scheduler.close();
			scheduler = null;
		}
	}

	// Draws again only if a line on screen has other colours now.
	private void restyled() {
		Text text = document.text();
		for (int i = 0; i < shownCount; ++i) {
			int n = shownFirst + i;
			if (n >= text.lineCount() ||
					!shown[i].shows(text.line(n), styles.runs(n))) {
				invalidate();
				return;
			}
		}
	}

	// The line one reads stays where it is on screen.
	private void appearanceChanged() {
		boolean shown = document != null && getHeight() > 0;
		int anchor = shown ? anchorLine() : -1;
		float anchorTop = shown ? lineTop(anchor) - getScrollY() : 0;
		appearance.measure();
		lines.setLineHeight(appearance.lineHeight);
		forgetLines();
		shownWidest = 0;
		if (shown) {
			int y = clamp(Math.round(lineTop(anchor) - anchorTop),
					maxScrollY());
			// The widths of the last frame are of the old appearance.
			Text text = document.text();
			for (int i = lineAt(y), last = lineAt(y + getHeight());
					i <= last; ++i) {
				shownWidest = Math.max(shownWidest, cache.get(text.line(i),
						styles != null ? styles.runs(i) : null, false)
						.layout().width());
			}
			scrollInside(getScrollX(), y);
		}
		gutterLast = -1;
		invalidate();
	}

	private void show(int from, int to) {
		if (shown.length < to - from) {
			shown = new LineCache.Line[(to - from) * 2];
		}
		shownFirst = from;
		shownCount = to - from;
	}

	private void forgetLines() {
		cache.clear();
		shownCount = 0;
	}

	private void restartBlink() {
		handler.removeCallbacks(blink);
		caretOn = document != null && isAttachedToWindow() &&
				hasWindowFocus() && isFocused() &&
				document.selection().isEmpty();
		if (caretOn) {
			handler.postDelayed(blink, BLINK);
		}
	}

	private void blink() {
		caretOn = !caretOn;
		// Unless something else was invalidated since the last frame.
		caretOnly = caretOnly || !isDirty();
		super.invalidate();
		handler.postDelayed(blink, BLINK);
	}
}
