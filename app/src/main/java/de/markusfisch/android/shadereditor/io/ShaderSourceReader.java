package de.markusfisch.android.shadereditor.io;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;

public final class ShaderSourceReader {
	public static final int MAX_SOURCE_LENGTH = 256 * 1024;

	private ShaderSourceReader() {
	}

	public static String read(Reader reader) throws IOException {
		char[] buffer = new char[4096];
		StringBuilder source = new StringBuilder();
		while (true) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedIOException();
			}
			int len = reader.read(buffer, 0,
					Math.min(buffer.length, MAX_SOURCE_LENGTH - source.length() + 1));
			if (len == -1) {
				return source.toString();
			}
			if (len > MAX_SOURCE_LENGTH - source.length()) {
				throw new IOException("Shader source is too large");
			}
			source.append(buffer, 0, len);
		}
	}
}
