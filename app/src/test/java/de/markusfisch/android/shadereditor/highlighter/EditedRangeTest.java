package de.markusfisch.android.shadereditor.highlighter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import de.markusfisch.android.shadereditor.syntax.GlslLexer;
import de.markusfisch.android.shadereditor.syntax.Tokens;

public class EditedRangeTest {
	@Test
	public void nothingEdited() {
		EditedRange range = new EditedRange();
		assertTrue(range.isEmpty());
		assertEquals("", restyled(range, "float x;", "float x;"));
	}

	@Test
	public void typing() {
		EditedRange range = new EditedRange();
		range.replace(4, 0, 1);
		range.replace(5, 0, 1);
		assertRange(4, 6, range);
	}

	@Test
	public void deleting() {
		EditedRange range = new EditedRange();
		range.replace(5, 1, 0);
		range.replace(4, 1, 0);
		assertRange(4, 4, range);
	}

	@Test
	public void editAfterTheRangeExtendsIt() {
		EditedRange range = new EditedRange();
		range.replace(2, 0, 3);
		range.replace(10, 2, 1);
		assertRange(2, 11, range);
	}

	@Test
	public void editBeforeTheRangeShiftsItsEnd() {
		EditedRange range = new EditedRange();
		range.replace(10, 0, 2);
		range.replace(2, 3, 1);
		assertRange(2, 10, range);
	}

	@Test
	public void editInsideTheRange() {
		EditedRange range = new EditedRange();
		range.replace(2, 0, 6);
		range.replace(4, 2, 0);
		assertRange(2, 6, range);
	}

	@Test
	public void deletionOverTheEndOfTheRange() {
		EditedRange range = new EditedRange();
		range.replace(2, 0, 4);
		range.replace(4, 6, 1);
		assertRange(2, 5, range);
	}

	@Test
	public void resetEmptiesTheRange() {
		EditedRange range = new EditedRange();
		range.replace(2, 0, 4);
		range.reset();
		assertTrue(range.isEmpty());
	}

	@Test
	public void deleteAndRetypeTheEndOfAWord() {
		// vec4, delete "ec4" with backspace, type "ec4" again before the
		// colours update. The tokens before and after are the same.
		EditedRange range = new EditedRange();
		range.replace(3, 1, 0);
		range.replace(2, 1, 0);
		range.replace(1, 1, 0);
		range.replace(1, 0, 1);
		range.replace(2, 0, 1);
		range.replace(3, 0, 1);
		assertEquals("", restyled(new EditedRange(), "vec4 v;", "vec4 v;"));
		assertEquals("vec4", restyled(range, "vec4 v;", "vec4 v;"));
	}

	@Test
	public void deleteAllAndPasteItAgain() {
		String text = "void main() {\n\tgl_FragColor = vec4(1.0);\n}";
		EditedRange range = new EditedRange();
		range.replace(0, text.length(), 0);
		range.replace(0, 0, text.length());
		assertEquals("", restyled(new EditedRange(), text, text));
		assertEquals(text, restyled(range, text, text));
	}

	@Test
	public void unionWithChangedTokens() {
		// Typing "/*" changes every token after it.
		EditedRange range = new EditedRange();
		range.replace(0, 0, 2);
		assertEquals("/* float x; float y;",
				restyled(range, " float x; float y;", "/* float x; float y;"));
	}

	@Test
	public void widenedToTouchingTokens() {
		EditedRange range = new EditedRange();
		range.replace(1, 0, 0);
		assertEquals("vec4", restyled(range, "vec4 v;", "vec4 v;"));
		range.reset();
		range.replace(7, 1, 0);
		assertEquals(";", restyled(range, "vec4 v;;", "vec4 v;"));
	}

	@Test
	public void insertedArgumentLikeItsNeighbours() {
		EditedRange range = new EditedRange();
		range.replace(13, 0, 5);
		assertEquals("1.0, 1.0)",
				restyled(range, "vec3(1.0, 1.0)", "vec3(1.0, 1.0, 1.0)"));
	}

	private static void assertRange(int start, int end, @NonNull EditedRange range) {
		assertEquals("start", start, range.start());
		assertEquals("end", end, range.end());
	}

	/**
	 * The text whose colours the editor clears, followed by the tokens it
	 * colours again, when they differ.
	 */
	@NonNull
	private static String restyled(
			@NonNull EditedRange range,
			@NonNull String before,
			@NonNull String after) {
		Tokens tokens = GlslLexer.lex(after);
		EditedRange.Restyle restyle = range.restyle(tokens,
				TokenDiff.of(GlslLexer.lex(before), tokens));
		String cleared = after.substring(restyle.start, restyle.end);
		String coloured = restyle.firstToken < restyle.endToken
				? after.substring(tokens.start(restyle.firstToken),
				tokens.end(restyle.endToken - 1))
				: "";
		return cleared.equals(coloured) ? cleared : cleared + " | " + coloured;
	}
}
