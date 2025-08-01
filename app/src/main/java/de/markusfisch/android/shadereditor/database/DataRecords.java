package de.markusfisch.android.shadereditor.database;

/**
 * Contains immutable data records representing database entities.
 */
public final class DataRecords {
	/**
	 * Holds summary data for a shader, suitable for list displays.
	 */
	public record ShaderInfo(
			long id,
			String name,
			String modified,
			byte[] thumb
	) {
	}

	/**
	 * Holds the complete data for a single shader.
	 */
	public record Shader(
			long id,
			String fragmentShader,
			String name,
			String modified,
			float quality
	) {
	}

	/**
	 * Holds summary data for a texture, suitable for list displays.
	 */
	public record TextureInfo(
			long id,
			String name,
			int width,
			int height,
			byte[] thumb
	) {
	}

	/**
	 * Holds the complete data for a single texture, including the raw image data.
	 */
	public record Texture(
			long id,
			String name,
			int width,
			int height,
			float ratio,
			byte[] thumb,
			byte[] matrix // Raw PNG data for the full texture
	) {
	}
}