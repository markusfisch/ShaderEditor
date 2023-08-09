package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;


public final class CharIterator {
	/**
	 * Marks that the returned character of a {@link CharIterator} is invalid.
	 * <a href="https://softwareengineering.stackexchange.com/a/190417">0xDCDC</a> is used,
	 * because it is invalid unicode.
	 */
	public static final char INVALID = 0xDCDC;

	private CharIterator() {
	}

	/**
	 * Check whether this character is beyond an iterators bounds.
	 *
	 * @param ch The character to check
	 * @return whether {@code ch} marks is beyond an iterators bounds.
	 */
	public static boolean isValid(char ch) {
		return ch != INVALID;
	}

	/**
	 * Get at a character at an absolute position {@code position}. If the current index out of
	 * bounds, {@link #INVALID} is returned. This character is used, because it is
	 * invalid unicode.
	 *
	 * @param position The absolute char position.
	 * @param source   The source string.
	 * @return The char at the position.
	 */
	public static char ch(int position, @NonNull String source) {
		if (position < source.length()) {
			return source.charAt(position);
		}
		return CharIterator.INVALID;
	}

	/**
	 * Peek the next character. If the peeked index out of bounds, {@link #INVALID} is returned. This
	 * character is used, because it is invalid unicode.
	 *
	 * @param position The absolute current char position.
	 * @param source   The source string.
	 * @return The peeked character.
	 */
	public static char peek(int position, @NonNull String source) {
		return ch(position + 1, source);
	}


	/**
	 * Increment the iterators position.
	 *
	 * @param position The absolute current char position.
	 * @return The next position.
	 */
	public static int next(int position) {
		return position + 1;
	}

	/**
	 * Get the position of the end of a newline at this position.
	 *
	 * @param position The current position.
	 * @param source   The source string.
	 * @return The position of the end of the newline. If there was none, `iter` is returned.
	 */
	public static int nextNewline(int position, @NonNull String source) {
		char peek = ch(position, source);
		// is next \r or \n? -> iterate
		if (peek == '\r' || peek == '\n') {
			position = next(position);
			char second_peek = ch(position, source);
			// is next \r\n or \n\r? -> iterate
			if (second_peek != peek && (second_peek == '\r' || second_peek == '\n')) {
				position = next(position);
			}
		}
		// is line continuation -> iter moved
		return position;
	}

	/**
	 * Peek next char, skipping C-style line continuations. If the peeked index out of bounds,
	 * {@link #INVALID} is returned. This character is used, because it is invalid
	 * unicode.
	 *
	 * @param position The absolute current char position.
	 * @param source   The source string.
	 * @return the peeked char.
	 */
	public static char peekC(int position, @NonNull String source) {
		int length = source.length();
		int positionBeforeNewline;
		char ch = CharIterator.INVALID;
		do {
			position = next(position);
			if (position < length)
				ch = source.charAt(position);
			if (ch != '\\') break;
			positionBeforeNewline = position;
			position = nextNewline(position, source);
		} while (hasMoved(positionBeforeNewline, position));
		return ch;
	}

	/**
	 * Go to next char, skipping C-style line continuations.
	 *
	 * @param position The absolute current char position.
	 * @param source   The source string.
	 */
	public static int nextC(int position, @NonNull String source) {
		int positionBeforeNewline;
		char ch;
		do {
			position = next(position);
			positionBeforeNewline = position;
			ch = source.charAt(position);
		} while (ch == '\\' && hasMoved(
				positionBeforeNewline,
				position = nextNewline(position, source))
		);
		return position;
	}

	/**
	 * Returns whether the two iterators are equal.
	 *
	 * @param oldPosition Old position.
	 * @param newPosition New position.
	 * @return Whether the two iterators are equal.
	 */
	public static boolean hasMoved(int oldPosition, int newPosition) {
		return oldPosition != newPosition;
	}
}