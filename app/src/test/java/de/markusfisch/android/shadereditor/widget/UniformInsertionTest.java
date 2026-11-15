package de.markusfisch.android.shadereditor.widget;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UniformInsertionTest {
	@Test
	public void insertionFollowsVersionExtensionsAndPrecision() {
		String preamble = "#version 300 es\n" +
				"#extension GL_OES_EGL_image_external_essl3 : require\n" +
				"precision highp float;\n\n";
		assertEquals(preamble.length(), ShaderEditor.endIndexOfPreamble(
				preamble + "out vec4 color;\nvoid main() {}\n"));
	}

	@Test
	public void commentsAndWhitespaceStayInThePreamble() {
		String preamble = "// Header\n\n/* License\n * text */\n" +
				"#version 300 es\r\nprecision mediump float; // precision\r\n";
		assertEquals(preamble.length(), ShaderEditor.endIndexOfPreamble(
				preamble + "void main() {}\n"));
	}

	@Test
	public void continuedMacrosStayTogether() {
		for (String newline : new String[]{"\n", "\r\n"}) {
			String preamble = "#version 300 es" + newline +
					"#define VALUE \\" + newline +
					"    (1.0 + \\" + newline +
					"    2.0)" + newline +
					"precision mediump float;" + newline;
			assertEquals(preamble.length(), ShaderEditor.endIndexOfPreamble(
					preamble + "void main() {}" + newline));
		}
	}

	@Test
	public void longMacroLinesStayInThePreamble() {
		String preamble = "#define TEXT " + "x".repeat(16384) + "\n";
		assertEquals(preamble.length(), ShaderEditor.endIndexOfPreamble(
				preamble + "void main() {}\n"));
	}

	@Test
	public void manyMacroContinuationsDoNotOverflowTheStack() {
		String preamble = "#define VALUE (0.0 \\\n" +
				"    + 1.0 \\\n".repeat(5000) + ")\n";
		assertEquals(preamble.length(), ShaderEditor.endIndexOfPreamble(
				preamble + "void main() {}\n"));
	}

	@Test
	public void insertionStopsBeforeFunctionLocalPrecision() {
		assertEquals(0, ShaderEditor.endIndexOfPreamble(
				"void main() {\nprecision lowp float;\n}\n"));
	}

	@Test
	public void versionWithoutTrailingNewlineIsPreserved() {
		String source = "#version 300 es";
		assertEquals(source.length(), ShaderEditor.endIndexOfPreamble(source));
	}
	@Test
	public void insertionAfterEndifStaysWithinTheSource() {
		for (String suffix : new String[]{"", "\n", "\r\n"}) {
			String source = "#if 1\n#endif" + suffix;
			assertEquals(source.length(), ShaderEditor.endIndexOfLastEndIf(source));
		}
		assertEquals(-1, ShaderEditor.endIndexOfLastEndIf("void main() {}"));
	}

}
