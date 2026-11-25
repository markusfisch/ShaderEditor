package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.editor.LineStyles;
import de.markusfisch.android.shadereditor.highlighter.Highlight;
import de.markusfisch.android.shadereditor.syntax.Style;

// Every colour, size and stroke of a CodeView, read from resources and the
// theme in one place, and the metrics of its font. Changing the font or a size
// invalidates every LineLayout made before.
final class CodeAppearance {
	private static final String PRINTABLE;

	static {
		StringBuilder sb = new StringBuilder();
		for (char c = 0x20; c < 0x7f; ++c) {
			sb.append(c);
		}
		PRINTABLE = sb.toString();
	}

	final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
	final int[] styleColors = new int[Style.values().length];
	final int textColor;
	final int lineNumberColor;
	final int errorColor;
	// Of the bar and the number of a line with an error.
	final int markColor;
	final float markWidth;
	final int selectionColor;
	final int caretColor;
	final int handleColor;
	final float caretWidth;
	final float underline;
	final float padding;
	int tabWidth = 4;
	float charWidth;
	float digitWidth;
	int lineHeight;
	int baseline;
	boolean monospace;

	CodeAppearance(Context context) {
		Resources res = context.getResources();
		for (Style style : Style.values()) {
			styleColors[style.ordinal()] = ContextCompat.getColor(context,
					Highlight.colorOf(style));
		}
		textColor = ContextCompat.getColor(context, R.color.editor_text);
		lineNumberColor = ContextCompat.getColor(context,
				R.color.editor_line_numbers);
		errorColor = ContextCompat.getColor(context, R.color.syntax_error);
		markColor = MaterialColors.getColor(context,
				androidx.appcompat.R.attr.colorError, errorColor);
		markWidth = res.getDimension(R.dimen.editor_mark_width);
		caretColor = ContextCompat.getColor(context, R.color.editor_caret);
		selectionColor = ContextCompat.getColor(context,
				R.color.code_selection);
		TypedArray a = context.obtainStyledAttributes(new int[]{
				androidx.appcompat.R.attr.colorPrimary});
		try {
			handleColor = a.getColor(0, caretColor);
		} finally {
			a.recycle();
		}
		caretWidth = res.getDimension(R.dimen.editor_caret_width);
		underline = res.getDimension(R.dimen.editor_composing_underline);
		padding = res.getDimension(R.dimen.editor_padding);
		paint.setTypeface(Typeface.MONOSPACE);
		paint.setTextSize(res.getDimension(R.dimen.editor_text_size));
		measure();
	}

	// runs may be null for no colours.
	LineLayout layout(String text, LineStyles.Runs runs) {
		return text.length() > LineLayout.LONG ||
				(monospace && Columns.isSimple(text))
				? new MonoLineLayout(text, runs, this)
				: new PlatformLineLayout(text, runs, this);
	}

	void measure() {
		Paint.FontMetricsInt fm = paint.getFontMetricsInt();
		lineHeight = fm.descent - fm.ascent;
		baseline = -fm.ascent;
		float[] widths = new float[PRINTABLE.length()];
		paint.getTextWidths(PRINTABLE, widths);
		charWidth = widths[0];
		monospace = true;
		for (float width : widths) {
			monospace &= Math.abs(width - charWidth) < .01f;
		}
		digitWidth = 0;
		for (int digit = '0'; digit <= '9'; ++digit) {
			digitWidth = Math.max(digitWidth, widths[digit - 0x20]);
		}
	}
}
