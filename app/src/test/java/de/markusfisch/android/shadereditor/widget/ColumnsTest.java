package de.markusfisch.android.shadereditor.widget;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.markusfisch.android.shadereditor.editor.Document;
import de.markusfisch.android.shadereditor.editor.LineStyles;
import de.markusfisch.android.shadereditor.editor.Text;
import de.markusfisch.android.shadereditor.syntax.GlslSyntax;
import de.markusfisch.android.shadereditor.syntax.Style;
import de.markusfisch.android.shadereditor.syntax.Syntax;

public class ColumnsTest {
	private static final Syntax GLSL = GlslSyntax.INSTANCE;

	@Test
	public void tabsReachTheNextTabStop() {
		String line = "\ta\tbc\td";
		assertEquals(0, Columns.columnOf(line, 0, 4));
		assertEquals(4, Columns.columnOf(line, 1, 4));
		assertEquals(8, Columns.columnOf(line, 3, 4));
		assertEquals(12, Columns.columnOf(line, 6, 4));
		assertEquals(4, Columns.columnOf(line, 3, 2));
		assertEquals(8, Columns.columnOf(line, 6, 2));
		assertEquals(24, Columns.columnOf(line, 6, 8));
		assertEquals(25, Columns.columnOf(line, 99, 8));
	}

	@Test
	public void findsTheNearestOffsetToAColumn() {
		String line = "ab\tc";
		assertEquals(0, Columns.offsetAt(line, -3f, 4));
		assertEquals(0, Columns.offsetAt(line, .4f, 4));
		assertEquals(1, Columns.offsetAt(line, .6f, 4));
		// The tab spans columns 2 to 4.
		assertEquals(2, Columns.offsetAt(line, 2.9f, 4));
		assertEquals(3, Columns.offsetAt(line, 3.1f, 4));
		assertEquals(4, Columns.offsetAt(line, 40f, 4));
		for (int offset = 0; offset <= line.length(); ++offset) {
			assertEquals(offset, Columns.offsetAt(line,
					Columns.columnOf(line, offset, 4), 4));
		}
	}

	@Test
	public void tellsPrintableAsciiFromTheRest() {
		assertTrue(Columns.isSimple("\tvec4 c = vec4(1.); // ~"));
		assertFalse(Columns.isSimple("// café"));
		assertFalse(Columns.isSimple("a\u0001"));
	}

	@Test
	public void drawsOneColourRunPerPieceWithoutTabsOrBlanks() {
		String line = "\tfloat a = 1.0;\t// x";
		LineStyles.Runs runs = new LineStyles(new Document(Text.of(line)), GLSL,
				200).runs(0);
		int[] colors = new int[Style.values().length];
		for (int i = 0; i < colors.length; ++i) {
			colors[i] = 100 + i;
		}
		colors[Style.OPERATOR.ordinal()] = 1;
		colors[Style.IDENTIFIER.ordinal()] = 1;
		int keyword = colors[Style.PRIMITIVE.ordinal()];
		int number = colors[Style.NUMBER.ordinal()];
		int comment = colors[Style.COMMENT.ordinal()];
		assertArrayEquals(new int[]{
				1, 6, 4, keyword,
				// "a =" and ";" are plain, the number between them not.
				7, 10, 10, 1,
				11, 14, 14, number,
				14, 15, 17, 1,
				16, 20, 20, comment,
		}, Columns.pieces(line, runs, colors, 1, 4));
		assertArrayEquals(new int[]{1, 15, 4, 7, 16, 20, 20, 7},
				Columns.pieces(line, null, colors, 7, 4));
	}
}
