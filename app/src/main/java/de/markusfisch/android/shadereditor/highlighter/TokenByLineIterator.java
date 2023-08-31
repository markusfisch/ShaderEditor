package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Iterator;

public class TokenByLineIterator implements Iterator<Token> {
	private final @NonNull String source;
	private final @NonNull Iterator<Token> tokenIterator;
	private @Nullable Token original;
	private @NonNull Token current = new Token();
	private int endOffset = 0;
	private int end = 0;

	public TokenByLineIterator(@NonNull String source, @NonNull Iterator<Token> tokenIterator) {
		this.source = source;
		this.tokenIterator = tokenIterator;
	}

	@Override
	public boolean hasNext() {
		return current.type() != TokenType.EOF;
	}

	@Override
	public @NonNull Token next() {
		// What if original == null?
		// Start from original start and ++end until \n or end is reached.
		// if end == original end, then original = next
		// return current with end set
		int endMarker;
		if (source.isEmpty()) return current.setType(TokenType.EOF);
		if (original == null || end >= (endMarker = original.end())) {
			original = tokenIterator.hasNext() ? tokenIterator.next() : null;
			if (original == null) return current.setType(TokenType.EOF);
			current = new Token(original);
			end = original.start();
			endOffset = original.startOffset();
			endMarker = original.end();
			if (current.type() == TokenType.EOF) return current;
		}
		boolean isNewLine = false;
		while (end < endMarker) {
			isNewLine = (source.charAt(endOffset) == '\n');
			++end;
			endOffset += CharIterator.isSurrogate(source.charAt(endOffset)) ? 1 : 1;
			if (isNewLine) {
				break;
			}
		}
		current.setEnd(end)
			.setEndOffset(endOffset);
		Token result = new Token(current);
		if (isNewLine) {
			current.setLine((short) (current.line() + 1))
				.setColumn((short) 0);
		}
		current.setStartOffset(endOffset)
			.setStart(end);
		return result;
	}
}