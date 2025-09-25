package de.markusfisch.android.shadereditor.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.DataRecords;
import de.markusfisch.android.shadereditor.database.DatabaseContract;
import de.markusfisch.android.shadereditor.database.DatabaseTable;
import de.markusfisch.android.shadereditor.graphics.BitmapEditor;

public class TextureDao {
	@NonNull
	private final SQLiteOpenHelper dbHelper;
	private final int textureThumbnailSize;

	public TextureDao(@NonNull SQLiteOpenHelper dbHelper, @NonNull Context context) {
		this.dbHelper = dbHelper;
		this.textureThumbnailSize =
				Math.round(context.getResources().getDisplayMetrics().density * 48f);
	}

	// region Public API
	@Nullable
	public DataRecords.TextureInfo getTextureInfo(long id) {
		var query =
				"SELECT " +
						DatabaseContract.TextureColumns._ID + "," +
						DatabaseContract.TextureColumns.NAME + "," +
						DatabaseContract.TextureColumns.WIDTH + "," +
						DatabaseContract.TextureColumns.HEIGHT + "," +
						DatabaseContract.TextureColumns.THUMB +
						" FROM " + DatabaseContract.TextureColumns.TABLE_NAME +
						" WHERE " + DatabaseContract.TextureColumns._ID + " = ?";

		try (var db = dbHelper.getReadableDatabase();
				var cursor = db.rawQuery(query, new String[]{String.valueOf(id)})) {
			if (cursor.moveToFirst()) {
				return new DataRecords.TextureInfo(
						CursorHelpers.getLong(cursor, DatabaseContract.TextureColumns._ID),
						CursorHelpers.getString(cursor, DatabaseContract.TextureColumns.NAME),
						CursorHelpers.getInt(cursor, DatabaseContract.TextureColumns.WIDTH),
						CursorHelpers.getInt(cursor, DatabaseContract.TextureColumns.HEIGHT),
						CursorHelpers.getBlob(cursor, DatabaseContract.TextureColumns.THUMB));
			}
		}
		return null;
	}

	public List<DataRecords.TextureInfo> getTextures(@Nullable String substring) {
		return getTextures(substring, false);
	}

	public List<DataRecords.TextureInfo> getSamplerCubeTextures(@Nullable String substring) {
		return getTextures(substring, true);
	}

	@Nullable
	public Bitmap getTextureBitmap(long id) {
		String query =
				"SELECT " + DatabaseContract.TextureColumns.MATRIX +
						" FROM " + DatabaseContract.TextureColumns.TABLE_NAME +
						" WHERE " + DatabaseContract.TextureColumns._ID + " = ?";
		try (var db = dbHelper.getReadableDatabase();
				var cursor = db.rawQuery(query, new String[]{String.valueOf(id)})) {
			if (cursor.moveToFirst()) {
				var data = CursorHelpers.getBlob(cursor, DatabaseContract.TextureColumns.MATRIX);
				if (data != null) {
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inPremultiplied = false;
					return BitmapFactory.decodeByteArray(data, 0, data.length, options);
				}
			}
		} catch (OutOfMemoryError e) {
			// A texture might be too big to be loaded into memory.
		}
		return null;
	}

	@Nullable
	public Bitmap getTextureBitmap(@NonNull String name) {
		String query =
				"SELECT " + DatabaseContract.TextureColumns.MATRIX +
						" FROM " + DatabaseContract.TextureColumns.TABLE_NAME +
						" WHERE " + DatabaseContract.TextureColumns.NAME + " = ?";
		try (var db = dbHelper.getReadableDatabase();
				var cursor = db.rawQuery(query, new String[]{name})) {
			if (cursor.moveToFirst()) {
				var data = CursorHelpers.getBlob(cursor, DatabaseContract.TextureColumns.MATRIX);
				if (data != null) {
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inPremultiplied = false;
					return BitmapFactory.decodeByteArray(data, 0, data.length, options);
				}
			}
		} catch (OutOfMemoryError e) {
			// A texture might be too big to be loaded into memory.
		}
		return null;
	}

	public long insertTexture(String name, Bitmap bitmap) {
		try (var db = dbHelper.getWritableDatabase()) {
			return insertTexture(db, name, bitmap, textureThumbnailSize);
		}
	}

	public void removeTexture(long id) {
		try (var db = dbHelper.getWritableDatabase()) {
			db.delete(DatabaseContract.TextureColumns.TABLE_NAME,
					DatabaseContract.TextureColumns._ID + " = ?",
					new String[]{String.valueOf(id)});
		}
	}
	// endregion

	// region Package-private static helpers
	private static long insertTexture(SQLiteDatabase db, String name, Bitmap bitmap,
			int thumbnailSize) {
		try {
			var thumbnail = BitmapEditor.createScaledBitmapManual(bitmap, thumbnailSize, thumbnailSize);
			int w = bitmap.getWidth();
			int h = bitmap.getHeight();
			return insertTexture(db, name, w, h, calculateRatio(w, h),
					BitmapEditor.encodeAsPng(thumbnail),
					BitmapEditor.encodeAsPng(bitmap));
		} catch (IllegalArgumentException e) {
			return 0;
		}
	}
	// endregion

