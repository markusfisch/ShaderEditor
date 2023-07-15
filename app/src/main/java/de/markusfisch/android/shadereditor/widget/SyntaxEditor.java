package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.markusfisch.android.shadereditor.highlighter.Highlight;
import de.markusfisch.android.shadereditor.highlighter.Lexer;
import de.markusfisch.android.shadereditor.highlighter.TokenType;
import de.markusfisch.android.shadereditor.util.IntList;

public class SyntaxEditor extends View {
	private static final int[] colors = new int[Highlight.values().length];
	private final List<IntList> tokensByLine = new ArrayList<>();
	private final Rect visibleRect = new Rect();
	private final @NonNull AppCompatEditText source;
	boolean textDirty = true;
	private int tabWidthCharacters = 2;
	private int tabWidth = 0;
	private @NonNull int[] tokens = new int[256];
	private String currentText = "";

	public SyntaxEditor(Context context, @NonNull AppCompatEditText source) {
		super(context);
		this.source = source;
		float charWidth = source.getPaint().measureText("m");
		tabWidth = (int) (charWidth * tabWidthCharacters);
		getViewTreeObserver().addOnScrollChangedListener(this::invalidate);
		source.addTextChangedListener(new TextWatcher() {
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
				postInvalidate();
				ExecutorService executor = Executors.newSingleThreadExecutor();
				Handler handler = new Handler(Looper.getMainLooper());
				executor.execute(() -> {
					tokens = Lexer.runLexer(currentText, tokens, tabWidthCharacters);
					tokensByLine.clear();
					for (int i = 1, length = tokens[0]; i <= length; i += 5) {
						int line = tokens[i + 3];
						while (tokensByLine.size() <= line) tokensByLine.add(new IntList());
						IntList tokensForLine = tokensByLine.get(line);
						tokensForLine.add(i);
					}
					for (IntList list : tokensByLine)
						list.trimToSize();

					handler.post(() -> {
						textDirty = false;
						postInvalidate();
					});
				});
			}
		});
	}

	public static void initColors(@NonNull Context context) {
		for (Highlight highlight : Highlight.values()) {
			colors[highlight.ordinal()] = ContextCompat.getColor(context, highlight.id());
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (textDirty) return;
		Trace.beginSection("Syntax Highlighting Draw");
		Trace.beginSection("Highlighting (Java Side)");
		Paint paint = source.getPaint();
		Trace.endSection();
		if (currentText == null) return;
		Trace.beginSection("Measure Font");
//		Paint.FontMetrics fm = paint.getFontMetrics();
//		float lineHeight = fm.bottom - fm.top + fm.leading;
//		float descent = fm.descent;
		float charWidth = paint.measureText("m");
		Trace.endSection();
		tabWidth = (int) (charWidth * tabWidthCharacters);
		float paddingLeft = getPaddingLeft();
		float paddingTop = source.getExtendedPaddingTop();
		Layout layout = source.getLayout();
		Trace.beginSection("Visible Rect");
		getLocalVisibleRect(visibleRect);
		Trace.endSection();
		Trace.beginSection("Get visible lines");
		int firstLine = layout.getLineForVertical(visibleRect.top);
		int lastLine = layout.getLineForVertical(visibleRect.bottom) + 1;
		Trace.endSection();
		Trace.beginSection("Highlighting");
		int sourceMax = source.length();
		for (int line = firstLine; line <= lastLine; ++line) {
			if (line >= tokensByLine.size()) break;
			Trace.beginSection("Highlighting Line " + line);
			for (int i : tokensByLine.get(line).getRaw()) {
				Trace.beginSection("Highlighting Token " + i);
				TokenType type = TokenType.values()[tokens[i]];
				int start = Math.min(sourceMax, tokens[i + 1]);
				int end = Math.min(sourceMax, tokens[i + 2]);
				int column = tokens[i + 4];
				Highlight highlight = Highlight.from(type);
				paint.setColor(colors[highlight.ordinal()]);
				float y = layout.getLineTop(line) + paddingTop - layout.getLineDescent(line);
//				float y = lineHeight * line + paddingTop - descent;
				float x = 0;
				canvas.drawText(currentText, start, end, x + charWidth * column + paddingLeft, y, paint);
				Trace.endSection();
			}
			Trace.endSection();
		}
		Trace.endSection();
		super.onDraw(canvas); // draw normal text
		Trace.endSection();
	}

	public int getTabWidth() {
		return tabWidth;
	}

	public void setTabWidth(int tabWidthCharacters) {
		this.tabWidthCharacters = tabWidthCharacters;
	}
}
