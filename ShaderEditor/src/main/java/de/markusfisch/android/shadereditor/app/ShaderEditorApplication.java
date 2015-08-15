package de.markusfisch.android.shadereditor.app;

import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.preference.Preferences;
import de.markusfisch.android.shadereditor.R;

import android.app.Application;
import android.database.SQLException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class ShaderEditorApplication extends Application
{
	public static Preferences preferences;
	public static DataSource dataSource;

	@Override
	public void onCreate()
	{
		super.onCreate();

		PreferenceManager.setDefaultValues(
			this,
			R.xml.preferences,
			false );

		preferences = new Preferences( this );
		dataSource = new DataSource( this );

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
					R.string.error_database,
					Toast.LENGTH_LONG ).show();
			}
		}.execute();
	}
}
