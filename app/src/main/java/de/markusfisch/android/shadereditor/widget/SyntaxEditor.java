package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.highlighter.Highlight;
import de.markusfisch.android.shadereditor.highlighter.Lexer;
import de.markusfisch.android.shadereditor.highlighter.TokenType;
import de.markusfisch.android.shadereditor.util.IntList;

public class SyntaxEditor extends LineNumberEditText {
	private static final int[] colors = new int[Highlight.values().length];
	private final List<IntList> tokensByLine = new ArrayList<>();
	private final Rect visibleRect = new Rect();
	boolean textDirty = true;
	private int tabWidthCharacters = 2;
	private int tabWidth = 0;
	private @NonNull int[] tokens = new int[256];
	private String currentText = "";

	public SyntaxEditor(Context context) {
		this(context, null);
	}

	public SyntaxEditor(Context context, AttributeSet attrs) {
		super(context, attrs);
		float charWidth = getPaint().measureText("m");
		tabWidth = (int) (charWidth * tabWidthCharacters);
		addTextChangedListener(new TextWatcher() {
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
		highlight();
		Paint paint = getPaint();
		if (currentText == null) return;
		Paint.FontMetrics fm = paint.getFontMetrics();
//		float lineHeight = fm.bottom - fm.top + fm.leading;
//		float descent = fm.descent;
		float charWidth = paint.measureText("m");
		tabWidth = (int) (charWidth * tabWidthCharacters);
		float paddingLeft = getPaddingLeft();
		float paddingTop = getExtendedPaddingTop();
		Layout layout = getLayout();
		getLocalVisibleRect(visibleRect);
		int firstLine = layout.getLineForVertical(visibleRect.top);
		int lastLine = layout.getLineForVertical(visibleRect.bottom) + 1;
		for (int line = firstLine; line <= lastLine; ++line) {
			if (line >= tokensByLine.size()) break;
			for (int i : tokensByLine.get(line).getRaw()) {
				TokenType type = TokenType.values()[tokens[i]];
				int start = tokens[i + 1];
				int end = tokens[i + 2];
				int column = tokens[i + 4];
				Highlight highlight = Highlight.from(type);
				paint.setColor(colors[highlight.ordinal()]);
				float y = layout.getLineTop(line) + paddingTop - layout.getLineDescent(line);
//				float y = lineHeight * line + paddingTop - descent;
				float x = 0;
				canvas.drawText(currentText, start, end, x + charWidth * column + paddingLeft, y, paint);
			}
		}
		setTextColor(0);
		super.onDraw(canvas); // draw normal text
	}

	void highlight() {
		if (textDirty) {
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
			textDirty = false;
		}
	}

	public int getTabWidth() {
		return tabWidth;
	}

	public void setTabWidth(int tabWidthCharacters) {
		this.tabWidthCharacters = tabWidthCharacters;
	}
}
