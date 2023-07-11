//
// Created by Anton Pieper on 10.07.23.
//

#include <android/log.h>
#include <jni.h>
#include <malloc.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>

#include "lexer.h"
#include "trie.h"

static jintArray highlight(JNIEnv *env, jclass clazz, jstring source) {
  const char *string = (*env)->GetStringUTFChars(env, source, NULL);
  Lexer lexer = create_lexer(string);
  size_t tokens_capacity = 3 * 256;
  size_t tokens_length = 0;
  jint *tokens = malloc(tokens_capacity * sizeof(*tokens));
  if (!tokens) return NULL;
  for (Token current; (current = next_token(&lexer)).type != TOKEN_EOF;) {
    tokens[tokens_length] = current.type;
    tokens[tokens_length + 1] = current.start;
    tokens[tokens_length + 2] = current.end;
    tokens_length += 3;
    //    (*env)->CallVoidMethod(env, spannable, set_span, span,
    //    (jint)current.start,
    //                           (jint)current.end, exclusive_exclusive);
    if (tokens_length >= tokens_capacity) {
      tokens_capacity = tokens_length + tokens_length / 2;
      tokens_capacity += tokens_capacity % 3;  // round up to multiple of 3
      jint *new_tokens = realloc(tokens, tokens_capacity * sizeof(*tokens));
      if (!new_tokens) {
        free(tokens);
        return NULL;
      }
      tokens = new_tokens;
    }
  }
  jintArray array = (*env)->NewIntArray(env, tokens_length);
  (*env)->SetIntArrayRegion(env, array, 0, tokens_length, tokens);
  free(tokens);
  (*env)->ReleaseStringUTFChars(env, source, string);
  return array;
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
      env,
      "de/markusfisch/android/shadereditor/highlighter/Highlighter$Native");
  if (c == NULL) return JNI_ERR;

  // Register your class' native methods.
  static const JNINativeMethod methods[] = {
      {"highlight", "(Ljava/lang/String;)[I", (void *)highlight}};
  int rc = (*env)->RegisterNatives(env, c, methods,
                                   sizeof(methods) / sizeof(*methods));
  if (rc != JNI_OK) return rc;

  return JNI_VERSION_1_6;
}
