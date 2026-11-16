package de.markusfisch.android.shadereditor.io;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class ExternalFileTest {
	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void anExistingFileIsKept() throws IOException {
		File dir = folder.getRoot();
		String name = "shader-editor-backup-2026-11-15.db";
		assertEquals(new File(dir, name), ExternalFile.uniqueFile(dir, name));
		folder.newFile(name);
		assertEquals(new File(dir, "shader-editor-backup-2026-11-15_1.db"),
				ExternalFile.uniqueFile(dir, name));
		folder.newFile("shader-editor-backup-2026-11-15_1.db");
		assertEquals(new File(dir, "shader-editor-backup-2026-11-15_2.db"),
				ExternalFile.uniqueFile(dir, name));
		folder.newFile("backup");
		assertEquals(new File(dir, "backup_1"),
				ExternalFile.uniqueFile(dir, "backup"));
	}
}
