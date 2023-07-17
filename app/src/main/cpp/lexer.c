#include "lexer.h"

#include <ctype.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include "iter.h"
#include "keywords.h"
#include "token_type.h"
#include "trie.h"

// Preprocessor directives
static const KeywordToken PREPROC_DIRECTIVES[] = {
    {.type = PREPROC_IF, .name = "if"},
    {.type = PREPROC_IFDEF, .name = "ifdef"},
    {.type = PREPROC_IFNDEF, .name = "ifndef"},
    {.type = PREPROC_ELIF, .name = "elif"},
    {.type = PREPROC_ELSE, .name = "else"},
    {.type = PREPROC_ENDIF, .name = "endif"},
    {.type = PREPROC_INCLUDE, .name = "include"},
    {.type = PREPROC_DEFINE, .name = "define"},
    {.type = PREPROC_UNDEF, .name = "undef"},
    {.type = PREPROC_LINE, .name = "line"},
    {.type = PREPROC_ERROR, .name = "error"},
    {.type = PREPROC_PRAGMA, .name = "pragma"},
    {.type = PREPROC_VERSION, .name = "version"}};
static const size_t DIRECTIVES_LENGTH =
    sizeof(PREPROC_DIRECTIVES) / sizeof(*PREPROC_DIRECTIVES);

static TrieNode keywords_trie = {};
static TrieNode directives_trie = {};
// Forward declarations
static void read_next(Lexer *lexer);
static uint32_t skip_whitespace(Lexer *lexer);
static const char *read_identifier(Lexer *lexer);
static TokenType read_number(Lexer *lexer);
static uint8_t utf8_codepoint_length(uint8_t utf8);

Lexer create_lexer(const char *input, uint32_t tab_width) {
  Lexer lexer = {.source = input,
                 .iter = input,
                 .read_position = 1,
                 .tab_width = tab_width};
  next_token(&lexer);
  return lexer;
}

inline static TokenType advance(Lexer *lexer, TokenType type) {
  read_next(lexer);
  return type;
}

