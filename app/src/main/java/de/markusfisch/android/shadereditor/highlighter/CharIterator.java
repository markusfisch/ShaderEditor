package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;


public final class CharIterator {
	private final @NonNull String source;
	private int position = 0;
	public CharIterator(@NonNull String source) {
		this.source = source;
	}
	public char ch() {
		return source.charAt(position);
	}
	public char peek() {
		return peek(position + 1);
	}
	private char peek(int position) {
		if (position >= source.length()) {
			return 0;
		}
		return source.charAt(position);
	}
	public void next() {
		++position;
	}

	/**
	 * Peek next char, skipping C-style line continuations.
	 * @return the peeked char.
	 */
	public char peekC() {
		char ch = 0;
		int position = this.position;
		int length = source.length();
		do {
			if (++position < length)
				ch = source.charAt(position);
			if (ch != '\\') break;
		} while (moveNewline());
		return ch;
	}

	private boolean moveNewline() {
		final int start = position;
		position = nextNewline(start);
		// is line continuation -> iter moved
		return position != start;
	}

	/**
	 * Go to next char, skipping C-style line continuations
	 */
	public void nextC() {
		char ch;
		do {
			++position;
			ch = source.charAt(position);
		} while (ch == '\\' && moveNewline());
	}

	private int nextNewline(int iter) {
		char peek = peek(iter);
		// is next \r or \n? -> iterate
		if (peek == '\r' || peek == '\n') {
			++iter;
			char second_peek = peek(iter);
			// is next \r\n or \n\r? -> iterate
			if (second_peek != peek && (second_peek == '\r' || second_peek == '\n'))
				++iter;
		}
		// is line continuation -> iter moved
		return iter;
	}
}