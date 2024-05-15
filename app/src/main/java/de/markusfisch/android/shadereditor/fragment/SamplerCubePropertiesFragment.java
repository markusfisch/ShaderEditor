package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.graphics.BitmapEditor;
import de.markusfisch.android.shadereditor.widget.CubeMapView;

public class SamplerCubePropertiesFragment extends AbstractSamplerPropertiesFragment {
	private static final String FACES = "faces";

	private CubeMapView.Face[] faces;

	public static Fragment newInstance(CubeMapView.Face[] faces) {
		Bundle args = new Bundle();
		args.putParcelableArray(FACES, faces);

		SamplerCubePropertiesFragment fragment =
				new SamplerCubePropertiesFragment();
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			ViewGroup container,
			Bundle state) {
		Activity activity = getActivity();
		if (activity == null) {
			return null;
		}
		activity.setTitle(R.string.texture_properties);

		Bundle args;
		View view;

		if ((args = getArguments()) == null ||
				(faces = (CubeMapView.Face[]) args.getParcelableArray(
						FACES)) == null ||
				(view = initView(
						activity,
						inflater,
						container)) == null) {
			activity.finish();
			return null;
		}

		setSizeCaption(getString(R.string.face_size));
		setMaxValue(7);
		setSamplerType(SAMPLER_CUBE);

		return view;
	}

	@Override
	protected int saveSampler(
			Context context,
			String name,
			int size) {
		int width = size * 2;
		int height = size * 3;
		Bitmap mapBitmap = Bitmap.createBitmap(
				width,
				height,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(mapBitmap);
		int x = 0;
		int y = 0;

		for (int i = 0, l = 6; i < l; ++i) {
			if (faces[i].getUri() == null) {
				return R.string.cannot_pick_image;
			}

			RectF clip = faces[i].getClip();
			float rotation = faces[i].getRotation();

			float nw = clip.width();
			int max = Math.round(size + size / nw * (1f - nw));

			Bitmap bitmap = BitmapEditor.getBitmapFromUri(
					context,
					faces[i].getUri(),
					max);

			if (bitmap == null ||
					(bitmap = BitmapEditor.crop(
							bitmap, clip, rotation)) == null) {
				return R.string.cannot_pick_image;
			}

			bitmap = Bitmap.createScaledBitmap(
					bitmap, size, size, true);
			canvas.drawBitmap(bitmap, x, y, null);
			bitmap.recycle();

			x += size;

			if (x >= width) {
				y += size;
				x = 0;
			}
		}

		return ShaderEditorApp.db.insertTexture(name, mapBitmap) < 1
				? R.string.name_already_taken
				: 0;
	}
}
