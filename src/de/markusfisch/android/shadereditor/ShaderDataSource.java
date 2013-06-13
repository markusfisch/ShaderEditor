package de.markusfisch.android.shadereditor;

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
import java.text.SimpleDateFormat;
import java.util.Date;

public class ShaderDataSource
{
	public static final String TABLE = "shaders";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_SHADER = "shader";
	public static final String COLUMN_THUMB = "thumb";
	public static final String COLUMN_CREATED = "created";
	public static final String COLUMN_MODIFIED = "modified";

	private SQLiteDatabase db;
	private OpenHelper helper;
	private Context context;

	public ShaderDataSource( Context c )
	{
		helper = new OpenHelper( c );
		context = c;
	}

	public void open() throws SQLException
	{
		db = helper.getWritableDatabase();
	}

	public void close()
	{
		helper.close();
	}

	public Cursor queryAll()
	{
		return db.rawQuery(
			"SELECT "+
				COLUMN_ID+","+
				COLUMN_SHADER+","+
				COLUMN_THUMB+","+
				COLUMN_CREATED+","+
				COLUMN_MODIFIED+
				" FROM "+TABLE+
				" ORDER BY "+COLUMN_ID,
			null );
	}

	public String getShader( long id )
	{
		try
		{
			Cursor c = db.rawQuery(
				"SELECT "+
					COLUMN_SHADER+
					" FROM "+TABLE+
					" WHERE "+COLUMN_ID+"="+id,
				null );

			if( c == null )
				return null;

			c.moveToFirst();

			return c.getString( 0 );
		}
		catch( Exception e )
		{
			return null;
		}
	}

	public Cursor getRandomShader()
	{
		try
		{
			Cursor c = db.rawQuery(
				"SELECT "+
					COLUMN_ID+", "+
					COLUMN_SHADER+
					" FROM "+TABLE+
					" ORDER BY RANDOM() LIMIT 1",
				null );

			if( c == null )
				return null;

			c.moveToFirst();

			return c;
		}
		catch( Exception e )
		{
			return null;
		}
	}

	public static long insert(
		SQLiteDatabase db,
		String shader,
		byte[] thumbnail )
	{
		String now = currentTime();

		ContentValues cv = new ContentValues();
		cv.put( COLUMN_SHADER, shader );
		cv.put( COLUMN_THUMB, thumbnail );
		cv.put( COLUMN_CREATED, now );
		cv.put( COLUMN_MODIFIED, now );

		return db.insert( TABLE, null, cv );
	}

	public long insert( String shader, byte[] thumbnail )
	{
		return insert( db, shader, thumbnail );
	}

	public long insert()
	{
		try
		{
			return insert(
				db,
				loadRawResource( R.raw.new_shader ),
				loadBitmapResource( R.drawable.thumbnail_new_shader ) );
		}
		catch( Exception e )
		{
			return 0;
		}
	}

	public void update( long id, String shader, byte[] thumbnail )
	{
		ContentValues cv = new ContentValues();
		cv.put( COLUMN_SHADER, shader );
		cv.put( COLUMN_THUMB, thumbnail );
		cv.put( COLUMN_MODIFIED, currentTime() );

		db.update(
			TABLE,
			cv,
			COLUMN_ID+"="+id,
			null );
	}

	public void remove( long id )
	{
		db.delete(
			TABLE,
			COLUMN_ID+"="+id,
			null );
	}

	private static String currentTime()
	{
		return new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss" ).format(
			new Date() );
	}

	private String loadRawResource( int id ) throws Exception
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
		Bitmap image = BitmapFactory.decodeResource(
			context.getResources(),
			id );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		image.compress( Bitmap.CompressFormat.PNG, 100, out );

		return out.toByteArray();
	}

	private class OpenHelper extends SQLiteOpenHelper
	{
		public OpenHelper( Context c )
		{
			super( c, "shaders.db", null, 1 );
		}

		@Override
		public void onCreate( SQLiteDatabase db )
		{
			db.execSQL(
				"CREATE TABLE "+TABLE+" ("+
					COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+
					COLUMN_SHADER+" TEXT NOT NULL,"+
					COLUMN_THUMB+" BLOB,"+
					COLUMN_CREATED+" DATETIME,"+
					COLUMN_MODIFIED+" DATETIME );" );

			try
			{
				ShaderDataSource.insert(
					db,
					loadRawResource(
						R.raw.aurora_streaks ),
					loadBitmapResource(
						R.drawable.thumbnail_aurora_streaks ) );

				ShaderDataSource.insert(
					db,
					loadRawResource(
						R.raw.color_hole ),
					loadBitmapResource(
						R.drawable.thumbnail_color_hole ) );
			}
			catch( Exception e )
			{
			}
		}

		@Override
		public void onUpgrade(
			SQLiteDatabase db,
			int oldVersion,
			int newVersion )
		{
			db.execSQL( "DROP TABLE IF EXISTS "+TABLE );
			onCreate( db );
		}
	}
}
