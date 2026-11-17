package de.markusfisch.android.shadereditor.highlighter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.markusfisch.android.shadereditor.syntax.GlslCompletion;
import de.markusfisch.android.shadereditor.syntax.GlslLexer;
import de.markusfisch.android.shadereditor.syntax.Style;
import de.markusfisch.android.shadereditor.syntax.Tokens;

public class LexerCompletionTest {
	@Test
	public void findTokenReturnsTheTokenContainingTheOffset() {
		Tokens tokens = GlslLexer.lex("float x;");
		assertEquals(Style.PRIMITIVE, tokens.style(tokens.indexAt(0)));
		assertEquals(Style.PRIMITIVE, tokens.style(tokens.indexAt(4)));
		assertEquals(Style.IDENTIFIER, tokens.style(tokens.indexAt(6)));
		assertEquals(Style.OPERATOR, tokens.style(tokens.indexAt(7)));
	}

	@Test
	public void findTokenReturnsNullBetweenTokensAndAtTheEnd() {
		Tokens tokens = GlslLexer.lex("float x;");
		assertEquals(-1, tokens.indexAt(5));
		assertEquals(-1, tokens.indexAt(8));
		assertEquals(-1, GlslLexer.lex("").indexAt(0));
	}

	@Test
	public void completeKeywordsByPrefix() {
		assertEquals(Arrays.asList("vec2", "vec3", "vec4"),
				complete("vec"));
		assertEquals(Arrays.asList("flat", "float", "floatBitsToInt", "floatBitsToUint",
						"floor"),
				complete("fl"));
		assertEquals(Arrays.asList("float", "floatBitsToInt", "floatBitsToUint"),
				complete("float"));
	}

	@Test
	public void completeNothing() {
		assertEquals(Collections.emptyList(), complete(""));
		assertEquals(Collections.emptyList(), complete("xyz"));
		assertEquals(Collections.emptyList(), complete("Vec"));
	}

	@Test
	public void completeDirectivesAndKeywordsInDirectives() {
		assertEquals(Arrays.asList("pragma"),
				GlslCompletion.words("prag", true));
		assertEquals(Arrays.asList("define", "default", "degrees", "determinant"),
				GlslCompletion.words("de", true));
	}

	@Test
	public void prefixInsideAWord() {
		assertEquals("fl", prefixAt("x = flo;", 6));
	}

	@Test
	public void prefixAtTheEndOfTheText() {
		assertEquals("fl", prefixAt("x = fl", 6));
	}

	@Test
	public void prefixBeforeWhitespace() {
		assertEquals("fl", prefixAt("x = fl ", 6));
	}

	@Test
	public void prefixBeforeAClosingParenthesis() {
		assertEquals("no", prefixAt("vec3(no)", 7));
	}

	@Test
	public void prefixBeforeASemicolon() {
		assertEquals("fl", prefixAt("x = fl;", 6));
	}

	@Test
	public void insideAComment() {
		assertTrue(inComment("// fl", 5));
		assertTrue(inComment("x; // fl\ny", 8));
		assertTrue(inComment("/* fl", 5));
		assertTrue(inComment("/* fl */ x", 5));
		assertTrue(inComment("/*", 2));
	}

	@Test
	public void notInsideAComment() {
		assertFalse(inComment("x = fl", 6));
		assertFalse(inComment("/* a */", 7));
		assertFalse(inComment("/* a */fl", 9));
		assertFalse(inComment("// a\nfl", 7));
	}

	@NonNull
	private static List<String> complete(@NonNull String prefix) {
		return GlslCompletion.words(prefix, false);
	}

	/**
	 * The text the editor completes: from the start of the token containing
	 * the character before the cursor to the cursor.
	 */
	@NonNull
	private static String prefixAt(@NonNull String text, int cursor) {
		Tokens tokens = GlslLexer.lex(text);
		int index = GlslCompletion.tokenBefore(tokens, cursor);
		if (index < 0) {
			return "";
		}
		return text.substring(tokens.start(index), cursor);
	}

	private static boolean inComment(@NonNull String text, int cursor) {
		Tokens tokens = GlslLexer.lex(text);
		int index = GlslCompletion.tokenBefore(tokens, cursor);
		return index > -1 &&
				GlslCompletion.isInComment(tokens, text, index, cursor);
	}
}
