package de.markusfisch.android.shadereditor.io;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.DataRecords;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.database.Database;

public class ImportExportAsFiles {
	private static final String IMPORT_EXPORT_DIRECTORY = "ShaderEditor";
	private static final String SHADER_FILE_EXTENSION = ".glsl";
	private static final String ILLEGAL_CHARACTER_REPLACEMENT = "_";

	public static void importFromDirectory(Context context) {
		try {
			DataSource dataSource = Database.getInstance(context).getDataSource();
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
						// Use the modern DataSource to insert the shader.
						dataSource.shader.insertShader(fragmentShader, shaderName, null, 1f);
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

	public static void exportToDirectory(Context context) {
		try {
			DataSource dataSource = Database.getInstance(context).getDataSource();
			File exportDirectory = getImportExportDirectory(context, true);

			// Get a list of shader info objects instead of a cursor.
			List<DataRecords.ShaderInfo> shaderInfos = dataSource.shader.getShaders(false);
			if (shaderInfos.isEmpty()) {
				throw new IOException(context.getString(R.string.no_shaders_found));
			}

			int successCount = 0;
			int failCount = 0;

			for (DataRecords.ShaderInfo info : shaderInfos) {
				// Get the full shader object to access the fragment shader text.
				DataRecords.Shader fullShader = dataSource.shader.getShader(info.id());
				if (fullShader == null) {
					failCount++;
					continue;
				}

				String shaderName = info.name();
				if (shaderName == null || shaderName.isEmpty()) {
					shaderName = info.modified(); // Fallback to modified date.
				}
				String fragmentShader = fullShader.fragmentShader();

				try {
					writeShaderToDirectory(exportDirectory, shaderName, fragmentShader);
					successCount++;
				} catch (IOException e) {
					failCount++;
				}
			}

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
			String shaderName,
			String fragmentShader) throws IOException {
		String filename = shaderName.replaceAll("[^a-zA-Z0-9 \\-_,()]",
				ILLEGAL_CHARACTER_REPLACEMENT);
		File shaderFile = new File(directory, filename + SHADER_FILE_EXTENSION);
		int fileCounter = 1;
		while (shaderFile.exists()) {
			shaderFile = new File(directory, String.format(Locale.US, "%s_%d%s",
					filename, fileCounter++, SHADER_FILE_EXTENSION));
		}

		try (OutputStream os = new FileOutputStream(shaderFile)) {
			os.write(fragmentShader.getBytes(StandardCharsets.UTF_8));
		}
	}

	@NonNull
	private static File getImportExportDirectory(
			Context context,
			boolean create) throws IOException {
		File downloadsDirectory = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS);
		File file = new File(downloadsDirectory, IMPORT_EXPORT_DIRECTORY);

		// A path is valid if it's already a directory.
		boolean isValid = file.isDirectory();

		// If it's not valid, but we're allowed to, try to create it.
		if (!isValid && create) {
			// Attempt creation. The path becomes valid if mkdirs succeeds.
			isValid = file.mkdirs();
		}

		// If the path is still not a valid directory after all checks, throw.
		if (!isValid) {
			throw new IOException(context.getString(
					R.string.path_is_no_directory, file.getAbsolutePath()));
		}

		return file;
	}

	private static String readFile(File file) throws IOException {
		try (InputStream fis = new FileInputStream(file)) {
			StringBuilder sb = new StringBuilder();
			byte[] buffer = new byte[1024];
			int n;
			while ((n = fis.read(buffer)) != -1) {
				sb.append(new String(buffer, 0, n, StandardCharsets.UTF_8));
			}
			return sb.toString();
		}
	}
}