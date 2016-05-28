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
import android.os.AsyncTask;
import android.widget.Toast;

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
	public static final String TEXTURES_WIDTH = "width";
	public static final String TEXTURES_HEIGHT = "height";
	public static final String TEXTURES_RATIO = "ratio";
	public static final String TEXTURES_THUMB = "thumb";
	public static final String TEXTURES_MATRIX = "matrix";

	private SQLiteDatabase db;
	private Context context;
	private int textureThumbnailSize;

	public void openAsync( final Context context )
	{
		this.context = context;

		textureThumbnailSize = Math.round(
			context
				.getResources()
				.getDisplayMetrics()
				.density*48f );

		final OpenHelper helper = new OpenHelper( context );

		new AsyncTask<Void, Void, Boolean>()
		{
			@Override
			protected Boolean doInBackground( Void... nothings )
			{
				try
				{
					return (db = helper.getWritableDatabase()) != null;
				}
				catch( SQLException e )
				{
					return false;
				}
			}

			@Override
			protected void onPostExecute( Boolean success )
			{
				if( success )
					return;

				Toast.makeText(
					context,
					R.string.cannot_open_database,
					Toast.LENGTH_LONG ).show();
			}
		}.execute();
	}

	public boolean isOpen()
	{
		return db != null;
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
				TEXTURES_WIDTH+","+
				TEXTURES_HEIGHT+","+
				TEXTURES_THUMB+
				" FROM "+TEXTURES+
				" WHERE "+TEXTURES_RATIO+" = 1"+
				" ORDER BY "+TEXTURES_ID,
			null );
	}

	public Cursor getSamplerCubeTextures()
	{
		return db.rawQuery(
			"SELECT "+
				TEXTURES_ID+","+
				TEXTURES_NAME+","+
				TEXTURES_WIDTH+","+
				TEXTURES_HEIGHT+","+
				TEXTURES_THUMB+
				" FROM "+TEXTURES+
				" WHERE "+TEXTURES_RATIO+" = 1.5"+
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
				TEXTURES_WIDTH+","+
				TEXTURES_HEIGHT+","+
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

		Bitmap bm = textureFromCursor( cursor );
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

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		ContentValues cv = new ContentValues();
		cv.put( TEXTURES_NAME, name );
		cv.put( TEXTURES_WIDTH, w );
		cv.put( TEXTURES_HEIGHT, h );
		cv.put( TEXTURES_RATIO, calculateRatio( w, h ) );
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

	private static Bitmap textureFromCursor( Cursor cursor )
	{
		byte data[] = cursor.getBlob(
			cursor.getColumnIndex(
				TEXTURES_MATRIX ) );

		return BitmapFactory.decodeByteArray(
			data,
			0,
			data.length );
	}

	private String loadRawResource( int id ) throws IOException
	{
		InputStream in = context
			.getResources()
			.openRawResource( id );

		int l = in.available();
		byte b[] = new byte[l];

		return in.read( b ) == l ?
			new String( b, "UTF-8" ) :
			null;
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

	private static float calculateRatio( int width, int height )
	{
		// round to two decimal places to avoid problems with
		// rounding errors; the query will filter precisely 1 or 1.5
		return Math.round( ((float)height/width)*100f )/100f;
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

	private static void addShadersQuality( SQLiteDatabase db )
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
				TEXTURES_WIDTH+" INTEGER,"+
				TEXTURES_HEIGHT+" INTEGER,"+
				TEXTURES_RATIO+" REAL,"+
				TEXTURES_THUMB+" BLOB,"+
				TEXTURES_MATRIX+" BLOB );" );

		insertInitalTextures( db );
	}

	private static void addTexturesWidthHeightRatio( SQLiteDatabase db )
	{
		db.execSQL(
			"ALTER TABLE "+TEXTURES+
				" ADD COLUMN "+TEXTURES_WIDTH+" INTEGER;" );
		db.execSQL(
			"ALTER TABLE "+TEXTURES+
				" ADD COLUMN "+TEXTURES_HEIGHT+" INTEGER;" );
		db.execSQL(
			"ALTER TABLE "+TEXTURES+
				" ADD COLUMN "+TEXTURES_RATIO+" REAL;" );

		Cursor cursor = db.rawQuery(
			"SELECT "+
				TEXTURES_ID+","+
				TEXTURES_MATRIX+
				" FROM "+TEXTURES,
			null );

		if( closeIfEmpty( cursor ) )
			return;

		do
		{
			Bitmap bm = textureFromCursor( cursor );

			if( bm == null )
				continue;

			int width = bm.getWidth();
			int height = bm.getHeight();
			float ratio = calculateRatio( width, height );
			bm.recycle();

			db.execSQL(
				"UPDATE "+TEXTURES+
					" SET "+
						TEXTURES_WIDTH+" = "+width+", "+
						TEXTURES_HEIGHT+" = "+height+", "+
						TEXTURES_RATIO+" = "+ratio+
					" WHERE "+TEXTURES_ID+" = "+cursor.getLong(
						cursor.getColumnIndex( TEXTURES_ID ) )+";" );

		} while( cursor.moveToNext() );

		cursor.close();
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
		public OpenHelper( Context context )
		{
			super( context, "shaders.db", null, 4 );
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
			// an exception; can never happen in production
		}

		@Override
		public void onUpgrade(
			SQLiteDatabase db,
			int oldVersion,
			int newVersion )
		{
			if( oldVersion < 2 )
			{
				createTexturesTable( db );
				insertInitalShaders( db );
			}

			if( oldVersion < 3 )
				addShadersQuality( db );

			if( oldVersion < 4 )
				addTexturesWidthHeightRatio( db );
		}
	}
}
