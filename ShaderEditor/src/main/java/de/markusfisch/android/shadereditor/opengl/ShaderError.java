package de.markusfisch.android.shadereditor.opengl;

public class ShaderError
{
	private static String message;
	private static int errorLine;

	public static String getMessage()
	{
		return message;
	}

	public static int getErrorLine()
	{
		return errorLine;
	}

	public static void parseError( String glError )
	{
		message = null;
		errorLine = 0;

		if( glError == null )
			return;

		int from;

		if( (from = glError.indexOf( "ERROR: 0:" )) > -1 )
			from += 9;
		else if( (from = glError.indexOf( "0:" )) > -1 )
			from += 2;

		if( from > -1 )
		{
			int to;

			if( (to = glError.indexOf( ":", from )) > -1 )
			{
				try
				{
					errorLine = Integer.valueOf(
						glError.substring( from, to ).trim() );
				}
				catch( NullPointerException e )
				{
					// can't do anything about it
				}

				from = ++to;
			}

			if( (to = glError.indexOf( "\n", from )) < 0 )
				to = glError.length();

			glError = glError.substring( from, to ).trim();
		}

		message = glError;
	}
}
