package de.markusfisch.android.shadereditor.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.os.Build;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.markusfisch.android.shadereditor.R;

public class Database {
	public static final String FILE_NAME = "shaders.db";

	public static final String SHADERS = "shaders";
	public static final String SHADERS_ID = "_id";
	public static final String SHADERS_FRAGMENT_SHADER = "shader";
	public static final String SHADERS_THUMB = "thumb";
	public static final String SHADERS_NAME = "name";
	public static final String SHADERS_CREATED = "created";
	public static final String SHADERS_MODIFIED = "modified";
	public static final String SHADERS_QUALITY = "quality";

	public static final String TEXTURES = "textures";
	public static final String TEXTURES_ID = "_id";
	public static final String TEXTURES_NAME = "name";
	public static final String TEXTURES_WIDTH = "width";
	public static final String TEXTURES_HEIGHT = "height";
	public static final String TEXTURES_RATIO = "ratio";
	public static final String TEXTURES_THUMB = "thumb";
	public static final String TEXTURES_MATRIX = "matrix";

	private SQLiteDatabase db;
	private int textureThumbnailSize;

	public String importDatabase(Context context, String fileName) {
		SQLiteDatabase edb = null;
		try {
			edb = new ImportHelper(new ExternalDatabaseContext(context),
					fileName).getReadableDatabase();
			db.beginTransaction();
			if (importShaders(db, edb) && importTextures(db, edb)) {
				db.setTransactionSuccessful();
				return null;
			} else {
				return context.getString(R.string.import_failed, "");
			}
		} catch (SQLException e) {
			return e.getMessage();
		} finally {
			if (db.inTransaction()) {
				db.endTransaction();
			}
			if (edb != null) {
				edb.close();
			}
		}
	}

	public void open(Context context) {
		textureThumbnailSize = Math.round(
				context.getResources().getDisplayMetrics().density * 48f);
		try {
			db = new OpenHelper(context).getWritableDatabase();
		} catch (SQLException e) {
			Toast.makeText(
					context,
					R.string.cannot_open_database,
					Toast.LENGTH_LONG).show();
		}
	}

	public boolean isOpen() {
		return db != null;
	}

	public static long getLong(Cursor cursor, String column) {
		int index = cursor.getColumnIndex(column);
		if (index < 0) {
			return 0L;
		}
		return cursor.getLong(index);
	}

	public static float getFloat(Cursor cursor, String column) {
		int index = cursor.getColumnIndex(column);
		if (index < 0) {
			return 0f;
		}
		return cursor.getFloat(index);
	}

	public static String getString(Cursor cursor, String column) {
		int index = cursor.getColumnIndex(column);
		if (index < 0) {
			return "";
		}
		return cursor.getString(index);
	}

	public static byte[] getBlob(Cursor cursor, String column) {
		int index = cursor.getColumnIndex(column);
		if (index < 0) {
			return null;
		}
		return cursor.getBlob(index);
	}

	public static boolean closeIfEmpty(Cursor cursor) {
		try {
			if (cursor != null && cursor.moveToFirst()) {
				return false;
			}
		} catch (SQLException e) {
			// Cursor.moveToFirst() may throw an exception when
			// the row is too big to fit into CursorWindow.
			// Catch this exception but still try to close the
			// Cursor object to free its resources.
		}

		if (cursor != null) {
			cursor.close();
		}

		return true;
	}

	public Cursor getShaders() {
		return db.rawQuery(
				"SELECT " +
						SHADERS_ID + "," +
						SHADERS_THUMB + "," +
						SHADERS_NAME + "," +
						SHADERS_MODIFIED +
						" FROM " + SHADERS +
						" ORDER BY " + SHADERS_ID,
				null);
	}

