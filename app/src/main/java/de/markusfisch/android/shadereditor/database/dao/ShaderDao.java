package de.markusfisch.android.shadereditor.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.DataRecords;
import de.markusfisch.android.shadereditor.database.DatabaseContract;
import de.markusfisch.android.shadereditor.database.DatabaseTable;
import de.markusfisch.android.shadereditor.resource.Resources;

public class ShaderDao {
	@NonNull
	private final SQLiteOpenHelper dbHelper;
	@NonNull
	private final Context appContext;

	public ShaderDao(@NonNull SQLiteOpenHelper dbHelper, @NonNull Context context) {
		this.dbHelper = dbHelper;
		this.appContext = context;
	}

	// region Public API
	@Nullable
	public DataRecords.Shader getShader(long id) {
		String query =
				"SELECT " + DatabaseContract.ShaderColumns._ID + "," + DatabaseContract.ShaderColumns.FRAGMENT_SHADER + "," + DatabaseContract.ShaderColumns.NAME + "," +
						DatabaseContract.ShaderColumns.MODIFIED + "," + DatabaseContract.ShaderColumns.QUALITY + " FROM " + DatabaseContract.ShaderColumns.TABLE_NAME + " WHERE " + DatabaseContract.ShaderColumns._ID + " = ?";

		try (var db = dbHelper.getReadableDatabase();
				var cursor = db.rawQuery(query, new String[]{String.valueOf(id)})) {
			if (cursor.moveToFirst()) {
				return new DataRecords.Shader(
						CursorHelpers.getLong(cursor, DatabaseContract.ShaderColumns._ID),
						CursorHelpers.getString(cursor,
								DatabaseContract.ShaderColumns.FRAGMENT_SHADER),
						CursorHelpers.getString(cursor, DatabaseContract.ShaderColumns.NAME),
						CursorHelpers.getString(cursor, DatabaseContract.ShaderColumns.MODIFIED),
						CursorHelpers.getFloat(cursor, DatabaseContract.ShaderColumns.QUALITY));
			}
		}
		return null;
	}

	public List<DataRecords.ShaderInfo> getShaders(boolean sortByLastModification) {
		List<DataRecords.ShaderInfo> shaders = new ArrayList<>();
		String query =
				"SELECT " + DatabaseContract.ShaderColumns._ID + "," + DatabaseContract.ShaderColumns.THUMB + "," + DatabaseContract.ShaderColumns.NAME + "," + DatabaseContract.ShaderColumns.MODIFIED +
						" FROM " + DatabaseContract.ShaderColumns.TABLE_NAME + " ORDER BY " + (sortByLastModification ?
						DatabaseContract.ShaderColumns.MODIFIED + " DESC" :
						DatabaseContract.ShaderColumns._ID);

		try (var db = dbHelper.getReadableDatabase();
				var cursor = db.rawQuery(query, null)) {
			if (cursor.moveToFirst()) {
				do {
					shaders.add(new DataRecords.ShaderInfo(
							CursorHelpers.getLong(cursor, DatabaseContract.ShaderColumns._ID),
							CursorHelpers.getString(cursor, DatabaseContract.ShaderColumns.NAME),
							CursorHelpers.getString(cursor,
									DatabaseContract.ShaderColumns.MODIFIED),
							CursorHelpers.getBlob(cursor, DatabaseContract.ShaderColumns.THUMB)));
				} while (cursor.moveToNext());
			}
		}
		return shaders;
	}

	@Nullable
	public DataRecords.Shader getRandomShader() {
		String query =
				"SELECT " + DatabaseContract.ShaderColumns._ID + "," + DatabaseContract.ShaderColumns.FRAGMENT_SHADER + "," + DatabaseContract.ShaderColumns.NAME + "," +
						DatabaseContract.ShaderColumns.MODIFIED + "," + DatabaseContract.ShaderColumns.QUALITY + " FROM " + DatabaseContract.ShaderColumns.TABLE_NAME +
						" ORDER BY RANDOM() LIMIT 1";

		try (var db = dbHelper.getReadableDatabase();
				var cursor = db.rawQuery(query, null)) {
			if (cursor.moveToFirst()) {
				return new DataRecords.Shader(
						CursorHelpers.getLong(cursor, DatabaseContract.ShaderColumns._ID),
						CursorHelpers.getString(cursor,
								DatabaseContract.ShaderColumns.FRAGMENT_SHADER),
						CursorHelpers.getString(cursor, DatabaseContract.ShaderColumns.NAME),
						CursorHelpers.getString(cursor, DatabaseContract.ShaderColumns.MODIFIED),
						CursorHelpers.getFloat(cursor, DatabaseContract.ShaderColumns.QUALITY));
			}
		}
		return null;
	}

	@Nullable
	public byte[] getThumbnail(long id) {
		try (var db = dbHelper.getReadableDatabase();
				var cursor = db.query(
						DatabaseContract.ShaderColumns.TABLE_NAME,
						new String[]{DatabaseContract.ShaderColumns.THUMB},
						DatabaseContract.ShaderColumns._ID + " = ?",
						new String[]{String.valueOf(id)},
						null, null, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return CursorHelpers.getBlob(cursor, DatabaseContract.ShaderColumns.THUMB);
			}
		}
		return null;
	}

