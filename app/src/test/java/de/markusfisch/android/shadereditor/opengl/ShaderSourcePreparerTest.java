package de.markusfisch.android.shadereditor.opengl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ShaderSourcePreparerTest {
	@Test
	public void invalidPeriodsUseDefault() {
		for (String period : new String[]{".", "1.2.3", "0", "0.0",
				"9".repeat(100), "0." + "0".repeat(100) + "1"}) {
			assertEquals(period, 3f, preparePeriod(period), 0f);
		}
	}

	@Test
	public void validPeriodsArePreserved() {
		assertEquals(1.5f, preparePeriod("1.5"), 0f);
		assertEquals(0.25f, preparePeriod(".25"), 0f);
		assertEquals(12f, preparePeriod("12"), 0f);
	}

	@Test
	public void injectedDirectivesFollowVersionAfterComments() {
		for (String header : new String[]{"", "\n\n", "// License\n",
				"/* License\n * text */\n", "\n// #version 100\n/* header */\n"}) {
			String preamble = header + "#version 300 es\n";
			String body = "precision highp float;\nout vec4 color;\n" +
					"void main() { color = vec4(1.); }\n";
			var prepared = ShaderSourcePreparer.prepare(preamble + body, 3, 32);
			assertEquals(preamble + "#define SHADER_EDITOR 1\n" + body,
					prepared.getFragmentShader().getSource());
			assertEquals(header.split("\n", -1).length + 2,
					prepared.getFragmentShader().getLineMapping().toSourceLine(
							header.split("\n", -1).length + 3));
		}
	}

	private static float preparePeriod(String period) {
		return ShaderSourcePreparer.prepare(
				"#define FTIME_PERIOD " + period + "\n", 2, 32).getFTimeMax();
	}
}
