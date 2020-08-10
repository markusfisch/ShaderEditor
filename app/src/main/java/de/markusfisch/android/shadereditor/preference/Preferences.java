package de.markusfisch.android.shadereditor.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;

import de.markusfisch.android.shadereditor.R;

public class Preferences {
	public static final String WALLPAPER_SHADER = "shader";
	public static final String SAVE_BATTERY = "save_battery";
	public static final String RUN_MODE = "run_mode";
	public static final String UPDATE_DELAY = "update_delay";
	public static final String SENSOR_DELAY = "sensor_delay";
	public static final String TEXT_SIZE = "text_size";
	public static final String TAB_WIDTH = "tab_width";
	public static final String SHOW_INSERT_TAB = "show_insert_tab";
	public static final String EXPORT_TABS = "export_tabs";
	public static final String SAVE_ON_RUN = "save_on_run";
	public static final String DEFAULT_NEW_SHADER = "default_new_shader";
	public static final String DISABLE_HIGHLIGHTING = "disable_highlighting";
	public static final String AUTO_SAVE = "auto_save";
	public static final String IMPORT_FROM_DIRECTORY = "import_from_directory";
	public static final String EXPORT_TO_DIRECTORY = "export_to_directory";
	public static final String IMPORT_FROM_DATABASE = "import_from_database";
	public static final String EXPORT_TO_DATABASE = "export_to_database";

	private static final int RUN_AUTO = 1;
	private static final int RUN_MANUALLY = 2;
	private static final int RUN_MANUALLY_EXTRA = 3;
	private static final int RUN_MANUALLY_EXTRA_NEW = 4;

	private SharedPreferences preferences;
	private long wallpaperShaderId = 1;
	private boolean saveBattery = true;
	private int runMode = RUN_AUTO;
	private int updateDelay = 1000;
	private int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
	private int textSize = 12;
	private int tabWidth = 4;
	private boolean exportTabs = false;
	private boolean showInsertTab = true;
	private boolean saveOnRun = true;
	private boolean batteryLow = false;
	private int systemBarColor;
	private long defaultNewShaderId = 0;
	private boolean disableHighlighting = false;
	private boolean autoSave = true;

	public void init(Context context) {
		systemBarColor = ContextCompat.getColor(
				context,
				R.color.primary_dark_translucent);

		PreferenceManager.setDefaultValues(
				context,
				R.xml.preferences,
				false);

		preferences = PreferenceManager.getDefaultSharedPreferences(
				context);

		update();
	}

	public SharedPreferences getSharedPreferences() {
		return preferences;
	}

	public void update() {
		wallpaperShaderId = parseLong(
				preferences.getString(WALLPAPER_SHADER, null),
				wallpaperShaderId);
		saveBattery = preferences.getBoolean(
				SAVE_BATTERY,
				saveBattery);
		runMode = parseInt(
				preferences.getString(RUN_MODE, null),
				runMode);
		updateDelay = parseInt(
				preferences.getString(UPDATE_DELAY, null),
				updateDelay);
		sensorDelay = parseSensorDelay(
				preferences.getString(SENSOR_DELAY, null),
				sensorDelay);
		textSize = parseInt(
				preferences.getString(TEXT_SIZE, null),
				textSize);
		tabWidth = parseInt(
				preferences.getString(TAB_WIDTH, null),
				tabWidth);
		exportTabs = preferences.getBoolean(
				EXPORT_TABS,
				exportTabs);
		showInsertTab = preferences.getBoolean(
				SHOW_INSERT_TAB,
				showInsertTab);
		saveOnRun = preferences.getBoolean(
				SAVE_ON_RUN,
				saveOnRun);
		disableHighlighting = preferences.getBoolean(
				DISABLE_HIGHLIGHTING,
				disableHighlighting);
		autoSave = preferences.getBoolean(
				AUTO_SAVE,
				autoSave);
		defaultNewShaderId = parseLong(
				preferences.getString(DEFAULT_NEW_SHADER, null),
				defaultNewShaderId);
	}

	public boolean saveBattery() {
		return saveBattery;
	}

	public boolean doesRunOnChange() {
		return runMode == RUN_AUTO;
	}

	public boolean doesRunInBackground() {
		return runMode != RUN_MANUALLY_EXTRA &&
				runMode != RUN_MANUALLY_EXTRA_NEW;
	}

	public boolean doesRunInNewTask() {
		return runMode == RUN_MANUALLY_EXTRA_NEW;
	}

	public int getUpdateDelay() {
		return updateDelay;
	}

	public int getSensorDelay() {
		return sensorDelay;
	}

	public int getTextSize() {
		return textSize;
	}

	public int getTabWidth() {
		return tabWidth;
	}

	public boolean exportTabs() {
		return exportTabs;
	}

	public boolean doesShowInsertTab() {
		return showInsertTab;
	}

	public boolean doesSaveOnRun() {
		return saveOnRun;
	}

	public long getWallpaperShader() {
		return wallpaperShaderId;
	}

	public void setWallpaperShader(long id) {
		wallpaperShaderId = id;
		putString(WALLPAPER_SHADER, String.valueOf(wallpaperShaderId));
	}

	public long getDefaultNewShader() {
		return defaultNewShaderId;
	}

	public void setDefaultNewShader(long id) {
		defaultNewShaderId = id;
		putString(DEFAULT_NEW_SHADER, String.valueOf(defaultNewShaderId));
	}

	public boolean isBatteryLow() {
		return batteryLow;
	}

	public void setBatteryLow(boolean isLow) {
		batteryLow = isLow;
	}

	public boolean disableHighlighting() {
		return disableHighlighting;
	}

	public boolean autoSave() {
		return autoSave;
	}

	public int getSystemBarColor() {
		return systemBarColor;
	}

	private void putString(String key, String value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(key, value);
		editor.apply();
	}

	private static int parseInt(String s, int preset) {
		try {
			if (s != null && s.length() > 0) {
				return Integer.parseInt(s);
			}
		} catch (NumberFormatException e) {
			// use preset
		}

		return preset;
	}

	private static long parseLong(String s, long preset) {
		try {
			if (s != null && s.length() > 0) {
				return Long.parseLong(s);
			}
		} catch (NumberFormatException e) {
			// use preset
		}

		return preset;
	}

	private static int parseSensorDelay(String s, int preset) {
		if (s == null) {
			return preset;
		}

		switch (s) {
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
