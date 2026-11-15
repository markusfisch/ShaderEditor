package de.markusfisch.android.shadereditor.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

public class ShaderSourceReaderTest {
	@Test
	public void readsEmptySource() throws IOException {
		assertEquals("", ShaderSourceReader.read(new StringReader("")));
	}

	@Test
	public void preservesSourceAcrossBufferBoundaries() throws IOException {
		String source = "// äöü\n".repeat(1000);
		assertEquals(source, ShaderSourceReader.read(new StringReader(source)));
	}

	@Test
	public void acceptsSourceAtLimit() throws IOException {
		String source = "x".repeat(ShaderSourceReader.MAX_SOURCE_LENGTH);
		assertEquals(source, ShaderSourceReader.read(new StringReader(source)));
	}

	@Test
	public void rejectsSourceAboveLimit() {
		String source = "x".repeat(ShaderSourceReader.MAX_SOURCE_LENGTH + 1);
		assertThrows(IOException.class,
				() -> ShaderSourceReader.read(new StringReader(source)));
	}

	@Test
	public void stopsReadingAnUnboundedStreamAtLimit() {
		Reader reader = new Reader() {
			private int total;

			@Override
			public int read(char[] buffer, int offset, int length) {
				total += length;
				if (total > ShaderSourceReader.MAX_SOURCE_LENGTH + 1) {
					throw new AssertionError("Read past the size limit");
				}
				Arrays.fill(buffer, offset, offset + length, 'x');
				return length;
			}

			@Override
			public void close() {
			}
		};
		assertThrows(IOException.class, () -> ShaderSourceReader.read(reader));
	}

	@Test
	public void honorsInterruption() {
		Thread.currentThread().interrupt();
		try {
			assertThrows(InterruptedIOException.class,
					() -> ShaderSourceReader.read(new StringReader("shader")));
		} finally {
			Thread.interrupted();
		}
	}
}
