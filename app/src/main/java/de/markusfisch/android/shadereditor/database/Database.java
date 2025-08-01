package de.markusfisch.android.shadereditor.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.DatabaseContract.ShaderColumns;
import de.markusfisch.android.shadereditor.database.DatabaseContract.TextureColumns;

/**
 * Manages the SQLiteOpenHelper lifecycle and provides a single access point
 * to the database via the DataSource. This class is a singleton.
 */
public final class Database {
	private static volatile Database instance;
	private final OpenHelper dbHelper;
	private final DataSource dataSource;
	private final Context appContext;

	private Database(@NonNull Context context) {
		// Use application context to avoid memory leaks.
		this.appContext = context.getApplicationContext();
		this.dbHelper = new OpenHelper(appContext);
		this.dataSource = new DataSource(dbHelper, appContext);
	}

	/**
	 * Gets the singleton instance of the Database manager.
	 *
	 * @param context The application context.
	 * @return The singleton Database instance.
	 */
	public static synchronized Database getInstance(Context context) {
		if (instance == null) {
			instance = new Database(context);
		}
		return instance;
	}

	/**
	 * Provides access to all data query and manipulation methods.
	 *
	 * @return The DataSource instance for performing database operations.
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	public String importDatabase(String tempFileName) {
		// Close the DB to release the lock on the file.
		dbHelper.close();

		File dbFile = appContext.getDatabasePath(DatabaseContract.FILE_NAME);
		File newDb = new File(appContext.getFilesDir(), tempFileName);

		try (InputStream in = new FileInputStream(newDb);
				OutputStream out = new FileOutputStream(dbFile)) {
			byte[] buf = new byte[4096];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} catch (IOException e) {
			// Try to restore the original file if copy fails.
			// This is complex, for now we just report the error.
			return e.getMessage();
		}

		return null; // Success
	}

	/**
	 * The internal SQLiteOpenHelper to manage database creation and versioning.
	 * This is kept private to encapsulate the database management.
	 */
	private static class OpenHelper extends SQLiteOpenHelper {
		private final Context context;

		private OpenHelper(Context context) {
			super(context, DatabaseContract.FILE_NAME, null, 5);
			this.context = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createShadersTable(db);
			createTexturesTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				createTexturesTable(db);
				insertInitialShaders(db);
			}
			if (oldVersion < 3) {
				DataSource.addShadersQuality(db);
			}
			if (oldVersion < 4) {
				DataSource.addTexturesWidthHeightRatio(db);
			}
			if (oldVersion < 5) {
				DataSource.addShaderNames(db);
			}
		}

		@Override
		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// To support downgrading, you might want to drop and recreate tables.
			// For now, we do nothing to prevent data loss.
		}

		private void createShadersTable(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + ShaderColumns.TABLE_NAME + " (" +
					ShaderColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
					ShaderColumns.FRAGMENT_SHADER + " TEXT NOT NULL," +
					ShaderColumns.THUMB + " BLOB," +
					ShaderColumns.NAME + " TEXT," +
					ShaderColumns.CREATED + " DATETIME," +
					ShaderColumns.MODIFIED + " DATETIME," +
					ShaderColumns.QUALITY + " REAL);");
			insertInitialShaders(db);
		}

		private void createTexturesTable(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TextureColumns.TABLE_NAME + " (" +
					TextureColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
					TextureColumns.NAME + " TEXT NOT NULL UNIQUE," +
					TextureColumns.WIDTH + " INTEGER," +
					TextureColumns.HEIGHT + " INTEGER," +
					TextureColumns.RATIO + " REAL," +
					TextureColumns.THUMB + " BLOB," +
					TextureColumns.MATRIX + " BLOB);");
			insertInitialTextures(db);
		}

		private void insertInitialShaders(SQLiteDatabase db) {
			try {
				String defaultShader = DataSource.loadRawResource(context, R.raw.default_shader);
				byte[] defaultThumb = DataSource.loadBitmapResource(context,
						R.drawable.thumbnail_default);
				DataSource.insertShader(db,
						defaultShader,
						context.getString(R.string.default_shader),
						defaultThumb,
						1f);
			} catch (IOException e) {
				// This should not happen with packaged resources.
				Toast.makeText(context, "Failed to load initial shaders", Toast.LENGTH_SHORT).show();
			}
		}

		private void insertInitialTextures(SQLiteDatabase db) {
			Bitmap noiseBitmap = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.texture_noise);
			int thumbnailSize =
					Math.round(context.getResources().getDisplayMetrics().density * 48f);
			DataSource.insertTexture(db,
					context.getString(R.string.texture_name_noise),
					noiseBitmap,
					thumbnailSize);
		}
	}
}
