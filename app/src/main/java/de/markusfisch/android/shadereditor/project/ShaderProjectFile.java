package de.markusfisch.android.shadereditor.project;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;

public record ShaderProjectFile(
		@NonNull String path,
		@NonNull String source
) implements Serializable {
	public ShaderProjectFile {
		path = ShaderProjectPaths.normalize(path);
		source = Objects.requireNonNull(source);
	}

	@NonNull
	public String getName() {
		return ShaderProjectPaths.getFileName(path);
	}

	@NonNull
	public ShaderProjectFile withSource(@NonNull String source) {
		return new ShaderProjectFile(path, source);
	}
}
