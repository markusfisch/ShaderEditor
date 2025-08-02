package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.List;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.TextureSpinnerAdapter;
import de.markusfisch.android.shadereditor.database.DataRecords.TextureInfo;
import de.markusfisch.android.shadereditor.database.Database;
import de.markusfisch.android.shadereditor.opengl.BackBufferParameters;

public class BackBufferParametersView extends LinearLayout {
	private TextureSpinnerAdapter adapter;
	private Spinner presetView;

	public BackBufferParametersView(Context context) {
		super(context);
	}

	public BackBufferParametersView(Context context, AttributeSet attr) {
		super(context, attr);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		Context context = getContext();

		// 1. Get the list of textures from the modern DataSource.
		List<TextureInfo> textures = Database
				.getInstance(context)
				.getDataSource()
				.texture
				.getTextures(null);

		// 2. Create a "dummy" record for the "(no preset)" option.
		TextureInfo noPreset = new TextureInfo(
				-1, // Use a negative ID to signify it's not a real texture.
				context.getString(R.string.no_preset),
				0,
				0,
				null);

		// 3. Add the "no preset" option to the beginning of the list.
		textures.add(0, noPreset);

		// 4. Create the adapter and set its data.
		adapter = new TextureSpinnerAdapter(context);
		adapter.setData(textures);

		// 5. Find the Spinner and attach the adapter.
		presetView = findViewById(R.id.backbuffer_preset);
		presetView.setAdapter(adapter);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		// Clear the adapter's data to release references.
		if (adapter != null) {
			adapter.setData(null);
		}
	}

	/**
	 * Updates the given BackBufferParameters object with the preset ID
	 * selected in the spinner.
	 */
	public void getParameters(BackBufferParameters tp) {
		if (tp == null) {
			return;
		}

		Object item = presetView.getSelectedItem();
		if (!(item instanceof TextureInfo textureInfo)) {
			return;
		}

		long id = textureInfo.id();

		// The preset is stored as a stringified ID.
		if (id > 0) {
			tp.setPreset(String.valueOf(id));
		} else {
			// If "(no preset)" is selected, clear the preset.
			tp.setPreset(null);
		}
	}

}