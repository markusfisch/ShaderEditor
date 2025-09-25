package de.markusfisch.android.shadereditor.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class BitmapEditor {
	@NonNull
	@Contract("null -> new")
	public static byte[] encodeAsPng(Bitmap bitmap) {
		if (bitmap == null) {
			return new byte[0];
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
				bitmap.getColorSpace() != null &&
				!bitmap.getColorSpace().equals(ColorSpace.get(ColorSpace.Named.SRGB))) {
			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		}
		var out = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
		return out.toByteArray();
	}

	@Nullable
	public static Bitmap getBitmapFromUri(
			@NonNull Context context,
			Uri uri,
			int maxSize) {
		InputStream in = null;
		try {
			in = context.getContentResolver().openInputStream(uri);
			if (in == null) {
				return null;
			}
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPremultiplied = false;
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
					// Ignore.
				}
			}
		}
	}

	public static Bitmap crop(
			Bitmap bitmap,
			RectF rect,
			float rotation) {

		if (bitmap == null || bitmap.isRecycled()) {
			return null;
		}

		try {
			Bitmap sourceBitmap = bitmap;
			boolean sourceBitmapIsTemp = false;

			// 1. Handle rotation.
			// This is done before cropping to simplify the cropping logic.
			if (rotation % 360f != 0) {
				sourceBitmap = rotateBitmapManual(bitmap, rotation);
				if (sourceBitmap != bitmap) {
					sourceBitmapIsTemp = true;
				}
			}

			// 2. Calculate the crop area in pixels based on the (possibly rotated)
			// source bitmap's dimensions.
			int srcWidth = sourceBitmap.getWidth();
			int srcHeight = sourceBitmap.getHeight();
			int cropX = Math.round(rect.left * srcWidth);
			int cropY = Math.round(rect.top * srcHeight);
			int cropWidth = Math.round(rect.width() * srcWidth);
			int cropHeight = Math.round(rect.height() * srcHeight);

			// 3. Validate crop dimensions.
			if (cropWidth <= 0 || cropHeight <= 0 || cropX < 0 || cropY < 0 ||
					cropX + cropWidth > srcWidth || cropY + cropHeight > srcHeight) {
				Log.e("BitmapEditor", "Invalid crop parameters.");
				if (sourceBitmapIsTemp) {
					sourceBitmap.recycle();
				}
				return null;
			}

			// 4. Extract the rectangular block of pixels from the source bitmap.
			// getPixels() always returns non-premultiplied ARGB values.
			int[] croppedPixels = new int[cropWidth * cropHeight];
			sourceBitmap.getPixels(croppedPixels, 0, cropWidth, cropX, cropY, cropWidth,
					cropHeight);

			// If a temporary rotated bitmap was created, it's no longer needed.
			if (sourceBitmapIsTemp) {
				sourceBitmap.recycle();
			}

			// 5. Create the final bitmap from the array of cropped pixels.
			// We cannot use createBitmap(pixels...) as that always creates a
			// premultiplied bitmap. Instead, we create an empty mutable bitmap
			// and set its pixels, which respects the non-premultiplied flag.
			Bitmap croppedBitmap = Bitmap.createBitmap(cropWidth, cropHeight,
					Objects.requireNonNull(bitmap.getConfig())); // Use original bitmap's config
			// for safety.
			croppedBitmap.setPremultiplied(false);
			croppedBitmap.setPixels(croppedPixels, 0, cropWidth, 0, 0, cropWidth, cropHeight);

			return croppedBitmap;

		} catch (OutOfMemoryError | IllegalArgumentException e) {
			Log.e("BitmapEditor", "Failed to crop bitmap", e);
			return null;
		}
	}

	/**
	 * Scales a bitmap using manual bilinear interpolation to preserve color data
	 * in pixels with zero alpha. This method avoids the Android Canvas and its
	 * destructive premultiplication behavior.
	 *
	 * @param src       The source bitmap, which must be non-premultiplied.
	 * @param dstWidth  The width of the new bitmap.
	 * @param dstHeight The height of the new bitmap.
	 * @return The new scaled bitmap, in a non-premultiplied state.
	 */
	@NonNull
	public static Bitmap createScaledBitmapManual(
			@NonNull Bitmap src, int dstWidth, int dstHeight) {

		int srcWidth = src.getWidth();
		int srcHeight = src.getHeight();

		int[] srcPixels = new int[srcWidth * srcHeight];
		src.getPixels(srcPixels, 0, srcWidth, 0, 0, srcWidth, srcHeight);

		int[] dstPixels = new int[dstWidth * dstHeight];

		float xRatio = (float) (srcWidth - 1) / dstWidth;
		float yRatio = (float) (srcHeight - 1) / dstHeight;

		for (int y = 0; y < dstHeight; y++) {
			for (int x = 0; x < dstWidth; x++) {
				float gx = x * xRatio;
				float gy = y * yRatio;

				int gxi = (int) gx;
				int gyi = (int) gy;

				int c00 = srcPixels[gyi * srcWidth + gxi];
				int c10 = srcPixels[gyi * srcWidth + gxi + 1];
				int c01 = srcPixels[(gyi + 1) * srcWidth + gxi];
				int c11 = srcPixels[(gyi + 1) * srcWidth + gxi + 1];

				float fracX = gx - gxi;
				float fracY = gy - gyi;

				// Interpolate each channel (A, R, G, B) separately.
				int a = (int) interpolate(
						interpolate((c00 >> 24) & 0xff, (c10 >> 24) & 0xff, fracX),
						interpolate((c01 >> 24) & 0xff, (c11 >> 24) & 0xff, fracX),
						fracY);

				int r = (int) interpolate(
						interpolate((c00 >> 16) & 0xff, (c10 >> 16) & 0xff, fracX),
						interpolate((c01 >> 16) & 0xff, (c11 >> 16) & 0xff, fracX),
						fracY);

				int g = (int) interpolate(
						interpolate((c00 >> 8) & 0xff, (c10 >> 8) & 0xff, fracX),
						interpolate((c01 >> 8) & 0xff, (c11 >> 8) & 0xff, fracX),
						fracY);

				int b = (int) interpolate(
						interpolate(c00 & 0xff, c10 & 0xff, fracX),
						interpolate(c01 & 0xff, c11 & 0xff, fracX),
						fracY);

				dstPixels[y * dstWidth + x] = (a << 24) | (r << 16) | (g << 8) | b;
			}
		}

		Bitmap finalBitmap = Bitmap.createBitmap(
				dstWidth, dstHeight, Objects.requireNonNull(src.getConfig()));
		finalBitmap.setPremultiplied(false);
		finalBitmap.setPixels(dstPixels, 0, dstWidth, 0, 0, dstWidth, dstHeight);

		return finalBitmap;
	}

	@SuppressWarnings("SuspiciousNameCombination")
	private static Bitmap rotateBitmapManual(Bitmap src, float degrees) {
		if (src == null || degrees % 360 == 0) {
			return src;
		}

		final int width = src.getWidth();
		final int height = src.getHeight();
		final Bitmap.Config config = src.getConfig();

		final int[] srcPixels = new int[width * height];
		// getPixels returns non-premultiplied ARGB values.
		src.getPixels(srcPixels, 0, width, 0, 0, width, height);

		final int[] dstPixels;
		final int newWidth, newHeight;

		final int rotation = (int) (degrees + 360) % 360;

		switch (rotation) {
			case 90:
				newWidth = height;
				newHeight = width;
				dstPixels = new int[newWidth * newHeight];
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						dstPixels[x * newWidth + (newWidth - 1 - y)] = srcPixels[y * width + x];
					}
				}
				break;
			case 180:
				newWidth = width;
				newHeight = height;
				dstPixels = new int[newWidth * newHeight];
				for (int i = 0; i < srcPixels.length; i++) {
					dstPixels[srcPixels.length - 1 - i] = srcPixels[i];
				}
				break;
			case 270:
				newWidth = height;
				newHeight = width;
				dstPixels = new int[newWidth * newHeight];
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						dstPixels[((newHeight - 1 - x) * newWidth) + y] = srcPixels[y * width + x];
					}
				}
				break;
			default:
				// Should not happen as UI only allows 90 degree increments
				return src;
		}

		assert config != null;
		Bitmap rotatedBitmap = Bitmap.createBitmap(newWidth, newHeight, config);
		// The created bitmap must be flagged as non-premultiplied because
		// the pixel data from getPixels() and our manual transformation is
		// non-premultiplied.
		rotatedBitmap.setPremultiplied(false);
		rotatedBitmap.setPixels(dstPixels, 0, newWidth, 0, 0, newWidth, newHeight);

		return rotatedBitmap;
	}

	private static float interpolate(float a, float b, float frac) {
		return a * (1 - frac) + b * frac;
	}

	private static void setSampleSize(
			@NonNull BitmapFactory.Options options,
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
