package de.markusfisch.android.shadereditor.syntax;

// A line starting with lineComment (null for none) continues it; a line ending
// in one of levelAfter adds a level; opening and closing open and close one
// wherever they are.
public record Indentation(String lineComment, String levelAfter,
		String opening, String closing) {
	public static final Indentation C = new Indentation("//", "{+-*/%^=",
			"(", ")");
}
