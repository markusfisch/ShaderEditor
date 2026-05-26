package de.markusfisch.android.shadereditor.project;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class FolderShaderProjectSource implements ShaderProjectSource {
	@NonNull
	private final String title;
	@NonNull
	private final String entryFilePath;
	@NonNull
	private final List<ShaderProjectFile> files;
	private final float quality;
	@NonNull
	private final ShaderProjectOrigin origin;

	public FolderShaderProjectSource(@Nullable Uri rootUri,
			@Nullable String title,
			@NonNull String entryFilePath,
			@NonNull Collection<ShaderProjectFile> files,
			float quality) {
		this.title = getTitle(rootUri, title);
		this.entryFilePath = ShaderProjectPaths.normalize(entryFilePath);
		this.files = new ArrayList<>(Objects.requireNonNull(files));
		this.quality = quality;
		origin = new ShaderProjectOrigin(
				ShaderProjectOrigin.Type.FOLDER,
				rootUri != null ? rootUri.toString() : "folder:" + this.title,
				this.title);
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
				entryFilePath,
				files);
		return new ShaderProjectSession(project, project.getEntryFilePath(), quality);
	}

	@NonNull
	private static String getTitle(@Nullable Uri rootUri, @Nullable String title) {
		if (title != null && !title.trim().isEmpty()) {
			return title.trim();
		}
		String fallback = null;
		if (rootUri != null) {
			fallback = rootUri.getLastPathSegment();
			if (fallback != null) {
				int slash = fallback.lastIndexOf('/');
				if (slash >= 0 && slash + 1 < fallback.length()) {
					fallback = fallback.substring(slash + 1);
				}
				int colon = fallback.lastIndexOf(':');
				if (colon >= 0 && colon + 1 < fallback.length()) {
					fallback = fallback.substring(colon + 1);
				}
				fallback = fallback.trim();
			}
		}
		return fallback == null || fallback.isEmpty() ? "Project" : fallback;
	}
}
