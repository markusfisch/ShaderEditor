package de.markusfisch.android.shadereditor.project;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public final class LooseShaderFileProjectSource implements ShaderProjectSource {
	@NonNull
	private final String filePath;
	@NonNull
	private final String source;
	private final float quality;
	@NonNull
	private final ShaderProjectOrigin origin;
	@NonNull
	private final String title;

	public LooseShaderFileProjectSource(@Nullable Uri fileUri,
			@NonNull String source,
			float quality) {
		this(fileUri, getDefaultFilePath(fileUri), source, quality);
	}

	public LooseShaderFileProjectSource(@Nullable Uri fileUri,
			@NonNull String filePath,
			@NonNull String source,
			float quality) {
		this.filePath = ShaderProjectPaths.normalize(filePath);
		this.source = Objects.requireNonNull(source);
		this.quality = quality;
		title = getDisplayName(fileUri, this.filePath);
		origin = new ShaderProjectOrigin(
				ShaderProjectOrigin.Type.LOOSE_FILE,
				fileUri != null ? fileUri.toString() : "scratch:" + this.filePath,
				title);
	}

	@NonNull
	@Override
	public ShaderProjectOrigin getOrigin() {
		return origin;
	}

	@NonNull
	@Override
	public ShaderProjectSession openSession() {
		ShaderProject project = new ShaderProject(
				origin,
				title,
				filePath,
				List.of(new ShaderProjectFile(filePath, source)));
		return new ShaderProjectSession(project, project.getEntryFilePath(), quality);
	}

	@NonNull
	private static String getDefaultFilePath(@Nullable Uri fileUri) {
		String displayName = getDisplayName(fileUri,
				ShaderProjectPaths.DEFAULT_ENTRY_FILE);
		return displayName.isEmpty()
				? ShaderProjectPaths.DEFAULT_ENTRY_FILE
				: displayName;
	}

	@NonNull
	private static String getDisplayName(@Nullable Uri fileUri,
			@NonNull String fallback) {
		String name = null;
		if (fileUri != null) {
			name = fileUri.getLastPathSegment();
			if (name != null) {
				int slash = name.lastIndexOf('/');
				if (slash >= 0 && slash + 1 < name.length()) {
					name = name.substring(slash + 1);
				}
				int colon = name.lastIndexOf(':');
				if (colon >= 0 && colon + 1 < name.length()) {
					name = name.substring(colon + 1);
				}
				name = name.trim();
			}
		}
		return name == null || name.isEmpty() ? fallback : name;
	}
}
