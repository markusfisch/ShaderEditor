package de.markusfisch.android.shadereditor.widget;

import android.graphics.Canvas;
import android.graphics.Paint;

import de.markusfisch.android.shadereditor.editor.LineStyles;

// A line of printable ASCII and tabs in a font whose characters all have one
// width: positions are arithmetic, and each run of one colour is one drawText.
// A line longer than LONG is drawn this way whatever it holds.
final class MonoLineLayout implements LineLayout {
	private final String text;
	private final CodeAppearance appearance;
	private final int[] pieces;
	private final float width;

	MonoLineLayout(String text, LineStyles.Runs runs,
			CodeAppearance appearance) {
		this.text = text;
		this.appearance = appearance;
		int tabWidth = appearance.tabWidth;
		width = Columns.columnOf(text, text.length(), tabWidth) *
				appearance.charWidth;
		pieces = text.length() > LONG
				? null
				: Columns.pieces(text, runs, appearance.styleColors,
						appearance.textColor, tabWidth);
	}

	@Override
	public float xOf(int offset) {
		return Columns.columnOf(text, offset, appearance.tabWidth) *
				appearance.charWidth;
	}

	@Override
	public int offsetAt(float x) {
		int offset = Columns.offsetAt(text, x / appearance.charWidth,
				appearance.tabWidth);
		// Only a long line can hold a pair here; never split it.
		return offset > 0 && offset < text.length() &&
				Character.isLowSurrogate(text.charAt(offset))
				? offset - 1
				: offset;
	}

	@Override
	public float width() {
		return width;
	}

	@Override
	public boolean isWhole() {
		return pieces != null;
	}

	@Override
	public void draw(Canvas canvas, float left, float right) {
		Paint paint = appearance.paint;
		float charWidth = appearance.charWidth;
		float y = appearance.baseline;
		if (pieces != null) {
			for (int i = 0; i < pieces.length; i += 4) {
				paint.setColor(pieces[i + 3]);
				canvas.drawText(text, pieces[i], pieces[i + 1],
						pieces[i + 2] * charWidth, y, paint);
			}
			return;
		}
		paint.setColor(appearance.textColor);
		int tabWidth = appearance.tabWidth;
		int from = offsetAt(Math.max(0, left - charWidth));
		int to = Math.min(text.length(), offsetAt(right) + 1);
		if (to < text.length() && Character.isLowSurrogate(text.charAt(to))) {
			++to;
		}
		int column = Columns.columnOf(text, from, tabWidth);
		for (int start = from, i = from; i <= to; ++i) {
			if (i == to || text.charAt(i) == '\t') {
				if (i > start) {
					canvas.drawText(text, start, i,
							(column - (i - start)) * charWidth, y, paint);
				}
				if (i < to) {
					column = Columns.next('\t', column, tabWidth);
				}
				start = i + 1;
			} else {
				++column;
			}
		}
	}

	@Override
	public void drawSelection(Canvas canvas, int from, int to, Paint paint) {
		canvas.drawRect(xOf(from), 0, xOf(to), appearance.lineHeight, paint);
	}
}
