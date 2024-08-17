package de.markusfisch.android.shadereditor.io;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ExternalFile {
	@NonNull
	public static OutputStream openExternalOutputStream(
			@NonNull Context context,
			@NonNull String fileName,
			String mimeType) throws IOException {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			File file = new File(
					Environment.getExternalStoragePublicDirectory(
							Environment.DIRECTORY_DOWNLOADS
					),
					fileName);
			if (file.exists()) {
				throw new IOException();
			}
			return new FileOutputStream(file);
		} else {
			ContentResolver resolver = context.getContentResolver();
			ContentValues cv = new ContentValues();
			cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
			cv.put(MediaStore.Downloads.MIME_TYPE, mimeType);
			Uri uri = resolver.insert(
					MediaStore.Downloads.EXTERNAL_CONTENT_URI,
					cv);
			if (uri == null) {
				throw new IOException();
			}
			OutputStream out = resolver.openOutputStream(uri);
			if (out == null) {
				throw new IOException();
			}
			return out;
		}
	}
}
