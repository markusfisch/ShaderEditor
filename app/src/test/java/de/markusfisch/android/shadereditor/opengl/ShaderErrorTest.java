package de.markusfisch.android.shadereditor.opengl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

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

	@Test
	public void adrenoSummaryIsDropped() {
		List<ShaderError> errors = ShaderError.parseAll(
				"ERROR: 0:27: 'iTime' : Syntax error:  syntax error\n" +
						"INTERNAL ERROR: no main() function!\n" +
						"ERROR: 1 compilation errors.  No code generated.\n\n");
		assertEquals(2, errors.size());
		assertEquals(27, errors.get(0).getLine());
		assertEquals("INTERNAL ERROR: no main() function!",
				errors.get(1).getMessage());
	}

	@Test
	public void adrenoSummaryAloneIsKept() {
		String summary = "ERROR: 2 compilation errors.  No code generated.";
		List<ShaderError> errors = ShaderError.parseAll(summary + "\n\n");
		assertEquals(1, errors.size());
		assertEquals(summary, errors.get(0).getMessage());
	}
}
