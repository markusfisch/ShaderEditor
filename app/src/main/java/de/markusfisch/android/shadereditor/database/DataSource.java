package de.markusfisch.android.shadereditor.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.database.DataRecords.Shader;
import de.markusfisch.android.shadereditor.database.DataRecords.ShaderInfo;
import de.markusfisch.android.shadereditor.database.DataRecords.TextureInfo;
import de.markusfisch.android.shadereditor.database.DatabaseContract.ShaderColumns;
import de.markusfisch.android.shadereditor.database.DatabaseContract.TextureColumns;

public class DataSource {

	private final SQLiteOpenHelper dbHelper;
	private final Context appContext;
	private final int textureThumbnailSize;

	DataSource(SQLiteOpenHelper dbHelper, @NonNull Context context) {
		this.dbHelper = dbHelper;
		this.appContext = context;
		this.textureThumbnailSize =
				Math.round(context.getResources().getDisplayMetrics().density * 48f);
	}

	// region Static package-private helpers
	static long insertShader(@NonNull SQLiteDatabase db, String shader, String name,
			byte[] thumbnail,
			float quality) {
		String now = currentTime();
		ContentValues cv = new ContentValues();
		cv.put(ShaderColumns.FRAGMENT_SHADER, shader);
		cv.put(ShaderColumns.THUMB, thumbnail);
		cv.put(ShaderColumns.NAME, name);
		cv.put(ShaderColumns.CREATED, now);
		cv.put(ShaderColumns.MODIFIED, now);
		cv.put(ShaderColumns.QUALITY, quality);
		return db.insert(ShaderColumns.TABLE_NAME, null, cv);
	}

