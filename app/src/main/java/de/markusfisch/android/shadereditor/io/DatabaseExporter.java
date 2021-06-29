package de.markusfisch.android.shadereditor.io;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.Database;

public class DatabaseExporter {
	public static int exportDatabase(Context context) {
		final File current = new File(Environment.getDataDirectory(),
				"//data//" + context.getPackageName() +
						"//databases//" + Database.FILE_NAME);
		if (!current.exists()) {
			return R.string.cant_find_db;
		}
		String fileName = String.format("shader-editor-backup-%s.db",
				new SimpleDateFormat("yyyy-MM-dd",
						Locale.getDefault()).format(new Date()));
		OutputStream out = null;
		FileInputStream in = null;
		try {
			out = ExternalFile.openExternalOutputStream(context, fileName,
					"application/vnd.sqlite3");
			in = new FileInputStream(current);
			byte[] buffer = new byte[4096];
			int len;
			while ((len = in.read(buffer)) != -1) {
				out.write(buffer, 0, len);
			}
			return R.string.successfully_exported;
		} catch (IOException e) {
			return R.string.storage_not_writeable;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				// ignore, can't do anything about it
			}
		}
	}
}
