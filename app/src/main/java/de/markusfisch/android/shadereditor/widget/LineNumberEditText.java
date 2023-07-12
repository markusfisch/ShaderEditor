package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;

import de.markusfisch.android.shadereditor.R;

public class LineNumberEditText extends AppCompatEditText {
	private final float lineNumberSpacing;
	private final float lineNumberPadding;
	private boolean showLineNumbers;
	private Paint lineNumberPaint;
	private float bigChar;
	private int paddingLeft;

	public LineNumberEditText(Context context) {
		this(context, null);
	}

	public LineNumberEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		int lineColor;
		//noinspection resource
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.LineNumberEditText,
				0, 0);

		try {
			showLineNumbers = a.getBoolean(R.styleable.LineNumberEditText_showLineNumbers, true);
			lineColor = a.getColor(R.styleable.LineNumberEditText_lineNumberColor, 0x88888888);
			lineNumberSpacing = a.getDimension(R.styleable.LineNumberEditText_lineNumberSpacing, 16);
			lineNumberPadding = a.getDimension(R.styleable.LineNumberEditText_lineNumberPadding, 8);
		} finally {
			a.recycle();
		}
		init(lineColor);
	}

	public void setShowLineNumbers(boolean showLineNumbers) {
		if (showLineNumbers != this.showLineNumbers) {
			this.showLineNumbers = showLineNumbers;
			postInvalidate();
		}
	}

	private void init(int color) {
		paddingLeft = getPaddingLeft();
		lineNumberPaint = new Paint(getPaint());
		lineNumberPaint.setColor(color);
		bigChar = lineNumberPaint.measureText("m");
	}

	@Override
	public void setPadding(int left, int top, int right, int bottom) {
		this.paddingLeft = left;
		super.setPadding(left, top, right, bottom);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (!showLineNumbers) {
			super.setPadding(paddingLeft, getPaddingTop(), getPaddingRight(), getPaddingBottom());
			super.onDraw(canvas);
			return;
		}
		int lineCount = getLineCount();
		final int lineCountNumDigits = numDigits(lineCount);
		int editTextPaddingLeft = (int) (bigChar * lineCountNumDigits + lineNumberPadding);
		super.setPadding((int) (editTextPaddingLeft + lineNumberSpacing), getPaddingTop(), getPaddingRight(), getPaddingBottom());
		super.onDraw(canvas);
		for (int i = 0, lineNumber = 1; i < lineCount; ++i) {
			int baseline = getLineBounds(i, null);
			canvas.drawText(Integer.toString(lineNumber),
					bigChar * (lineCountNumDigits - numDigits(lineNumber)) + lineNumberPadding, baseline,
					lineNumberPaint);
			++lineNumber;
		}
		canvas.drawLine(editTextPaddingLeft + lineNumberSpacing * .5f, 0, editTextPaddingLeft + lineNumberSpacing * .5f, getHeight(), lineNumberPaint);

	}

	/**
	 * Taken from <a href="https://www.baeldung.com/java-number-of-digits-in-int#5-divide-and-conquer">Baeldung (Number of Digits in an Integer in Java)</a>
	 *
	 * @param number the number of which you want to get the number of digits.
	 * @return the number of decimal digits of the given {@code number}
	 */
	private int numDigits(int number) {
		if (number < 100000) {
			if (number < 100) {
				if (number < 10) return 1;
				else return 2;
			} else if (number < 1000) {
				return 3;
			} else if (number < 10000) {
				return 4;
			} else return 5;
		} else if (number < 10000000) {
			if (number < 1000000) return 6;
			else return 7;
		} else if (number < 100000000) {
			return 8;
		} else if (number < 1000000000) {
			return 9;
		} else return 10;
	}

}
