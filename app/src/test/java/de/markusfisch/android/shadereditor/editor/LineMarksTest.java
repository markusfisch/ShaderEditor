package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.opengl.ShaderError;

public class LineMarksTest {
	private static final String TEXT = "a0\nb1\nc2\nd3\ne4";

	@Test
	public void marksTheLinesOfErrors() {
		Document document = new Document(Text.of(TEXT));
		LineMarks marks = new LineMarks(document);
		marks.set(List.of(error(2), error(4), error(2), error(0), error(6),
				ShaderError.createGeneral("e")));
		assertEquals("_ E _ E _", marked(marks, document));
		marks.set(List.of());
		assertEquals("_ _ _ _ _", marked(marks, document));
	}

	@Test
	public void movesWithLinesInsertedOrRemovedAbove() {
		Document document = new Document(Text.of(TEXT));
		LineMarks marks = marked(document, error(3));
		document.replace(1, 1, Text.of("\nx\n"));
		assertEquals("_ _ _ _ E _ _", marked(marks, document));
		document.replace(2, 6, Text.EMPTY);
		assertEquals("_ _ E _ _", marked(marks, document));
	}

	@Test
	public void staysWhenItsLineIsEditedOrSplit() {
		Document document = new Document(Text.of(TEXT));
		LineMarks marks = marked(document, error(2));
		document.replace(4, 4, Text.of("xy"));
		document.replace(4, 5, Text.of("\n"));
		document.replace(document.text().lineStart(2) - 1,
				document.text().lineStart(2) - 1, Text.of("\n"));
		assertEquals("_ E _ _ _ _ _", marked(marks, document));
	}

	@Test
	public void followsItsTextWhenALineBreakIsTypedAtItsStart() {
		Document document = new Document(Text.of(TEXT));
		LineMarks marks = marked(document, error(2));
		document.replace(3, 3, Text.of("\n\n"));
		assertEquals("_ _ _ E _ _ _", marked(marks, document));
	}

	@Test
	public void isDroppedWhenItsLineIsJoinedToTheOneBefore() {
		Document document = new Document(Text.of(TEXT));
		LineMarks marks = marked(document, error(3));
		document.replace(5, 6, Text.EMPTY);
		assertEquals("_ _ _ _", marked(marks, document));
		assertEquals("a0\nb1c2\nd3\ne4", document.text().toString());
	}

	@Test
	public void staysOnItsLineThroughAJoinAndItsUndo() {
		Document document = new Document(Text.of("a\nb"));
		LineMarks marks = marked(document, error(2));
		document.replace(1, 2, Text.EMPTY);
		document.undo();
		assertEquals("_ _", marked(marks, document));
	}

	@Test
	public void movesWhenAllInFrontOfItsLineIsRemoved() {
		Document document = new Document(Text.of("a\nb"));
		LineMarks marks = marked(document, error(2));
		document.replace(0, 2, Text.EMPTY);
		assertEquals("E", marked(marks, document));
		document.undo();
		assertEquals("_ E", marked(marks, document));
	}

	@Test
	public void staysOnItsLineWhenLinesAreIndented() {
		Document document = new Document(Text.of(TEXT));
		LineMarks marks = marked(document, error(2), error(3));
		document.select(0, document.text().length());
		new EditCommands(document).indent();
		assertEquals("_ E E _ _", marked(marks, document));
		document.undo();
		assertEquals("_ E E _ _", marked(marks, document));
	}

	@Test
	public void isDroppedWhenAllTextOfItsLineIsRemoved() {
		Document document = new Document(Text.of(TEXT));
		LineMarks marks = marked(document, error(2), error(3), error(5));
		// The whole second line with its line break.
		document.replace(3, 6, Text.EMPTY);
		// The text of the new second line, typed over.
		document.replace(3, 5, Text.of("x"));
		assertEquals("_ _ _ E", marked(marks, document));
	}

	@Test
	public void onAnEmptyLineIsDroppedWhenTextNextToItsStartIsRemoved() {
		Document document = new Document(Text.of("a\n\n\nb"));
		LineMarks marks = marked(document, error(2), error(3));
		document.replace(2, 2, Text.of("x"));
		assertEquals("_ E E _", marked(marks, document));
		// Backspace at the start of the empty line.
		document.replace(3, 4, Text.EMPTY);
		assertEquals("_ E _", marked(marks, document));
	}

	@Test
	public void countsChangesOfTheMarksOnly() {
		Document document = new Document(Text.of(TEXT));
		LineMarks marks = marked(document, error(3));
		int changes = marks.changes();
		document.replace(7, 7, Text.of("xy"));
		assertEquals(changes, marks.changes());
		document.replace(0, 0, Text.of("\n"));
		assertEquals(changes + 1, marks.changes());
		marks.set(List.of(error(4)));
		assertEquals(changes + 2, marks.changes());
	}

	@Test
	public void staysWhereItIsOnceClosed() {
		Document document = new Document(Text.of(TEXT));
		LineMarks marks = marked(document, error(3));
		marks.close();
		document.replace(0, 0, Text.of("x\n"));
		assertEquals("_ _ E _ _ _", marked(marks, document));
	}

	private static LineMarks marked(Document document,
			ShaderError... errors) {
		LineMarks marks = new LineMarks(document);
		marks.set(List.of(errors));
		return marks;
	}

	private static String marked(LineMarks marks, Document document) {
		List<String> lines = new ArrayList<>();
		for (int i = 0, n = document.text().lineCount(); i < n; ++i) {
			lines.add(marks.has(i) ? "E" : "_");
		}
		return String.join(" ", lines);
	}

	private static ShaderError error(int line) {
		return ShaderError.parse("ERROR: 0:" + line + ": e");
	}
}
