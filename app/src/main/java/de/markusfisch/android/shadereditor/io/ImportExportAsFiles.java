package de.markusfisch.android.shadereditor.io;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.Database;

public class ImportExportAsFiles {
	private static final String IMPORT_EXPORT_DIRECTORY = "ShaderEditor";
	private static final String SHADER_FILE_EXTENSION = ".glsl";
	private static final String ILLEGAL_CHARACTER_REPLACEMENT = "_";

	public static void importFromDirectory(@NonNull Context context) {
		try {
			File importDirectory = getImportExportDirectory(context, false);
			File[] files = importDirectory.listFiles();
			if (files == null) {
				return;
			}

			int successCount = 0;
			int failCount = 0;

			for (File file : files) {
				if (file.getName().toLowerCase(Locale.US).endsWith(SHADER_FILE_EXTENSION)) {
					try {
						String shaderName = file.getName();
						shaderName = shaderName.substring(0, shaderName.length()
								- SHADER_FILE_EXTENSION.length());
						String fragmentShader = readFile(file);
						ShaderEditorApp.db.insertShader(context, fragmentShader, shaderName);
						successCount++;
					} catch (IOException e) {
						failCount++;
					}
				}
			}

			if (successCount > 0) {
				Resources res = context.getResources();
				String str;
				if (failCount > 0) {
					str = res.getQuantityString(R.plurals.n_shaders_successfully_imported_m_failed,
							successCount, successCount, failCount);
				} else {
					str = res.getQuantityString(R.plurals.n_shaders_successfully_imported,
							successCount, successCount);
				}
				Toast.makeText(context, str, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context, R.string.no_shaders_found, Toast.LENGTH_LONG).show();
			}
		} catch (IOException e) {
			Toast.makeText(context, context.getString(R.string.import_failed, e.getMessage()),
					Toast.LENGTH_LONG).show();
		}
	}

	public static void exportToDirectory(@NonNull Context context) {
		try {
			File exportDirectory = getImportExportDirectory(context, true);

			Cursor shadersCursor = ShaderEditorApp.db.getShaders();
			if (Database.closeIfEmpty(shadersCursor)) {
				throw new IOException(context.getString(R.string.no_shaders_found));
			}

			int successCount = 0;
			int failCount = 0;

			do {
				long shaderId = Database.getLong(shadersCursor, Database.SHADERS_ID);
				Cursor shaderCursor = ShaderEditorApp.db.getShader(shaderId);
				if (Database.closeIfEmpty(shaderCursor)) {
					continue;
				}
				String shaderName = Database.getString(shaderCursor,
						Database.SHADERS_NAME);
				if (shaderName == null) {
					shaderName = Database.getString(shaderCursor,
							Database.SHADERS_MODIFIED);
				}
				String fragmentShader = Database.getString(shaderCursor,
						Database.SHADERS_FRAGMENT_SHADER);
				shaderCursor.close();

				try {
					writeShaderToDirectory(exportDirectory, shaderName, fragmentShader);
					successCount++;
				} catch (IOException e) {
					failCount++;
				}
			} while (shadersCursor.moveToNext());
			shadersCursor.close();

			if (successCount == 0) {
				throw new IOException(context.getString(R.string.no_shaders_could_be_written));
			} else {
				Resources res = context.getResources();
				String str;
				if (failCount > 0) {
					str = res.getQuantityString(R.plurals.n_shaders_successfully_exported_m_failed,
							successCount, successCount, failCount);
				} else {
					str = res.getQuantityString(R.plurals.n_shaders_successfully_exported,
							successCount, successCount);
				}
				Toast.makeText(context, str, Toast.LENGTH_LONG).show();
			}
		} catch (IOException e) {
			Toast.makeText(context, context.getString(R.string.n_shaders_export_failed,
					e.getMessage()), Toast.LENGTH_LONG).show();
		}
	}

	private static void writeShaderToDirectory(
			File directory,
			@NonNull String shaderName,
			@NonNull String fragmentShader) throws IOException {
		String filename = shaderName.replaceAll("[^a-zA-Z0-9 \\-_,()]",
				ILLEGAL_CHARACTER_REPLACEMENT);
		File shaderFile = new File(directory, filename + SHADER_FILE_EXTENSION);
		int fileCounter = 1;
		while (shaderFile.exists()) {
			shaderFile = new File(directory, String.format(Locale.US, "%s_%d%s",
					filename, fileCounter++, SHADER_FILE_EXTENSION));
		}

		FileOutputStream os = null;
		try {
			os = new FileOutputStream(shaderFile);
			os.write(fragmentShader.getBytes("UTF-8"));
		} finally {
			if (os != null) {
				os.close();
			}
		}
	}

	@NonNull
	private static File getImportExportDirectory(
			@NonNull Context context,
			boolean create) throws IOException {
		File downloadsDirectory = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS);
		File file = new File(downloadsDirectory, IMPORT_EXPORT_DIRECTORY);

		if (create && !file.mkdirs()) {
			// Throws in next block. Just to make FindBugs happy.
		}

		if (!file.isDirectory()) {
			throw new IOException(context.getString(
					R.string.path_is_no_directory, downloadsDirectory));
		}

		return file;
	}

	@NonNull
	private static String readFile(File file) throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			StringBuilder sb = new StringBuilder();
			byte[] buffer = new byte[1024];
			int n;
			while ((n = fis.read(buffer)) != -1) {
				// StandardCharsets.UTF_8 would require API level 19.
				sb.append(new String(buffer, 0, n, "UTF-8"));
			}
			return sb.toString();
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}
}
