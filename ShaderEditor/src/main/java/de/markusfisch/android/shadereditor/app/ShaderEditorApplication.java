package de.markusfisch.android.shadereditor.app;

import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.R;

import android.app.Application;
import android.database.SQLException;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.widget.Toast;

public class ShaderEditorApplication extends Application
{
	public static int systemBarColor;
	public static Preferences preferences;
	public static DataSource dataSource;
	public static boolean batteryLow = false;

	@Override
	public void onCreate()
	{
		super.onCreate();

		systemBarColor =
			ContextCompat.getColor(
				this,
				R.color.primary_dark_translucent );

		PreferenceManager.setDefaultValues(
			this,
			R.xml.preferences,
			false );

		preferences = new Preferences( this );
		dataSource = new DataSource( this );

		openDataSourceAsync();
	}

	private void openDataSourceAsync()
	{
		new AsyncTask<Void, Void, Boolean>()
		{
			@Override
			protected Boolean doInBackground( Void... nothings )
			{
				try
				{
					return dataSource.open();
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
					ShaderEditorApplication.this,
					R.string.cannot_open_database,
					Toast.LENGTH_LONG ).show();
			}
		}.execute();
	}
}