	public Cursor getTextures(String substring) {
		boolean useSubstring = substring != null;
		return db.rawQuery(
				"SELECT " +
						TEXTURES_ID + "," +
						TEXTURES_NAME + "," +
						TEXTURES_WIDTH + "," +
						TEXTURES_HEIGHT + "," +
						TEXTURES_THUMB +
						" FROM " + TEXTURES +
						" WHERE " + TEXTURES_RATIO + " = 1" +
						(useSubstring
								? " AND " + TEXTURES_NAME + " LIKE ?"
								: "") +
						" ORDER BY " + TEXTURES_ID,
				useSubstring
						? new String[]{"%" + substring + "%"}
						: null);
	}

	public Cursor getSamplerCubeTextures(String substring) {
		boolean useSubstring = substring != null;
		return db.rawQuery(
				"SELECT " +
						TEXTURES_ID + "," +
						TEXTURES_NAME + "," +
						TEXTURES_WIDTH + "," +
						TEXTURES_HEIGHT + "," +
						TEXTURES_THUMB +
						" FROM " + TEXTURES +
						" WHERE " + TEXTURES_RATIO + " = 1.5" +
						(useSubstring
								? " AND " + TEXTURES_NAME + " LIKE ?"
								: "") +
						" ORDER BY " + TEXTURES_ID,
				useSubstring
						? new String[]{"%" + substring + "%"}
						: null);
	}

