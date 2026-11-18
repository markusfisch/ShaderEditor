package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.markusfisch.android.shadereditor.syntax.Indentation;

// Pinned from what ShaderEditor's auto-indent does today. Most texts
// start with a line "x" because today's rule skips the first line.
public class AutoIndentTest {
	@Test
	public void copiesTheIndentation() {
		assertEquals("\t  ", indentAt("x\n\t  a;|\ny"));
		assertEquals(" \t ", indentAt("x\n \t a;|\ny"));
		assertEquals("", indentAt("x\na;|\ny"));
	}

	@Test
	public void addsALevelAfterAnOpeningBrace() {
		assertEquals("\t\t", indentAt("x\n\tif (a) {|\n"));
		assertEquals("\t\t", indentAt("x\n\tif (a) {  |\n"));
	}

	@Test
	public void addsALevelAfterAnOperator() {
		for (char c : "+-*/%^=".toCharArray()) {
			assertEquals("\t\t", indentAt("x\n\ta " + c + "|\n"));
		}
		assertEquals("\t", indentAt("x\n\ta = b|\n"));
	}

	@Test
	public void addsOneLevelWhileParenthesesAreOpen() {
		assertEquals("\t\t", indentAt("x\n\tf(a,|\n"));
		assertEquals("\t\t", indentAt("x\n\tf(g(|\n"));
		assertEquals("\t", indentAt("x\n\tf(a);|\n"));
	}

	@Test
	public void aClosingBraceKeepsTheIndentation() {
		assertEquals("\t\t", indentAt("x\n\t\t}|\n"));
	}

	@Test
	public void onlyTheTextBeforeTheCaretCounts() {
		assertEquals("\t\t", indentAt("x\n\tf(|a)\n"));
		assertEquals("\t\t", indentAt("x\n\tif (a) {|}\n"));
		assertEquals("\t", indentAt("x\n\ta;| {\n"));
	}

	@Test
	public void copiesNoMoreIndentationThanBeforeTheCaret() {
		assertEquals("\t", indentAt("x\n\t|\tfoo\n"));
	}

	@Test
	public void continuesACommentInsideTheLine() {
		assertEquals("\t//", indentAt("x\n\t// abc| def\n"));
		assertEquals("\t", indentAt("x\n\t// abc|\n"));
	}

	@Test
	public void bugASlashContinuesAsIfItWereAComment() {
		// Today's check for "//" compares the slash with itself, so a
		// line that starts with any slash continues with its first two
		// characters.
		assertEquals("\t/ ", indentAt("x\n\t/ 2.0|;\n"));
	}

	@Test
	public void bugTheFirstLineGivesNoIndentation() {
		// Today's rule copies the indentation only of a line after a
		// line break; the added level still counts.
		assertEquals("", indentAt("\ta;|\nb"));
		assertEquals("\t", indentAt("\tvoid main() {|\n}"));
	}

	@Test
	public void indentsAtTheEndOfTheText() {
		// The old editor does not indent there at all.
		assertEquals("\t\t", indentAt("x\n\tf() {|"));
	}

	@Test
	public void aSelectionIndentsFromItsStart() {
		Text text = Text.of("x\n\tf(bar)\n");
		assertEquals("\t\t", AutoIndent.indentAfterBreak(text, 5, 8, "\t",
				Indentation.C));
	}

	@Test
	public void aLevelIsWhatTheSettingSays() {
		assertEquals("        ", AutoIndent.indentAfterBreak(
				Text.of("x\n    f(\n"), 8, 8, "    ", Indentation.C));
	}

	// Returns the indentation after a line break typed at '|'.
	private static String indentAt(String state) {
		int caret = state.indexOf('|');
		Text text = Text.of(state.replace("|", ""));
		return AutoIndent.indentAfterBreak(text, caret, caret, "\t",
				Indentation.C);
	}
}
