package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.Spinner;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.TextureSpinnerAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
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
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{
				Database.TEXTURES_ID,
				Database.TEXTURES_NAME,
				Database.TEXTURES_WIDTH,
				Database.TEXTURES_HEIGHT,
				Database.TEXTURES_THUMB
		});
		matrixCursor.addRow(new Object[]{
				-1,
				context.getString(R.string.no_preset),
				0,
				0,
				null
		});

		MergeCursor mergeCursor = new MergeCursor(new Cursor[]{
				matrixCursor,
				ShaderEditorApp.db.getTextures(null)
		});

		adapter = new TextureSpinnerAdapter(context, mergeCursor);

		presetView = findViewById(R.id.backbuffer_preset);
		presetView.setAdapter(adapter);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		adapter.changeCursor(null);
	}

	public void setParameters(BackBufferParameters tp) {
		Cursor cursor = (Cursor) presetView.getSelectedItem();
		if (cursor == null) {
			return;
		}
		long id = Database.getLong(cursor, Database.TEXTURES_ID);
		if (id > 0 && tp != null) {
			tp.setPreset(Database.getString(cursor, Database.TEXTURES_NAME));
		}
	}
}
