package de.markusfisch.android.shadereditor.opengl;

public class InfoLog
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

	public static void parse( String infoLog )
	{
		message = null;
		errorLine = 0;

		if( infoLog == null )
			return;

		int from;

		if( (from = infoLog.indexOf( "ERROR: 0:" )) > -1 )
			from += 9;
		else if( (from = infoLog.indexOf( "0:" )) > -1 )
			from += 2;

		if( from > -1 )
		{
			int to;

			if( (to = infoLog.indexOf( ":", from )) > -1 )
			{
				try
				{
					errorLine = Integer.parseInt(
						infoLog.substring( from, to ).trim() );
				}
				catch( NumberFormatException e )
				{
					// can't do anything about it
				}
				catch( NullPointerException e )
				{
					// can't do anything about it
				}

				from = ++to;
			}

			if( (to = infoLog.indexOf( "\n", from )) < 0 )
				to = infoLog.length();

			infoLog = infoLog.substring( from, to ).trim();
		}

		message = infoLog;
	}
}