	static long insertTexture(SQLiteDatabase db, String name, Bitmap bitmap, int thumbnailSize) {
		try {
			Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, thumbnailSize, thumbnailSize,
					true);
			int w = bitmap.getWidth();
			int h = bitmap.getHeight();
			return insertTexture(db, name, w, h, calculateRatio(w, h), bitmapToPng(thumbnail),
					bitmapToPng(bitmap));
		} catch (IllegalArgumentException e) {
			return 0;
		}
	}

	private static long insertTexture(@NonNull SQLiteDatabase db, String name, int width,
			int height,
			float ratio, byte[] thumb, byte[] matrix) {
		ContentValues cv = new ContentValues();
		cv.put(TextureColumns.NAME, name);
		cv.put(TextureColumns.WIDTH, width);
		cv.put(TextureColumns.HEIGHT, height);
		cv.put(TextureColumns.RATIO, ratio);
		cv.put(TextureColumns.THUMB, thumb);
		cv.put(TextureColumns.MATRIX, matrix);
		return db.insert(TextureColumns.TABLE_NAME, null, cv);
	}

	@NonNull
	static String loadRawResource(Context context, int id) throws IOException {
		try (InputStream in = context.getResources().openRawResource(id)) {
			byte[] b = new byte[in.available()];
			if (in.read(b) > 0) return new String(b, StandardCharsets.UTF_8);
			return "";
		}
	}

	@NonNull
	static byte[] loadBitmapResource(@NonNull Context context, int id) {
		return bitmapToPng(BitmapFactory.decodeResource(context.getResources(), id));
	}

	static void addShadersQuality(@NonNull SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + ShaderColumns.TABLE_NAME + " ADD COLUMN " + ShaderColumns.QUALITY + " REAL;");
		db.execSQL("UPDATE " + ShaderColumns.TABLE_NAME + " SET " + ShaderColumns.QUALITY + " = " +
				"1;");
	}

	static void addTexturesWidthHeightRatio(@NonNull SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + TextureColumns.TABLE_NAME + " ADD COLUMN " + TextureColumns.WIDTH + " INTEGER;");
		db.execSQL("ALTER TABLE " + TextureColumns.TABLE_NAME + " ADD COLUMN " + TextureColumns.HEIGHT + " INTEGER;");
		db.execSQL("ALTER TABLE " + TextureColumns.TABLE_NAME + " ADD COLUMN " + TextureColumns.RATIO + " REAL;");

		try (Cursor cursor =
				db.rawQuery("SELECT " + TextureColumns._ID + "," + TextureColumns.MATRIX + " FROM "
						+ TextureColumns.TABLE_NAME, null)) {
			if (cursor == null || !cursor.moveToFirst()) return;
			do {
				byte[] data = getBlob(cursor, TextureColumns.MATRIX);
				if (data == null) continue;

				Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
				if (bm == null) continue;

				db.execSQL("UPDATE " + TextureColumns.TABLE_NAME + " SET " +
						TextureColumns.WIDTH + " = " + bm.getWidth() + ", " +
						TextureColumns.HEIGHT + " = " + bm.getHeight() + ", " +
						TextureColumns.RATIO + " = " + calculateRatio(bm.getWidth(),
						bm.getHeight()) +
						" WHERE " + TextureColumns._ID + " = " + getLong(cursor,
						TextureColumns._ID) + ";");
				bm.recycle();
			} while (cursor.moveToNext());
		}
	}

	static void addShaderNames(@NonNull SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + ShaderColumns.TABLE_NAME + " ADD COLUMN " + ShaderColumns.NAME + " TEXT;");
	}

	// region Private Helpers
	@NonNull
	private static String currentTime() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
	}

	private static float calculateRatio(int width, int height) {
		if (width == 0) return 0;
		return Math.round(((float) height / width) * 100f) / 100f;
	}
	// endregion

	@NonNull
	@Contract("null -> new")
	private static byte[] bitmapToPng(Bitmap bitmap) {
		if (bitmap == null) return new byte[0];
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.getColorSpace() != null &&
				!bitmap.getColorSpace().equals(ColorSpace.get(ColorSpace.Named.SRGB))) {
			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
		return out.toByteArray();
	}

	private static int getInt(@NonNull Cursor c, String col) {
		int i = c.getColumnIndex(col);
		return i > -1 ? c.getInt(i) : 0;
	}

	private static long getLong(@NonNull Cursor c, String col) {
		int i = c.getColumnIndex(col);
		return i > -1 ? c.getLong(i) : 0L;
	}

	private static float getFloat(@NonNull Cursor c, String col) {
		int i = c.getColumnIndex(col);
		return i > -1 ? c.getFloat(i) : 0f;
	}

	private static String getString(@NonNull Cursor c, String col) {
		int i = c.getColumnIndex(col);
		return i > -1 ? c.getString(i) : "";
	}

	@Nullable
	private static byte[] getBlob(@NonNull Cursor c, String col) {
		int i = c.getColumnIndex(col);
		return i > -1 ? c.getBlob(i) : null;
	}
	// endregion

	// region Shader API
	public List<ShaderInfo> getShaders(boolean sortByLastModification) {
		List<ShaderInfo> shaders = new ArrayList<>();
		String query =
				"SELECT " + ShaderColumns._ID + "," + ShaderColumns.THUMB + "," + ShaderColumns.NAME + "," + ShaderColumns.MODIFIED +
						" FROM " + ShaderColumns.TABLE_NAME + " ORDER BY " + (sortByLastModification ?
						ShaderColumns.MODIFIED + " DESC" : ShaderColumns._ID);

		try (SQLiteDatabase db = dbHelper.getReadableDatabase(); Cursor cursor = db.rawQuery(query
				, null)) {
			if (cursor.moveToFirst()) {
				do {
					shaders.add(new ShaderInfo(
							getLong(cursor, ShaderColumns._ID),
							getString(cursor, ShaderColumns.NAME),
							getString(cursor, ShaderColumns.MODIFIED),
							getBlob(cursor, ShaderColumns.THUMB)));
				} while (cursor.moveToNext());
			}
		}
		return shaders;
	}

	@Nullable
	public Shader getShader(long id) {
		String query =
				"SELECT " + ShaderColumns._ID + "," + ShaderColumns.FRAGMENT_SHADER + "," + ShaderColumns.NAME + "," +
						ShaderColumns.MODIFIED + "," + ShaderColumns.QUALITY + " FROM " + ShaderColumns.TABLE_NAME + " WHERE " + ShaderColumns._ID + " = ?";

		try (SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)})) {
			if (cursor.moveToFirst()) {
				return new Shader(
						getLong(cursor, ShaderColumns._ID),
						getString(cursor, ShaderColumns.FRAGMENT_SHADER),
						getString(cursor, ShaderColumns.NAME),
						getString(cursor, ShaderColumns.MODIFIED),
						getFloat(cursor, ShaderColumns.QUALITY));
			}
		}
		return null;
	}

	@Nullable
	public Shader getRandomShader() {
		String query =
				"SELECT " + ShaderColumns._ID + "," + ShaderColumns.FRAGMENT_SHADER + "," + ShaderColumns.NAME + "," +
						ShaderColumns.MODIFIED + "," + ShaderColumns.QUALITY + " FROM " + ShaderColumns.TABLE_NAME +
						" ORDER BY RANDOM() LIMIT 1";

		try (SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor cursor = db.rawQuery(query, null)) {
			if (cursor.moveToFirst()) {
				return new Shader(
						getLong(cursor, ShaderColumns._ID),
						getString(cursor, ShaderColumns.FRAGMENT_SHADER),
						getString(cursor, ShaderColumns.NAME),
						getString(cursor, ShaderColumns.MODIFIED),
						getFloat(cursor, ShaderColumns.QUALITY));
			}
		}
		return null;
	}

	@Nullable
	public byte[] getThumbnail(long id) {
		try (SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor cursor = db.query(
						ShaderColumns.TABLE_NAME,
						new String[]{ShaderColumns.THUMB},
						ShaderColumns._ID + " = ?",
						new String[]{String.valueOf(id)},
						null, null, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return getBlob(cursor, ShaderColumns.THUMB);
			}
		}
		return null;
	}

	public long getFirstShaderId() {
		try (SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor cursor =
						db.rawQuery("SELECT " + ShaderColumns._ID + " FROM " + ShaderColumns.TABLE_NAME + " ORDER BY "
								+ ShaderColumns._ID + " LIMIT 1", null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return getLong(cursor, ShaderColumns._ID);
			}
		}
		return 0;
	}

	public long insertShaderFromResource(Context context, String name, int sourceId, int thumbId,
			float quality) {
		try {
			return insertShader(
					loadRawResource(context, sourceId),
					name,
					loadBitmapResource(context, thumbId),
					quality);
		} catch (IOException e) {
			return 0;
		}
	}

	public long insertNewShader() {
		return insertShaderFromResource(appContext,
				appContext.getString(R.string.add_shader),
				R.raw.new_shader,
				R.drawable.thumbnail_new_shader, 1f);
	}

	public long insertShader(String shader, String name, byte[] thumbnail, float quality) {
		try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
			return insertShader(db, shader, name, thumbnail, quality);
		}
	}

	public void updateShader(long id, @Nullable String shader, @Nullable byte[] thumbnail,
			float quality) {
		try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
			ContentValues cv = new ContentValues();
			cv.put(ShaderColumns.MODIFIED, currentTime());
			cv.put(ShaderColumns.QUALITY, quality);
			if (shader != null) {
				cv.put(ShaderColumns.FRAGMENT_SHADER, shader);
			}
			if (thumbnail != null) {
				cv.put(ShaderColumns.THUMB, thumbnail);
			}
			db.update(ShaderColumns.TABLE_NAME, cv, ShaderColumns._ID + " = ?",
					new String[]{String.valueOf(id)});
		}
	}
	// endregion

	public void updateShaderName(long id, String name) {
		try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
			ContentValues cv = new ContentValues();
			cv.put(ShaderColumns.NAME, name);
			db.update(ShaderColumns.TABLE_NAME, cv, ShaderColumns._ID + " = ?",
					new String[]{String.valueOf(id)});
		}
	}

	public void removeShader(long id) {
		try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
			db.delete(ShaderColumns.TABLE_NAME, ShaderColumns._ID + " = ?",
					new String[]{String.valueOf(id)});
		}
	}

	// region Texture API
	public List<TextureInfo> getTextures(String substring) {
		return getTextures(substring, false);
	}

	public List<TextureInfo> getSamplerCubeTextures(String substring) {
		return getTextures(substring, true);
	}

	@NonNull
	private List<TextureInfo> getTextures(String substring, boolean isCubeMap) {
		List<TextureInfo> textures = new ArrayList<>();
		String where = TextureColumns.RATIO + (isCubeMap ? " = 1.5" : " = 1") +
				(substring != null ? " AND " + TextureColumns.NAME + " LIKE ?" : "");
		String[] args = substring != null ? new String[]{"%" + substring + "%"} : null;

		String query =
				"SELECT " + TextureColumns._ID + "," + TextureColumns.NAME + "," + TextureColumns.WIDTH +
						"," + TextureColumns.HEIGHT + "," + TextureColumns.THUMB + " FROM " + TextureColumns.TABLE_NAME +
						" WHERE " + where + " ORDER BY " + TextureColumns._ID;

		try (SQLiteDatabase db = dbHelper.getReadableDatabase(); Cursor cursor = db.rawQuery(query
				, args)) {
			if (cursor.moveToFirst()) {
				do {
					textures.add(new TextureInfo(
							getLong(cursor, TextureColumns._ID),
							getString(cursor, TextureColumns.NAME),
							getInt(cursor, TextureColumns.WIDTH),
							getInt(cursor, TextureColumns.HEIGHT),
							getBlob(cursor, TextureColumns.THUMB)));
				} while (cursor.moveToNext());
			}
		}
		return textures;
	}

	@Nullable
	public TextureInfo getTextureInfo(long id) {
		String query =
				"SELECT " + TextureColumns._ID + "," + TextureColumns.NAME + "," + TextureColumns.WIDTH +
						"," + TextureColumns.HEIGHT + "," + TextureColumns.THUMB + " FROM " + TextureColumns.TABLE_NAME +
						" WHERE " + TextureColumns._ID + " = ?";

		try (SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)})) {
			if (cursor.moveToFirst()) {
				return new TextureInfo(
						getLong(cursor, TextureColumns._ID),
						getString(cursor, TextureColumns.NAME),
						getInt(cursor, TextureColumns.WIDTH),
						getInt(cursor, TextureColumns.HEIGHT),
						getBlob(cursor, TextureColumns.THUMB));
			}
		}
		return null;
	}


	@Nullable
	public Bitmap getTextureBitmap(long id) {
		return getTextureBitmap(String.valueOf(id));
	}

	@Nullable
	public Bitmap getTextureBitmap(String name) {
		String query = "SELECT " + TextureColumns.MATRIX + " FROM " + TextureColumns.TABLE_NAME +
				" WHERE " + TextureColumns.NAME + " = ?";
		try (SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor cursor = db.rawQuery(query, new String[]{name})) {
			if (cursor.moveToFirst()) {
				byte[] data = getBlob(cursor, TextureColumns.MATRIX);
				if (data != null) {
					return BitmapFactory.decodeByteArray(data, 0, data.length);
				}
			}
		} catch (OutOfMemoryError e) {
			// A texture might be too big to be loaded into memory.
		}
		return null;
	}

	public long insertTexture(String name, Bitmap bitmap) {
		try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
			return insertTexture(db, name, bitmap, textureThumbnailSize);
		}
	}

	public void removeTexture(long id) {
		try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
			db.delete(TextureColumns.TABLE_NAME, TextureColumns._ID + " = ?",
					new String[]{String.valueOf(id)});
		}
	}
	// endregion
}