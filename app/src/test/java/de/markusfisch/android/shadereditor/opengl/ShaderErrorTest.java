package de.markusfisch.android.shadereditor.opengl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ShaderErrorTest {
	@Test
	public void lineIsReadFromCommonFormat() {
		var error = ShaderError.parse(
				"ERROR: 0:24: 'float' : Syntax error:  syntax error");
		assertEquals(24, error.getLine());
		assertEquals("'float' : Syntax error:  syntax error",
				error.getMessage());
	}

	@Test
	public void lineIsReadFromMesaFormat() {
		var error = ShaderError.parse(
				"0:6(1): error: syntax error, unexpected '}', expecting ',' or ';'");
		assertEquals(6, error.getLine());
		assertEquals("error: syntax error, unexpected '}', expecting ',' or ';'",
				error.getMessage());
		error = ShaderError.parse(
				"0:4(1): preprocessor error: #error no colour yet");
		assertEquals(4, error.getLine());
		assertEquals("preprocessor error: #error no colour yet",
				error.getMessage());
	}
}
