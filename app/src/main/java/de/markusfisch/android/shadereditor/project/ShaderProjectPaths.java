package de.markusfisch.android.shadereditor.project;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;

public final class ShaderProjectPaths {
	public static final String DEFAULT_ENTRY_FILE = "main.glsl";

	private ShaderProjectPaths() {
	}

	@NonNull
	public static String normalize(@NonNull String path) {
		String normalized = Objects.requireNonNull(path)
				.replace('\\', '/')
				.trim();

		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Path must not be empty");
		}

		String[] parts = normalized.split("/+");
		ArrayDeque<String> segments = new ArrayDeque<>(parts.length);
		for (String part : parts) {
			if (part == null || part.isEmpty() || ".".equals(part)) {
				continue;
			}
			if ("..".equals(part)) {
				if (segments.isEmpty()) {
					throw new IllegalArgumentException(
							"Path escapes project root: " + path);
				}
				segments.removeLast();
				continue;
			}
			segments.addLast(part);
		}

		if (segments.isEmpty()) {
			throw new IllegalArgumentException("Path must not be empty: " + path);
		}

		StringBuilder builder = new StringBuilder();
		Iterator<String> iterator = segments.iterator();
		while (iterator.hasNext()) {
			builder.append(iterator.next());
			if (iterator.hasNext()) {
				builder.append('/');
			}
		}
		return builder.toString();
	}

	@NonNull
	public static String getFileName(@NonNull String path) {
		String normalized = normalize(path);
		int slash = normalized.lastIndexOf('/');
		return slash >= 0 ? normalized.substring(slash + 1) : normalized;
	}
}
