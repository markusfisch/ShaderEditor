package de.markusfisch.android.shadereditor.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

public class BitmapEditor {
	public static Bitmap getBitmapFromUri(
			Context context,
			Uri uri,
			int maxSize) {
		InputStream in = null;
		try {
			in = context.getContentResolver().openInputStream(uri);
			if (in == null) {
				return null;
			}
			BitmapFactory.Options options = new BitmapFactory.Options();
			setSampleSize(options, in, maxSize, maxSize);

			in.close();
			in = context.getContentResolver().openInputStream(uri);

			return BitmapFactory.decodeStream(in, null, options);
		} catch (OutOfMemoryError | SecurityException | IOException e) {
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	public static Bitmap crop(
			Bitmap bitmap,
			RectF rect,
			float rotation) {
		if (bitmap == null) {
			return null;
		}

		try {
			if (rotation % 360f != 0) {
				Matrix matrix = new Matrix();
				matrix.setRotate(rotation);

				bitmap = Bitmap.createBitmap(
						bitmap,
						0,
						0,
						bitmap.getWidth(),
						bitmap.getHeight(),
						matrix,
						true);
			}

			float w = bitmap.getWidth();
			float h = bitmap.getHeight();

			return Bitmap.createBitmap(
					bitmap,
					Math.round(rect.left * w),
					Math.round(rect.top * h),
					Math.round(rect.width() * w),
					Math.round(rect.height() * h));
		} catch (OutOfMemoryError | IllegalArgumentException e) {
			return null;
		}
	}

	private static void setSampleSize(
			BitmapFactory.Options options,
			InputStream in,
			int maxWidth,
			int maxHeight) {
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(in, null, options);
		options.inJustDecodeBounds = false;
		options.inSampleSize = calculateSampleSize(
				options.outWidth,
				options.outHeight,
				maxWidth,
				maxHeight);
	}

	private static int calculateSampleSize(
			int width,
			int height,
			int maxWidth,
			int maxHeight) {
		int size = 1;

		if (width > maxWidth || height > maxHeight) {
			final int hw = width / 2;
			final int hh = height / 2;

			while (hw / size > maxWidth && hh / size > maxHeight) {
				size *= 2;
			}
		}

		return size;
	}
}
