package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Trace;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import de.markusfisch.android.shadereditor.R;

public class LineNumbers extends AppCompatTextView implements TextWatcher {
	private final int sourceId;
	private TextView source;

	public LineNumbers(Context context) {
		this(context, null);
	}

	public LineNumbers(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Try-with-resources not allowed for API < 31
		// noinspection resource
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.LineNumbers,
				0, 0);
		try {
			sourceId = a.getResourceId(R.styleable.LineNumbers_source, 0);
		} finally {
			a.recycle();
		}
		setClickable(false);
		setFocusable(false);
		setTextIsSelectable(false);
		setText("0");
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (widthMode != MeasureSpec.UNSPECIFIED || heightMode != MeasureSpec.UNSPECIFIED) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			return;
		}
		Paint.FontMetricsInt fm = getPaint().getFontMetricsInt();
		int lineHeight = fm.bottom - fm.top + fm.leading;
		int lineCount = source.getLineCount();
		setMeasuredDimension((int) (getPaint().measureText("m") * numDigits(lineCount)) + getPaddingLeft() + getPaddingRight(), lineCount * lineHeight + getPaddingTop() + getPaddingBottom());
	}

	public void setSource(@NonNull TextView source) {
		if (this.source != null) source.removeTextChangedListener(this);
		this.source = source;
		source.addTextChangedListener(this);
		post(() -> {
			updateLineNumbers();
			invalidate();
		});
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		TextView source = getRootView().findViewById(sourceId);
		if (source != null)
			setSource(source);

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

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	private void updateLineNumbers() {
		Trace.beginSection("Draw Line Numbers");
		Trace.beginSection("Get Line Count Num Digits");
		int lineCount = source.getLineCount();
		final String lineFormat = "%" + numDigits(lineCount) + "d\n";
		Trace.endSection();
		Trace.beginSection("Add line numbers");
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < lineCount; ++i) {
			builder.append(String.format(lineFormat, i + 1));
		}
		setText(builder);
		Trace.endSection();
		Trace.endSection();
	}

	@Override
	public void afterTextChanged(Editable s) {
		post(this::updateLineNumbers);
	}
}
