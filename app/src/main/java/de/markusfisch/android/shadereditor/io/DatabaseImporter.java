package de.markusfisch.android.shadereditor.io;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

public class DatabaseImporter {
	public static boolean importDatabase(Context context, Uri uri) {
		if (uri == null) {
			return false;
		}
		ContentResolver cr = context.getContentResolver();
		if (cr == null) {
			return false;
		}
		final String fileName = "import.db";
		InputStream in = null;
		OutputStream out = null;
		try {
			in = cr.openInputStream(uri);
			if (in == null) {
				return false;
			}
			out = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			byte[] buffer = new byte[4096];
			int len;
			while ((len = in.read(buffer)) != -1) {
				out.write(buffer, 0, len);
			}
		} catch (IOException e) {
			return false;
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
		boolean success = ShaderEditorApp.db.importDatabase(context, fileName);
		context.deleteFile(fileName);
		return success;
	}
}
