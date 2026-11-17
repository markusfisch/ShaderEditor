package de.markusfisch.android.shadereditor.highlighter;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import org.junit.Test;

import de.markusfisch.android.shadereditor.syntax.GlslLexer;
import de.markusfisch.android.shadereditor.syntax.Tokens;

/**
 * After an edit the editor colours the tokens from {@code start} to
 * {@code insertEnd} of the new text again; {@link #assertDiff} also shows
 * that text. All punctuation has one style, so a changed operator alone
 * is no difference.
 */
public class TokenDiffTest {
	@Test
	public void unchangedText() {
		assertDiff("float x = 1.0;", "float x = 1.0;",
				5, 5, 5, "");
	}

	@Test
	public void replacedTokenInTheMiddle() {
		assertDiff("float x = 1.0;", "float x = y;",
				3, 4, 4, "y");
	}

	@Test
	public void insertionInTheMiddle() {
		assertDiff("float x = 1.0;", "float x = 1.0 + y;",
				5, 5, 7, "y;");
	}

	@Test
	public void insertionAtTheStart() {
		assertDiff("x;", "float x;",
				0, 0, 1, "float");
	}

	@Test
	public void deletionAtTheStart() {
		assertDiff("float x;", "x;",
				0, 1, 0, "");
	}

	@Test
	public void insertionAtTheEnd() {
		assertDiff("float x", "float x;",
				2, 2, 3, ";");
	}

	@Test
	public void deletionAtTheEnd() {
		assertDiff("float x;", "float x",
				2, 3, 2, "");
	}

	@Test
	public void insertionIntoEmptyText() {
		assertDiff("", "x",
				0, 0, 1, "x");
	}

	@Test
	public void deletionOfAllText() {
		assertDiff("x", "",
				0, 1, 0, "");
	}

	@Test
	public void insertedArgumentLikeItsNeighbours() {
		assertDiff("vec3(1.0, 1.0)", "vec3(1.0, 1.0, 1.0)",
				6, 6, 8, "1.0)");
	}

	@Test
	public void deletedArgumentLikeItsNeighbours() {
		assertDiff("vec3(1.0, 1.0, 1.0)", "vec3(1.0, 1.0)",
				6, 8, 6, "");
	}

	@Test
	public void insertedLineLikeTheLineAbove() {
		assertDiff("x = 1.0;", "x = 1.0;\ny = 2.0;",
				4, 4, 8, "y = 2.0;");
	}

	private static void assertDiff(
			@NonNull String before,
			@NonNull String after,
			int start,
			int deleteEnd,
			int insertEnd,
			@NonNull String restyled) {
		Tokens edited = GlslLexer.lex(after);
		TokenDiff diff = TokenDiff.of(GlslLexer.lex(before), edited);
		assertEquals("start", start, diff.start);
		assertEquals("deleteEnd", deleteEnd, diff.deleteEnd);
		assertEquals("insertEnd", insertEnd, diff.insertEnd);
		String actual = diff.start < diff.insertEnd
				? after.substring(edited.start(diff.start),
				edited.end(diff.insertEnd - 1))
				: "";
		assertEquals("restyled", restyled, actual);
	}
}
