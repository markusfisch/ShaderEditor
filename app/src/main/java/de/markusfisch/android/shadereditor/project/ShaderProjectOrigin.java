package de.markusfisch.android.shadereditor.project;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;

public record ShaderProjectOrigin(
		@NonNull Type type,
		@NonNull String id,
		@NonNull String displayName
) implements Serializable {
	public enum Type {
		FOLDER,
		LOOSE_FILE,
		LEGACY_DB
	}

	public ShaderProjectOrigin {
		type = Objects.requireNonNull(type);
		id = requireText(id, "id");
		displayName = requireText(displayName, "displayName");
	}

	@NonNull
	private static String requireText(String value, String name) {
		String text = Objects.requireNonNull(value, name).trim();
		if (text.isEmpty()) {
			throw new IllegalArgumentException(name + " must not be empty");
		}
		return text;
	}
}
