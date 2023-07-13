package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import de.markusfisch.android.shadereditor.highlighter.Highlight;
import de.markusfisch.android.shadereditor.highlighter.Lexer;
import de.markusfisch.android.shadereditor.highlighter.TokenType;

public class SyntaxEditor extends LineNumberEditText {
	private static final int[] colors = new int[Highlight.values().length];
	private int tabWidth = 2;

	private @NonNull int[] tokens = new int[256];

	public SyntaxEditor(Context context) {
		this(context, null);
	}

	public SyntaxEditor(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public static void init_colors(@NonNull Context context) {
		for (Highlight highlight : Highlight.values()) {
			colors[highlight.ordinal()] = ContextCompat.getColor(context, highlight.id());
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		highlight();
		Paint paint = getPaint();
		int oldColor = paint.getColor();
		setTextColor(0);
		super.onDraw(canvas); // draw normal text
		CharSequence text = getText();
		if (text == null) return;
		Paint.FontMetrics fm = paint.getFontMetrics();
		float lineHeight = fm.descent - fm.ascent + fm.leading;
		paint.getFontMetrics();
		float charWidth = paint.measureText("m");
		int length = tokens[0];
		float paddLeft = getPaddingLeft() + getLineNumberSpacing() * .5f;
		float paddtop = getPaddingTop();
		for (int i = 1; i <= length; i += 5) {
			TokenType type = TokenType.values()[tokens[i]];
			int start = tokens[i + 1];
			int end = tokens[i + 2];
			int line = tokens[i + 3];
			int column = tokens[i + 4];
			// Log.d("JNI-Echo", String.format("start: %d, end: %d, line: %d, type: %s", start, end, line, type.name()));
			Highlight highlight = Highlight.from(type);
			paint.setColor(colors[highlight.ordinal()]);
			canvas.drawText(text, start, end, charWidth * column + paddLeft, lineHeight * line + paddtop, paint);
		}
		setTextColor(oldColor);
	}

	public void highlight() {
		CharSequence e = getText();
		if (e == null) return;
		tokens = Lexer.runLexer(e.toString(), tokens, tabWidth);
	}

	public int getTabWidth() {
		return tabWidth;
	}

	public void setTabWidth(int tabWidth) {
		this.tabWidth = tabWidth;
	}
}
