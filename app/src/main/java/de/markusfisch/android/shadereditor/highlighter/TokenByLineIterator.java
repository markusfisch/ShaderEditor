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
		return current.getType() != TokenType.EOF;
	}

	@Override
	public @NonNull Token next() {
		// What if original == null?
		// Start from original start and ++end until \n or end is reached.
		// if end == original end, then original = next
		// return current with end set
		int endMarker;
		if (source.length() == 0) return current.setType(TokenType.EOF);
		if (original == null || end >= (endMarker = original.getEnd())) {
			original = tokenIterator.hasNext() ? tokenIterator.next() : null;
			if (original == null) return current.setType(TokenType.EOF);
			current = new Token(original);
			end = original.getStart();
			endOffset = original.getStartOffset();
			endMarker = original.getEnd();
			if (current.getType() == TokenType.EOF) return current;
		}
		while (end < endMarker) {
			boolean isNewLine = (source.charAt(endOffset) == '\n');
			++end;
			endOffset += CharIterator.isSurrogate(source.charAt(endOffset)) ? 2 : 1;
			if (isNewLine) {
				current.setLine((short) (current.getLine()+1));
				current.setColumn((short) 0);
				break;
			}
		}
		current.setEnd(end);
		Token result = new Token(current);
		current.setStartOffset(endOffset);
		current.setStart(end);
		return result;
	}
}