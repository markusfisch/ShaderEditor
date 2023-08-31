package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;

public class Token {
	public enum Category {
		NORMAL,
		PREPROC,
		TRIVIA
	}

	public Token() {
	}

	@Override
	public @NonNull String toString() {
		return "Token{" +
				"start=" + start +
				", end=" + end +
				", startOffset=" + startOffset +
				", line=" + line +
				", column=" + column +
				", type=" + type +
				", category=" + category +
				'}';
	}

	public Token(@NonNull Token from) {
		this.setStart(from.start());
		this.setEnd(from.end());
		this.setStartOffset(from.startOffset());
		this.setEndOffset(from.endOffset());
		this.setLine(from.line());
		this.setColumn(from.column());
		this.setType(from.type());
		this.setCategory(from.category());
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

	private int start;
	private int end;
	private int startOffset;
	private int endOffset;
	private short line;
	private short column;
	private @NonNull TokenType type = TokenType.INVALID;
	private @NonNull Category category = Category.NORMAL;
}
