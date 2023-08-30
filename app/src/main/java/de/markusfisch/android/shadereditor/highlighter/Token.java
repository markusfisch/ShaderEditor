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
		this.setStart(from.getStart());
		this.setEnd(from.getEnd());
		this.setStartOffset(from.getStartOffset());
		this.setLine(from.getLine());
		this.setColumn(from.getColumn());
		this.setType(from.getType());
		this.setCategory(from.getCategory());
	}

	public int getStart() {
		return start;
	}

	public Token setStart(int start) {
		this.start = start;
		return this;
	}

	public int getEnd() {
		return end;
	}

	public Token setEnd(int end) {
		this.end = end;
		return this;
	}

	public int getStartOffset() {
		return startOffset;
	}

	public Token setStartOffset(int startOffset) {
		this.startOffset = startOffset;
		return this;
	}

	public short getLine() {
		return line;
	}

	public Token setLine(short line) {
		this.line = line;
		return this;
	}

	public short getColumn() {
		return column;
	}

	public Token setColumn(short column) {
		this.column = column;
		return this;
	}

	public @NonNull TokenType getType() {
		return type;
	}

	public Token setType(@NonNull TokenType type) {
		this.type = type;
		return this;
	}

	public @NonNull Category getCategory() {
		return category;
	}

	public Token setCategory(@NonNull Category category) {
		this.category = category;
		return this;
	}

	private int start;
	private int end;
	private int startOffset;
	private short line;
	private short column;
	private @NonNull TokenType type = TokenType.INVALID;
	private @NonNull Category category = Category.NORMAL;
}
