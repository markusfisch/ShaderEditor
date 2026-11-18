package de.markusfisch.android.shadereditor.syntax;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

// The shaders in res/raw, by file name.
public final class BundledShaders {
	private BundledShaders() {
	}

	public static Map<String, String> read() throws IOException {
		File[] files = findRawDirectory().listFiles((dir, name) ->
				name.endsWith(".glsl"));
		if (files == null || files.length == 0) {
			throw new AssertionError("No bundled shaders");
		}
		Map<String, String> shaders = new TreeMap<>();
		for (File file : files) {
			shaders.put(file.getName(), new String(
					Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
		}
		return shaders;
	}

	private static File findRawDirectory() {
		String relative = "src/main/res/raw";
		for (String base : new String[]{".", "app"}) {
			File dir = new File(base, relative);
			if (dir.isDirectory()) {
				return dir;
			}
		}
		throw new AssertionError("Cannot find " + relative + " from " +
				new File(".").getAbsolutePath());
	}
}
