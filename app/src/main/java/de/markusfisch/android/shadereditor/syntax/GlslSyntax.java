package de.markusfisch.android.shadereditor.syntax;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Completes keywords as the old editor does, from the caret's line alone.
public final class GlslSyntax implements Syntax {
	public static final GlslSyntax INSTANCE = new GlslSyntax();

	private static final List<String> BRACKETS = Collections.unmodifiableList(
			Arrays.asList("{", "}", "(", ")", "[", "]"));

	private GlslSyntax() {
	}

	@Override
	public int lexLine(CharSequence line, boolean breakFollows, int state,
			LineTokens out) {
		return GlslLexer.lexLine(line, breakFollows, state, out);
	}

	@Override
	public void complete(LexedLine line, int column, Offer offer) {
		int index = column > 0 ? line.indexAt(column - 1) : -1;
		if (index < 0) {
			// The line break before the line may be part of a comment.
			offer.offer(column == 0 && GlslLexer.inComment(line.state())
					? Collections.emptyList()
					: BRACKETS, 0);
			return;
		}
		String text = line.text();
		int start = line.start(index);
		if (line.style(index) == Style.COMMENT &&
				!(column >= line.end(index) && closes(text, start,
						line.end(index), line.state()))) {
			offer.offer(Collections.emptyList(), 0);
			return;
		}
		String prefix = text.substring(start, column);
		List<String> words = GlslCompletion.words(prefix,
				line.inDirective(index));
		if (words.isEmpty() ||
				(words.size() == 1 && words.get(0).equals(prefix))) {
			offer.offer(BRACKETS, 0);
		} else {
			offer.offer(words, column - start);
		}
	}

	@Override
	public Indentation indentation() {
		return Indentation.C;
	}

	// Whether the comment piece from start to end closes a block comment.
	private static boolean closes(String text, int start, int end,
			int state) {
		if (!text.startsWith("*/", end - 2)) {
			return false;
		}
		// A piece that continues a comment from the line before closes only
		// a block comment.
		if (start == 0 && GlslLexer.inComment(state)) {
			return GlslLexer.inBlockComment(state);
		}
		return text.startsWith("/*", start) && end - start >= 4;
	}
}
