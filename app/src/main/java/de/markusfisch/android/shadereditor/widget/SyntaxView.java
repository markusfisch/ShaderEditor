package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.highlighter.Highlight;
import de.markusfisch.android.shadereditor.highlighter.Lexer;
import de.markusfisch.android.shadereditor.highlighter.TokenType;
import de.markusfisch.android.shadereditor.util.IntList;

public class SyntaxView extends View implements TextWatcher {
	/**
	 * Not using {@link java.util.function.IntSupplier IntSupplier},
	 * because that would require API >= 24. Also {@code getWidth()}
	 * has a semantic meaning, which {@code getAsInt()} lacks.
	 */
	@FunctionalInterface
	public interface TabSupplier {
		int getWidth();
	}

	public static final int MAX_HIGHLIGHT_LENGTH = 8192;
	private static final int WINDOW_SIZE = 2; // Could be tuned for performance
	private static final int[] colors = new int[Highlight.values().length];
	private final List<IntList> tokensByLine = new ArrayList<>();
	private final Rect visibleRect = new Rect();
	private final Paint paint = new Paint();
	private final Paint.FontMetricsInt fm = new Paint.FontMetricsInt();
	private volatile boolean noDraw = false;
	private @Nullable TextView source;
	private @NonNull TabSupplier tabSupplier = () -> 2;
	private @NonNull int[] tokens = new int[256];
	private String currentText = "";
	private int maxX = ViewGroup.LayoutParams.WRAP_CONTENT;
	private int maxY = ViewGroup.LayoutParams.WRAP_CONTENT;
	private int visibleFirstLine = 0;
	private int visibleLastLine = 0;

	public SyntaxView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		getViewTreeObserver().addOnScrollChangedListener(this::updateVisibleLines);
	}

	public static void initColors(@NonNull Context context) {
		for (Highlight highlight : Highlight.values()) {
			colors[highlight.ordinal()] = ContextCompat.getColor(context, highlight.id());
		}
	}

	public int getMaxY() {
		return maxY;
	}

	public int getMaxX() {
		return maxX;
	}

	public void setSource(@NonNull TextView source) {
		if (this.source != null) {
			this.source.removeTextChangedListener(this);
		}
		this.source = source;
		setPadding(source.getPaddingLeft(), source.getPaddingTop(), source.getPaddingRight(), source.getPaddingBottom());
		source.addTextChangedListener(this);
		updateHighlight(source.getText());
		postInvalidate();
	}

	public void setTabSupplier(@NonNull TabSupplier tabSupplier) {
		this.tabSupplier = tabSupplier;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override
	public void afterTextChanged(Editable s) {
		updateHighlight(s);
		postInvalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(maxX, maxY);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (source == null || currentText == null) return;
		paint.set(source.getPaint());


		paint.getFontMetricsInt(fm);
		float charWidth = paint.measureText("m");

		int lineHeight = fm.descent - fm.ascent + fm.leading;
		float paddingLeft = getPaddingLeft();
		float lineOffsetY = source.getExtendedPaddingTop() - fm.descent;

		int firstLine = visibleFirstLine;
		int lastLine = visibleLastLine;

		int sourceMax = source.length();
		float currentY = firstLine * lineHeight + lineOffsetY;

		synchronized (tokensByLine) {
			for (int line = firstLine; line <= lastLine; ++line, currentY += lineHeight) {
				if (line >= tokensByLine.size()) break;
				for (int i : tokensByLine.get(line).getRaw()) {
					TokenType type = TokenType.values()[tokens[i]];
					Highlight highlight = Highlight.from(type);
					int start = Math.min(sourceMax, tokens[i + 1]);
					int end = Math.min(sourceMax, tokens[i + 2]);
					int column = tokens[i + 4];
					paint.setColor(colors[highlight.ordinal()]);
					canvas.drawText(currentText, start, end, charWidth * column + paddingLeft, currentY, paint);
				}
			}
		}
		super.onDraw(canvas); // draw normal text
	}

	private void updateHighlight(CharSequence s) {
		if (ShaderEditorApp.preferences.disableHighlighting()) {
			noDraw = s.length() > MAX_HIGHLIGHT_LENGTH;
		} else {
			noDraw = false;
		}
		if (noDraw) return;
		synchronized (tokensByLine) {
			currentText = s.toString();
			highlight();
		}
	}

	private void highlight() {
		int maxColumn = 0;
		int maxLine = 0;

		if (source != null) paint.set(source.getPaint());
		paint.getFontMetricsInt(fm);
		float lineHeight = fm.descent - fm.ascent + fm.leading;
		float charWidth = paint.measureText("m");

		tokens = Lexer.runLexer(currentText, tokens, tabSupplier.getWidth());

		tokensByLine.clear();
		int sourceMax = source.length();
		for (int i = 1, length = tokens[0]; i <= length; i += 5) {
			int start = Math.min(sourceMax, tokens[i + 1]);
			int end = Math.min(sourceMax, tokens[i + 2]);
			int line = tokens[i + 3];
			int column = tokens[i + 4];

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
		if (maxX != this.maxX || maxY != this.maxY) {
			this.maxX = maxX;
			this.maxY = maxY;
			post(() -> {
				requestLayout();
				source.requestLayout();
			});
		}
	}
	private void updateVisibleLines() {
		getLocalVisibleRect(visibleRect);
		if (source != null) paint.set(source.getPaint());
		paint.getFontMetricsInt(fm);
		int lineHeight = fm.descent - fm.ascent + fm.leading;
		int firstLine = floorStep(visibleRect.top/ lineHeight, WINDOW_SIZE) ;
		int lastLine = ceilStep(visibleRect.bottom/ lineHeight, WINDOW_SIZE) ;
		if (firstLine != visibleFirstLine || lastLine != visibleLastLine) {
			visibleFirstLine = firstLine;
			visibleLastLine = lastLine;
			invalidate();
		}
	}

	private static int floorStep(int value, int stepSize) {
		return (value / stepSize) * stepSize;
	}
	private static int ceilStep(int value, int stepSize) {
		return ((value + stepSize - 1) / stepSize) * stepSize;
	}
}
