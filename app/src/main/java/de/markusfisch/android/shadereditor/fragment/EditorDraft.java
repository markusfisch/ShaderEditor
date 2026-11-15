package de.markusfisch.android.shadereditor.fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class EditorDraft {
	private final File file;

	EditorDraft(@NonNull File dir) {
		file = new File(dir, "editor_draft");
	}

	void write(@NonNull String key, @NonNull String text) {
		File tmp = new File(file.getPath() + ".tmp");
		try (var out = new FileOutputStream(tmp)) {
			out.write((key + "\n" + text).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			return;
		}
		tmp.renameTo(file);
	}

	@Nullable
	String read(@NonNull String key) {
		try (var in = new DataInputStream(new FileInputStream(file))) {
			byte[] bytes = new byte[(int) file.length()];
			in.readFully(bytes);
			String draft = new String(bytes, StandardCharsets.UTF_8);
			int newline = draft.indexOf('\n');
			return newline > -1 && draft.substring(0, newline).equals(key)
					? draft.substring(newline + 1)
					: null;
		} catch (IOException e) {
			return null;
		}
	}

	void delete() {
		file.delete();
	}
}
