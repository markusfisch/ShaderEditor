package de.markusfisch.android.shadereditor.database;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

import java.util.List;

import de.markusfisch.android.shadereditor.database.dao.ShaderDao;
import de.markusfisch.android.shadereditor.database.dao.TextureDao;

public class DataSource {

	public final ShaderDao shader;
	public final TextureDao texture;

	@NonNull
	@Contract("_ -> new")
	public static List<DatabaseTable> buildSchema(@NonNull Context context) {
		return List.of(ShaderDao.buildSchema(context), TextureDao.buildSchema(context));
	}

	public DataSource(SQLiteOpenHelper dbHelper, @NonNull Context context) {
		this.shader = new ShaderDao(dbHelper, context);
		this.texture = new TextureDao(dbHelper, context);
	}
}