	// region Private helpers
	@NonNull
	private List<DataRecords.TextureInfo> getTextures(@Nullable String substring,
			boolean isCubeMap) {
		List<DataRecords.TextureInfo> textures = new ArrayList<>();
		String where = DatabaseContract.TextureColumns.RATIO + (isCubeMap ? " = 1.5" : " = 1") +
				(substring != null ? " AND " + DatabaseContract.TextureColumns.NAME + " LIKE ?" :
						"");
		var args = substring != null ? new String[]{"%" + substring + "%"} : null;

		String query =
				"SELECT " +
						DatabaseContract.TextureColumns._ID + "," +
						DatabaseContract.TextureColumns.NAME + "," +
						DatabaseContract.TextureColumns.WIDTH + "," +
						DatabaseContract.TextureColumns.HEIGHT + "," +
						DatabaseContract.TextureColumns.THUMB +
						" FROM " + DatabaseContract.TextureColumns.TABLE_NAME +
						" WHERE " + where +
						" ORDER BY " + DatabaseContract.TextureColumns._ID;

		try (var db = dbHelper.getReadableDatabase();
				var cursor = db.rawQuery(query, args)) {
			if (cursor.moveToFirst()) {
				do {
					textures.add(new DataRecords.TextureInfo(
							CursorHelpers.getLong(cursor, DatabaseContract.TextureColumns._ID),
							CursorHelpers.getString(cursor, DatabaseContract.TextureColumns.NAME),
							CursorHelpers.getInt(cursor, DatabaseContract.TextureColumns.WIDTH),
							CursorHelpers.getInt(cursor, DatabaseContract.TextureColumns.HEIGHT),
							CursorHelpers.getBlob(cursor, DatabaseContract.TextureColumns.THUMB)));
				} while (cursor.moveToNext());
			}
		}
		return textures;
	}

	private static float calculateRatio(int width, int height) {
		if (width == 0) {
			return 0;
		}
		return Math.round(((float) height / width) * 100f) / 100f;
	}

	public static long insertTexture(@NonNull SQLiteDatabase db, String name, int width,
			int height,
			float ratio, @NonNull byte[] thumb, @NonNull byte[] matrix) {
		ContentValues cv = new ContentValues();
		cv.put(DatabaseContract.TextureColumns.NAME, name);
		cv.put(DatabaseContract.TextureColumns.WIDTH, width);
		cv.put(DatabaseContract.TextureColumns.HEIGHT, height);
		cv.put(DatabaseContract.TextureColumns.RATIO, ratio);
		cv.put(DatabaseContract.TextureColumns.THUMB, thumb);
		cv.put(DatabaseContract.TextureColumns.MATRIX, matrix);
		return db.insert(DatabaseContract.TextureColumns.TABLE_NAME, null, cv);
	}
	// endregion

	@NonNull
	@Contract("_ -> new")
	public static DatabaseTable buildSchema(@NonNull Context context) {
		return new DatabaseTable() {
			@Override
			public void onCreate(@NonNull SQLiteDatabase db) {
				createTexturesTable(db);
			}

			@Override
			public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
				if (oldVersion < 4) {
					addTexturesWidthHeightRatio(db);
				}
			}

			@Override
			public boolean onImport(@NonNull SQLiteDatabase intoDb,
					@NonNull SQLiteDatabase fromDb) {
				return importTextures(intoDb, fromDb);
			}

			private void createTexturesTable(@NonNull SQLiteDatabase db) {
				db.execSQL("CREATE TABLE " + DatabaseContract.TextureColumns.TABLE_NAME + " (" +
						DatabaseContract.TextureColumns._ID + " INTEGER PRIMARY KEY " +
						"AUTOINCREMENT," +
						DatabaseContract.TextureColumns.NAME + " TEXT NOT NULL UNIQUE," +
						DatabaseContract.TextureColumns.WIDTH + " INTEGER," +
						DatabaseContract.TextureColumns.HEIGHT + " INTEGER," +
						DatabaseContract.TextureColumns.RATIO + " REAL," +
						DatabaseContract.TextureColumns.THUMB + " BLOB," +
						DatabaseContract.TextureColumns.MATRIX + " BLOB);");
				insertInitialTextures(db);
			}

			private void insertInitialTextures(@NonNull SQLiteDatabase db) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPremultiplied = false;
				Bitmap noiseBitmap = BitmapFactory.decodeResource(context.getResources(),
						R.drawable.texture_noise, options);
				int thumbnailSize =
						Math.round(context.getResources().getDisplayMetrics().density * 48f);
				insertTexture(db,
						context.getString(R.string.texture_name_noise),
						noiseBitmap,
						thumbnailSize);
			}

