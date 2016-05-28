package de.markusfisch.android.shadereditor.activity;

import de.markusfisch.android.shadereditor.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public abstract class AbstractContentActivity
	extends AbstractSubsequentActivity
{
	@Override
	protected void onCreate( Bundle state )
	{
		super.onCreate( state );
		setContentView( R.layout.activity_subsequent );

		initSystemBars( this );
		initToolbar( this );

		if( state == null )
			AbstractSubsequentActivity.setFragment(
				getSupportFragmentManager(),
				defaultFragment() );
	}

	protected abstract Fragment defaultFragment();
}
