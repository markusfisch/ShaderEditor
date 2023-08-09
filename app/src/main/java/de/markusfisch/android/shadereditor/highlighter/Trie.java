//
// Created by Anton Pieper on 09.07.23.
//

#include "trie.h"

#include <stddef.h>
#include <stdlib.h>

#include "iter.h"

// Converts key current character into index
inline static int char_to_index(char c) {
  static const int lookup[] = {
      ['a'] = 0,  ['b'] = 1,  ['c'] = 2,  ['d'] = 3,  ['e'] = 4,  ['f'] = 5,
      ['g'] = 6,  ['h'] = 7,  ['i'] = 8,  ['j'] = 9,  ['k'] = 10, ['l'] = 11,
      ['m'] = 12, ['n'] = 13, ['o'] = 14, ['p'] = 15, ['q'] = 16, ['r'] = 17,
      ['s'] = 18, ['t'] = 19, ['u'] = 20, ['v'] = 21, ['w'] = 22, ['x'] = 23,
      ['y'] = 24, ['z'] = 25, ['A'] = 26, ['B'] = 27, ['C'] = 28, ['D'] = 29,
      ['E'] = 30, ['F'] = 31, ['G'] = 32, ['H'] = 33, ['I'] = 34, ['J'] = 35,
      ['K'] = 36, ['L'] = 37, ['M'] = 38, ['N'] = 39, ['O'] = 40, ['P'] = 41,
      ['Q'] = 42, ['R'] = 43, ['S'] = 44, ['T'] = 45, ['U'] = 46, ['V'] = 47,
      ['W'] = 48, ['X'] = 49, ['Y'] = 50, ['Z'] = 51, ['0'] = 52, ['1'] = 53,
      ['2'] = 54, ['3'] = 55, ['4'] = 56, ['5'] = 57, ['6'] = 58, ['7'] = 59,
      ['8'] = 60, ['9'] = 61, ['_'] = 62};
  return lookup[c];
}

TrieNode *create_node(void) {
  TrieNode *pNode = NULL;
  pNode = (TrieNode *)calloc(1, sizeof(*pNode));
  return pNode;
}

void trie_insert(TrieNode *root, const char *key, uint16_t value) {
  TrieNode *current_node = root;

  for (const char *ch = key; *ch; ++ch) {
    int index = char_to_index(*ch);
    if (!current_node->children[index])
      current_node->children[index] = create_node();
    current_node = current_node->children[index];
  }
  current_node->value = value;
}

uint16_t trie_find(TrieNode *root, const char *key, size_t key_length) {
  TrieNode *current_node = root;
  ptrdiff_t i = 0;
  for (const char *iter = key; *iter && i < key_length; i = iter - key) {
    int index = char_to_index(*iter);

    if (!current_node->children[index]) return 0;
    current_node = current_node->children[index];
    iter = iter_next_c(iter);
  }

  return current_node->value;
}