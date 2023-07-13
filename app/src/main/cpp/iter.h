//
// Created by Anton Pieper on 10.07.23.
//

#ifndef HIGHLIGHTER__ITER_H_
#define HIGHLIGHTER__ITER_H_

#include <stdbool.h>

/// peek next character
char iter_peek(const char *iter);
/// peek next character, ignoring C-like line continuations
char iter_peek_c(const char *iter);

/// create iterator to next character, ignoring C-like line continuations
const char *iter_next_c(const char *iter);

/// mutates iter to the end of the newline token if there is one
bool iter_move_newline(const char **iter);

/// create iterator to the end of the newline token if there is one
const char *iter_next_newline(const char *iter);
#endif  // HIGHLIGHTER__ITER_H_
