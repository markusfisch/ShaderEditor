package de.markusfisch.android.shadereditor.project;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;

public final class ShaderProjectSession implements Serializable {
	@NonNull
	private final ShaderProject project;
	@NonNull
	private final String activeFilePath;
	private final float quality;

	public ShaderProjectSession(@NonNull ShaderProject project,
			@NonNull String activeFilePath,
			float quality) {
		this.project = Objects.requireNonNull(project);
		this.activeFilePath = ShaderProjectPaths.normalize(activeFilePath);
		this.project.requireFile(this.activeFilePath);
		this.quality = quality;
	}

	@NonNull
	public ShaderProject getProject() {
		return project;
	}

	@NonNull
	public String getActiveFilePath() {
		return activeFilePath;
	}

	@NonNull
	public ShaderProjectFile getActiveFile() {
		return project.requireFile(activeFilePath);
	}

	@NonNull
	public ShaderProjectFile getEntryFile() {
		return project.getEntryFile();
	}

	@NonNull
	public String getEntryPointSource() {
		return getEntryFile().source();
	}

	public float getQuality() {
		return quality;
	}

	@NonNull
	public ShaderProjectSession withActiveFile(@NonNull String path) {
		return new ShaderProjectSession(project, path, quality);
	}

	@NonNull
	public ShaderProjectSession withQuality(float quality) {
		return new ShaderProjectSession(project, activeFilePath, quality);
	}

	@NonNull
	public ShaderProjectSession withEntryPointSource(@NonNull String source) {
		return new ShaderProjectSession(
				project.withFileSource(project.getEntryFilePath(), source),
				activeFilePath,
				quality);
	}

	@NonNull
	public ShaderProjectSession withFileSource(@NonNull String path,
			@NonNull String source) {
		String normalizedPath = ShaderProjectPaths.normalize(path);
		return new ShaderProjectSession(
				project.withFileSource(normalizedPath, source),
				activeFilePath.equals(normalizedPath)
						? normalizedPath
						: activeFilePath,
				quality);
	}
}
