package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public abstract class AbstractSubsequentActivity extends AppCompatActivity
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

	public static void initToolbar( AppCompatActivity activity )
	{
		Toolbar toolbar = (Toolbar)activity.findViewById( R.id.toolbar );
		activity.setSupportActionBar( toolbar );
		activity.getSupportActionBar().setDisplayHomeAsUpEnabled( true );
	}

	public static void initSystemBars( AppCompatActivity activity )
	{
		MainActivity.setSystemBarColor(
			activity.getWindow(),
			ShaderEditorApplication
				.preferences
				.getSystemBarColor(),
			false );
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
		fm = getSupportFragmentManager();
	}
}
