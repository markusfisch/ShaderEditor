package de.markusfisch.android.shadereditor.database.dao;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import org.jetbrains.annotations.Contract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DbUtils {
	public static int getInt(@NonNull Cursor c, @NonNull String col) {
		int i = c.getColumnIndex(col);
		return i > -1 ? c.getInt(i) : 0;
	}

	public static long getLong(@NonNull Cursor c, @NonNull String col) {
		int i = c.getColumnIndex(col);
		return i > -1 ? c.getLong(i) : 0L;
	}

	public static float getFloat(@NonNull Cursor c, @NonNull String col) {
		int i = c.getColumnIndex(col);
		return i > -1 ? c.getFloat(i) : 0f;
	}

	public static String getString(@NonNull Cursor c, @NonNull String col) {
		int i = c.getColumnIndex(col);
		return i > -1 ? c.getString(i) : "";
	}

	@Nullable
	public static byte[] getBlob(@NonNull Cursor c, @NonNull String col) {
		int i = c.getColumnIndex(col);
		return i > -1 ? c.getBlob(i) : null;
	}

	@NonNull
	public static String loadRawResource(@NonNull Context context, @RawRes int id) throws IOException {
		try (var in = context.getResources().openRawResource(id)) {
			var b = new byte[in.available()];
			if (in.read(b) > 0) return new String(b, StandardCharsets.UTF_8);
			return "";
		}
	}

	@NonNull
	public static byte[] loadBitmapResource(@NonNull Context context, int id) {
		return bitmapToPng(BitmapFactory.decodeResource(context.getResources(), id));
	}

	@NonNull
	@Contract("null -> new")
	public static byte[] bitmapToPng(Bitmap bitmap) {
		if (bitmap == null) return new byte[0];
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.getColorSpace() != null &&
				!bitmap.getColorSpace().equals(ColorSpace.get(ColorSpace.Named.SRGB))) {
			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		}
		var out = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
		return out.toByteArray();
	}
}
