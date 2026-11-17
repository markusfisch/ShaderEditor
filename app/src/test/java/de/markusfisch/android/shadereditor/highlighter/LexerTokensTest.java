package de.markusfisch.android.shadereditor.highlighter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import de.markusfisch.android.shadereditor.syntax.GlslLexer;
import de.markusfisch.android.shadereditor.syntax.Style;
import de.markusfisch.android.shadereditor.syntax.Tokens;

public class LexerTokensTest {
	@Test
	public void bundledShadersHaveOrderedTokensInsideTheText() throws IOException {
		File[] shaders = findRawDirectory().listFiles((dir, name) -> name.endsWith(".glsl"));
		assertTrue(shaders != null && shaders.length > 0);
		Arrays.sort(shaders);
		for (File shader : shaders) {
			String source = new String(Files.readAllBytes(shader.toPath()),
					StandardCharsets.UTF_8);
			int previousEnd = 0;
			Tokens tokens = GlslLexer.lex(source);
			for (int i = 0; i < tokens.size(); ++i) {
				String where = shader.getName() + ": " + i;
				assertTrue(where, tokens.start(i) >= previousEnd);
				assertTrue(where, tokens.end(i) > tokens.start(i));
				assertTrue(where, tokens.end(i) <= source.length());
				previousEnd = tokens.end(i);
			}
		}
	}

	@Test
	public void emptyTextHasNoTokens() {
		assertEquals(0, GlslLexer.lex("").size());
	}

	@Test
	public void hexadecimalNumbersTakeTheUnsignedSuffix() {
		for (String number : new String[]{"0x7fu", "0X7FU", "7u", "0u", "0x7f"}) {
			Tokens tokens = GlslLexer.lex(number);
			assertEquals(number, 1, tokens.size());
			assertEquals(number, Style.NUMBER, tokens.style(0));
			assertEquals(number, number.length(), tokens.end(0));
		}
	}

	private static File findRawDirectory() {
		String relative = "src/main/res/raw";
		for (String base : new String[]{".", "app"}) {
			File dir = new File(base, relative);
			if (dir.isDirectory()) {
				return dir;
			}
		}
		throw new AssertionError("Cannot find " + relative + " from working directory " +
				new File(".").getAbsolutePath());
	}
}
