package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.markusfisch.android.shadereditor.editor.Document.Selection;

public class EditCommandsTest {
	@Test
	public void moveCollapsesASelectionToItsEnds() {
		Document document = documentOf("a^bc|d");
		new EditCommands(document).moveBackward(false);
		assertEquals("a|bcd", stateOf(document));
		document = documentOf("a|bc^d");
		new EditCommands(document).moveForward(false);
		assertEquals("abc|d", stateOf(document));
	}

	@Test
	public void extendKeepsTheAnchor() {
		Document document = documentOf("a^bc|d");
		EditCommands commands = new EditCommands(document);
		commands.moveForward(true);
		assertEquals("a^bcd|", stateOf(document));
		commands.moveWordBackward(true);
		assertEquals("|a^bcd", stateOf(document));
	}

	@Test
	public void movesByCharAndWord() {
		Document document = documentOf("|ab cd");
		EditCommands commands = new EditCommands(document);
		commands.moveForward(false);
		assertEquals("a|b cd", stateOf(document));
		commands.moveWordForward(false);
		assertEquals("ab| cd", stateOf(document));
		commands.moveWordForward(false);
		assertEquals("ab cd|", stateOf(document));
		commands.moveBackward(false);
		assertEquals("ab c|d", stateOf(document));
	}

	@Test
	public void verticalMovesKeepTheWantedColumn() {
		Document document = documentOf("abcde|f\nab\nabcdefgh");
		EditCommands commands = new EditCommands(document);
		commands.moveLines(1, false);
		assertEquals("abcdef\nab|\nabcdefgh", stateOf(document));
		commands.moveLines(1, false);
		assertEquals("abcdef\nab\nabcde|fgh", stateOf(document));
	}

	@Test
	public void anotherMoveForgetsTheWantedColumn() {
		Document document = documentOf("abcde|f\nab\nabcdefgh");
		EditCommands commands = new EditCommands(document);
		commands.moveLines(1, false);
		commands.moveBackward(false);
		commands.moveLines(1, false);
		assertEquals("abcdef\nab\na|bcdefgh", stateOf(document));
	}

	@Test
	public void movingAwayAndBackForgetsTheWantedColumn() {
		Document document = documentOf("abcd|\nx\nabcdef");
		EditCommands commands = new EditCommands(document);
		commands.moveLines(1, false);
		commands.moveBackward(false);
		commands.moveForward(false);
		commands.moveLines(1, false);
		assertEquals("abcd\nx\na|bcdef", stateOf(document));
	}

	@Test
	public void selectingAwayAndBackForgetsTheWantedColumn() {
		Document document = documentOf("abcd|\nx\nabcdef");
		EditCommands commands = new EditCommands(document);
		commands.moveLines(1, false);
		document.select(0, 0);
		document.select(6, 6);
		commands.moveLines(1, false);
		assertEquals("abcd\nx\na|bcdef", stateOf(document));
	}

	@Test
	public void anotherTabWidthForgetsTheWantedColumn() {
		Document document = documentOf("\tx|\n\tabcdefgh\nabcdefgh");
		EditCommands commands = new EditCommands(document);
		commands.moveLines(1, false);
		commands.setTabWidth(2);
		commands.moveLines(1, false);
		assertEquals("\tx\n\tabcdefgh\nabc|defgh", stateOf(document));
	}

	@Test
	public void anEditForgetsTheWantedColumn() {
		Document document = documentOf("abcde|f\nab\nabcdefgh");
		EditCommands commands = new EditCommands(document);
		commands.moveLines(1, false);
		document.replace(10, 10, Text.of("x"));
		commands.moveLines(1, false);
		assertEquals("abcdef\nab\nxa|bcdefgh", stateOf(document));
	}

	@Test
	public void verticalMovesMeasureTabStops() {
		Document document = documentOf("\t|x\nabcdef");
		EditCommands commands = new EditCommands(document);
		commands.moveLines(1, false);
		assertEquals("\tx\nabcd|ef", stateOf(document));
		commands.setTabWidth(2);
		commands.moveToStart(false);
		commands.moveForward(false);
		commands.moveLines(1, true);
		assertEquals("\t^x\nab|cdef", stateOf(document));
	}