	public boolean isShaderAvailable(long id) {
		Cursor cursor = db.rawQuery(
				"SELECT " +
						SHADERS_ID +
						" FROM " + SHADERS +
						" WHERE " + SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
		if (cursor == null) {
			return false;
		}
		boolean exists = cursor.moveToFirst();
		cursor.close();
		return exists;
	}

	public Cursor getShader(long id) {
		return db.rawQuery(
				"SELECT " +
						SHADERS_ID + "," +
						SHADERS_FRAGMENT_SHADER + "," +
						SHADERS_NAME + "," +
						SHADERS_MODIFIED + "," +
						SHADERS_QUALITY +
						" FROM " + SHADERS +
						" WHERE " + SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public Cursor getRandomShader() {
		return db.rawQuery(
				"SELECT " +
						SHADERS_ID + "," +
						SHADERS_FRAGMENT_SHADER + "," +
						SHADERS_MODIFIED + "," +
						SHADERS_QUALITY +
						" FROM " + SHADERS +
						" ORDER BY RANDOM() LIMIT 1",
				null);
	}

	public byte[] getThumbnail(long id) {
		Cursor cursor = db.rawQuery(
				"SELECT " +
						SHADERS_THUMB +
						" FROM " + SHADERS +
						" WHERE " + SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});

		if (closeIfEmpty(cursor)) {
			return null;
		}

		byte[] thumbnail = getBlob(cursor, SHADERS_THUMB);
		cursor.close();

		return thumbnail;
	}

	public long getFirstShaderId() {
		Cursor cursor = db.rawQuery(
				"SELECT " +
						SHADERS_ID +
						" FROM " + SHADERS +
						" ORDER BY " + SHADERS_ID +
						" LIMIT 1",
				null);

		if (closeIfEmpty(cursor)) {
			return 0;
		}

		long id = getLong(cursor, SHADERS_ID);
		cursor.close();

		return id;
	}

	public Cursor getTexture(long id) {
		return db.rawQuery(
				"SELECT " +
						TEXTURES_NAME + "," +
						TEXTURES_WIDTH + "," +
						TEXTURES_HEIGHT + "," +
						TEXTURES_MATRIX +
						" FROM " + TEXTURES +
						" WHERE " + TEXTURES_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public Bitmap getTextureBitmap(String name) {
		Cursor cursor = db.rawQuery(
				"SELECT " +
						TEXTURES_MATRIX +
						" FROM " + TEXTURES +
						" WHERE " + TEXTURES_NAME + " = ?",
				new String[]{name});
		Bitmap bm = getTextureBitmap(cursor);
		cursor.close();
		return bm;
	}

	public Bitmap getTextureBitmap(Cursor cursor) {
		return closeIfEmpty(cursor)
				? null
				: textureFromCursor(cursor);
	}

	public static long insertShader(
			SQLiteDatabase db,
			String shader,
			String name,
			String created,
			String modified,
			byte[] thumbnail,
			float quality) {
		ContentValues cv = new ContentValues();
		cv.put(SHADERS_FRAGMENT_SHADER, shader);
		cv.put(SHADERS_THUMB, thumbnail);
		cv.put(SHADERS_NAME, name);
		cv.put(SHADERS_CREATED, created);
		cv.put(SHADERS_MODIFIED, modified);
		cv.put(SHADERS_QUALITY, quality);
		return db.insert(SHADERS, null, cv);
	}

	public static long insertShader(
			SQLiteDatabase db,
			String shader,
			String name,
			byte[] thumbnail,
			float quality) {
		String now = currentTime();
		return insertShader(db, shader, name, now, now, thumbnail, quality);
	}

	public long insertShader(
			String shader,
			byte[] thumbnail,
			float quality) {
		return insertShader(db, shader, null, thumbnail, quality);
	}

	public long insertShader(
			Context context,
			String shader,
			String name) {
		return insertShader(db, shader, name,
				loadBitmapResource(context, R.drawable.thumbnail_new_shader),
				1f);
	}

	public long insertNewShader(Context context) {
		return insertShaderFromResource(
				context,
				null,
				R.raw.new_shader,
				R.drawable.thumbnail_new_shader,
				1f);
	}

	public long insertShaderFromResource(
			Context context,
			String name,
			int sourceId,
			int thumbId,
			float quality) {
		try {
			return insertShader(
					db,
					loadRawResource(context, sourceId),
					name,
					loadBitmapResource(context, thumbId),
					quality);
		} catch (IOException e) {
			return 0;
		}
	}

	public static long insertTexture(
			SQLiteDatabase db,
			String name,
			int width,
			int height,
			float ratio,
			byte[] thumb,
			byte[] matrix) {
		ContentValues cv = new ContentValues();
		cv.put(TEXTURES_NAME, name);
		cv.put(TEXTURES_WIDTH, width);
		cv.put(TEXTURES_HEIGHT, height);
		cv.put(TEXTURES_RATIO, ratio);
		cv.put(TEXTURES_THUMB, thumb);
		cv.put(TEXTURES_MATRIX, matrix);
		return db.insert(TEXTURES, null, cv);
	}

	public static long insertTexture(
			SQLiteDatabase db,
			String name,
			Bitmap bitmap,
			int thumbnailSize) {
		Bitmap thumbnail;

		try {
			thumbnail = Bitmap.createScaledBitmap(
					bitmap,
					thumbnailSize,
					thumbnailSize,
					true);
		} catch (IllegalArgumentException e) {
			return 0;
		}

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		return insertTexture(
				db,
				name,
				w,
				h,
				calculateRatio(w, h),
				bitmapToPng(thumbnail),
				bitmapToPng(bitmap));
	}

	public long insertTexture(String name, Bitmap bitmap) {
		return insertTexture(
				db,
				name,
				bitmap,
				textureThumbnailSize);
	}

	public void updateShader(
			long id,
			String shader,
			byte[] thumbnail,
			float quality) {
		ContentValues cv = new ContentValues();
		cv.put(SHADERS_FRAGMENT_SHADER, shader);
		cv.put(SHADERS_MODIFIED, currentTime());
		cv.put(SHADERS_QUALITY, quality);

		if (thumbnail != null) {
			cv.put(SHADERS_THUMB, thumbnail);
		}

		db.update(SHADERS, cv, SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public void updateShaderQuality(
			long id,
			float quality) {
		ContentValues cv = new ContentValues();
		cv.put(SHADERS_QUALITY, quality);

		db.update(SHADERS, cv, SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public void updateShaderName(long id, String name) {
		ContentValues cv = new ContentValues();
		cv.put(SHADERS_NAME, name);
		db.update(SHADERS, cv, SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public void removeShader(long id) {
		db.delete(SHADERS, SHADERS_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	public void removeTexture(long id) {
		db.delete(TEXTURES, TEXTURES_ID + " = ?",
				new String[]{String.valueOf(id)});
	}

	private static String currentTime() {
		return new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss",
				Locale.US).format(new Date());
	}

	private static Bitmap textureFromCursor(Cursor cursor) {
		byte[] data = getBlob(cursor, TEXTURES_MATRIX);
		return data == null
				? null
				: BitmapFactory.decodeByteArray(data, 0, data.length);
	}

	private static String loadRawResource(
			Context context,
			int id) throws IOException {
		InputStream in = null;
		try {
			in = context.getResources().openRawResource(id);
			int l = in.available();
			byte[] b = new byte[l];
			// StandardCharsets.UTF_8 would require API level 19.
			return in.read(b) == l ? new String(b, "UTF-8") : null;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	private static byte[] loadBitmapResource(Context context, int id) {
		return bitmapToPng(BitmapFactory.decodeResource(
				context.getResources(),
				id));
	}

	private static byte[] bitmapToPng(Bitmap bitmap) {
		// Convert color space to SRGB because GLUtils.texImage2D()
		// can't handle other formats.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
				bitmap.getColorSpace() != ColorSpace.get(ColorSpace.Named.SRGB)) {
			int width = bitmap.getWidth();
			int height = bitmap.getWidth();
			Bitmap copy = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			int[] pixels = new int[width * height];
			bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
			copy.setPixels(pixels, 0, width, 0, 0, width, height);
			bitmap = copy;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
		return out.toByteArray();
	}

	private static float calculateRatio(int width, int height) {
		// Round to two decimal places to avoid problems with
		// rounding errors. The query will filter precisely 1 or 1.5.
		return Math.round(((float) height / width) * 100f) / 100f;
	}

	private void createShadersTable(SQLiteDatabase db, Context context) {
		db.execSQL("DROP TABLE IF EXISTS " + SHADERS);
		db.execSQL("CREATE TABLE " + SHADERS + " (" +
				SHADERS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				SHADERS_FRAGMENT_SHADER + " TEXT NOT NULL," +
				SHADERS_THUMB + " BLOB," +
				SHADERS_NAME + " TEXT," +
				SHADERS_CREATED + " DATETIME," +
				SHADERS_MODIFIED + " DATETIME," +
				SHADERS_QUALITY + " REAL);");

		insertInitalShaders(db, context);
	}

	private static void addShadersQuality(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + SHADERS +
				" ADD COLUMN " + SHADERS_QUALITY + " REAL;");
		db.execSQL("UPDATE " + SHADERS +
				" SET " + SHADERS_QUALITY + " = 1;");
	}

	private void insertInitalShaders(SQLiteDatabase db, Context context) {
		try {
			insertShader(
					db,
					loadRawResource(
							context,
							R.raw.default_shader),
					context.getString(R.string.default_shader),
					loadBitmapResource(
							context,
							R.drawable.thumbnail_default),
					1f);
		} catch (IOException e) {
			// Shouldn't ever happen in production
			// and nothing can be done if it does.
		}
	}

	private void createTexturesTable(SQLiteDatabase db, Context context) {
		db.execSQL("DROP TABLE IF EXISTS " + TEXTURES);
		db.execSQL("CREATE TABLE " + TEXTURES + " (" +
				TEXTURES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				TEXTURES_NAME + " TEXT NOT NULL UNIQUE," +
				TEXTURES_WIDTH + " INTEGER," +
				TEXTURES_HEIGHT + " INTEGER," +
				TEXTURES_RATIO + " REAL," +
				TEXTURES_THUMB + " BLOB," +
				TEXTURES_MATRIX + " BLOB);");

		insertInitalTextures(db, context);
	}

	private static void addTexturesWidthHeightRatio(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + TEXTURES +
				" ADD COLUMN " + TEXTURES_WIDTH + " INTEGER;");
		db.execSQL("ALTER TABLE " + TEXTURES +
				" ADD COLUMN " + TEXTURES_HEIGHT + " INTEGER;");
		db.execSQL("ALTER TABLE " + TEXTURES +
				" ADD COLUMN " + TEXTURES_RATIO + " REAL;");

		Cursor cursor = db.rawQuery(
				"SELECT " +
						TEXTURES_ID + "," +
						TEXTURES_MATRIX +
						" FROM " + TEXTURES,
				null);

		if (closeIfEmpty(cursor)) {
			return;
		}

		do {
			Bitmap bm = textureFromCursor(cursor);

			if (bm == null) {
				continue;
			}

			int width = bm.getWidth();
			int height = bm.getHeight();
			float ratio = calculateRatio(width, height);
			bm.recycle();

			db.execSQL("UPDATE " + TEXTURES +
					" SET " +
					TEXTURES_WIDTH + " = " + width + ", " +
					TEXTURES_HEIGHT + " = " + height + ", " +
					TEXTURES_RATIO + " = " + ratio +
					" WHERE " + TEXTURES_ID + " = " +
					getLong(cursor, TEXTURES_ID) + ";");

		} while (cursor.moveToNext());

		cursor.close();
	}

	private static void addShaderNames(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + SHADERS +
				" ADD COLUMN " + SHADERS_NAME + " TEXT;");
	}

	private void insertInitalTextures(SQLiteDatabase db, Context context) {
		Database.insertTexture(
				db,
				context.getString(R.string.texture_name_noise),
				BitmapFactory.decodeResource(
						context.getResources(),
						R.drawable.texture_noise),
				textureThumbnailSize);
	}

	private static boolean importShaders(
			SQLiteDatabase dst,
			SQLiteDatabase src) {
		Cursor cursor = src.rawQuery(
				"SELECT *" +
						" FROM " + SHADERS +
						" ORDER BY " + SHADERS_ID,
				null);
		if (cursor == null) {
			return false;
		}
		int shaderIndex = cursor.getColumnIndex(SHADERS_FRAGMENT_SHADER);
		int thumbIndex = cursor.getColumnIndex(SHADERS_THUMB);
		int nameIndex = cursor.getColumnIndex(SHADERS_NAME);
		int createdIndex = cursor.getColumnIndex(SHADERS_CREATED);
		int modifiedIndex = cursor.getColumnIndex(SHADERS_MODIFIED);
		int qualityIndex = cursor.getColumnIndex(SHADERS_QUALITY);
		boolean success = true;
		if (cursor.moveToFirst()) {
			do {
				String createdDate = cursor.getString(createdIndex);
				String modifiedDate = cursor.getString(modifiedIndex);
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
		cursor.close();
		return success;
	}

	private static boolean shaderExists(
			SQLiteDatabase db,
			String createdDate,
			String modifiedDate) {
		Cursor cursor = db.rawQuery(
				"SELECT " + SHADERS_ID +
						" FROM " + SHADERS +
						" WHERE " + SHADERS_CREATED + " = ?" +
						" AND " + SHADERS_MODIFIED + " = ?",
				new String[]{createdDate, modifiedDate});
		if (cursor == null) {
			return false;
		}
		boolean exists = cursor.moveToFirst() && cursor.getCount() > 0;
		cursor.close();
		return exists;
	}

	private static boolean importTextures(
			SQLiteDatabase dst,
			SQLiteDatabase src) {
		Cursor cursor = src.rawQuery(
				"SELECT " +
						TEXTURES_ID + ", " +
						TEXTURES_NAME +
						" FROM " + TEXTURES +
						" ORDER BY " + TEXTURES_ID,
				null);
		if (cursor == null) {
			return false;
		}
		int idIndex = cursor.getColumnIndex(TEXTURES_ID);
		int nameIndex = cursor.getColumnIndex(TEXTURES_NAME);
		boolean success = true;
		if (cursor.moveToFirst()) {
			do {
				String name = cursor.getString(nameIndex);
				if (name == null || textureExists(dst, name)) {
					continue;
				}
				// Transfer textures one at a time because all of them
				// may be too big for one cursor window.
				if (!importTexture(dst, src, cursor.getLong(idIndex))) {
					success = false;
					break;
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return success;
	}

	private static boolean importTexture(
			SQLiteDatabase dst,
			SQLiteDatabase src,
			long srcId) {
		Cursor cursor = src.rawQuery(
				"SELECT * " +
						" FROM " + TEXTURES +
						" WHERE " + TEXTURES_ID + " = ?",
				new String[]{String.valueOf(srcId)});
		if (cursor == null) {
			return false;
		}
		int nameIndex = cursor.getColumnIndex(TEXTURES_NAME);
		int widthIndex = cursor.getColumnIndex(TEXTURES_WIDTH);
		int heightIndex = cursor.getColumnIndex(TEXTURES_HEIGHT);
		int ratioIndex = cursor.getColumnIndex(TEXTURES_RATIO);
		int thumbIndex = cursor.getColumnIndex(TEXTURES_THUMB);
		int matrixIndex = cursor.getColumnIndex(TEXTURES_MATRIX);
		boolean success = true;
		if (moveToFirstAndCatchOutOfMemory(cursor)) {
			do {
				long textureId = insertTexture(dst,
						cursor.getString(nameIndex),
						cursor.getInt(widthIndex),
						cursor.getInt(heightIndex),
						cursor.getFloat(ratioIndex),
						cursor.getBlob(thumbIndex),
						cursor.getBlob(matrixIndex));
				if (textureId < 1) {
					success = false;
					break;
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return success;
	}

	private static boolean moveToFirstAndCatchOutOfMemory(Cursor cursor) {
		try {
			return cursor.moveToFirst();
		} catch (SQLException e) {
			// Catch Row too big exceptions when the BLOB larger than
			// the Cursor Window.
			return false;
		}
	}

	private static boolean textureExists(SQLiteDatabase db, String name) {
		Cursor cursor = db.rawQuery(
				"SELECT " + TEXTURES_ID +
						" FROM " + TEXTURES +
						" WHERE " + TEXTURES_NAME + " = ?",
				new String[]{name});
		if (cursor == null) {
			return false;
		}
		boolean exists = cursor.moveToFirst() && cursor.getCount() > 0;
		cursor.close();
		return exists;
	}

	private class OpenHelper extends SQLiteOpenHelper {
		private final Context context;

		@Override
		public void onCreate(SQLiteDatabase db) {
			createShadersTable(db, context);
			createTexturesTable(db, context);
		}

		@Override
		public void onDowngrade(
				SQLiteDatabase db,
				int oldVersion,
				int newVersion) {
			// Without onDowngrade(), a downgrade will throw
			// an exception. Can never happen in production.
		}

		@Override
		public void onUpgrade(
				SQLiteDatabase db,
				int oldVersion,
				int newVersion) {
			if (oldVersion < 2) {
				createTexturesTable(db, context);
				insertInitalShaders(db, context);
			}

			if (oldVersion < 3) {
				addShadersQuality(db);
			}

			if (oldVersion < 4) {
				addTexturesWidthHeightRatio(db);
			}

			if (oldVersion < 5) {
				addShaderNames(db);
			}
		}

		private OpenHelper(Context context) {
			super(context, FILE_NAME, null, 5);
			this.context = context;
		}
	}

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

	// Somehow it's required to use this ContextWrapper to access the
	// tables in an external database. Without this, the database will
	// only contain the table "android_metadata".
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