	public long getFirstShaderId() {
		try (var db = dbHelper.getReadableDatabase();
				var cursor =
						db.rawQuery("SELECT " + DatabaseContract.ShaderColumns._ID + " FROM " + DatabaseContract.ShaderColumns.TABLE_NAME + " ORDER BY "
								+ DatabaseContract.ShaderColumns._ID + " LIMIT 1", null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return CursorHelpers.getLong(cursor, DatabaseContract.ShaderColumns._ID);
			}
		}
		return 0;
	}

	public long insertShaderFromResource(@NonNull Context context, @Nullable String name,
			int sourceId, int thumbId,
			float quality) {
		try {
			return insertShader(
					Resources.loadRawResource(context, sourceId),
					name,
					Resources.loadBitmapResource(context, thumbId),
					quality);
		} catch (IOException e) {
			return 0;
		}
	}

	public long insertNewShader() {
		return insertShaderFromResource(
				appContext,
				null,
				R.raw.new_shader,
				R.drawable.thumbnail_new_shader,
				1f);
	}

	public long insertShader(@NonNull String shader, @Nullable String name,
			@Nullable byte[] thumbnail, float quality) {
		try (var db = dbHelper.getWritableDatabase()) {
			return insertShader(db, shader, name, thumbnail, quality);
		}
	}

	public void updateShader(long id, @Nullable String shader, @Nullable byte[] thumbnail,
			float quality) {
		try (var db = dbHelper.getWritableDatabase()) {
			var cv = new ContentValues();
			cv.put(DatabaseContract.ShaderColumns.MODIFIED, currentTime());
			cv.put(DatabaseContract.ShaderColumns.QUALITY, quality);
			if (shader != null) {
				cv.put(DatabaseContract.ShaderColumns.FRAGMENT_SHADER, shader);
			}
			if (thumbnail != null) {
				cv.put(DatabaseContract.ShaderColumns.THUMB, thumbnail);
			}
			db.update(DatabaseContract.ShaderColumns.TABLE_NAME, cv,
					DatabaseContract.ShaderColumns._ID + " = ?",
					new String[]{String.valueOf(id)});
		}
	}

	public void updateShaderName(long id, @NonNull String name) {
		try (var db = dbHelper.getWritableDatabase()) {
			var cv = new ContentValues();
			cv.put(DatabaseContract.ShaderColumns.NAME, name);
			db.update(DatabaseContract.ShaderColumns.TABLE_NAME, cv,
					DatabaseContract.ShaderColumns._ID + " = ?",
					new String[]{String.valueOf(id)});
		}
	}

	public void removeShader(long id) {
		try (var db = dbHelper.getWritableDatabase()) {
			db.delete(DatabaseContract.ShaderColumns.TABLE_NAME,
					DatabaseContract.ShaderColumns._ID + " = ?",
					new String[]{String.valueOf(id)});
		}
	}
	// endregion

	// region Package-private static helpers
	static long insertShader(@NonNull SQLiteDatabase db, @NonNull String shader,
			@Nullable String name,
			@Nullable byte[] thumbnail,
			float quality) {
		String now = currentTime();
		var cv = new ContentValues();
		cv.put(DatabaseContract.ShaderColumns.FRAGMENT_SHADER, shader);
		cv.put(DatabaseContract.ShaderColumns.THUMB, thumbnail);
		cv.put(DatabaseContract.ShaderColumns.NAME, name);
		cv.put(DatabaseContract.ShaderColumns.CREATED, now);
		cv.put(DatabaseContract.ShaderColumns.MODIFIED, now);
		cv.put(DatabaseContract.ShaderColumns.QUALITY, quality);
		return db.insert(DatabaseContract.ShaderColumns.TABLE_NAME, null, cv);
	}

	public static long insertShader(
			@NonNull SQLiteDatabase db,
			@NonNull String shader,
			@Nullable String name,
			@NonNull String created,
			@NonNull String modified,
			@Nullable byte[] thumbnail,
			float quality) {
		ContentValues cv = new ContentValues();
		cv.put(DatabaseContract.ShaderColumns.FRAGMENT_SHADER, shader);
		cv.put(DatabaseContract.ShaderColumns.THUMB, thumbnail);
		cv.put(DatabaseContract.ShaderColumns.NAME, name);
		cv.put(DatabaseContract.ShaderColumns.CREATED, created);
		cv.put(DatabaseContract.ShaderColumns.MODIFIED, modified);
		cv.put(DatabaseContract.ShaderColumns.QUALITY, quality);
		return db.insert(DatabaseContract.ShaderColumns.TABLE_NAME, null, cv);
	}

