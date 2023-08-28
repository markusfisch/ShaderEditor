package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;

/**
 * A specialized trie for characters that can represent Identifiers in C-like code.
 */
public final class Trie {
	final static class Node {
		public short value() {
			return value;
		}

		public void insert(final @NonNull String key, short value) {
			Node currentNode = this;

			for (int i = 0, length = key.length(); i < length; ++i) {
				int childIndex = Node.charToIndex(key.charAt(i));

				if (currentNode.children[childIndex] == null)
					currentNode.children[childIndex] = new Node();
				currentNode = currentNode.children[childIndex];
			}
			currentNode.value = value;
		}

		public short find(@NonNull String keySource, int keyStart, int keyLength) {
			Node currentNode = this;
			int i = 0;
			int keyPosition = keyStart;
			for (
					char ch = CharIterator.ch(keyPosition, keySource);
					CharIterator.isValid(ch) && i < keyLength;
					i = keyPosition - keyStart
			) {
				int index = charToIndex(ch);

				if (currentNode.children[index] == null) return 0;
				currentNode = currentNode.children[index];
				keyPosition = CharIterator.nextC(keyPosition, keySource);
			}

			return currentNode.value();
		}

		private short value;
		private final Node[] children = new Node[COUNT_OF_CHILDREN];

		private static final int[] CHAR_INDEX_LOOKUP;
		private static final int COUNT_OF_CHILDREN;

		/*
		 * Generate a lookup for fast child index retrieval.
		 */
		static {
			// Java char is 16 Bit, however only ASCII-Values make sense anyways.
			// (2^8 - 1) * 4 bytes = 1020 Bytes
			int[] result = new int[Byte.MAX_VALUE];
			int index = 1;
			for (int i = 'a'; i <= 'z'; ++i) {
				result[i] = index++;
			}
			for (int i = 'A'; i <= 'Z'; ++i) {
				result[i] = index++;
			}
			for (int i = '0'; i <= '9'; ++i) {
				result[i] = index++;
			}
			result['_'] = index++;

			COUNT_OF_CHILDREN = index;
			CHAR_INDEX_LOOKUP = result;
		}

		/**
		 * Converts a character of a key into an index.
		 *
		 * @return the index into the lookup-table
		 */
		private static int charToIndex(char c) {
			if (c < CHAR_INDEX_LOOKUP.length)
				return CHAR_INDEX_LOOKUP[c];
			return 0;
		}
	}
}