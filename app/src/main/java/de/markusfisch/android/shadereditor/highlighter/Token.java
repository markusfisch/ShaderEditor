package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;

public class Token {
	private int start;
	private int end;
	private int startOffset;
	private int endOffset;
	private short line;
	private short column;
	private @NonNull TokenType type = TokenType.INVALID;
	private @NonNull Category category = Category.NORMAL;

	public enum Category {
		NORMAL,
		PREPROC,
		TRIVIA
	}

	public int start() {
		return start;
	}

	public Token setStart(int start) {
		this.start = start;
		return this;
	}

	public int end() {
		return end;
	}

	public Token setEnd(int end) {
		this.end = end;
		return this;
	}

	public int startOffset() {
		return startOffset;
	}

	public Token setStartOffset(int startOffset) {
		this.startOffset = startOffset;
		return this;
	}

	public int endOffset() {
		return endOffset;
	}

	public Token setEndOffset(int endOffset) {
		this.endOffset = endOffset;
		return this;
	}

	public short line() {
		return line;
	}

	public Token setLine(short line) {
		this.line = line;
		return this;
	}

	public short column() {
		return column;
	}

	public Token setColumn(short column) {
		this.column = column;
		return this;
	}

	public @NonNull TokenType type() {
		return type;
	}

	public Token setType(@NonNull TokenType type) {
		this.type = type;
		return this;
	}

	public @NonNull Category category() {
		return category;
	}

	public Token setCategory(@NonNull Category category) {
		this.category = category;
		return this;
	}

	@Override
	public @NonNull String toString() {
		return "Token{" + type +
				"[" + category +
				"], range=" + start +
				"-" + end +
				", offset=" + startOffset +
				"-" + endOffset +
				", pos=" + line +
				":" + column +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Token token = (Token) o;

		if (end - start != token.end - token.start) return false;
		if (endOffset - startOffset != token.endOffset - token.startOffset) return false;
		if (type != token.type) return false;
		return category == token.category;
	}

	@Override
	public int hashCode() {
		int result = end - start;
		result = 31 * result + (endOffset - startOffset);
		result = 31 * result + type.hashCode();
		result = 31 * result + category.hashCode();
		return result;
	}
}
