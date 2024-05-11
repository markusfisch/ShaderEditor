package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A specialized trie for characters that can represent Identifiers in C-like code.
 */
public final class TrieNode {
	private short value;
	@Nullable
	private TrieNode[] children;

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
			if (currentNode.children == null) {
				currentNode.children = new TrieNode[Byte.MAX_VALUE];
			}
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

			if (currentNode.children == null || currentNode.children[index] == null) return 0;
			currentNode = currentNode.children[index];
			keyPosition = CharIterator.nextC(keyPosition, keySource);
		}

		return currentNode.value();
	}

	public void findAll(String prefix, short invalid, List<String> result) {
		if (prefix == null || prefix.isEmpty()) {
			return; // Optionally handle edge cases or invalid input
		}

		TrieNode current = this;
		// Navigate to the node for the end of the prefix
		for (int position = 0, length = prefix.length(); position < length; ++position) {
			char c = prefix.charAt(position);
			int index = charToIndex(c);
			if (current.children == null || current.children[index] == null) {
				return; // Prefix not in trie
			}
			current = current.children[index];
		}

		// Use a stack for iterative DFS
		Deque<Pair<TrieNode, StringBuilder>> stack = new ArrayDeque<>();
		stack.push(new Pair<>(current, new StringBuilder(prefix)));

		while (!stack.isEmpty()) {
			Pair<TrieNode, StringBuilder> nodePair = stack.pop();
			TrieNode node = nodePair.first;
			StringBuilder currentPrefix = nodePair.second;

			if (node.value != invalid) {
				result.add(currentPrefix.toString());
			}

			if (node.children == null) {
				continue;
			}
			// Explore all children of the current node
			for (int i = node.children.length - 1; i >= 0; i--) {
				TrieNode child = node.children[i];
				if (child != null) {
					StringBuilder newPrefix = new StringBuilder(currentPrefix);
					newPrefix.append(indexToChar(i)); // Modify according to your trie design
					stack.push(new Pair<>(child, newPrefix));
				}
			}
		}
	}

	static class Pair<T, U> {
		T first;
		U second;

		public Pair(T first, U second) {
			this.first = first;
			this.second = second;
		}
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

	private static char indexToChar(int index) {
		return (char) index;
	}
}