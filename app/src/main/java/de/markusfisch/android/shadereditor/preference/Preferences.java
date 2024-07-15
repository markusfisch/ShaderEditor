package de.markusfisch.android.shadereditor.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

import de.markusfisch.android.shadereditor.R;

public class Preferences {
	public static final String WALLPAPER_SHADER = "shader";
	public static final String SAVE_BATTERY = "save_battery";
	public static final String SORT_BY_LAST_MODIFICATION = "sort_by_last_modification";
	public static final String RUN_MODE = "run_mode";
	public static final String UPDATE_DELAY = "update_delay";
	public static final String SENSOR_DELAY = "sensor_delay";
	public static final String TEXT_SIZE = "text_size";
	public static final String FONT = "font";
	public static final String USE_LIGATURES = "use_ligatures";
	public static final String TAB_WIDTH = "tab_width";
	public static final String SHOW_INSERT_TAB = "show_insert_tab";
	public static final String USE_TAB_FOR_INDENT = "use_tab_for_indent";
	public static final String EXPORT_TABS = "export_tabs";
	public static final String SAVE_ON_RUN = "save_on_run";
	public static final String DEFAULT_NEW_SHADER = "default_new_shader";
	public static final String DISABLE_HIGHLIGHTING = "disable_highlighting";
	public static final String AUTO_SAVE = "auto_save";
	public static final String IMPORT_FROM_DIRECTORY = "import_from_directory";
	public static final String EXPORT_TO_DIRECTORY = "export_to_directory";
	public static final String IMPORT_DATABASE = "import_database";
	public static final String EXPORT_DATABASE = "export_database";
	public static final String SHOW_LINE_NUMBERS = "show_line_numbers";
	public static final String SHOW_EXTRA_KEYS = "show_extra_keys";
	public static final String AUTO_HIDE_EXTRA_KEYS = "auto_hide_extra_keys";
	public static final String HIDE_NATIVE_SUGGESTIONS = "hide_native_suggestions";

	private static final int RUN_AUTO = 1;
	private static final int RUN_MANUALLY = 2;
	private static final int RUN_MANUALLY_EXTRA = 3;
	private static final int RUN_MANUALLY_EXTRA_NEW = 4;
	private static final Map<String, Integer> fontNameToResId = new HashMap<>();

	static {
		fontNameToResId.put("fira_code", R.font.fira_code);
		fontNameToResId.put("ibm_plex_mono", R.font.ibm_plex_mono);
		fontNameToResId.put("jetbrains_mono", R.font.jetbrains_mono);
		fontNameToResId.put("roboto_mono", R.font.roboto_mono);
		fontNameToResId.put("source_code_pro", R.font.source_code_pro);
	}

	private SharedPreferences preferences;
	private long wallpaperShaderId = 1;
	private boolean saveBattery = true;
	private boolean sortByLastModification = true;
	private int runMode = RUN_AUTO;
	private int updateDelay = 1000;
	private int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
	private int textSize = 12;
	private @NonNull Typeface font = Typeface.MONOSPACE;
	private boolean useLigatures = false;
	private int tabWidth = 4;
	private boolean exportTabs = false;
	private boolean showInsertTab = true;
	private boolean useTabForIndent = true;
	private boolean saveOnRun = true;
	private boolean batteryLow = false;
	private boolean powerConnected = false;
	private int systemBarColor;
	private long defaultNewShaderId = 0;
	private boolean disableHighlighting = false;
	private boolean autoSave = true;
	private boolean showLineNumbers = true;
	private boolean showExtraKeys = true;
	private boolean autoHideExtraKeys = true;
	private boolean hideNativeSuggestions = true;
	private String defaultFont;

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

		defaultFont = context.getString(R.string.default_font_value);

		update(context);
	}

	public SharedPreferences getSharedPreferences() {
		return preferences;
	}

	public void update(Context context) {
		wallpaperShaderId = parseLong(
				preferences.getString(WALLPAPER_SHADER, null),
				wallpaperShaderId);
		saveBattery = preferences.getBoolean(
				SAVE_BATTERY,
				saveBattery);
		sortByLastModification = preferences.getBoolean(
				SORT_BY_LAST_MODIFICATION,
				sortByLastModification);
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
		font = loadFont(context, preferences.getString(FONT, defaultFont));
		useLigatures = preferences.getBoolean(
				USE_LIGATURES,
				useLigatures);
		tabWidth = parseInt(
				preferences.getString(TAB_WIDTH, null),
				tabWidth);
		exportTabs = preferences.getBoolean(
				EXPORT_TABS,
				exportTabs);
		showInsertTab = preferences.getBoolean(
				SHOW_INSERT_TAB,
				showInsertTab);
		useTabForIndent = preferences.getBoolean(
				USE_TAB_FOR_INDENT,
				useTabForIndent);
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
		showLineNumbers = preferences.getBoolean(
				SHOW_LINE_NUMBERS,
				showLineNumbers);
		showExtraKeys = preferences.getBoolean(
				SHOW_EXTRA_KEYS,
				showExtraKeys);
		autoHideExtraKeys = preferences.getBoolean(
				AUTO_HIDE_EXTRA_KEYS,
				autoHideExtraKeys);
		hideNativeSuggestions = preferences.getBoolean(
				HIDE_NATIVE_SUGGESTIONS,
				hideNativeSuggestions);
	}

	public boolean autoHideExtraKeys() {
		return autoHideExtraKeys;
	}

	public boolean hideNativeSuggestions() {
		return hideNativeSuggestions;
	}

	private @NonNull Typeface loadFont(
			@NonNull Context context,
			@NonNull String fontName) {
		if (fontName.equals("monospace")) {
			return Typeface.MONOSPACE;
		}
		Integer resId = fontNameToResId.get(fontName);
		if (resId == null) {
			throw new IllegalArgumentException(
					"font \"" + fontName + "\" not found!");
		}
		Typeface tf = ResourcesCompat.getFont(context, resId);
		if (tf == null) {
			throw new IllegalArgumentException(
					"font \"" + fontName + "\" could not be loaded!");
		}
		return tf;
	}

	public boolean saveBattery() {
		return saveBattery;
	}

	public boolean sortByLastModification() {
		return sortByLastModification;
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

	public Typeface getFont() {
		return font;
	}

	public boolean useLigatures() {
		return useLigatures;
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

	public boolean useTabForIndent() {
		return useTabForIndent;
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

	public boolean isPowerConnected() {
		return powerConnected;
	}

	public void setPowerConnected(boolean isConnected) {
		powerConnected = isConnected;
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

	public boolean showLineNumbers() {
		return showLineNumbers;
	}

	public boolean showExtraKeys() {
		return showExtraKeys;
	}

	public boolean toggleShowExtraKeys() {
		showExtraKeys = !showExtraKeys;
		preferences.edit().putBoolean(SHOW_EXTRA_KEYS, this.showExtraKeys).apply();
		return showExtraKeys;
	}

	private void putString(String key, String value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(key, value);
		editor.apply();
	}

	private static int parseInt(String s, int preset) {
		try {
			if (s != null && !s.isEmpty()) {
				return Integer.parseInt(s);
			}
		} catch (NumberFormatException e) {
			// Use preset.
		}

		return preset;
	}

	private static long parseLong(String s, long preset) {
		try {
			if (s != null && !s.isEmpty()) {
				return Long.parseLong(s);
			}
		} catch (NumberFormatException e) {
			// Use preset.
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