	@Test
	public void verticalMovesStopAtTheEnds() {
		Document document = documentOf("ab|c\nabc");
		EditCommands commands = new EditCommands(document);
		commands.moveLines(-1, false);
		assertEquals("|abc\nabc", stateOf(document));
		commands.moveLines(5, true);
		assertEquals("^abc\nabc|", stateOf(document));
		commands.moveLines(-1, false);
		assertEquals("ab|c\nabc", stateOf(document));
	}

	@Test
	public void movesToLineAndTextEnds() {
		Document document = documentOf("ab\n\tc|d\nef");
		EditCommands commands = new EditCommands(document);
		commands.moveToLineStart(false);
		assertEquals("ab\n|\tcd\nef", stateOf(document));
		commands.moveToLineEnd(true);
		assertEquals("ab\n^\tcd|\nef", stateOf(document));
		commands.moveToEnd(false);
		assertEquals("ab\n\tcd\nef|", stateOf(document));
		commands.moveToStart(true);
		assertEquals("|ab\n\tcd\nef^", stateOf(document));
	}

	@Test
	public void selectsAll() {
		Document document = documentOf("float x;\nvec2 uv;");
		EditCommands commands = new EditCommands(document);
		commands.selectAll();
		assertEquals(new Selection(0, 17), document.selection());
	}

	@Test
	public void deleteBackwardRemovesOneChar() {
		Document document = documentOf("ab😀|c\nd");
		EditCommands commands = new EditCommands(document);
		commands.deleteBackward();
		assertEquals("ab|c\nd", stateOf(document));
		document.select(4, 4);
		commands.deleteBackward();
		assertEquals("abc|d", stateOf(document));
		commands.moveToStart(false);
		commands.deleteBackward();
		assertEquals("|abcd", stateOf(document));
	}

	@Test
	public void jamoOfOneSyllableAreOneChar() {
		Document document = documentOf("|\u1100\u1161\u11a8");
		EditCommands commands = new EditCommands(document);
		commands.moveForward(false);
		assertEquals(3, document.selection().head());
		commands.deleteBackward();
		assertEquals("", document.text().toString());
	}

	@Test
	public void aJoinerDoesNotJoinALetterAfterIt() {
		Document document = documentOf("a\u200db|");
		new EditCommands(document).deleteBackward();
		assertEquals("a\u200d", document.text().toString());
	}

	@Test
	public void anOffsetInsideACharActsOnAllOfIt() {
		for (String c : new String[]{"\ud83d\ude00", "e\u0301"}) {
			String text = "x" + c + "y";
			Document document = new Document(Text.of(text));
			EditCommands commands = new EditCommands(document);
			document.select(2, 2);
			commands.deleteBackward();
			assertEquals("xy", document.text().toString());
			document = new Document(Text.of(text));
			commands = new EditCommands(document);
			document.select(2, 2);
			commands.deleteForward();
			assertEquals("xy", document.text().toString());
			document = new Document(Text.of(text));
			commands = new EditCommands(document);
			document.select(2, 2);
			commands.moveForward(false);
			assertEquals(3, document.selection().head());
			document.select(2, 2);
			commands.moveBackward(false);
			assertEquals(1, document.selection().head());
		}
	}

	@Test
	public void deleteForwardRemovesOneChar() {
		Document document = documentOf("a|é\nb");
		EditCommands commands = new EditCommands(document);
		commands.deleteForward();
		assertEquals("a|\nb", stateOf(document));
		commands.deleteForward();
		assertEquals("a|b", stateOf(document));
		commands.moveToEnd(false);
		commands.deleteForward();
		assertEquals("ab|", stateOf(document));
	}

	@Test
	public void deleteRemovesTheSelection() {
		Document document = documentOf("a^bc|d");
		new EditCommands(document).deleteBackward();
		assertEquals("a|d", stateOf(document));
		document = documentOf("a|bc^d");
		new EditCommands(document).deleteWordForward();
		assertEquals("a|d", stateOf(document));
	}

