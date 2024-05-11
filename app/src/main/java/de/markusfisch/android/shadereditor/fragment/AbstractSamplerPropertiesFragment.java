package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;
import de.markusfisch.android.shadereditor.opengl.TextureParameters;
import de.markusfisch.android.shadereditor.view.SoftKeyboard;
import de.markusfisch.android.shadereditor.widget.TextureParametersView;

public abstract class AbstractSamplerPropertiesFragment extends Fragment {
	public static final String TEXTURE_NAME_PATTERN = "[a-zA-Z0-9_]+";
	public static final String SAMPLER_2D = "sampler2D";
	public static final String SAMPLER_CUBE = "samplerCube";

	private static final Pattern NAME_PATTERN = Pattern.compile(
			"^" + TEXTURE_NAME_PATTERN + "$");

	private static boolean inProgress = false;

	private TextView sizeCaption;
	private SeekBar sizeBarView;
	private TextView sizeView;
	private EditText nameView;
	private CheckBox addUniformView;
	private TextureParametersView textureParameterView;
	private View progressView;
	private String samplerType = SAMPLER_2D;

	protected void setSizeCaption(String caption) {
		sizeCaption.setText(caption);
	}

	protected void setMaxValue(int max) {
		sizeBarView.setMax(max);
	}

	protected void setSamplerType(String name) {
		samplerType = name;
	}

	protected abstract int saveSampler(
			Context context,
			String name,
			int size);

	protected View initView(
			Activity activity,
			LayoutInflater inflater,
			ViewGroup container) {
		View view = inflater.inflate(
				R.layout.fragment_sampler_properties,
				container,
				false);

		sizeCaption = view.findViewById(R.id.size_caption);
		sizeBarView = view.findViewById(R.id.size_bar);
		sizeView = view.findViewById(R.id.size);
		nameView = view.findViewById(R.id.name);
		addUniformView = view.findViewById(
				R.id.should_add_uniform);
		textureParameterView = view.findViewById(
				R.id.texture_parameters);
		progressView = view.findViewById(R.id.progress_view);

		view.findViewById(R.id.save).setOnClickListener(v -> saveSamplerAsync());

		if (activity.getCallingActivity() == null) {
			addUniformView.setVisibility(View.GONE);
			addUniformView.setChecked(false);
			textureParameterView.setVisibility(View.GONE);
		}

		initSizeView();
		initNameView();

		return view;
	}

	private void initSizeView() {
		setSizeView(sizeBarView.getProgress());
		sizeBarView.setOnSeekBarChangeListener(
				new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(
							SeekBar seekBar,
							int progressValue,
							boolean fromUser) {
						setSizeView(progressValue);
					}

					@Override
					public void onStartTrackingTouch(
							SeekBar seekBar) {
					}

					@Override
					public void onStopTrackingTouch(
							SeekBar seekBar) {
					}
				});
	}

	private void setSizeView(int power) {
		int size = getPower(power);
		sizeView.setText(String.format(
				Locale.US,
				"%d x %d",
				size,
				size));
	}

	private void initNameView() {
		nameView.setFilters(new InputFilter[]{
				(source, start, end, dest, dstart, dend) -> NAME_PATTERN
						.matcher(source)
						.find() ? null : ""});
	}

	private void saveSamplerAsync() {
		final Context context = getActivity();

		if (context == null || inProgress) {
			return;
		}

		final String name = nameView.getText().toString();
		final TextureParameters tp = new TextureParameters();
		textureParameterView.setParameters(tp);
		final String params = tp.toString();

		if (name.trim().isEmpty()) {
			Toast.makeText(
					context,
					R.string.missing_name,
					Toast.LENGTH_SHORT).show();

			return;
		} else if (!name.matches(TEXTURE_NAME_PATTERN) ||
				name.equals(ShaderRenderer.UNIFORM_BACKBUFFER)) {
			Toast.makeText(
					context,
					R.string.invalid_texture_name,
					Toast.LENGTH_SHORT).show();

			return;
		}

		SoftKeyboard.hide(context, nameView);

		final int size = getPower(sizeBarView.getProgress());

		inProgress = true;
		progressView.setVisibility(View.VISIBLE);

		Handler handler = new Handler(Looper.getMainLooper());
		Executors.newSingleThreadExecutor().execute(() -> {
			int messageId = saveSampler(context, name, size);
			handler.post(() -> {
				inProgress = false;
				progressView.setVisibility(View.GONE);

				Activity activity = getActivity();
				if (activity == null) {
					return;
				}

				if (messageId > 0) {
					Toast.makeText(
							activity,
							messageId,
							Toast.LENGTH_SHORT).show();

					return;
				}

				if (addUniformView.isChecked()) {
					AddUniformActivity.setAddUniformResult(
							activity,
							"uniform " + samplerType + " " + name + ";" +
									params);
				}

				activity.finish();
			});
		});
	}

	private static int getPower(int power) {
		return 1 << (power + 1);
	}
}
