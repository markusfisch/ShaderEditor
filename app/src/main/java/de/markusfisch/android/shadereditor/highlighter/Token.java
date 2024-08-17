package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

	@NonNull
	public Token setStart(int start) {
		this.start = start;
		return this;
	}

	public int end() {
		return end;
	}

	@NonNull
	public Token setEnd(int end) {
		this.end = end;
		return this;
	}

	public int startOffset() {
		return startOffset;
	}

	@NonNull
	public Token setStartOffset(int startOffset) {
		this.startOffset = startOffset;
		return this;
	}

	public int endOffset() {
		return endOffset;
	}

	@NonNull
	public Token setEndOffset(int endOffset) {
		this.endOffset = endOffset;
		return this;
	}

	public short line() {
		return line;
	}

	@NonNull
	public Token setLine(short line) {
		this.line = line;
		return this;
	}

	public short column() {
		return column;
	}

	@NonNull
	public Token setColumn(short column) {
		this.column = column;
		return this;
	}

	public @NonNull TokenType type() {
		return type;
	}

	@NonNull
	public Token setType(@NonNull TokenType type) {
		this.type = type;
		return this;
	}

	public @NonNull Category category() {
		return category;
	}

	@NonNull
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
	public boolean equals(@Nullable Object o) {
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
