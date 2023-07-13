//
// Created by Anton Pieper on 10.07.23.
//
#include <jni.h>
#include <malloc.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>

#include "iter.h"
#include "lexer.h"
#include "trie.h"

typedef struct JNIToken {
  jint type;
  jint start;
  jint end;
  jint line;
  jint column;
} JNIToken;

inline static uint8_t utf8_codepoint_length(const uint8_t utf8) {
  return 1 + (utf8 >= 0xC0) + (utf8 >= 0xE0) + (utf8 >= 0xF0);
}

static jintArray run_lexer(JNIEnv *env, jclass clazz, jstring source,
                           jintArray old_tokens, jint tab_width) {
  const char *string = (*env)->GetStringUTFChars(env, source, NULL);
  Lexer lexer = create_lexer(string, tab_width);
  size_t tokens_capacity = 256;
  size_t tokens_length = 0;
  JNIToken *tokens = malloc(tokens_capacity * sizeof(*tokens));
  if (!tokens) return NULL;
  for (Token current; (current = next_token(&lexer)).type != TOKEN_EOF;) {
    const char *end_offset = &string[current.start_offset];
    uint32_t end_marker = current.end;
    uint32_t start;
    uint32_t column = current.column;
    uint32_t end = current.start;
    uint32_t line = current.line;
    TokenType type = current.type;
    bool unfinished = true;
    do {
      start = end;
      while ((unfinished = end < end_marker) && *end_offset != '\n') {
        ++end;
        end_offset += utf8_codepoint_length(*end_offset);
      }
      if (*end_offset == '\n' && end < end_marker) {
        ++line;
        column = 0;
      }
      JNIToken token = {.type = type,
                        .start = start,
                        .end = end,
                        .line = line,
                        .column = column};
      tokens[tokens_length] = token;
      ++tokens_length;
      if (tokens_length >= tokens_capacity) {
        tokens_capacity = tokens_length + tokens_length / 2;
        JNIToken *new_tokens =
            realloc(tokens, tokens_capacity * sizeof(*tokens));
        if (!new_tokens) {
          free(tokens);
          return NULL;
        }
        tokens = new_tokens;
      }
    } while (unfinished);
  }
  jint array_size = tokens_length * (sizeof(*tokens) / sizeof(jint));
  jint array_capacity = tokens_capacity * (sizeof(*tokens) / sizeof(jint));
  jint old_size = (*env)->GetArrayLength(env, old_tokens);
  jintArray new_tokens = old_tokens;
  if (old_size <= array_size) {
    jintArray array = (*env)->NewIntArray(env, (jsize)array_capacity + 1);
    if (!array) {
      free(tokens);
      return NULL;
    }
    new_tokens = array;
  }
  (*env)->SetIntArrayRegion(env, new_tokens, 0, 1, &array_size);
  (*env)->SetIntArrayRegion(env, new_tokens, 1, array_size,
                            (const jint *)tokens);
  free(tokens);
  (*env)->ReleaseStringUTFChars(env, source, string);
  return new_tokens;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  lexer_init_lookup();
  JNIEnv *env;
  if ((*vm)->GetEnv(vm, (void **)(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }
  // Find your class. JNI_OnLoad is called from the correct class loader context
  // for this to work.
  jclass c = (*env)->FindClass(
      env, "de/markusfisch/android/shadereditor/highlighter/Lexer");
  if (c == NULL) return JNI_ERR;

  // Register your class' native methods.
  static const JNINativeMethod methods[] = {
      {"runLexer", "(Ljava/lang/String;[II)[I", (void *)run_lexer}};
  int rc = (*env)->RegisterNatives(env, c, methods,
                                   sizeof(methods) / sizeof(*methods));
  if (rc != JNI_OK) return rc;

  return JNI_VERSION_1_6;
}
