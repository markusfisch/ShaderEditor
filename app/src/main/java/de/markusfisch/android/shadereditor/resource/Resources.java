package de.markusfisch.android.shadereditor.resource;

import android.content.Context;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import de.markusfisch.android.shadereditor.graphics.BitmapEditor;

public class Resources {
	@NonNull
	public static String loadRawResource(@NonNull Context context,
			@RawRes int id) throws IOException {
		try (var in = context.getResources().openRawResource(id)) {
			var b = new byte[in.available()];
			if (in.read(b) > 0) {
				return new String(b, StandardCharsets.UTF_8);
			}
			return "";
		}
	}

	@NonNull
	public static byte[] loadBitmapResource(@NonNull Context context, int id) {
		return BitmapEditor.encodeAsPng(BitmapFactory.decodeResource(
				context.getResources(), id));
	}
}
