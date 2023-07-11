//
// Created by Anton Pieper on 10.07.23.
//
#include "iter.h"

#include <stdbool.h>

char iter_peek(const char *iter) {
  if (*iter)
    return iter[1];
  else
    return 0;
}

char iter_peek_c(const char *iter) {
  char ch = 0;
  do {
  if (*iter)
    ch =  iter[1];
  if (ch != '\\') break;
  ++iter;
  } while (iter_move_newline(&iter));
  return ch;
}

bool iter_move_newline(const char **const iter) {
  const char *start = *iter;
  // is line continuation -> iter moved
  return (*iter = iter_next_newline(start)) != start;
}

const char *iter_next_c(const char *iter) {
  do {
    ++iter;
  } while (*iter == '\\' && iter_move_newline(&iter));
  return iter;
}

const char *iter_next_newline(const char *iter) {
  char peek = iter_peek(iter);
  // is next \r or \n? -> iterate
  if (peek == '\r' || peek == '\n') {
    ++iter;
    char second_peek = iter_peek(iter);
    // is next \r\n or \n\r? -> iterate
    if (second_peek != peek && (second_peek == '\r' || second_peek == '\n'))
      ++iter;
  }
  // is line continuation -> iter moved
  return iter;
}