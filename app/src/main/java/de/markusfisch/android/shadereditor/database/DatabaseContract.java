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
	public interface ShaderColumns extends BaseColumns {
		String TABLE_NAME = "shaders";
		String FRAGMENT_SHADER = "shader";
		String THUMB = "thumb";
		String NAME = "name";
		String CREATED = "created";
		String MODIFIED = "modified";
		String QUALITY = "quality";
	}

	/**
	 * Defines the contents of the 'textures' table.
	 */
	public interface TextureColumns extends BaseColumns {
		String TABLE_NAME = "textures";
		String NAME = "name";
		String WIDTH = "width";
		String HEIGHT = "height";
		String RATIO = "ratio";
		String THUMB = "thumb";
		String MATRIX = "matrix";
	}
}
