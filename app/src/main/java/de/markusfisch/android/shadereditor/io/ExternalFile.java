package de.markusfisch.android.shadereditor.io;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public class ExternalFile {
	public static OutputStream openExternalOutputStream(
			Context context,
			String fileName,
			String mimeType) throws IOException {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			File directory = Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_DOWNLOADS);
			// The directory may not exist yet.
			directory.mkdirs();
			return new FileOutputStream(uniqueFile(directory, fileName));
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

	// Keeps an earlier file of the same name, as MediaStore does.
	static File uniqueFile(File directory, String fileName) {
		int dot = fileName.lastIndexOf('.');
		if (dot < 0) {
			dot = fileName.length();
		}
		File file = new File(directory, fileName);
		for (int i = 1; file.exists(); ++i) {
			file = new File(directory, String.format(Locale.US, "%s_%d%s",
					fileName.substring(0, dot), i, fileName.substring(dot)));
		}
		return file;
	}
}
