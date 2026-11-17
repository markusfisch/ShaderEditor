package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;

import de.markusfisch.android.shadereditor.syntax.Tokens;

/**
 * The tokens in which two token lists differ in style or length.
 */
public final class TokenDiff {
	public final int start;
	// Exclusive ends in the original and in the edited tokens.
	public final int deleteEnd;
	public final int insertEnd;

	private TokenDiff(int start, int deleteEnd, int insertEnd) {
		this.start = start;
		this.deleteEnd = deleteEnd;
		this.insertEnd = insertEnd;
	}

	@NonNull
	public static TokenDiff of(@NonNull Tokens original,
			@NonNull Tokens edited) {
		int max = Math.min(original.size(), edited.size());
		int start = 0;
		while (start < max && same(original, start, edited, start)) {
			++start;
		}
		int deleteEnd = original.size();
		int insertEnd = edited.size();
		while (deleteEnd > start && insertEnd > start &&
				same(original, deleteEnd - 1, edited, insertEnd - 1)) {
			--deleteEnd;
			--insertEnd;
		}
		return new TokenDiff(start, deleteEnd, insertEnd);
	}

	private static boolean same(Tokens a, int i, Tokens b, int j) {
		return a.style(i) == b.style(j) &&
				a.end(i) - a.start(i) == b.end(j) - b.start(j);
	}
}
