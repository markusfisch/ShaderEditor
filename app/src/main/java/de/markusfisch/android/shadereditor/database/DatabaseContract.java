package de.markusfisch.android.shadereditor.database;

import android.provider.BaseColumns;

/**
 * Defines the database schema, including table and column names.
 * Using a contract class ensures that constants are defined in one place,
 * preventing typos and making schema changes easier to manage.
 */
public final class DatabaseContract {
	public static final String FILE_NAME = "shaders.db";

	// To prevent someone from accidentally instantiating the contract class,
	// make the constructor private.
	private DatabaseContract() {
	}

	/**
	 * Defines the contents of the 'shaders' table.
	 * Implementing BaseColumns adds the standard _ID and _COUNT columns.
	 */
	public static class ShaderColumns implements BaseColumns {
		public static final String TABLE_NAME = "shaders";
		public static final String FRAGMENT_SHADER = "shader";
		public static final String THUMB = "thumb";
		public static final String NAME = "name";
		public static final String CREATED = "created";
		public static final String MODIFIED = "modified";
		public static final String QUALITY = "quality";
	}

	/**
	 * Defines the contents of the 'textures' table.
	 */
	public static class TextureColumns implements BaseColumns {
		public static final String TABLE_NAME = "textures";
		public static final String NAME = "name";
		public static final String WIDTH = "width";
		public static final String HEIGHT = "height";
		public static final String RATIO = "ratio";
		public static final String THUMB = "thumb";
		public static final String MATRIX = "matrix";
	}
}
