package de.markusfisch.android.shadereditor.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.style.ForegroundColorSpan;
import android.text.style.TabStopSpan;

import java.text.Bidi;

import de.markusfisch.android.shadereditor.editor.LineStyles;

// A StaticLayout of one line with colour spans: for any character but printable
// ASCII, and for every line in a font whose characters differ in width. It
// brings font fallback, emoji, combining marks, right-to-left runs and offsets
// by grapheme cluster.
final class PlatformLineLayout implements LineLayout {
	// Wide enough that a line of LONG characters never wraps.
	private static final int WIDTH = 1 << 24;
	private static final int MAX_TAB_STOPS = 1024;

	private final StaticLayout layout;
	private final CodeAppearance appearance;
	private final CharSequence text;
	// Moves the layout's baseline onto the line's.
	private final float dy;

	PlatformLineLayout(String text, LineStyles.Runs runs,
			CodeAppearance appearance) {
		this.appearance = appearance;
		this.text = oneLine(text);
		SpannableString spannable = new SpannableString(this.text);
		for (int i = 0, n = runs == null ? 0 : runs.size(); i < n; ++i) {
			int color = appearance.styleColors[runs.style(i).ordinal()];
			if (color != appearance.textColor) {
				spannable.setSpan(new ForegroundColorSpan(color),
						runs.start(i), Math.min(text.length(), runs.end(i)),
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		addTabStops(spannable, text);
		layout = StaticLayout.Builder.obtain(spannable, 0, text.length(),
						appearance.paint, WIDTH)
				.setIncludePad(false)
				.setTextDirection(TextDirectionHeuristics.LTR)
				.build();
		dy = appearance.baseline - layout.getLineBaseline(0);
	}

	@Override
	public float xOf(int offset) {
		return layout.getPrimaryHorizontal(offset);
	}

	// The advances of one measure add up to the places of a line in one
	// direction without tab stops; any other asks the layout for each.
	@Override
	public void xsOf(int from, int to, float[] xs) {
		String s = text.toString();
		if (s.indexOf('\t') > -1 ||
				Bidi.requiresBidi(s.toCharArray(), 0, s.length())) {
			LineLayout.super.xsOf(from, to, xs);
			return;
		}
		float[] advances = new float[s.length()];
		appearance.paint.getTextWidths(s, 0, s.length(), advances);
		float x = 0;
		for (int i = 0; i <= to; ++i) {
			if (i >= from) {
				xs[i - from] = x;
			}
			if (i < advances.length) {
				x += advances[i];
			}
		}
	}

	@Override
	public int offsetAt(float x) {
		return layout.getOffsetForHorizontal(0, x);
	}

	@Override
	public float width() {
		return layout.getLineWidth(0);
	}

	@Override
	public boolean isWhole() {
		return true;
	}

	@Override
	public void draw(Canvas canvas, float left, float right) {
		appearance.paint.setColor(appearance.textColor);
		canvas.translate(0, dy);
		layout.draw(canvas);
		canvas.translate(0, -dy);
	}

	@Override
	public void drawSelection(Canvas canvas, int from, int to, Paint paint) {
		Path path = new Path();
		layout.getSelectionPath(from, to, path);
		int save = canvas.save();
		canvas.clipRect(0, 0, WIDTH, appearance.lineHeight);
		canvas.translate(0, dy);
		canvas.drawPath(path, paint);
		canvas.restoreToCount(save);
	}

	// Tabs reach the next multiple of the tab width, as many stops as the
	// last tab can need.
	private void addTabStops(SpannableString spannable, String text) {
		int last = text.lastIndexOf('\t');
		if (last < 0) {
			return;
		}
		float step = appearance.charWidth * Math.max(1, appearance.tabWidth);
		int tabs = 0;
		for (int i = 0; i <= last; ++i) {
			if (text.charAt(i) == '\t') {
				++tabs;
			}
		}
		float reach = appearance.paint.measureText(text, 0, last) +
				tabs * step;
		int stops = Math.min(MAX_TAB_STOPS, (int) (reach / step) + 1);
		for (int i = 1; i <= stops; ++i) {
			spannable.setSpan(new TabStopSpan.Standard(Math.round(i * step)),
					0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		}
	}

	// The layout breaks the line at these; they show as replacement
	// characters instead, so the line stays one line.
	private static CharSequence oneLine(String text) {
		char[] chars = null;
		for (int i = 0, n = text.length(); i < n; ++i) {
			char c = text.charAt(i);
			if (c == '\u000b' || c == '\u000c' || c == '\u0085' ||
					c == ' ' || c == ' ') {
				if (chars == null) {
					chars = text.toCharArray();
				}
				chars[i] = '�';
			}
		}
		return chars == null ? text : new String(chars);
	}
}
