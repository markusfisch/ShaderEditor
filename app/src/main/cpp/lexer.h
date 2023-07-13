#include <stddef.h>
#include <stdint.h>

#include "token_type.h"

#ifndef MAIN_H
#define MAIN_H

typedef enum __attribute((__packed__)) TokenCategory {
  NORMAL,
  PREPROC,
  TRIVIA
} TokenCategory;
#ifdef DEBUG
const char *CATEGORY_DEBUG[] = {
    [NORMAL] = "NORMAL",
    [PREPROC] = "PREPROC",
    [TRIVIA] = "TRIVIA",
};
#endif

typedef struct Token {
  uint32_t start;
  uint32_t end;
  uint32_t start_offset;
  uint32_t line;
  uint16_t column;
  TokenType type;
  TokenCategory category;
} Token;

typedef struct Lexer {
  Token previous;
  const char *const source;
  const char *iter;  // codepoint index
  uint32_t line_start;
  uint32_t line_offset;
  uint32_t position;       // current position byte offset
  uint32_t read_position;  // read position byte offset
  uint32_t line_count;
  uint32_t tab_width;
  uint32_t line_tab_count;
} Lexer;

/// Initializes a lexer with the given source code.
/// The source has to use LF line endings
Lexer create_lexer(const char *const input, uint32_t tab_width);

/// Get the next token from the lexer.
Token next_token(Lexer *highlighter);

/// Needs to be run <b>once</b> before creating any lexer.
void lexer_init_lookup();

#endif
