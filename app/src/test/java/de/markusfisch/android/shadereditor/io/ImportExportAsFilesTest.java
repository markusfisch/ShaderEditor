package de.markusfisch.android.shadereditor.io;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ImportExportAsFilesTest {
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void preservesUtf8AcrossByteBufferBoundaries() throws IOException {
		// Split multibyte characters at both the old 1 KiB boundary
		// and the decoder's 8 KiB input buffer boundary.
		String source = "x".repeat(1023) + "ä" +
				"x".repeat(7166) + "🌍" + "\nvoid main() {}\n";
		File file = temporaryFolder.newFile("shader.glsl");
		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write(source.getBytes(StandardCharsets.UTF_8));
		}

		assertEquals(source, ImportExportAsFiles.readFile(file));
	}
}
