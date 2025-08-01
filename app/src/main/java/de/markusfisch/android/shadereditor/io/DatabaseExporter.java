package de.markusfisch.android.shadereditor.io;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.DatabaseContract;

public class DatabaseExporter {
	public static int exportDatabase(Context context) {
		// Use the modern, robust way to get the database file path.
		final File current = context.getDatabasePath(DatabaseContract.FILE_NAME);

		if (!current.exists()) {
			return R.string.cant_find_db;
		}

		String fileName = String.format("shader-editor-backup-%s.db",
				new SimpleDateFormat("yyyy-MM-dd",
						Locale.getDefault()).format(new Date()));

		// Use try-with-resources to automatically close streams.
		try (FileInputStream in = new FileInputStream(current);
				OutputStream out = ExternalFile.openExternalOutputStream(
						context,
						fileName,
						"application/vnd.sqlite3")) {

			byte[] buffer = new byte[4096];
			int len;
			while ((len = in.read(buffer)) != -1) {
				out.write(buffer, 0, len);
			}

			return R.string.successfully_exported;
		} catch (IOException e) {
			return R.string.storage_not_writeable;
		}
	}
}