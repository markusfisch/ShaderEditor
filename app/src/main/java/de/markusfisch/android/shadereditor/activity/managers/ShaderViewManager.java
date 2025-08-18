package de.markusfisch.android.shadereditor.activity.managers;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.opengl.ShaderError;
import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;
import de.markusfisch.android.shadereditor.widget.ShaderView;

public class ShaderViewManager {
	public interface Listener {
		void onFramesPerSecond(int fps);

		void onInfoLog(@NonNull List<ShaderError> infoLog);

		void onQualityChanged(float quality);
	}

	@NonNull
	private final ShaderView shaderView;
	@NonNull
	private final Spinner qualitySpinner;
	@NonNull
	private final Listener listener;
	@NonNull
	private float[] qualityValues;
	private float quality = 1f;

	public ShaderViewManager(@NonNull Activity activity,
			@NonNull ShaderView shaderView,
			@NonNull Spinner qualitySpinner,
			@NonNull Listener listener) {
		this.shaderView = shaderView;
		this.qualitySpinner = qualitySpinner;
		this.listener = listener;

		initQualitySpinner(activity);
		initShaderView();
	}

	public void onPause() {
		if (shaderView.getVisibility() == View.VISIBLE) {
			shaderView.onPause();
		}
	}

	public void onResume() {
		if (shaderView.getVisibility() == View.VISIBLE) {
			shaderView.onResume();
		}
	}

	public void setFragmentShader(@Nullable String src) {
		shaderView.setFragmentShader(src, quality);
	}

	public void setVisibility(boolean visible) {
		shaderView.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	public byte[] getThumbnail() {
		return shaderView.getRenderer().getThumbnail();
	}

	public void setQuality(float quality) {
		for (int i = 0; i < qualityValues.length; ++i) {
			if (qualityValues[i] == quality) {
				qualitySpinner.setSelection(i);
				this.quality = quality;
				return;
			}
		}
	}

	private void initShaderView() {
		shaderView.getRenderer().setOnRendererListener(new ShaderRenderer.OnRendererListener() {
			@Override
			public void onFramesPerSecond(int fps) {
				listener.onFramesPerSecond(fps);
			}

			@Override
			public void onInfoLog(@NonNull List<ShaderError> infoLog) {
				listener.onInfoLog(infoLog);
			}
		});
	}

	private void initQualitySpinner(@NonNull Activity activity) {
		String[] qualityStringValues =
				activity.getResources().getStringArray(R.array.quality_values);
		qualityValues = new float[qualityStringValues.length];
		for (int i = 0; i < qualityStringValues.length; ++i) {
			qualityValues[i] = Float.parseFloat(qualityStringValues[i]);
		}

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				activity,
				R.array.quality_names,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		qualitySpinner.setAdapter(adapter);
		qualitySpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				float q = qualityValues[position];
				if (q == quality) {
					return;
				}
				quality = q;
				listener.onQualityChanged(quality);
				// Refresh renderer with new quality
				shaderView.getRenderer().setQuality(quality);
				shaderView.onPause();
				shaderView.onResume();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}
}
