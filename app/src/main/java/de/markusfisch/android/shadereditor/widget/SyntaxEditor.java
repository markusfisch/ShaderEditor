package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Trace;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.highlighter.Highlight;
import de.markusfisch.android.shadereditor.highlighter.Lexer;
import de.markusfisch.android.shadereditor.highlighter.TokenType;
import de.markusfisch.android.shadereditor.util.IntList;

public class SyntaxEditor extends View implements TextWatcher {
	private static final int[] colors = new int[Highlight.values().length];
	private final List<IntList> tokensByLine = new ArrayList<>();
	private final Rect visibleRect = new Rect();
	private final Paint paint = new Paint();
	boolean needsEcho = false;
	private boolean textDirty = true;
	private @Nullable EditText source;
	private @NonNull TabSupplier tabSupplier = () -> 2;
	private @NonNull int[] tokens = new int[256];
	private String currentText = "";

	public SyntaxEditor(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		getViewTreeObserver().addOnScrollChangedListener(this::postInvalidate);
	}

	public static void initColors(@NonNull Context context) {
		for (Highlight highlight : Highlight.values()) {
			colors[highlight.ordinal()] = ContextCompat.getColor(context, highlight.id());
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		ViewGroup.LayoutParams layoutParams = getLayoutParams();
		setMeasuredDimension(layoutParams.width, layoutParams.height);
	}

	public void setSource(@NonNull EditText source) {
		if (this.source != null) {
			this.source.setLayoutParams(new ViewGroup.LayoutParams(getLayoutParams()));
			this.source.setOnScrollChangeListener(null);
			this.source.removeTextChangedListener(this);
		}
		this.source = source;
		source.setLayoutParams(getLayoutParams());
		setPadding(source.getPaddingLeft(), source.getPaddingTop(), source.getPaddingRight(), source.getPaddingBottom());
		source.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> setScrollY(scrollY));
		source.addTextChangedListener(this);
	}

	public void setTabSupplier(@NonNull TabSupplier tabSupplier) {
		this.tabSupplier = tabSupplier;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (source == null || textDirty) return;

		Trace.beginSection("Syntax Highlighting Draw");

		Trace.beginSection("Highlighting (Java Side)");
		paint.set(source.getPaint());
		Trace.endSection();

		if (currentText == null) return;

		Trace.beginSection("Measure Font");
		Paint.FontMetricsInt fm = paint.getFontMetricsInt();
		float charWidth = paint.measureText("m");
		Trace.endSection();

		int lineHeight = fm.bottom - fm.top + fm.leading;
		float paddingLeft = getPaddingLeft();
		float lineOffsetY = source.getExtendedPaddingTop() - fm.descent;

		Trace.beginSection("Visible Rect");
		getLocalVisibleRect(visibleRect);
		Trace.endSection();

		Trace.beginSection("Get visible lines");
		int firstLine = visibleRect.top / lineHeight;
		int lastLine = visibleRect.bottom / lineHeight;
		Trace.endSection();

		Trace.beginSection("Highlighting");
		int sourceMax = source.length();
		float currentY = firstLine * lineHeight + lineOffsetY;
		for (int line = firstLine; line <= lastLine; ++line, currentY += lineHeight) {
			if (line >= tokensByLine.size()) break;
			Trace.beginSection("Highlighting Line");
			for (int i : tokensByLine.get(line).getRaw()) {
				Trace.beginSection("Highlighting Token");
				TokenType type = TokenType.values()[tokens[i]];
				Highlight highlight = Highlight.from(type);
				int start = Math.min(sourceMax, tokens[i + 1]);
				int end = Math.min(sourceMax, tokens[i + 2]);
				int column = tokens[i + 4];
				if (needsEcho)
					Log.d("Token-echo", String.format("%s %d:%d | %d-%d", type.name(), line, column, start, end));
				paint.setColor(colors[highlight.ordinal()]);
				canvas.drawText(currentText, start, end, charWidth * column + paddingLeft, currentY, paint);
				Trace.endSection();
			}
			Trace.endSection();
		}
		needsEcho = false;
		Trace.endSection();

		super.onDraw(canvas); // draw normal text
		Trace.endSection();
	}

	private void highlight() {
		needsEcho = true;
		int maxColumn = 0;
		int maxLine = 0;
		if (source != null) paint.set(source.getPaint());
		Paint.FontMetricsInt fm = paint.getFontMetricsInt();
		float lineHeight = fm.bottom - fm.top + fm.leading;
		float charWidth = paint.measureText("m");
		tokens = Lexer.runLexer(currentText, tokens, tabSupplier.getWidth());
		tokensByLine.clear();
		int sourceMax = source.length();
		for (int i = 1, length = tokens[0]; i <= length; i += 5) {
			TokenType type = TokenType.values()[tokens[i]];
			int start = Math.min(sourceMax, tokens[i + 1]);
			int end = Math.min(sourceMax, tokens[i + 2]);
			int line = tokens[i + 3];
			int column = tokens[i + 4];
			Log.d("Token", String.format("%s %d:%d | %d-%d", type.name(), line, column, start, end));
			maxLine = Math.max(maxLine, line);
			maxColumn = Math.max(maxColumn, column + (end - start));
			while (tokensByLine.size() <= line) tokensByLine.add(new IntList());
			IntList tokensForLine = tokensByLine.get(line);
			tokensForLine.add(i);
		}
		for (IntList list : tokensByLine)
			list.trimToSize();
		int maxX = (int) ((maxColumn + 1) * charWidth) + getPaddingLeft() + getPaddingRight();
		int maxY = (int) ((maxLine + 1) * lineHeight + getPaddingTop() + getPaddingBottom());
		ViewGroup.LayoutParams layoutParams = getLayoutParams();
		if (maxX != layoutParams.width || maxY != layoutParams.height) {
			layoutParams.width = maxX;
			layoutParams.height = maxY;
			requestLayout();
			source.requestLayout();
		}
		textDirty = false;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override
	public void afterTextChanged(Editable s) {
		currentText = s.toString();
		textDirty = true;
		post(() -> {
			highlight();
			invalidate();
		});
	}

	@FunctionalInterface
	public interface TabSupplier {
		int getWidth();
	}
}
