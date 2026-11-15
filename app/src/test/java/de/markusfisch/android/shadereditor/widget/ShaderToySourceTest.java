package de.markusfisch.android.shadereditor.widget;

import static org.junit.Assert.assertEquals;
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

	@Test
	public void inputsWithAnEqualUniformAreRenamed() {
		assertEquals("resolution time time frame mouse date",
				ShaderToySource.rename(
						"iResolution iTime iGlobalTime iFrame iMouse iDate"));
	}

	@Test
	public void onlyWholeNamesAreRenamed() {
		assertEquals("iTimeDelta iChannel0 iSampleRate myiTime iFrameRate",
				ShaderToySource.rename(
						"iTimeDelta iChannel0 iSampleRate myiTime iFrameRate"));
	}

	@Test
	public void usedInputsAreDeclared() {
		assertEquals("uniform vec2 resolution;\n" +
						"uniform float time;\n" +
						"uniform int frame;\n\n",
				ShaderToySource.declarations(
						"void mainImage(out vec4 c, in vec2 p) {\n" +
								"\tc = vec4(p / iResolution.xy, " +
								"sin(iTime) + float(iFrame), 1.0);\n}\n"));
		assertEquals("uniform float time;\n\n",
				ShaderToySource.declarations("iTime + iGlobalTime"));
		assertEquals("", ShaderToySource.declarations("myiTime iTimeDelta"));
	}

	@Test
	public void ownFrameAndTimeAreNotDeclared() {
		String declarations = ShaderToySource.declarations(
				"float time = 2.0;\n" +
						"float frame(vec2 p) { return step(0.9, fract(p.x * 8.0)); }\n" +
						"void mainImage(out vec4 c, in vec2 p) {\n" +
						"\tc = vec4(frame(p / iResolution.xy), time, 0.0, 1.0);\n}\n");
		assertEquals("uniform vec2 resolution;\n\n", declarations);
	}

	// #122: a converted shader must not be wrapped again when its
	// main() is cut, and pasting a ShaderToy shader still converts it.
	@Test
	public void onlyAnInsertedMainImageNeedsConversion() {
		String toy = "void mainImage(out vec4 c, in vec2 p) {\n" +
				"\tc = vec4(1.0);\n}\n";
		assertTrue(ShaderToySource.needsConversion(toy, toy));
		String converted = "precision mediump float;\n\n" + toy +
				"\n\nvoid main() {\n" +
				"\tvec4 fragment_color;\n" +
				"\tmainImage(fragment_color, gl_FragCoord.xy);\n" +
				"\tgl_FragColor = fragment_color;\n" +
				"}\n";
		String cut = converted.substring(0, converted.indexOf("void main()"));
		// The text alone looks like a ShaderToy shader again.
		assertTrue(ShaderToySource.needsConversion(cut));
		assertFalse(ShaderToySource.needsConversion(cut, ""));
		assertFalse(ShaderToySource.needsConversion(cut + "x", "x"));
		assertFalse(ShaderToySource.needsConversion(converted, converted));
	}
}
