package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.opengl.TextureParameters;

public class TextureParametersView extends LinearLayout {
	private Spinner minView;
	private Spinner magView;
	private Spinner wrapSView;
	private Spinner wrapTView;

	public TextureParametersView(Context context) {
		super(context);
	}

	public TextureParametersView(Context context, AttributeSet attr) {
		super(context, attr);
	}

	@Override
	public void onFinishInflate() {
		super.onFinishInflate();

		minView = findViewById(R.id.min);
		magView = findViewById(R.id.mag);
		wrapSView = findViewById(R.id.wrap_s);
		wrapTView = findViewById(R.id.wrap_t);

		initSpinner(minView, R.array.min_names);
		initSpinner(magView, R.array.mag_names);
		initSpinner(wrapSView, R.array.wrap_names);
		initSpinner(wrapTView, R.array.wrap_names);
	}

	public void setDefaults(TextureParameters tp) {
		setSpinnerValue(minView, R.array.min_values, tp.getMinShortcut());
		setSpinnerValue(magView, R.array.mag_values, tp.getMagShortcut());
		setSpinnerValue(wrapSView, R.array.wrap_values, tp.getWrapSShortcut());
		setSpinnerValue(wrapTView, R.array.wrap_values, tp.getWrapTShortcut());
	}

	public void setParameters(TextureParameters tp) {
		tp.set(getSpinnerValue(minView, R.array.min_values),
				getSpinnerValue(magView, R.array.mag_values),
				getSpinnerValue(wrapSView, R.array.wrap_values),
				getSpinnerValue(wrapTView, R.array.wrap_values));
	}

	private void initSpinner(Spinner spinner, int namesId) {
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				spinner.getContext(),
				namesId,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}

	private String getSpinnerValue(Spinner spinner, int valuesId) {
		String[] values = getResources().getStringArray(valuesId);
		return values[spinner.getSelectedItemPosition()];
	}

	private void setSpinnerValue(
			Spinner spinner,
			int valuesId,
			String value) {
		String[] values = getResources().getStringArray(valuesId);
		for (int i = 0; i < values.length; ++i) {
			if (values[i].equals(value)) {
				spinner.setSelection(i);
				return;
			}
		}
	}
}
