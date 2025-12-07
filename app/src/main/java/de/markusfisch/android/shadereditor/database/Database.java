package de.markusfisch.android.shadereditor.database;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;

import de.markusfisch.android.shadereditor.R;

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
		var tables = DataSource.buildSchema(appContext);
		this.dbHelper = new OpenHelper(appContext, tables);
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

	@Nullable
	public String importDatabase(String tempFileName, @NonNull Uri originalUri) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		var tables = dbHelper.getTables();
		try (var helper = new ImportHelper(new ExternalDatabaseContext(appContext), tempFileName);
				var edb = helper.getReadableDatabase()) {
			db.beginTransaction();
			boolean success = true;
			for (var table : tables) {
				success &= table.onImport(db, edb);
			}
			if (success) {
				db.setTransactionSuccessful();
				return null;
			} else {
				return appContext.getString(R.string.import_failed, originalUri.toString());
			}
		} catch (SQLException e) {
			return e.getMessage();
		} finally {
			if (db.inTransaction()) {
				db.endTransaction();
			}
		}
	}

	/**
	 * The internal SQLiteOpenHelper to manage database creation and versioning.
	 * This is kept private to encapsulate the database management.
	 */
	private static class OpenHelper extends SQLiteOpenHelper {
		public List<DatabaseTable> getTables() {
			return tables;
		}

		private final List<DatabaseTable> tables;

		private OpenHelper(Context context, List<DatabaseTable> tables) {
			super(context, DatabaseContract.FILE_NAME, null, 6);
			this.tables = tables;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			for (var table : tables) {
				table.onCreate(db);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			for (var table : tables) {
				table.onUpgrade(db, oldVersion, newVersion);
			}
		}
	}

	/**
	 * SQLite OpenHelper for importing the database. It does not modify the
	 * imported database.
	 */
	private static class ImportHelper extends SQLiteOpenHelper {
		private ImportHelper(Context context, String path) {
			super(context, path, null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// Do nothing.
		}

		@Override
		public void onDowngrade(
				SQLiteDatabase db,
				int oldVersion,
				int newVersion) {
			// Do nothing, but without that method we cannot open
			// different versions.
		}

		@Override
		public void onUpgrade(
				SQLiteDatabase db,
				int oldVersion,
				int newVersion) {
			// Do nothing, but without that method we cannot open
			// different versions.
		}
	}

	public static class ExternalDatabaseContext extends ContextWrapper {
		public ExternalDatabaseContext(Context base) {
			super(base);
		}

		@Override
		public File getDatabasePath(String name) {
			return new File(getFilesDir(), name);
		}

		@Override
		public SQLiteDatabase openOrCreateDatabase(String name, int mode,
				SQLiteDatabase.CursorFactory factory,
				DatabaseErrorHandler errorHandler) {
			return openOrCreateDatabase(name, mode, factory);
		}

		@Override
		public SQLiteDatabase openOrCreateDatabase(String name, int mode,
				SQLiteDatabase.CursorFactory factory) {
			return SQLiteDatabase.openOrCreateDatabase(
					getDatabasePath(name), null);
		}
	}
}
