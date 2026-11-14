package de.markusfisch.android.shadereditor.io;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.Database;
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

		File snapshot = null;
		try {
			snapshot = File.createTempFile("shader-backup-", ".db", context.getCacheDir());
			Database.getInstance(context).createSnapshot(snapshot);
			return exportSnapshot(context, snapshot, fileName);
		} catch (IOException | SQLException e) {
			return R.string.storage_not_writeable;
		} finally {
			if (snapshot != null) {
				SQLiteDatabase.deleteDatabase(snapshot);
			}
		}
	}

	private static int exportSnapshot(Context context, File snapshot, String fileName) throws IOException {
		try (FileInputStream in = new FileInputStream(snapshot);
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
		}
	}
}
