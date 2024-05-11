package de.markusfisch.android.shadereditor.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.graphics.BitmapEditor;

public class Sampler2dPropertiesFragment extends AbstractSamplerPropertiesFragment {
	private static final String IMAGE_URI = "image_uri";
	private static final String CROP_RECT = "crop_rect";
	private static final String ROTATION = "rotation";

	private Uri imageUri;
	private RectF cropRect;
	private float imageRotation;

	public static Fragment newInstance(
			Uri uri,
			RectF rect,
			float rotation) {
		Bundle args = new Bundle();
		args.putParcelable(IMAGE_URI, uri);
		args.putParcelable(CROP_RECT, rect);
		args.putFloat(ROTATION, rotation);

		Sampler2dPropertiesFragment fragment =
				new Sampler2dPropertiesFragment();
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
				(imageUri = args.getParcelable(
						IMAGE_URI)) == null ||
				(cropRect = args.getParcelable(
						CROP_RECT)) == null ||
				(view = initView(
						activity,
						inflater,
						container)) == null) {
			activity.finish();
			return null;
		}

		imageRotation = args.getFloat(ROTATION);

		return view;
	}

	@Override
	protected int saveSampler(
			Context context,
			String name,
			int size) {
		return saveTexture(
				// Try to get a bigger source image in
				// case the cut out is quite small.
				BitmapEditor.getBitmapFromUri(
						context,
						imageUri,
						// Which doesn't work for some devices.
						// 2048 is too much => out of memory.
						1024),
				cropRect,
				imageRotation,
				name,
				size);
	}

	private static int saveTexture(
			Bitmap bitmap,
			RectF rect,
			float rotation,
			String name,
			int size) {
		if ((bitmap = BitmapEditor.crop(
				bitmap,
				rect,
				rotation)) == null) {
			return R.string.illegal_rectangle;
		}

		if (ShaderEditorApp.db.insertTexture(
				name,
				Bitmap.createScaledBitmap(
						bitmap,
						size,
						size,
						true)) < 1) {
			return R.string.name_already_taken;
		}

		return 0;
	}
}