// TODO: implement preprocessor tokenization
Token next_token(Lexer *lexer) {
  bool is_new_logic_line = skip_whitespace(lexer) != 0;
  uint32_t start = lexer->position;
  uint32_t start_offset = lexer->iter - lexer->source;
  Token tok = {.type = INVALID,
               .start = start,
               .end = start,
               .line = lexer->line_count,
               .category = NORMAL,
               .start_offset = start_offset,
               .column = lexer->position - lexer->line_start -
                         lexer->line_tab_count +
                         lexer->line_tab_count * lexer->tab_width};
  Token previous = lexer->previous;

  if (!is_new_logic_line && previous.category == PREPROC) {
    tok.category = PREPROC;
  }
  char ch = *lexer->iter;
  char peek = iter_peek_c(lexer->iter);
  switch (ch) {
    case '#':
      if (is_new_logic_line) {
        tok.category = PREPROC;
        tok.type = advance(lexer, PREPROC_HASH);
      } else {
        tok.type = advance(lexer, INVALID);
      }
      break;
      // 1 - 3 char cases + comments
      // Comparison operations
    case '<':
      switch (peek) {
        case '=':
          read_next(lexer);
          tok.type = advance(lexer, LE_OP);
          break;
        case '<':
          read_next(lexer);
          if (iter_peek_c(lexer->iter) == '=') {
            read_next(lexer);
            tok.type = advance(lexer, LEFT_ASSIGN);
          } else {
            tok.type = advance(lexer, LEFT_OP);
          }
          break;
        default:
          tok.type = advance(lexer, LEFT_ANGLE);
          break;
      }
      break;
    case '>':
      switch (peek) {
        case '=':
          read_next(lexer);
          tok.type = advance(lexer, GE_OP);
          break;
        case '>':
          read_next(lexer);
          if (iter_peek_c(lexer->iter) == '=') {
            read_next(lexer);
            tok.type = advance(lexer, RIGHT_ASSIGN);
          } else {
            tok.type = advance(lexer, RIGHT_OP);
          }
          break;
        default:
          tok.type = advance(lexer, RIGHT_ANGLE);
          break;
      }
      break;
      // Arithmetic operations + comments
    case '+':
      switch (peek) {
        case '+':
          read_next(lexer);
          tok.type = advance(lexer, INC_OP);
          break;
        case '=':
          read_next(lexer);
          tok.type = advance(lexer, ADD_ASSIGN);
          break;
        default:
          tok.type = advance(lexer, PLUS);
          break;
      }
      break;
    case '-':
      switch (peek) {
        case '-':
          read_next(lexer);
          tok.type = advance(lexer, DEC_OP);
          break;
        case '=':
          read_next(lexer);
          tok.type = advance(lexer, SUB_ASSIGN);
          break;
        default:
          tok.type = advance(lexer, DASH);
          break;
      }
      break;
    case '*':
      if (peek == '=') {
        read_next(lexer);
        tok.type = advance(lexer, MUL_ASSIGN);
      } else {
        tok.type = advance(lexer, STAR);
      }
      break;
    case '/':
      switch (peek) {
        case '=':
          read_next(lexer);
          tok.type = advance(lexer, DIV_ASSIGN);
          break;
        case '*':
          tok.type = advance(lexer, BLOCK_COMMENT);
          tok.category = TRIVIA;
          do {
            read_next(lexer);
          } while (*lexer->iter &&
                   !(*lexer->iter == '*' && iter_peek_c(lexer->iter) == '/'));
          if (*lexer->iter) {
            read_next(lexer);
            read_next(lexer);
          }
          break;
        case '/':
          tok.type = advance(lexer, LINE_COMMENT);
          tok.category = TRIVIA;
          do {
            read_next(lexer);
          } while (*lexer->iter && *lexer->iter != '\n');
          break;
        default:
          tok.type = advance(lexer, SLASH);
      }
      break;
    case '%':
      if (peek == '=') {
        read_next(lexer);
        tok.type = advance(lexer, MOD_ASSIGN);
      } else {
        tok.type = advance(lexer, PERCENT);
      }
      break;
      // Logical and binary operations
    case '&':
      switch (peek) {
        case '&':
          read_next(lexer);
          tok.type = advance(lexer, AND_OP);
          break;
        case '=':
          read_next(lexer);
          tok.type = advance(lexer, AND_ASSIGN);
          break;
        default:
          tok.type = advance(lexer, AMPERSAND);
          break;
      }
      break;
    case '|':
      switch (peek) {
        case '|':
          read_next(lexer);
          tok.type = advance(lexer, OR_OP);
          break;
        case '=':
          read_next(lexer);
          tok.type = advance(lexer, OR_ASSIGN);
          break;
        default:
          tok.type = advance(lexer, VERTICAL_BAR);
          break;
      }
      break;
    case '^':
      switch (peek) {
        case '^':
          read_next(lexer);
          tok.type = advance(lexer, XOR_OP);
          break;
        case '=':
          read_next(lexer);
          tok.type = advance(lexer, XOR_ASSIGN);
          break;
        default:
          tok.type = advance(lexer, CARET);
          break;
      }
      break;
      // cases =, ==, !, !=
    case '=':
      if (peek == '=') {
        read_next(lexer);
        tok.type = advance(lexer, EQ_OP);
      } else {
        tok.type = advance(lexer, EQUAL);
      }
      break;
    case '!':
      if (peek == '=') {
        read_next(lexer);
        tok.type = advance(lexer, NE_OP);
      } else {
        tok.type = advance(lexer, BANG);
      }
      break;
      // Simple cases
    case ',':
      tok.type = advance(lexer, COMMA);
      break;
    case '~':
      tok.type = advance(lexer, TILDE);
      break;
    case ';':
      tok.type = advance(lexer, SEMICOLON);
      break;
    case ':':
      tok.type = advance(lexer, COLON);
      break;
    case '?':
      tok.type = advance(lexer, QUESTION);
      break;
    case '(':
      tok.type = advance(lexer, LEFT_PAREN);
      break;
    case ')':
      tok.type = advance(lexer, RIGHT_PAREN);
      break;
    case '{':
      tok.type = advance(lexer, LEFT_BRACE);
      break;
    case '}':
      tok.type = advance(lexer, RIGHT_BRACE);
      break;
    case '[':
      tok.type = advance(lexer, LEFT_BRACKET);
      break;
    case ']':
      tok.type = advance(lexer, RIGHT_BRACKET);
      break;
    case 0:
      tok.type = advance(lexer, TOKEN_EOF);
      break;
    case '.':
      if (isdigit(peek)) {
        tok.type = read_number(lexer);
      } else {
        tok.type = advance(lexer, DOT);
      }
      break;
    default:
      if (isdigit(ch)) {
        tok.type = read_number(lexer);
      } else if (isalpha(ch) || ch == '_') {
        const char *identifier = read_identifier(lexer);
        // first check if previous token indicates a preprocessor directive
        if (previous.type == PREPROC_HASH) {
          tok.type =
              trie_find(&directives_trie, identifier, lexer->iter - identifier);
        }
        // is not a preprocessor directive? -> is it a keyword?
        if (tok.type == 0) {
          tok.type =
              trie_find(&keywords_trie, identifier, lexer->iter - identifier);
        }
        if (tok.type == 0) {
          tok.type = IDENTIFIER;
        }
        // is not a keyword? -> check if in same category as previous token
        if (tok.type == IDENTIFIER && previous.category == tok.category) {
          if (previous.type == DOT)
            tok.type = FIELD_SELECTION;
          else if (previous.type == STRUCT)
            tok.type = TYPE_NAME;
          else if (previous.type == IDENTIFIER)
            previous.type = TYPE_NAME;
        }
      } else {
        tok.type = INVALID;
        lexer->position = lexer->read_position;
        ++lexer->read_position;
        lexer->iter +=
            utf8_codepoint_length(*(const unsigned char *)lexer->iter);
      }
      break;
  }
  tok.end = lexer->position;

  lexer->previous = tok;
  return previous;
}

