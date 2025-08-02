package de.markusfisch.android.shadereditor.database;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

public interface DatabaseTable {
	void onCreate(@NonNull SQLiteDatabase db);

	void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion);

	boolean onImport(@NonNull SQLiteDatabase intoDb, @NonNull SQLiteDatabase fromDb);
}