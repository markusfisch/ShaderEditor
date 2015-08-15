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
	public static final String SHADERS_SHADER = "shader";
	public static final String SHADERS_THUMB = "thumb";
	public static final String SHADERS_CREATED = "created";
	public static final String SHADERS_MODIFIED = "modified";

	private SQLiteDatabase db;
	private OpenHelper helper;
	private Context context;

	public DataSource( Context context )
	{
		helper = new OpenHelper( context );

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

	public Cursor queryShaders()
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

	public Cursor getShader( long id )
	{
		return db.rawQuery(
			"SELECT "+
				SHADERS_ID+","+
				SHADERS_SHADER+","+
				SHADERS_MODIFIED+
				" FROM "+SHADERS+
				" WHERE "+SHADERS_ID+"="+id,
			null );
	}

	public Cursor getRandomShader()
	{
		return db.rawQuery(
			"SELECT "+
				SHADERS_ID+","+
				SHADERS_SHADER+","+
				SHADERS_MODIFIED+
				" FROM "+SHADERS+
				" ORDER BY RANDOM() LIMIT 1",
			null );
	}

	public static long insert(
		SQLiteDatabase db,
		String shader,
		byte[] thumbnail )
	{
		String now = currentTime();

		ContentValues cv = new ContentValues();
		cv.put( SHADERS_SHADER, shader );
		cv.put( SHADERS_THUMB, thumbnail );
		cv.put( SHADERS_CREATED, now );
		cv.put( SHADERS_MODIFIED, now );

		return db.insert( SHADERS, null, cv );
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
				loadRawResource( R.raw.shader_new_shader ),
				loadBitmapResource( R.drawable.thumbnail_new_shader ) );
		}
		catch( IOException e )
		{
			return 0;
		}
	}

	public void update( long id, String shader, byte[] thumbnail )
	{
		ContentValues cv = new ContentValues();
		cv.put( SHADERS_SHADER, shader );
		cv.put( SHADERS_THUMB, thumbnail );
		cv.put( SHADERS_MODIFIED, currentTime() );

		db.update(
			SHADERS,
			cv,
			SHADERS_ID+"="+id,
			null );
	}

	public void remove( long id )
	{
		db.delete(
			SHADERS,
			SHADERS_ID+"="+id,
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
			db.execSQL( "DROP TABLE IF EXISTS "+SHADERS );
			db.execSQL(
				"CREATE TABLE "+SHADERS+" ("+
					SHADERS_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+
					SHADERS_SHADER+" TEXT NOT NULL,"+
					SHADERS_THUMB+" BLOB,"+
					SHADERS_CREATED+" DATETIME,"+
					SHADERS_MODIFIED+" DATETIME );" );

			try
			{
				DataSource.insert(
					db,
					loadRawResource(
						R.raw.shader_laser_lines ),
					loadBitmapResource(
						R.drawable.thumbnail_laser_lines ) );

				DataSource.insert(
					db,
					loadRawResource(
						R.raw.shader_color_hole ),
					loadBitmapResource(
						R.drawable.thumbnail_color_hole ) );
			}
			catch( IOException e )
			{
				// fail silently
			}
		}

		@Override
		public void onDowngrade(
			SQLiteDatabase db,
			int oldVersion,
			int newVersion )
		{
			// without that method, a downgrade will
			// cause an exception
		}

		@Override
		public void onUpgrade(
			SQLiteDatabase db,
			int oldVersion,
			int newVersion )
		{
			// there'll be upgrades
		}
	}
}