	private static void addShaderNames(@NonNull SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + DatabaseContract.ShaderColumns.TABLE_NAME + " ADD COLUMN " + DatabaseContract.ShaderColumns.NAME + " TEXT;");
	}

	private static void addShadersQuality(@NonNull SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + DatabaseContract.ShaderColumns.TABLE_NAME + " ADD COLUMN " + DatabaseContract.ShaderColumns.QUALITY + " REAL;");
		db.execSQL("UPDATE " + DatabaseContract.ShaderColumns.TABLE_NAME + " SET " + DatabaseContract.ShaderColumns.QUALITY + " = " +
				"1;");
	}
	// endregion

	// region Private helpers
	@NonNull
	private static String currentTime() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
	}
	// endregion

	@NonNull
	@Contract("_ -> new")
	public static DatabaseTable buildSchema(@NonNull Context context) {
		return new DatabaseTable() {
			@Override
			public void onCreate(@NonNull SQLiteDatabase db) {
				createShadersTable(db);
			}

			@Override
			public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
				if (oldVersion < 3) {
					addShadersQuality(db);
				}
				if (oldVersion < 5) {
					addShaderNames(db);
				}
			}

			@Override
			public boolean onImport(@NonNull SQLiteDatabase intoDb,
					@NonNull SQLiteDatabase fromDb) {
				return importShaders(intoDb, fromDb);
			}

			private void createShadersTable(@NonNull SQLiteDatabase db) {
				db.execSQL("CREATE TABLE " + DatabaseContract.ShaderColumns.TABLE_NAME + " (" +
						DatabaseContract.ShaderColumns._ID + " INTEGER PRIMARY KEY " +
						"AUTOINCREMENT," +
						DatabaseContract.ShaderColumns.FRAGMENT_SHADER + " TEXT NOT NULL," +
						DatabaseContract.ShaderColumns.THUMB + " BLOB," +
						DatabaseContract.ShaderColumns.NAME + " TEXT," +
						DatabaseContract.ShaderColumns.CREATED + " DATETIME," +
						DatabaseContract.ShaderColumns.MODIFIED + " DATETIME," +
						DatabaseContract.ShaderColumns.QUALITY + " REAL);");
				insertInitialShaders(db);
			}

			private void insertInitialShaders(SQLiteDatabase db) {
				try {
					String defaultShader = Resources.loadRawResource(context,
							R.raw.default_shader);
					var defaultThumb = Resources.loadBitmapResource(context,
							R.drawable.thumbnail_default);
					insertShader(db,
							defaultShader,
							context.getString(R.string.default_shader),
							defaultThumb,
							1f);
				} catch (IOException e) {
					// This should not happen with packaged resources.
					Toast.makeText(context, "Failed to load initial shaders", Toast.LENGTH_SHORT).show();
				}
			}
		};
	}

	private static boolean importShaders(
			SQLiteDatabase dst,
			SQLiteDatabase src) {
		try (Cursor cursor = src.rawQuery(
				"SELECT *" +
						" FROM " + DatabaseContract.ShaderColumns.TABLE_NAME +
						" ORDER BY " + DatabaseContract.ShaderColumns._ID,
				null)) {
			if (cursor == null) {
				return false;
			}
			int shaderIndex =
					cursor.getColumnIndex(DatabaseContract.ShaderColumns.FRAGMENT_SHADER);
			int thumbIndex = cursor.getColumnIndex(DatabaseContract.ShaderColumns.THUMB);
			int nameIndex = cursor.getColumnIndex(DatabaseContract.ShaderColumns.NAME);
			int qualityIndex = cursor.getColumnIndex(DatabaseContract.ShaderColumns.QUALITY);
			boolean success = true;
			if (cursor.moveToFirst()) {
				do {
					String createdDate = CursorHelpers.getString(cursor,
							DatabaseContract.ShaderColumns.CREATED);
					String modifiedDate = CursorHelpers.getString(cursor,
							DatabaseContract.ShaderColumns.MODIFIED);
					if (createdDate == null || modifiedDate == null ||
							shaderExists(dst, createdDate, modifiedDate)) {
						continue;
					}
					long shaderId = insertShader(dst,
							cursor.getString(shaderIndex),
							cursor.getString(nameIndex),
							createdDate,
							modifiedDate,
							cursor.getBlob(thumbIndex),
							cursor.getFloat(qualityIndex));
					if (shaderId < 1) {
						success = false;
						break;
					}
				} while (cursor.moveToNext());
			}
			return success;
		}
	}

	private static boolean shaderExists(
			SQLiteDatabase db,
			String createdDate,
			String modifiedDate) {
		try (Cursor cursor = db.rawQuery(
				"SELECT " + DatabaseContract.ShaderColumns._ID +
						" FROM " + DatabaseContract.ShaderColumns.TABLE_NAME +
						" WHERE " + DatabaseContract.ShaderColumns.CREATED + " = ?" +
						" AND " + DatabaseContract.ShaderColumns.MODIFIED + " = ?",
				new String[]{createdDate, modifiedDate})) {
			return cursor != null && cursor.moveToFirst() && cursor.getCount() > 0;
		}
	}
}