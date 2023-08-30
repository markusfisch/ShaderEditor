package de.markusfisch.android.shadereditor.util;

import androidx.annotation.NonNull;

import java.util.Iterator;

public class OneTimeIterable<T> implements Iterable<T> {
	private final @NonNull Iterator<T> iterator;
	public OneTimeIterable(@NonNull Iterator<T> iterator) {
		this.iterator = iterator;
	}

	@NonNull
	@Override
	public Iterator<T> iterator() {
		return iterator;
	}
}
