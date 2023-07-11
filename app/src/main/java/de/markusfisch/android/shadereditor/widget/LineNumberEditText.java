package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Editable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

public class LineNumberEditText extends AppCompatEditText {
    public LineNumberEditText(@NonNull Context context) {
        super(context);
        init();
    }

    public LineNumberEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LineNumberEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private Paint lineNumberPaint;
    private float bigChar;

    private void init() {
        lineNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lineNumberPaint.setColor(0xff888888);
        lineNumberPaint.setTypeface(getTypeface());
        lineNumberPaint.setTextSize(getTextSize());
        bigChar = lineNumberPaint.measureText("m");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Editable editable = getText();

        if (editable == null) return;

        int lineCount = getLineCount();
        final int lineCountNumDigits = numDigits(lineCount);

        for (int i = 0, lineNumber = 1; i < lineCount; ++i) {
            int baseline = getLineBounds(i, null);
            canvas.drawText(Integer.toString(lineNumber),
                    bigChar * (lineCountNumDigits - numDigits(lineNumber)), baseline,
                    lineNumberPaint);
            ++lineNumber;
        }
        int paddingLeft = (int) (bigChar * lineCountNumDigits);
        int spacing = 16;
        canvas.drawLine(paddingLeft + spacing * .5f, 0, paddingLeft + spacing * .5f, getHeight(), lineNumberPaint);
        setPadding(paddingLeft + spacing, getPaddingTop(), getPaddingRight(), getPaddingBottom());

        super.onDraw(canvas);
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
