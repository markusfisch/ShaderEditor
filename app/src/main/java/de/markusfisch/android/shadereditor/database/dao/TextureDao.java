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
import androidx.annotation.RawRes;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Arrays;
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

		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try (var cursor = db.rawQuery(query, new String[]{String.valueOf(id)})) {
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
		return getTextureBitmap(DatabaseContract.TextureColumns._ID, String.valueOf(id));
	}

	@Nullable
	public Bitmap getTextureBitmap(@NonNull String name) {
		return getTextureBitmap(DatabaseContract.TextureColumns.NAME, name);
	}

	@Nullable
	private Bitmap getTextureBitmap(String column, String value) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			byte[] data = readTextureBlob(db, column, value,
					DatabaseContract.TextureColumns.MATRIX);
			if (data != null) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPremultiplied = false;
				return BitmapFactory.decodeByteArray(data, 0, data.length, options);
			}
		} catch (OutOfMemoryError e) {
			// A texture might be too big to be loaded into memory.
		}
		return null;
	}

	@Nullable
	private static byte[] readTextureBlob(SQLiteDatabase db, String keyColumn,
			String key, String blobColumn) {
		String from = " FROM " + DatabaseContract.TextureColumns.TABLE_NAME +
				" WHERE " + keyColumn + " = ?";
		int length;
		try (Cursor cursor = db.rawQuery("SELECT length(" + blobColumn + ")" + from,
				new String[]{key})) {
			if (!cursor.moveToFirst() || cursor.isNull(0)) {
				return null;
			}
			length = cursor.getInt(0);
		}
		byte[] data = new byte[length];
		final int chunkSize = 65536;
		for (int offset = 0; offset < length; ) {
			try (Cursor cursor = db.rawQuery("SELECT substr(" + blobColumn + ", ?, ?)" + from,
					new String[]{Integer.toString(offset + 1), Integer.toString(chunkSize), key})) {
				if (!cursor.moveToFirst() || cursor.isNull(0)) {
					return null;
				}
				byte[] chunk = cursor.getBlob(0);
				if (chunk.length == 0 || chunk.length > length - offset) {
					return null;
				}
				System.arraycopy(chunk, 0, data, offset, chunk.length);
				offset += chunk.length;
			}
		}
		return data;
	}

	public long insertTexture(String name, Bitmap bitmap) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		return insertTexture(db, name, bitmap, textureThumbnailSize);
	}

	public void removeTexture(long id) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.delete(DatabaseContract.TextureColumns.TABLE_NAME,
				DatabaseContract.TextureColumns._ID + " = ?",
				new String[]{String.valueOf(id)});
	}
	// endregion

	// region Package-private static helpers
	private static long insertTexture(SQLiteDatabase db, String name, Bitmap bitmap,
			int thumbnailSize) {
		try {
			var thumbnail = BitmapEditor.createScaledBitmap(bitmap, thumbnailSize, thumbnailSize);
			int w = bitmap.getWidth();
			int h = bitmap.getHeight();
			try {
				return insertTexture(db, name, w, h, calculateRatio(w, h),
						BitmapEditor.encodeAsPng(thumbnail),
						BitmapEditor.encodeAsPng(bitmap));
			} finally {
				thumbnail.recycle();
			}
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

		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try (var cursor = db.rawQuery(query, args)) {
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
				if (oldVersion < 6) {
					insertInitialTextures(db);
				}
			}

			@Override
			public boolean onImport(@NonNull SQLiteDatabase intoDb,
					@NonNull SQLiteDatabase fromDb) {
				return importTextures(intoDb, fromDb, context);
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
				int thumbnailSize = Math.round(
						context.getResources().getDisplayMetrics().density * 48f);
				insertTextureIfMissing(db,
						context.getString(R.string.texture_name_noise),
						R.raw.texture_noise,
						thumbnailSize);
				insertTextureIfMissing(db,
						context.getString(R.string.texture_name_rgba_noise),
						R.raw.texture_noise_rgba,
						thumbnailSize);
			}

			private void insertTextureIfMissing(@NonNull SQLiteDatabase db,
					String name, @RawRes int rawResId, int thumbnailSize) {
				if (textureExists(db, name)) {
					return;
				}
				Bitmap bitmap = BitmapEditor.getBitmapFromResource(context, rawResId);
				if (bitmap == null) {
					return;
				}
				insertTexture(db, name, bitmap, thumbnailSize);
				bitmap.recycle();
			}

			private void addTexturesWidthHeightRatio(@NonNull SQLiteDatabase db) {
				db.execSQL("ALTER TABLE " + DatabaseContract.TextureColumns.TABLE_NAME +
						" ADD COLUMN " + DatabaseContract.TextureColumns.WIDTH + " INTEGER;");
				db.execSQL("ALTER TABLE " + DatabaseContract.TextureColumns.TABLE_NAME +
						" ADD COLUMN " + DatabaseContract.TextureColumns.HEIGHT + " INTEGER;");
				db.execSQL("ALTER TABLE " + DatabaseContract.TextureColumns.TABLE_NAME +
						" ADD COLUMN " + DatabaseContract.TextureColumns.RATIO + " REAL;");

				try (var cursor =
						db.rawQuery("SELECT " +
										DatabaseContract.TextureColumns._ID +
										" FROM " + DatabaseContract.TextureColumns.TABLE_NAME,
								null)) {
					if (!cursor.moveToFirst()) {
						return;
					}
					do {
						var data = readTextureBlob(db, DatabaseContract.TextureColumns._ID,
								Long.toString(CursorHelpers.getLong(cursor,
										DatabaseContract.TextureColumns._ID)),
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
				String projection = DatabaseContract.TextureColumns.NAME;
				// Older backups may not contain dimensions yet.
				try (Cursor columns = src.rawQuery("SELECT * FROM " +
						DatabaseContract.TextureColumns.TABLE_NAME + " LIMIT 0", null)) {
					for (String column : new String[]{DatabaseContract.TextureColumns.WIDTH,
							DatabaseContract.TextureColumns.HEIGHT,
							DatabaseContract.TextureColumns.RATIO}) {
						if (columns.getColumnIndex(column) >= 0) {
							projection += "," + column;
						}
					}
				}
				try (Cursor cursor = src.rawQuery(
						"SELECT " + projection +
								" FROM " + DatabaseContract.TextureColumns.TABLE_NAME +
								" WHERE " + DatabaseContract.TextureColumns._ID + " = ?",
						new String[]{String.valueOf(srcId)})) {
					boolean success = false;
					if (cursor.moveToFirst()) {
						long textureId = TextureDao.insertTexture(dst,
								CursorHelpers.getString(cursor,
										DatabaseContract.TextureColumns.NAME),
								CursorHelpers.getInt(cursor,
										DatabaseContract.TextureColumns.WIDTH),
								CursorHelpers.getInt(cursor,
										DatabaseContract.TextureColumns.HEIGHT),
								CursorHelpers.getFloat(cursor,
										DatabaseContract.TextureColumns.RATIO),
								Objects.requireNonNull(readTextureBlob(src,
										DatabaseContract.TextureColumns._ID, String.valueOf(srcId),
										DatabaseContract.TextureColumns.THUMB)),
								Objects.requireNonNull(readTextureBlob(src,
										DatabaseContract.TextureColumns._ID, String.valueOf(srcId),
										DatabaseContract.TextureColumns.MATRIX)));
						success = textureId > 0;
					}
					return success;
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

			private static boolean texturesMatch(SQLiteDatabase dst,
					SQLiteDatabase src, String name) {
				String where = " FROM " + DatabaseContract.TextureColumns.TABLE_NAME +
						" WHERE " + DatabaseContract.TextureColumns.NAME + " = ?";
				String lengthQuery = "SELECT length(" +
						DatabaseContract.TextureColumns.MATRIX + ")" + where;
				String[] args = {name};
				try (Cursor existing = dst.rawQuery(lengthQuery, args);
						Cursor incoming = src.rawQuery(lengthQuery, args)) {
					if (!existing.moveToFirst() || !incoming.moveToFirst() ||
							existing.isNull(0) || incoming.isNull(0)) {
						return false;
					}
					long length = existing.getLong(0);
					if (length != incoming.getLong(0)) {
						return false;
					}

					// Keep large PNGs out of the limited CursorWindow.
					final int chunkSize = 65536;
					String chunkQuery = "SELECT substr(" +
							DatabaseContract.TextureColumns.MATRIX + ", ?, ?)" + where;
					for (long offset = 1; offset <= length; offset += chunkSize) {
						String[] chunkArgs = {Long.toString(offset),
								Integer.toString(chunkSize), name};
						try (Cursor existingChunk = dst.rawQuery(chunkQuery, chunkArgs);
								Cursor incomingChunk = src.rawQuery(chunkQuery, chunkArgs)) {
							if (!existingChunk.moveToFirst() || !incomingChunk.moveToFirst() ||
									!Arrays.equals(existingChunk.getBlob(0), incomingChunk.getBlob(0))) {
								return false;
							}
						}
					}
					return true;
				}
			}

			private static boolean importTextures(
					SQLiteDatabase dst,
					SQLiteDatabase src,
					Context context) {
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
							if (name == null) {
								continue;
							}
							if (textureExists(dst, name)) {
								if (!texturesMatch(dst, src, name)) {
									throw new SQLException(context.getString(
											R.string.name_already_taken) + ": " + name);
								}
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
