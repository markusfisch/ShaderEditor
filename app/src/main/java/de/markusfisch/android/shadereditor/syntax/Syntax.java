package de.markusfisch.android.shadereditor.syntax;

import java.util.List;

// What the editor asks of a language. An implementation is immutable, so it
// may be called on any thread.
public interface Syntax {
	int TEXT_START = 0;

	interface Offer {
		// position: how many characters of the word before the caret each
		// word repeats.
		void offer(List<String> words, int position);
	}

	// Returns the state the next line starts in, 0 to 255, or TEXT_START if
	// no line break follows.
	int lexLine(CharSequence line, boolean breakFollows, int state,
			LineTokens out);

	void complete(LexedLine line, int column, Offer offer);

	Indentation indentation();
}
