package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.fragment.PreferencesFragment;
import de.markusfisch.android.shadereditor.R;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class PreferencesActivity extends AppCompatActivity
{
	@Override
	protected void onCreate( Bundle state )
	{
		super.onCreate( state );
		setContentView( R.layout.activity_preferences );

		initToolbar();

		if( state == null )
			getSupportFragmentManager()
				.beginTransaction()
				.replace(
					R.id.content_frame,
					new PreferencesFragment() )
				.commit();
	}

	private void initToolbar()
	{
		Toolbar toolbar = (Toolbar)findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );
		getSupportActionBar().setDisplayHomeAsUpEnabled( true );
	}
}
