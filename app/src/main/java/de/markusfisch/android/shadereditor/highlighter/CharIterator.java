package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;


public final class CharIterator {
	private final @NonNull String source;
	private int position = 0;

	public CharIterator(@NonNull String source) {
		this.source = source;
	}

	/**
	 * Get the current character. If the current index out of bounds, {@code Character.MAX_VALUE}
	 * is returned. This character is used, because it is invalid unicode.
	 *
	 * @return The current character.
	 */
	public char ch() {
		return ch(position);
	}

	/**
	 * Peek the next character. If the peeked index out of bounds, {@code Character.MAX_VALUE} is
	 * returned. This character is used, because it is invalid unicode.
	 *
	 * @return The peeked character.
	 */
	public char peek() {
		return ch(position + 1);
	}

	/**
	 * Get at a character at an absolute position {@code position}. If the current index out of
	 * bounds, {@code Character.MAX_VALUE} is returned. This character is used, because it is
	 * invalid unicode.
	 *
	 * @param position The absolute char position.
	 * @return The char at the position.
	 */
	private char ch(int position) {
		if (position < source.length()) {
			return source.charAt(position);
		}
		return Character.MAX_VALUE;
	}

	/**
	 * Increment the iterators position.
	 */
	public void next() {
		++position;
	}

	/**
	 * Peek next char, skipping C-style line continuations. If the peeked index out of bounds,
	 * {@code Character.MAX_VALUE} is returned. This character is used, because it is invalid
	 * unicode.
	 *
	 * @return the peeked char.
	 */
	public char peekC() {
		char ch = Character.MAX_VALUE;
		int position = this.position;
		int length = source.length();
		do {
			if (++position < length)
				ch = source.charAt(position);
			if (ch != '\\') break;
		} while (moveNewline());
		return ch;
	}

	/**
	 * Skip to the end of a newline, if there is any.
	 *
	 * @return whether there was a newline.
	 */
	private boolean moveNewline() {
		final int start = position;
		position = nextNewline(start);
		// is line continuation -> iter moved
		return position != start;
	}

	/**
	 * Go to next char, skipping C-style line continuations.
	 */
	public void nextC() {
		char ch;
		do {
			++position;
			ch = source.charAt(position);
		} while (ch == '\\' && moveNewline());
	}

	/**
	 * Get the position of the end of a newline at this position.
	 *
	 * @param iter the current position.
	 * @return The position of the end of the newline. If there was none, `iter` is returned.
	 */
	private int nextNewline(int iter) {
		char peek = ch(iter);
		// is next \r or \n? -> iterate
		if (peek == '\r' || peek == '\n') {
			++iter;
			char second_peek = ch(iter);
			// is next \r\n or \n\r? -> iterate
			if (second_peek != peek && (second_peek == '\r' || second_peek == '\n'))
				++iter;
		}
		// is line continuation -> iter moved
		return iter;
	}
}