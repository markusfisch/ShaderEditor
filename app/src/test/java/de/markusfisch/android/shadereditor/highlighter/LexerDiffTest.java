package de.markusfisch.android.shadereditor.highlighter;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.List;

/**
 * After an edit the editor colours the tokens from {@code start} to
 * {@code insertEnd} of the new text again; {@link #assertDiff} also shows
 * that text.
 */
public class LexerDiffTest {
	@Test
	public void unchangedText() {
		assertDiff("float x = 1.0;", "float x = 1.0;",
				6, 5, 5, "");
	}

	@Test
	public void replacedTokenInTheMiddle() {
		assertDiff("float x = 1.0;", "float x = y;",
				3, 3, 3, "y");
	}

	@Test
	public void insertionInTheMiddle() {
		assertDiff("float x = 1.0;", "float x = 1.0 + y;",
				4, 3, 5, "+ y");
	}

	@Test
	public void insertionAtTheStart() {
		assertDiff("x;", "float x;",
				0, 0, 1, "float x");
	}

	@Test
	public void deletionAtTheStart() {
		assertDiff("float x;", "x;",
				0, 1, 0, "x");
	}

	@Test
	public void insertionAtTheEnd() {
		assertDiff("float x", "float x;",
				2, 1, 2, ";");
	}

	@Test
	public void deletionAtTheEnd() {
		assertDiff("float x;", "float x",
				2, 2, 1, "");
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
				5, 4, 6, ", 1.0");
	}

	@Test
	public void deletedArgumentLikeItsNeighbours() {
		assertDiff("vec3(1.0, 1.0, 1.0)", "vec3(1.0, 1.0)",
				5, 6, 4, "");
	}

	@Test
	public void insertedLineLikeTheLineAbove() {
		assertDiff("x = 1.0;", "x = 1.0;\ny = 2.0;",
				4, 3, 7, "y = 2.0;");
	}

	private static void assertDiff(
			@NonNull String before,
			@NonNull String after,
			int start,
			int deleteEnd,
			int insertEnd,
			@NonNull String restyled) {
		List<Token> edited = Styles.tokens(after);
		Lexer.Diff diff = Lexer.diff(Styles.tokens(before), edited);
		assertEquals("start", start, diff.start);
		assertEquals("deleteEnd", deleteEnd, diff.deleteEnd);
		assertEquals("insertEnd", insertEnd, diff.insertEnd);
		String actual = diff.start > diff.insertEnd
				? ""
				: after.substring(edited.get(diff.start).startOffset(),
				edited.get(diff.insertEnd).endOffset());
		assertEquals("restyled", restyled, actual);
	}
}
