//
// Created by Anton Pieper on 09.07.23.
//

#ifndef HIGHLIGHTER__TRIE_H_
#define HIGHLIGHTER__TRIE_H_

#include <stddef.h>
#include <stdint.h>

// Alphabet size (# of symbols) a-z   A-Z   0-9   _
//                              ^26 + ^26 + ^10 + ^1 = 63
#define ALPHABET_SIZE 63
// trie node
typedef struct TrieNode {
  struct TrieNode *children[ALPHABET_SIZE];
  uint16_t value;
} TrieNode;

/// Create a new TrieNode, may return <code>NULL</code> on error.
TrieNode *create_node(void);

/// If not present, inserts key into trie.<br>
/// If the key is prefix of trie node, just marks leaf node.<br>
void trie_insert(TrieNode *root, const char *key, uint16_t value);

/// returns 0 if not found. matches key up to <code>key_length</code> non-null characters.
uint16_t trie_find(TrieNode *root, const char *key, size_t key_length);

#endif  // HIGHLIGHTER__TRIE_H_
