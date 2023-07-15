//
// Created by Anton Pieper on 10.07.23.
//
#include <jni.h>

#include <string>
#include <vector>

#include "iter.h"
#include "lexer.h"
#include "trie.h"

struct JNIToken {
  jint type;
  jint start;
  jint end;
  jint line;
  jint column;
};

static inline uint8_t utf8_codepoint_length(uint8_t utf8) {
  return 1 + (utf8 >= 0xC0) + (utf8 >= 0xE0) + (utf8 >= 0xF0);
}

static jintArray run_lexer(JNIEnv *env, jclass clazz, jstring source,
                           jintArray old_tokens, jint tab_width) {
  const char *string = env->GetStringUTFChars(source, nullptr);
  Lexer lexer = create_lexer(string, tab_width);
  std::vector<JNIToken> tokens;
  for (Token current; (current = next_token(&lexer)).type != TOKEN_EOF;) {
    const char *end_offset = &string[current.start_offset];
    uint32_t end_marker = current.end;
    jint start;
    jint column = current.column;
    jint end = current.start;
    jint line = current.line;
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
      tokens.push_back({.type = type,
                        .start = start,
                        .end = end,
                        .line = line,
                        .column = column});
    } while (unfinished);
  }
  constexpr size_t element_size_in_jint =
      sizeof(decltype(tokens)::value_type) / sizeof(jint);
  jint array_size = tokens.size() * element_size_in_jint;
  jint array_capacity = tokens.capacity() * element_size_in_jint;
  jint old_size = env->GetArrayLength(old_tokens);
  jintArray new_tokens = old_tokens;
  if (old_size <= array_size) {
    jintArray array = env->NewIntArray(array_capacity + 1);
    if (!array) {
      return nullptr;
    }
    new_tokens = array;
  }
  env->SetIntArrayRegion(new_tokens, 0, 1, &array_size);
  env->SetIntArrayRegion(new_tokens, 1, array_size,
                         reinterpret_cast<const jint *>(tokens.data()));
  env->ReleaseStringUTFChars(source, string);
  return new_tokens;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  lexer_init_lookup();
  JNIEnv *env;
  if (vm->GetEnv((void **)(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }
  // Find your class. JNI_OnLoad is called from the correct class loader context
  // for this to work.
  jclass c =
      env->FindClass("de/markusfisch/android/shadereditor/highlighter/Lexer");
  if (c == nullptr) return JNI_ERR;

  // Register your class' native methods.
  static const JNINativeMethod methods[] = {
      {"runLexer", "(Ljava/lang/String;[II)[I",
       reinterpret_cast<void *>(run_lexer)}};
  int rc = env->RegisterNatives(c, methods, sizeof(methods) / sizeof(*methods));
  if (rc != JNI_OK) return rc;

  return JNI_VERSION_1_6;
}
