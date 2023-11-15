package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;

/**
 * A specialized trie for characters that can represent Identifiers in C-like code.
 */
public final class TrieNode {
	private short value;
	private final TrieNode[] children = new TrieNode[Byte.MAX_VALUE];

	public short value() {
		return value;
	}

	/**
	 * Add to trie and return self.
	 *
	 * @param key   Where to insert the value.
	 * @param value Value to insert.
	 * @return this
	 */
	public TrieNode insert(final @NonNull String key, short value) {
		TrieNode currentNode = this;

		for (int i = 0, length = key.length(); i < length; i = CharIterator.nextC(i, key)) {
			int childIndex = charToIndex(key.charAt(i));

			if (currentNode.children[childIndex] == null)
				currentNode.children[childIndex] = new TrieNode();
			currentNode = currentNode.children[childIndex];
		}
		currentNode.value = value;
		return this;
	}

	/**
	 * Add to trie and return self.
	 *
	 * @param key   Where to insert the value.
	 * @param value Value to insert.
	 * @return this
	 * @see #insert(String, short)
	 */
	public TrieNode insert(final @NonNull String key, @NonNull Enum<?> value) {
		return insert(key, (short) value.ordinal());
	}

	public short find(@NonNull String keySource, int keyStart, int keyLength) {
		TrieNode currentNode = this;
		int i = 0;
		int keyPosition = keyStart;
		for (
				char ch = CharIterator.ch(keyPosition, keySource);
				CharIterator.isValid(ch) && i < keyLength;
				i = keyPosition - keyStart, ch = CharIterator.ch(keyPosition, keySource)
		) {
			int index = charToIndex(ch);

			if (currentNode.children[index] == null) return 0;
			currentNode = currentNode.children[index];
			keyPosition = CharIterator.nextC(keyPosition, keySource);
		}

		return currentNode.value();
	}

	/**
	 * Converts a character of a key into an index.
	 *
	 * @return the index into the lookup-table
	 */
	private static int charToIndex(char c) {
		if (c < Byte.MAX_VALUE) {
			return c;
		}
		return 0;
	}
}