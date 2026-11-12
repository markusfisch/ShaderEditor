package de.markusfisch.android.shadereditor.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShaderToySourceTest {
	@Test
	public void shaderToyEntryPoint() {
		assertTrue(ShaderToySource.needsConversion(
				"void mainImage(out vec4 fragColor, in vec2 fragCoord) {\n" +
						"\tfragColor = vec4(1.0);\n" +
						"}\n"));
		assertTrue(ShaderToySource.needsConversion(
				"float f;\nvoid  mainImage (out vec4 c, in vec2 p) {}"));
	}

	@Test
	public void mainEntryPoint() {
		assertFalse(ShaderToySource.needsConversion(
				"void main() {\n\tgl_FragColor = vec4(1.0);\n}\n"));
	}

	@Test
	public void bothEntryPoints() {
		assertFalse(ShaderToySource.needsConversion(
				"void mainImage(out vec4 c, in vec2 p) {}\n" +
						"void main() {\n\tmainImage(gl_FragColor, gl_FragCoord.xy);\n}\n"));
	}

	@Test
	public void mainImageWithoutDefinition() {
		assertFalse(ShaderToySource.needsConversion("float mainImage;"));
		assertFalse(ShaderToySource.needsConversion(""));
	}

	@Test(timeout = 1000)
	public void longLineWithoutEntryPoint() {
		assertFalse(ShaderToySource.needsConversion(
				"float x = " + "1.0 + ".repeat(10000) + "1.0;"));
	}

	@Test(timeout = 1000)
	public void longLineWithEntryPoint() {
		assertTrue(ShaderToySource.needsConversion(
				"void mainImage(out vec4 c, in vec2 p) { c = vec4(" +
						"1.0 + ".repeat(10000) + "1.0); }"));
	}
}