			private void addTexturesWidthHeightRatio(@NonNull SQLiteDatabase db) {
				db.execSQL("ALTER TABLE " + DatabaseContract.TextureColumns.TABLE_NAME +
						" ADD COLUMN " + DatabaseContract.TextureColumns.WIDTH + " INTEGER;");
				db.execSQL("ALTER TABLE " + DatabaseContract.TextureColumns.TABLE_NAME +
						" ADD COLUMN " + DatabaseContract.TextureColumns.HEIGHT + " INTEGER;");
				db.execSQL("ALTER TABLE " + DatabaseContract.TextureColumns.TABLE_NAME +
						" ADD COLUMN" + DatabaseContract.TextureColumns.RATIO + "REAL;");

				try (var cursor =
						db.rawQuery("SELECT " +
										DatabaseContract.TextureColumns._ID + "," +
										DatabaseContract.TextureColumns.MATRIX +
										" FROM " + DatabaseContract.TextureColumns.TABLE_NAME,
								null)) {
					if (!cursor.moveToFirst()) {
						return;
					}
					do {
						var data = CursorHelpers.getBlob(cursor,
								DatabaseContract.TextureColumns.MATRIX);
						if (data == null) {
							continue;
						}

						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inPremultiplied = false;
						var bm = BitmapFactory.decodeByteArray(data, 0, data.length, options);
						if (bm == null) {
							continue;
						}

						db.execSQL("UPDATE " + DatabaseContract.TextureColumns.TABLE_NAME +
								" SET " +
								DatabaseContract.TextureColumns.WIDTH + "=" +
								bm.getWidth() + "," +
								DatabaseContract.TextureColumns.HEIGHT + "=" +
								bm.getHeight() + "," +
								DatabaseContract.TextureColumns.RATIO + "=" +
								calculateRatio(bm.getWidth(), bm.getHeight()) +
								" WHERE " +
								DatabaseContract.TextureColumns._ID + "=" +
								CursorHelpers.getLong(cursor,
										DatabaseContract.TextureColumns._ID) +
								";");

						bm.recycle();
					} while (cursor.moveToNext());
				}
			}

			private static boolean importTexture(
					SQLiteDatabase dst,
					SQLiteDatabase src,
					long srcId) {
				try (Cursor cursor = src.rawQuery(
						"SELECT * " +
								" FROM " + DatabaseContract.TextureColumns.TABLE_NAME +
								" WHERE " + DatabaseContract.TextureColumns._ID + " = ?",
						new String[]{String.valueOf(srcId)})) {
					boolean success = true;
					if (moveToFirstAndCatchOutOfMemory(cursor)) {
						long textureId = TextureDao.insertTexture(dst,
								CursorHelpers.getString(cursor,
										DatabaseContract.TextureColumns.NAME),
								CursorHelpers.getInt(cursor,
										DatabaseContract.TextureColumns.WIDTH),
								CursorHelpers.getInt(cursor,
										DatabaseContract.TextureColumns.HEIGHT),
								CursorHelpers.getFloat(cursor,
										DatabaseContract.TextureColumns.RATIO),
								Objects.requireNonNull(CursorHelpers.getBlob(cursor,
										DatabaseContract.TextureColumns.THUMB)),
								Objects.requireNonNull(CursorHelpers.getBlob(cursor,
										DatabaseContract.TextureColumns.MATRIX)));
						if (textureId < 1) {
							success = false;
						}
					}
					return success;
				}
			}

			private static boolean moveToFirstAndCatchOutOfMemory(@NonNull Cursor cursor) {
				try {
					return cursor.moveToFirst();
				} catch (SQLException e) {
					return false;
				}
			}

			private static boolean textureExists(SQLiteDatabase db, String name) {
				try (Cursor cursor = db.rawQuery(
						"SELECT " + DatabaseContract.TextureColumns._ID +
								" FROM " + DatabaseContract.TextureColumns.TABLE_NAME +
								" WHERE " + DatabaseContract.TextureColumns.NAME + " = ?",
						new String[]{name})) {
					return cursor.moveToFirst() && cursor.getCount() > 0;
				}
			}

			private static boolean importTextures(
					SQLiteDatabase dst,
					SQLiteDatabase src) {
				try (Cursor cursor = src.rawQuery(
						"SELECT " +
								DatabaseContract.TextureColumns._ID + ", " +
								DatabaseContract.TextureColumns.NAME +
								" FROM " + DatabaseContract.TextureColumns.TABLE_NAME +
								" ORDER BY " + DatabaseContract.TextureColumns._ID,
						null)) {
					boolean success = true;
					if (cursor.moveToFirst()) {
						do {
							String name = CursorHelpers.getString(cursor,
									DatabaseContract.TextureColumns.NAME);
							if (name == null || textureExists(dst, name)) {
								continue;
							}
							if (!importTexture(dst, src, CursorHelpers.getLong(cursor,
									DatabaseContract.TextureColumns._ID))) {
								success = false;
								break;
							}
						} while (cursor.moveToNext());
					}
					return success;
				}
			}
		};
	}
}
