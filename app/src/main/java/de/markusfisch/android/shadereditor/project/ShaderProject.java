package de.markusfisch.android.shadereditor.project;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ShaderProject implements Serializable {
	@NonNull
	private final ShaderProjectOrigin origin;
	@NonNull
	private final String title;
	@NonNull
	private final String entryFilePath;
	@NonNull
	private final List<ShaderProjectFile> files;
	@NonNull
	private final Map<String, ShaderProjectFile> filesByPath;

	public ShaderProject(@NonNull ShaderProjectOrigin origin,
			@NonNull String title,
			@NonNull String entryFilePath,
			@NonNull List<ShaderProjectFile> files) {
		this.origin = Objects.requireNonNull(origin);
		this.title = requireText(title, "title");
		this.entryFilePath = ShaderProjectPaths.normalize(entryFilePath);

		ArrayList<ShaderProjectFile> orderedFiles = new ArrayList<>(
				Objects.requireNonNull(files).size());
		LinkedHashMap<String, ShaderProjectFile> indexedFiles = new LinkedHashMap<>();
		for (ShaderProjectFile file : files) {
			ShaderProjectFile projectFile = Objects.requireNonNull(file);
			if (indexedFiles.put(projectFile.path(), projectFile) != null) {
				throw new IllegalArgumentException(
						"Duplicate project file: " + projectFile.path());
			}
			orderedFiles.add(projectFile);
		}

		if (!indexedFiles.containsKey(this.entryFilePath)) {
			throw new IllegalArgumentException(
					"Missing entry file: " + this.entryFilePath);
		}

		this.files = Collections.unmodifiableList(orderedFiles);
		this.filesByPath = Collections.unmodifiableMap(indexedFiles);
	}

	@NonNull
	public ShaderProjectOrigin getOrigin() {
		return origin;
	}

	@NonNull
	public String getTitle() {
		return title;
	}

	@NonNull
	public String getEntryFilePath() {
		return entryFilePath;
	}

	@NonNull
	public List<ShaderProjectFile> getFiles() {
		return files;
	}

	@Nullable
	public ShaderProjectFile getFile(@NonNull String path) {
		return filesByPath.get(ShaderProjectPaths.normalize(path));
	}

	public boolean hasFile(@NonNull String path) {
		return getFile(path) != null;
	}

	@NonNull
	public ShaderProjectFile requireFile(@NonNull String path) {
		ShaderProjectFile file = getFile(path);
		if (file == null) {
			throw new IllegalArgumentException(
					"Unknown project file: " + ShaderProjectPaths.normalize(path));
		}
		return file;
	}

	@NonNull
	public ShaderProjectFile getEntryFile() {
		return requireFile(entryFilePath);
	}

	@NonNull
	public ShaderProject withFileSource(@NonNull String path, @NonNull String source) {
		String normalizedPath = ShaderProjectPaths.normalize(path);
		requireFile(normalizedPath);

		ArrayList<ShaderProjectFile> updatedFiles = new ArrayList<>(files.size());
		for (ShaderProjectFile file : files) {
			updatedFiles.add(file.path().equals(normalizedPath)
					? file.withSource(source)
					: file);
		}
		return new ShaderProject(origin, title, entryFilePath, updatedFiles);
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