static void read_next(Lexer *lexer) {
  uint32_t newline_count = 0;
  while (true) {
    ++lexer->iter;
    lexer->position = lexer->read_position++;
    // if ch == \, then check if line continuation
    if (*lexer->iter == '\\' && iter_move_newline(&lexer->iter)) {
      ++newline_count;
      lexer->position = lexer->read_position++;
      lexer->line_start = lexer->position + 1;
      lexer->line_offset = lexer->iter - lexer->source;
      continue;
    }
    lexer->line_count += newline_count;
    return;
  }
}

static uint32_t skip_whitespace(Lexer *lexer) {
  uint32_t newline_count = lexer->position == 0;
  while (isspace(*lexer->iter)) {
    if (*lexer->iter == '\n') {
      ++newline_count;
      lexer->line_start = lexer->position + 1;
      lexer->line_offset = lexer->iter - lexer->source + 1;
      lexer->line_tab_count = 0;
    } else if (*lexer->iter == '\t') {
      ++lexer->line_tab_count;
    }
    read_next(lexer);
  }
  lexer->line_count += newline_count;
  return newline_count;
}

// Read an identifier
static const char *read_identifier(Lexer *const lexer) {
  const char *const data = lexer->iter;
  while (*lexer->iter == '_' || isalnum(*lexer->iter)) {
    read_next(lexer);
  }
  return data;
}

// Read a number token
static TokenType read_number(Lexer *lexer) {
  TokenType type = INTCONSTANT;
  bool possible_octal = false;
  bool incorrect_octal = false;
  if (*lexer->iter == '0') {
    possible_octal = true;
    read_next(lexer);
    // Check for hexadecimal number
    if (*lexer->iter == 'x' || *lexer->iter == 'X') {
      read_next(lexer);

      // Read hexadecimal part
      while (isxdigit(*lexer->iter)) {
        read_next(lexer);
      }

      return INTCONSTANT;
    }
  }
  // Read integer part
  while (isdigit(*lexer->iter)) {
    if (*lexer->iter > '7') incorrect_octal = true;
    read_next(lexer);
  }

  // Check for decimal part
  if (*lexer->iter == '.') {
    type = FLOATCONSTANT;
    read_next(lexer);
    // Read decimal part
    while (isdigit(*lexer->iter)) {
      read_next(lexer);
    }
  }

  // Check for exponent part
  if ((*lexer->iter == 'e' || *lexer->iter == 'E')) {
    type = FLOATCONSTANT;
    read_next(lexer);

    // Check for sign of exponent
    if (*lexer->iter == '+' || *lexer->iter == '-') {
      read_next(lexer);
    }
    if (!isdigit(*lexer->iter)) return INVALID;
    // Read exponent part
    do {
      read_next(lexer);
    } while ((isdigit(*lexer->iter)));
  }
  if (type == FLOATCONSTANT) {
    if (*lexer->iter == 'f' || *lexer->iter == 'F') read_next(lexer);
    return FLOATCONSTANT;
  }
  // Check for unsigned suffix
  if (*lexer->iter == 'u' || *lexer->iter == 'U') {
    read_next(lexer);
    type = UINTCONSTANT;
  }

  // Integer without suffix
  return possible_octal && incorrect_octal ? INVALID : type;
}

inline static uint8_t utf8_codepoint_length(const uint8_t utf8) {
  return 1 + (utf8 >= 0xC0) + (utf8 >= 0xE0) + (utf8 >= 0xF0);
}

void lexer_init_lookup() {
  for (int i = 0; i < KEYWORDS_LENGTH; ++i) {
    trie_insert(&keywords_trie, KEYWORDS[i].name, KEYWORDS[i].type);
  }
  for (int i = 0; i < DIRECTIVES_LENGTH; ++i) {
    trie_insert(&directives_trie, PREPROC_DIRECTIVES[i].name,
                PREPROC_DIRECTIVES[i].type);
  }
}