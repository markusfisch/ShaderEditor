package de.markusfisch.android.shadereditor.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BitmapEditor {
	@NonNull
	@Contract("null -> new")
	public static byte[] encodeAsPng(Bitmap bitmap) {
		if (bitmap == null) {
			return new byte[0];
		}
		Bitmap bitmapToEncode = bitmap;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
				bitmap.getColorSpace() != null &&
				!bitmap.getColorSpace().equals(ColorSpace.get(ColorSpace.Named.SRGB))) {
			Bitmap convertedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
			if (convertedBitmap != null) {
				bitmapToEncode = convertedBitmap;
			}
		}
		try {
			var out = new ByteArrayOutputStream();
			bitmapToEncode.compress(Bitmap.CompressFormat.PNG, 100, out);
			return out.toByteArray();
		} finally {
			if (bitmapToEncode != bitmap && !bitmapToEncode.isRecycled()) {
				bitmapToEncode.recycle();
			}
		}
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

	@Nullable
	public static Bitmap getBitmapFromResource(
			@NonNull Context context,
			@DrawableRes int resId) {
		InputStream in = null;
		try {
			in = context.getResources().openRawResource(resId);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPremultiplied = false;
			return BitmapFactory.decodeStream(in, null, options);
		} catch (OutOfMemoryError | RuntimeException e) {
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
			Bitmap croppedBitmap = Bitmap.createBitmap(
					cropWidth,
					cropHeight,
					getSafeConfig(bitmap));
			croppedBitmap.setPremultiplied(false);
			croppedBitmap.setPixels(croppedPixels, 0, cropWidth, 0, 0, cropWidth, cropHeight);

			return croppedBitmap;

		} catch (OutOfMemoryError | IllegalArgumentException e) {
			Log.e("BitmapEditor", "Failed to crop bitmap", e);
			return null;
		}
	}

	@NonNull
	public static Bitmap createScaledBitmap(
			@NonNull Bitmap src,
			int dstWidth,
			int dstHeight) {
		return createScaledBitmapManual(src, dstWidth, dstHeight);
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
		if (src.isRecycled()) {
			throw new IllegalArgumentException("Cannot scale a recycled bitmap");
		}
		if (dstWidth <= 0 || dstHeight <= 0) {
			throw new IllegalArgumentException("dstWidth and dstHeight must be > 0");
		}

		int srcWidth = src.getWidth();
		int srcHeight = src.getHeight();

		int[] srcPixels = new int[srcWidth * srcHeight];
		src.getPixels(srcPixels, 0, srcWidth, 0, 0, srcWidth, srcHeight);

		int[] dstPixels = new int[dstWidth * dstHeight];

		float xRatio = dstWidth > 1
				? (float) (srcWidth - 1) / (dstWidth - 1)
				: 0f;
		float yRatio = dstHeight > 1
				? (float) (srcHeight - 1) / (dstHeight - 1)
				: 0f;

		for (int y = 0; y < dstHeight; y++) {
			float gy = y * yRatio;
			int gyi = (int) gy;
			int gyi1 = Math.min(gyi + 1, srcHeight - 1);
			float fracY = gy - gyi;

			for (int x = 0; x < dstWidth; x++) {
				float gx = x * xRatio;
				int gxi = (int) gx;
				int gxi1 = Math.min(gxi + 1, srcWidth - 1);

				int c00 = srcPixels[gyi * srcWidth + gxi];
				int c10 = srcPixels[gyi * srcWidth + gxi1];
				int c01 = srcPixels[gyi1 * srcWidth + gxi];
				int c11 = srcPixels[gyi1 * srcWidth + gxi1];

				float fracX = gx - gxi;

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
				dstWidth, dstHeight, getSafeConfig(src));
		finalBitmap.setPremultiplied(false);
		finalBitmap.setPixels(dstPixels, 0, dstWidth, 0, 0, dstWidth, dstHeight);

		return finalBitmap;
	}

	@NonNull
	public static Bitmap createDisplayBitmap(@NonNull Bitmap bitmap) {
		if (!bitmap.hasAlpha() || bitmap.isPremultiplied()) {
			return bitmap;
		}

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

		Bitmap displayBitmap = Bitmap.createBitmap(width, height, getSafeConfig(bitmap));
		displayBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return displayBitmap;
	}

	public static void copyBitmap(
			@NonNull Bitmap src,
			@NonNull Bitmap dst,
			int dstX,
			int dstY) {
		int width = src.getWidth();
		int height = src.getHeight();
		int[] pixels = new int[width * height];
		src.getPixels(pixels, 0, width, 0, 0, width, height);
		dst.setPixels(pixels, 0, width, dstX, dstY, width, height);
	}

	@NonNull
	public static ByteBuffer createRgbaBuffer(@NonNull Bitmap bitmap, boolean flipY) {
		if (bitmap.isRecycled()) {
			throw new IllegalArgumentException("Cannot copy pixels from a recycled bitmap");
		}

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

		ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4)
				.order(ByteOrder.nativeOrder());
		for (int y = 0; y < height; ++y) {
			int srcY = flipY ? height - y - 1 : y;
			int rowOffset = srcY * width;
			for (int x = 0; x < width; ++x) {
				int pixel = pixels[rowOffset + x];
				buffer.put((byte) ((pixel >> 16) & 0xff));
				buffer.put((byte) ((pixel >> 8) & 0xff));
				buffer.put((byte) (pixel & 0xff));
				buffer.put((byte) ((pixel >> 24) & 0xff));
			}
		}
		buffer.position(0);
		return buffer;
	}

	@NonNull
	public static Bitmap createBitmapFromRgbaBuffer(
			@NonNull ByteBuffer rgba,
			int width,
			int height,
			boolean flipY) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("width and height must be > 0");
		}

		ByteBuffer pixelsBuffer = rgba.duplicate();
		pixelsBuffer.position(0);

		int[] pixels = new int[width * height];
		for (int y = 0; y < height; ++y) {
			int dstY = flipY ? height - y - 1 : y;
			int rowOffset = dstY * width;
			for (int x = 0; x < width; ++x) {
				int r = pixelsBuffer.get() & 0xff;
				int g = pixelsBuffer.get() & 0xff;
				int b = pixelsBuffer.get() & 0xff;
				int a = pixelsBuffer.get() & 0xff;
				pixels[rowOffset + x] = (a << 24) | (r << 16) | (g << 8) | b;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setPremultiplied(false);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}

	@SuppressWarnings("SuspiciousNameCombination")
	private static Bitmap rotateBitmapManual(Bitmap src, float degrees) {
		if (src == null || degrees % 360 == 0) {
			return src;
		}

		final int width = src.getWidth();
		final int height = src.getHeight();

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

		Bitmap rotatedBitmap = Bitmap.createBitmap(newWidth, newHeight, getSafeConfig(src));
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

	@NonNull
	private static Bitmap.Config getSafeConfig(@NonNull Bitmap bitmap) {
		Bitmap.Config config = bitmap.getConfig();
		return config != null && config != Bitmap.Config.HARDWARE
				? config
				: Bitmap.Config.ARGB_8888;
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
