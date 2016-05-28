package de.markusfisch.android.shadereditor.graphics;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;

import java.io.IOException;

public class BitmapEditor
{
	public static Bitmap getBitmapFromUri(
		Context context,
		Uri uri,
		int maxSize )
	{
		try
		{
			AssetFileDescriptor fd = context
				.getContentResolver()
				.openAssetFileDescriptor( uri, "r" );

			if( fd == null )
				return null;

			BitmapFactory.Options options =
				new BitmapFactory.Options();
			options.inSampleSize = getSampleSizeForBitmap(
				fd,
				maxSize,
				maxSize );

			return BitmapFactory.decodeFileDescriptor(
				fd.getFileDescriptor(),
				null,
				options );
		}
		catch( SecurityException | IOException e )
		{
			// fall through
		}

		return null;
	}

	public static Bitmap crop(
		Bitmap bitmap,
		RectF rect,
		float rotation )
	{
		if( bitmap == null )
			return null;

		try
		{
			if( rotation % 360f != 0 )
			{
				Matrix matrix = new Matrix();
				matrix.setRotate( rotation );

				bitmap = Bitmap.createBitmap(
					bitmap,
					0,
					0,
					bitmap.getWidth(),
					bitmap.getHeight(),
					matrix,
					true );
			}

			float w = bitmap.getWidth();
			float h = bitmap.getHeight();

			return Bitmap.createBitmap(
				bitmap,
				Math.round( rect.left*w ),
				Math.round( rect.top*h ),
				Math.round( rect.width()*w ),
				Math.round( rect.height()*h ) );
		}
		catch( IllegalArgumentException e )
		{
			return null;
		}
		catch( OutOfMemoryError e )
		{
			return null;
		}
	}

	private static int getSampleSizeForBitmap(
		AssetFileDescriptor fd,
		int maxWidth,
		int maxHeight )
	{
		BitmapFactory.Options options =
			new BitmapFactory.Options();

		options.inJustDecodeBounds = true;

		BitmapFactory.decodeFileDescriptor(
			fd.getFileDescriptor(),
			null,
			options );

		return calculateSampleSize(
			options.outWidth,
			options.outHeight,
			maxWidth,
			maxHeight );
	}

	private static int calculateSampleSize(
		int width,
		int height,
		int maxWidth,
		int maxHeight )
	{
		int size = 1;

		if( width > maxWidth ||
			height > maxHeight )
		{
			final int hw = width/2;
			final int hh = height/2;

			while(
				hw/size > maxWidth &&
				hh/size > maxHeight )
				size *= 2;
		}

		return size;
	}
}
