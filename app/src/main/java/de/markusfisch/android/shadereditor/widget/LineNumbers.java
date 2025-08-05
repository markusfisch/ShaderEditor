package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.content.res.TypedArray;
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

	public void setSource(@NonNull TextView source) {
		if (this.source != null) source.removeTextChangedListener(this);
		this.source = source;
		setTypeface(source.getTypeface());
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
		if (source != null) {
			setSource(source);
		}
	}

	/**
	 * Taken from
	 * <a href="https://www.baeldung.com/java-number-of-digits-in-int#5-divide-and-conquer">Baeldung (Number of Digits in an Integer in Java)</a>
	 *
	 * @param number the number of which you want to get the number of digits.
	 * @return the number of decimal digits of the given {@code number}
	 */
	private int numDigits(int number) {
		if (number < 100000) {
			if (number < 100) {
				return number < 10 ? 1 : 2;
			} else if (number < 1000) {
				return 3;
			} else {
				return number < 10000 ? 4 : 5;
			}
		} else if (number < 10000000) {
			return (number < 1000000) ? 6 : 7;
		} else if (number < 100000000) {
			return 8;
		} else {
			return number < 1000000000 ? 9 : 10;
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	private void updateLineNumbers() {
		int lineCount = source.getLineCount();
		final String lineFormat = "%" + numDigits(lineCount) + "d\n";

		StringBuilder builder = new StringBuilder();
		for (int i = 1; i <= lineCount; ++i) {
			builder.append(String.format(lineFormat, i));
		}
		setText(builder);
	}

	@Override
	public void afterTextChanged(Editable s) {
		post(this::updateLineNumbers);
	}
}
