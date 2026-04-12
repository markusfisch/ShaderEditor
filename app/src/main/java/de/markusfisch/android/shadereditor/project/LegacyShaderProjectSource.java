package de.markusfisch.android.shadereditor.project;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Objects;

import de.markusfisch.android.shadereditor.database.DataRecords;

public final class LegacyShaderProjectSource implements ShaderProjectSource {
	@NonNull
	private final DataRecords.Shader shader;
	@NonNull
	private final ShaderProjectOrigin origin;
	@NonNull
	private final String title;

	public LegacyShaderProjectSource(@NonNull DataRecords.Shader shader) {
		this.shader = Objects.requireNonNull(shader);
		title = getTitle(shader);
		origin = new ShaderProjectOrigin(
				ShaderProjectOrigin.Type.LEGACY_DB,
				"shader:" + shader.id(),
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
				ShaderProjectPaths.DEFAULT_ENTRY_FILE,
				List.of(new ShaderProjectFile(
						ShaderProjectPaths.DEFAULT_ENTRY_FILE,
						shader.fragmentShader())));
		return new ShaderProjectSession(
				project,
				project.getEntryFilePath(),
				shader.quality());
	}

	@NonNull
	private static String getTitle(@NonNull DataRecords.Shader shader) {
		String title = shader.getTitle();
		if (title == null || title.trim().isEmpty()) {
			return "Shader " + shader.id();
		}
		return title;
	}
}
