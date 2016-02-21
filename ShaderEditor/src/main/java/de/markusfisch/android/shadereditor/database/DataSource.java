package de.markusfisch.android.shadereditor.database;

import de.markusfisch.android.shadereditor.R;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataSource
{
	public static final String SHADERS = "shaders";
	public static final String SHADERS_ID = "_id";
	public static final String SHADERS_FRAGMENT_SHADER = "shader";
	public static final String SHADERS_THUMB = "thumb";
	public static final String SHADERS_CREATED = "created";
	public static final String SHADERS_MODIFIED = "modified";
	public static final String SHADERS_QUALITY = "quality";

	public static final String TEXTURES = "textures";
	public static final String TEXTURES_ID = "_id";
	public static final String TEXTURES_NAME = "name";
	public static final String TEXTURES_THUMB = "thumb";
	public static final String TEXTURES_MATRIX = "matrix";

	private SQLiteDatabase db;
	private OpenHelper helper;
	private Context context;
	private int textureThumbnailSize;

	public DataSource( Context context )
	{
		helper = new OpenHelper( context );

		textureThumbnailSize = Math.round(
			context
				.getResources()
				.getDisplayMetrics()
				.density*48f );

		this.context = context;
	}

	public boolean isOpen()
	{
		return db != null;
	}

	public boolean open() throws SQLException
	{
		return (db = helper.getWritableDatabase()) != null;
	}

	public void close()
	{
		helper.close();
		db = null;
	}

	public static boolean closeIfEmpty( Cursor cursor )
	{
		if( cursor != null &&
			cursor.moveToFirst() )
			return false;

		if( cursor != null )
			cursor.close();

		return true;
	}

	public Cursor getShaders()
	{
		return db.rawQuery(
			"SELECT "+
				SHADERS_ID+","+
				SHADERS_THUMB+","+
				SHADERS_MODIFIED+
				" FROM "+SHADERS+
				" ORDER BY "+SHADERS_ID,
			null );
	}

	public Cursor getTextures()
	{
		return db.rawQuery(
			"SELECT "+
				TEXTURES_ID+","+
				TEXTURES_NAME+","+
				TEXTURES_THUMB+
				" FROM "+TEXTURES+
				" ORDER BY "+TEXTURES_ID,
			null );
	}

	public Cursor getShader( long id )
	{
		return db.rawQuery(
			"SELECT "+
				SHADERS_ID+","+
				SHADERS_FRAGMENT_SHADER+","+
				SHADERS_MODIFIED+","+
				SHADERS_QUALITY+
				" FROM "+SHADERS+
				" WHERE "+SHADERS_ID+"="+id,
			null );
	}

	public Cursor getRandomShader()
	{
		return db.rawQuery(
			"SELECT "+
				SHADERS_ID+","+
				SHADERS_FRAGMENT_SHADER+","+
				SHADERS_MODIFIED+","+
				SHADERS_QUALITY+
				" FROM "+SHADERS+
				" ORDER BY RANDOM() LIMIT 1",
			null );
	}

	public byte[] getThumbnail( long id )
	{
		Cursor cursor = db.rawQuery(
			"SELECT "+
				SHADERS_THUMB+
				" FROM "+SHADERS+
				" WHERE "+SHADERS_ID+"="+id,
			null );

		if( closeIfEmpty( cursor ) )
			return null;

		byte thumbnail[] = cursor.getBlob(
			cursor.getColumnIndex(
				SHADERS_THUMB ) );

		cursor.close();

		return thumbnail;
	}

	public long getFirstShaderId()
	{
		Cursor cursor = db.rawQuery(
			"SELECT "+
				SHADERS_ID+
				" FROM "+SHADERS+
				" ORDER BY "+SHADERS_ID+
				" LIMIT 1",
			null );

		if( closeIfEmpty( cursor ) )
			return 0;

		long id = cursor.getLong(
			cursor.getColumnIndex(
				SHADERS_ID ) );

		cursor.close();

		return id;
	}

	public Cursor getTexture( long id )
	{
		return db.rawQuery(
			"SELECT "+
				TEXTURES_NAME+","+
				TEXTURES_MATRIX+
				" FROM "+TEXTURES+
				" WHERE "+TEXTURES_ID+"=\""+id+"\"",
			null );
	}

	public Bitmap getTextureBitmap( String name )
	{
		return getTextureBitmap( db.rawQuery(
			"SELECT "+
				TEXTURES_MATRIX+
				" FROM "+TEXTURES+
				" WHERE "+TEXTURES_NAME+"=\""+name+"\"",
			null ) );
	}

	public Bitmap getTextureBitmap( Cursor cursor )
	{
		if( closeIfEmpty( cursor ) )
			return null;

		byte data[] = cursor.getBlob(
			cursor.getColumnIndex(
				TEXTURES_MATRIX ) );

		Bitmap bm = BitmapFactory.decodeByteArray(
			data,
			0,
			data.length );

		cursor.close();

		return bm;
	}

	public static long insertShader(
		SQLiteDatabase db,
		String shader,
		byte thumbnail[],
		float quality )
	{
		String now = currentTime();

		ContentValues cv = new ContentValues();
		cv.put( SHADERS_FRAGMENT_SHADER, shader );
		cv.put( SHADERS_THUMB, thumbnail );
		cv.put( SHADERS_CREATED, now );
		cv.put( SHADERS_MODIFIED, now );
		cv.put( SHADERS_QUALITY, quality );

		return db.insert( SHADERS, null, cv );
	}

	public long insertShader(
		String shader,
		byte[] thumbnail,
		float quality )
	{
		return insertShader( db, shader, thumbnail, quality );
	}

	public long insertShader()
	{
		try
		{
			return insertShader(
				db,
				loadRawResource( R.raw.shader_new_shader ),
				loadBitmapResource( R.drawable.thumbnail_new_shader ),
				1f );
		}
		catch( IOException e )
		{
			return 0;
		}
	}

	public static long insertTexture(
		SQLiteDatabase db,
		String name,
		Bitmap bitmap,
		int thumbnailSize )
	{
		Bitmap thumbnail;

		try
		{
			thumbnail = Bitmap.createScaledBitmap(
				bitmap,
				thumbnailSize,
				thumbnailSize,
				true );
		}
		catch( IllegalArgumentException e )
		{
			return 0;
		}

		ContentValues cv = new ContentValues();
		cv.put( TEXTURES_NAME, name );
		cv.put( TEXTURES_THUMB, bitmapToPng( thumbnail ) );
		cv.put( TEXTURES_MATRIX, bitmapToPng( bitmap ) );

		return db.insert( TEXTURES, null, cv );
	}

	public long insertTexture( String name, Bitmap bitmap )
	{
		return insertTexture(
			db,
			name,
			bitmap,
			textureThumbnailSize );
	}

	public void updateShader(
		long id,
		String shader,
		byte thumbnail[],
		float quality )
	{
		ContentValues cv = new ContentValues();
		cv.put( SHADERS_FRAGMENT_SHADER, shader );
		cv.put( SHADERS_MODIFIED, currentTime() );
		cv.put( SHADERS_QUALITY, quality );

		if( thumbnail != null )
			cv.put( SHADERS_THUMB, thumbnail );

		db.update(
			SHADERS,
			cv,
			SHADERS_ID+"="+id,
			null );
	}

	public void updateShaderQuality(
		long id,
		float quality )
	{
		ContentValues cv = new ContentValues();
		cv.put( SHADERS_QUALITY, quality );

		db.update(
			SHADERS,
			cv,
			SHADERS_ID+"="+id,
			null );
	}

	public void removeShader( long id )
	{
		db.delete(
			SHADERS,
			SHADERS_ID+"="+id,
			null );
	}

	public void removeTexture( long id )
	{
		db.delete(
			TEXTURES,
			TEXTURES_ID+"="+id,
			null );
	}

	private static String currentTime()
	{
		return new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss",
			Locale.US ).format( new Date() );
	}

	private String loadRawResource( int id ) throws IOException
	{
		InputStream in = context.getResources().openRawResource( id );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];

		for( int r; (r = in.read( buf )) != -1; )
			out.write( buf, 0, r );

		return out.toString();
	}

	private byte[] loadBitmapResource( int id )
	{
		return bitmapToPng( BitmapFactory.decodeResource(
			context.getResources(),
			id ) );
	}

	private static byte[] bitmapToPng( Bitmap bitmap )
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		bitmap.compress( Bitmap.CompressFormat.PNG, 100, out );

		return out.toByteArray();
	}

	private void createShadersTable( SQLiteDatabase db )
	{
		db.execSQL( "DROP TABLE IF EXISTS "+SHADERS );
		db.execSQL(
			"CREATE TABLE "+SHADERS+" ("+
				SHADERS_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+
				SHADERS_FRAGMENT_SHADER+" TEXT NOT NULL,"+
				SHADERS_THUMB+" BLOB,"+
				SHADERS_CREATED+" DATETIME,"+
				SHADERS_MODIFIED+" DATETIME,"+
				SHADERS_QUALITY+" REAL );" );

		insertInitalShaders( db );
	}

	private void addShadersQuality( SQLiteDatabase db )
	{
		db.execSQL(
			"ALTER TABLE "+SHADERS+
				" ADD COLUMN "+SHADERS_QUALITY+" REAL;" );
		db.execSQL(
			"UPDATE "+SHADERS+
				" SET "+SHADERS_QUALITY+" = 1;" );
	}

	private void insertInitalShaders( SQLiteDatabase db )
	{
		try
		{
			DataSource.insertShader(
				db,
				loadRawResource(
					R.raw.shader_color_hole ),
				loadBitmapResource(
					R.drawable.thumbnail_color_hole ),
				1f );

			DataSource.insertShader(
				db,
				loadRawResource(
					R.raw.shader_gravity ),
				loadBitmapResource(
					R.drawable.thumbnail_gravity ),
				1f );

			DataSource.insertShader(
				db,
				loadRawResource(
					R.raw.shader_laser_lines ),
				loadBitmapResource(
					R.drawable.thumbnail_laser_lines ),
				1f );
		}
		catch( IOException e )
		{
			// shouldn't ever happen in production
			// and nothing can be done if it does
		}
	}

	private void createTexturesTable( SQLiteDatabase db )
	{
		db.execSQL( "DROP TABLE IF EXISTS "+TEXTURES );
		db.execSQL(
			"CREATE TABLE "+TEXTURES+" ("+
				TEXTURES_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+
				TEXTURES_NAME+" TEXT NOT NULL UNIQUE,"+
				TEXTURES_THUMB+" BLOB,"+
				TEXTURES_MATRIX+" BLOB );" );

		insertInitalTextures( db );
	}

	private void insertInitalTextures( SQLiteDatabase db )
	{
		DataSource.insertTexture(
			db,
			context.getString( R.string.texture_name_noise ),
			BitmapFactory.decodeResource(
				context.getResources(),
				R.drawable.texture_noise ),
			textureThumbnailSize );
	}

	private class OpenHelper extends SQLiteOpenHelper
	{
		public OpenHelper( Context c )
		{
			super( c, "shaders.db", null, 3 );
		}

		@Override
		public void onCreate( SQLiteDatabase db )
		{
			createShadersTable( db );
			createTexturesTable( db );
		}

		@Override
		public void onDowngrade(
			SQLiteDatabase db,
			int oldVersion,
			int newVersion )
		{
			// without onDowngrade(), a downgrade will throw
			// an exception
		}

		@Override
		public void onUpgrade(
			SQLiteDatabase db,
			int oldVersion,
			int newVersion )
		{
			if( oldVersion == 1 )
			{
				createTexturesTable( db );
				insertInitalShaders( db );
			}

			if( oldVersion < 3 )
			{
				addShadersQuality( db );
			}
		}
	}
}
