package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public abstract class AbstractSecondaryActivity extends AppCompatActivity
{
	private FragmentManager fm;

	public static void addFragment(
		FragmentManager fm,
		Fragment fragment )
	{
		fm.beginTransaction()
			.replace(
				R.id.content_frame,
				fragment )
			.addToBackStack( null )
			.commit();
	}

	@Override
	public boolean onSupportNavigateUp()
	{
		if( fm.getBackStackEntryCount() > 0 )
			fm.popBackStack();
		else
			finish();

		return true;
	}

	@Override
	protected void onCreate( Bundle state )
	{
		super.onCreate( state );
		setContentView( R.layout.activity_secondary );

		initToolbar();

		fm = getSupportFragmentManager();

		if( state == null )
			fm.beginTransaction()
				.replace(
					R.id.content_frame,
					defaultFragment() )
				.commit();
	}

	protected abstract Fragment defaultFragment();

	private void initToolbar()
	{
		Toolbar toolbar = (Toolbar)findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );
		getSupportActionBar().setDisplayHomeAsUpEnabled( true );
	}
}
