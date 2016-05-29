package de.markusfisch.android.shadereditor.preference;

import de.markusfisch.android.shadereditor.R;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.TypedValue;

import java.lang.NumberFormatException;

public class Preferences
{
	public static final String WALLPAPER_SHADER = "shader";
	public static final String SAVE_BATTERY = "save_battery";
	public static final String RUN_MODE = "run_mode";
	public static final String UPDATE_DELAY = "update_delay";
	public static final String SENSOR_DELAY = "sensor_delay";
	public static final String TEXT_SIZE = "text_size";
	public static final String TAB_WIDTH = "tab_width";
	public static final String SHOW_INSERT_TAB = "show_insert_tab";
	public static final String SAVE_ON_RUN = "save_on_run";

	private static final int RUN_AUTO = 1;
	private static final int RUN_MANUALLY = 2;
	private static final int RUN_MANUALLY_EXTRA = 3;

	private SharedPreferences preferences;
	private long wallpaperShaderId = 1;
	private boolean saveBattery = true;
	private int runMode = RUN_AUTO;
	private int updateDelay = 1000;
	private int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
	private int textSize = 12;
	private int tabWidth = 4;
	private boolean showInsertTab = true;
	private boolean saveOnRun = true;
	private boolean batteryLow = false;
	private int systemBarColor;

	public void init( Context context )
	{
		systemBarColor =
			ContextCompat.getColor(
				context,
				R.color.primary_dark_translucent );

		PreferenceManager.setDefaultValues(
			context,
			R.xml.preferences,
			false );

		preferences = PreferenceManager
			.getDefaultSharedPreferences( context );

		update();
	}

	public SharedPreferences getSharedPreferences()
	{
		return preferences;
	}

	public void update()
	{
		wallpaperShaderId = parseLong(
			preferences.getString( WALLPAPER_SHADER, null ),
			wallpaperShaderId );
		saveBattery = preferences.getBoolean(
			SAVE_BATTERY,
			saveBattery );
		runMode = parseInt(
			preferences.getString( RUN_MODE, null ),
			runMode );
		updateDelay = parseInt(
			preferences.getString( UPDATE_DELAY, null ),
			updateDelay );
		sensorDelay = parseSensorDelay(
			preferences.getString( SENSOR_DELAY, null ),
			sensorDelay );
		textSize = parseInt(
			preferences.getString( TEXT_SIZE, null ),
			textSize );
		tabWidth = parseInt(
			preferences.getString( TAB_WIDTH, null ),
			tabWidth );
		showInsertTab = preferences.getBoolean(
			SHOW_INSERT_TAB,
			showInsertTab );
		saveOnRun = preferences.getBoolean(
			SAVE_ON_RUN,
			saveOnRun );
	}

	public boolean saveBattery()
	{
		return saveBattery;
	}

	public boolean doesRunOnChange()
	{
		return runMode == RUN_AUTO;
	}

	public boolean doesRunInBackground()
	{
		return runMode != RUN_MANUALLY_EXTRA;
	}

	public int getUpdateDelay()
	{
		return updateDelay;
	}

	public int getSensorDelay()
	{
		return sensorDelay;
	}

	public int getTextSize()
	{
		return textSize;
	}

	public int getTabWidth()
	{
		return tabWidth;
	}

	public boolean doesShowInsertTab()
	{
		return showInsertTab;
	}

	public boolean doesSaveOnRun()
	{
		return saveOnRun;
	}

	public long getWallpaperShader()
	{
		return wallpaperShaderId;
	}

	public void setWallpaperShader( long id )
	{
		wallpaperShaderId = id;

		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(
			WALLPAPER_SHADER,
			String.valueOf( wallpaperShaderId ) );
		editor.apply();
	}

	public boolean isBatteryLow()
	{
		return batteryLow;
	}

	public void setBatteryLow( boolean isLow )
	{
		batteryLow = isLow;
	}

	public int getSystemBarColor()
	{
		return systemBarColor;
	}

	public static int getStatusAndToolBarHeight( Context context )
	{
		return
			getStatusBarHeight( context.getResources() )+
			getToolBarHeight( context );
	}

	public static int getStatusBarHeight( Resources res )
	{
		// don't store status bar height because it may be
		// different for each context
		return getIdentifierDimen( res, "status_bar_height" );
	}

	public static void getNavigationBarHeight( Resources res, Point size )
	{
		// don't store navigation bar height because it may be
		// different for each context
		size.x = size.y = 0;

		if( !getIdentifierBoolean( res, "config_showNavigationBar" ) )
			return;

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2 )
		{
			Configuration conf = res.getConfiguration();

			if( conf.orientation == Configuration.ORIENTATION_LANDSCAPE &&
				// according to https://developer.android.com/training/multiscreen/screensizes.html#TaskUseSWQuali
				// only a screen < 600 dp is considered to be a phone and
				// can move its navigation bar to the side
				conf.smallestScreenWidthDp < 600 )
			{
				size.x = getIdentifierDimen(
					res,
					"navigation_bar_height_landscape" );

				return;
			}
		}

		size.y = getIdentifierDimen( res, "navigation_bar_height" );
	}

	public static int getToolBarHeight( Context context )
	{
		// don't store toolbar bar height because it may be
		// different for each context
		TypedValue tv = new TypedValue();

		return
			context.getTheme().resolveAttribute(
				android.R.attr.actionBarSize,
				tv,
				true ) ?
			TypedValue.complexToDimensionPixelSize(
				tv.data,
				context.getResources().getDisplayMetrics() ) :
			0;
	}

	public static int parseInt( String s, int preset )
	{
		try
		{
			if( s != null &&
				s.length() > 0 )
				return Integer.parseInt( s );
		}
		catch( NumberFormatException e )
		{
			// use preset
		}

		return preset;
	}

	public static long parseLong( String s, long preset )
	{
		try
		{
			if( s != null &&
				s.length() > 0 )
				return Long.parseLong( s );
		}
		catch( NumberFormatException e )
		{
			// use preset
		}

		return preset;
	}

	private static boolean getIdentifierBoolean(
		Resources res,
		String name )
	{
		int id = res.getIdentifier(
			name,
			"bool",
			"android" );

		return id > 0 && res.getBoolean( id );
	}

	private static int getIdentifierDimen(
		Resources res,
		String name )
	{
		int id = res.getIdentifier(
			name,
			"dimen",
			"android" );

		return id > 0 ?
			res.getDimensionPixelSize( id ) :
			0;
	}

	private static int parseSensorDelay( String s, int preset )
	{
		if( s == null )
			return preset;

		switch( s )
		{
			case "Fastest":
				return SensorManager.SENSOR_DELAY_FASTEST;
			case "Game":
				return SensorManager.SENSOR_DELAY_GAME;
			case "Normal":
				return SensorManager.SENSOR_DELAY_NORMAL;
			case "UI":
				return SensorManager.SENSOR_DELAY_UI;
		}

		return preset;
	}
}
