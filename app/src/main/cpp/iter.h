//
// Created by Anton Pieper on 10.07.23.
//

#ifndef SHADEREDITOR_APP_SRC_MAIN_CPP_ITER_H_
#define SHADEREDITOR_APP_SRC_MAIN_CPP_ITER_H_

#ifdef __cplusplus
extern "C" {
#endif

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

#ifdef __cplusplus
}
#endif

#endif  // SHADEREDITOR_APP_SRC_MAIN_CPP_ITER_H_
