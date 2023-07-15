//
// Created by Anton Pieper on 10.07.23.
//
#include <jni.h>

#include <string>
#include <vector>

#include "iter.h"
#include "lexer.h"
#include "trie.h"

#include <android/trace.h>
#include <dlfcn.h>
namespace MyTracing {
void *(*ATrace_beginSection)(const char *sectionName);
void *(*ATrace_endSection)(void);

typedef void *(*fp_ATrace_beginSection)(const char *sectionName);
typedef void *(*fp_ATrace_endSection)(void);
#define ATRACE_NAME(name) ScopedTrace ___tracer(name)

// ATRACE_CALL is an ATRACE_NAME that uses the current function name.
#define ATRACE_CALL() ATRACE_NAME(__FUNCTION__)

class ScopedTrace {
 public:
  inline ScopedTrace(const char *name) {
    ATrace_beginSection(name);
  }

  inline ~ScopedTrace() {
    ATrace_endSection();
  }
};
}

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
  MyTracing::ATRACE_CALL();
  const char *string = env->GetStringUTFChars(source, nullptr);
  Lexer lexer = create_lexer(string, tab_width);
  std::vector<JNIToken> tokens;
  MyTracing::ScopedTrace tokenization{"Tokenization"};
  int i = 0;
  for (Token current; (current = next_token(&lexer)).type != TOKEN_EOF;) {
    std::string s{"Token " + std::to_string(i++)};
    MyTracing::ScopedTrace token{s.data()};
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
  // Retrieve a handle to libandroid.
  void *lib = dlopen("libandroid.so", RTLD_NOW || RTLD_LOCAL);

  // Access the native tracing functions.
  if (lib != NULL) {
    // Use dlsym() to prevent crashes on devices running Android 5.1
    // (API level 22) or lower.
    MyTracing::ATrace_beginSection = reinterpret_cast<MyTracing::fp_ATrace_beginSection>(
        dlsym(lib, "ATrace_beginSection"));
    MyTracing::ATrace_endSection = reinterpret_cast<MyTracing::fp_ATrace_endSection>(
        dlsym(lib, "ATrace_endSection"));
  }
  return JNI_VERSION_1_6;
}
