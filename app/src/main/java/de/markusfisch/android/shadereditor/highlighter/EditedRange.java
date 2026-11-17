package de.markusfisch.android.shadereditor.highlighter;

import de.markusfisch.android.shadereditor.syntax.Tokens;

/**
 * The smallest range of the current text that covers everything typed or
 * deleted since colours were last applied.
 */
public final class EditedRange {
	private int start = -1;
	private int end = -1;

	public static final class Restyle {
		/** Offsets of the text whose colours are cleared. */
		public final int start;
		public final int end;
		/** Indices of the first and one past the last token to colour. */
		public final int firstToken;
		public final int endToken;

		Restyle(int start, int end, int firstToken, int endToken) {
			this.start = start;
			this.end = end;
			this.firstToken = firstToken;
			this.endToken = endToken;
		}
	}

	public boolean isEmpty() {
		return start < 0;
	}

	public int start() {
		return start;
	}

	public int end() {
		return end;
	}

	/**
	 * Records that the {@code before} characters at {@code start} were
	 * replaced by {@code count} characters.
	 */
	public void replace(int start, int before, int count) {
		int insertEnd = start + count;
		if (isEmpty()) {
			this.start = start;
			this.end = insertEnd;
			return;
		}
		if (end >= start + before) {
			end += count - before;
		} else if (end > start) {
			end = insertEnd;
		}
		this.start = Math.min(this.start, start);
		end = Math.max(end, insertEnd);
	}

	public void reset() {
		start = -1;
		end = -1;
	}

	/**
	 * Returns what to colour again: the union of this range and the range
	 * in which old and new tokens differ, widened to every non-empty token
	 * that overlaps or touches it.
	 *
	 * @param tokens The new tokens of the current text.
	 * @param diff   The difference between the old and the new tokens.
	 */
	public Restyle restyle(Tokens tokens, TokenDiff diff) {
		int from = Integer.MAX_VALUE;
		int to = Integer.MIN_VALUE;
		if (!isEmpty()) {
			from = start;
			to = end;
		}
		if (diff.start < diff.insertEnd) {
			from = Math.min(from, tokens.start(diff.start));
			to = Math.max(to, tokens.end(diff.insertEnd - 1));
		}
		if (from > to) {
			return new Restyle(0, 0, 0, 0);
		}
		int firstToken = tokens.size();
		int endToken = 0;
		for (int i = 0, size = tokens.size(); i < size; ++i) {
			if (tokens.start(i) > to) {
				break;
			}
			if (tokens.end(i) >= from) {
				firstToken = Math.min(firstToken, i);
				endToken = i + 1;
			}
		}
		if (firstToken < endToken) {
			from = Math.min(from, tokens.start(firstToken));
			to = Math.max(to, tokens.end(endToken - 1));
		} else {
			firstToken = endToken = 0;
		}
		return new Restyle(from, to, firstToken, endToken);
	}
}