	@Test
	public void deletesAWord() {
		Document document = documentOf("vec2 uv|");
		new EditCommands(document).deleteWordBackward();
		assertEquals("vec2 |", stateOf(document));
		document = documentOf("|vec2 uv");
		new EditCommands(document).deleteWordForward();
		assertEquals("| uv", stateOf(document));
	}

	@Test
	public void deletedCharsAreTyping() {
		Document document = documentOf("abc|");
		EditCommands commands = new EditCommands(document);
		commands.deleteBackward();
		commands.deleteBackward();
		document.undo();
		assertEquals("abc|", stateOf(document));
	}

	@Test
	public void enterIndentsTheNewLine() {
		Document document = documentOf("x\n\tf() {|\n}");
		new EditCommands(document).enter();
		assertEquals("x\n\tf() {\n\t\t|\n}", stateOf(document));
	}

	@Test
	public void tabInsertsALevelAtTheCaret() {
		Document document = documentOf("a|b");
		EditCommands commands = new EditCommands(document);
		commands.indent();
		assertEquals("a\t|b", stateOf(document));
	}

	@Test
	public void tabReplacesASelectionWithinALine() {
		Document document = documentOf("a^bc|d\ne");
		new EditCommands(document).indent();
		assertEquals("a\t|d\ne", stateOf(document));
	}

	@Test
	public void tabIndentsTheLinesOfASelection() {
		Document document = documentOf("a^b\n\nc|d\ne");
		new EditCommands(document).indent();
		assertEquals("\ta^b\n\n\tc|d\ne", stateOf(document));
	}

	@Test
	public void aSelectionEndingAtALineStartLeavesThatLine() {
		Document document = documentOf("^ab\ncd\n|ef");
		new EditCommands(document).indent();
		assertEquals("^\tab\n\tcd\n|ef", stateOf(document));
	}

	@Test
	public void unindentRemovesTheSpaceBeforeTheFirstTabStop() {
		Document document = documentOf("^\t\tab\n  \tcd\n  ef\ngh|");
		new EditCommands(document).unindent();
		assertEquals("^\tab\ncd\nef\ngh|", stateOf(document));
		document = documentOf("\t\ta|b");
		new EditCommands(document).unindent();
		assertEquals("\ta|b", stateOf(document));
		document = documentOf("\t|\tab");
		new EditCommands(document).unindent();
		assertEquals("|\tab", stateOf(document));
	}

	@Test
	public void indentingLinesIsOneUndoStep() {
		Document document = documentOf("a^b\ncd|");
		new EditCommands(document).indent();
		document.undo();
		assertEquals("a^b\ncd|", stateOf(document));
	}

	@Test
	public void indentingManyLinesIsOneChange() {
		Document document = new Document(Text.of("a\n".repeat(1000)));
		int[] changes = new int[1];
		document.addListener(new Document.Listener() {
			@Override
			public void onChange(Document.Change change, Text before) {
				++changes[0];
			}
		});
		document.select(0, document.text().length());
		EditCommands commands = new EditCommands(document);
		commands.indent();
		commands.unindent();
		document.undo();
		document.redo();
		assertEquals(4, changes[0]);
		assertEquals("a\n".repeat(1000), document.text().toString());
	}

	// Returns a document of state: '|' marks the head of the selection
	// and '^' its anchor, if that is elsewhere.
	static Document documentOf(String state) {
		int head = state.indexOf('|');
		int anchor = state.indexOf('^');
		String text = state.replace("|", "").replace("^", "");
		if (anchor < 0) {
			anchor = head;
		} else if (anchor > head) {
			--anchor;
		} else {
			--head;
		}
		Document document = new Document(Text.of(text));
		document.select(Math.max(anchor, 0), Math.max(head, 0));
		return document;
	}

	static String stateOf(Document document) {
		Selection selection = document.selection();
		StringBuilder sb = new StringBuilder(document.text().toString());
		int head = selection.head();
		int anchor = selection.anchor();
		if (anchor > head) {
			sb.insert(anchor, '^').insert(head, '|');
		} else {
			sb.insert(head, '|');
			if (anchor < head) {
				sb.insert(anchor, '^');
			}
		}
		return sb.toString();
	}
}
