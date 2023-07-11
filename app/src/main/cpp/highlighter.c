//
// Created by Anton Pieper on 10.07.23.
//

#include <android/log.h>
#include <jni.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>

#include "lexer.h"
#include "trie.h"

typedef enum Highlight {
  HIGHLIGHT_INVALID,
  HIGHLIGHT_KEYWORD,
  HIGHLIGHT_DIRECTIVE,
  HIGHLIGHT_PRIMITIVE,
  HIGHLIGHT_TYPE_VECTOR,
  HIGHLIGHT_TYPE_MATRIX,
  HIGHLIGHT_HIGHLIGHT_ATOMIC_COUNTER,
  HIGHLIGHT_TYPE_SAMPLER,
  HIGHLIGHT_TYPE_IMAGE,
  HIGHLIGHT_TYPE,
  HIGHLIGHT_IDENTIFIER,
  HIGHLIGHT_BUILTIN_FUNCTION,
  HIGHLIGHT_FIELD_SELECTION,
  HIGHLIGHT_NUMBER_LITERAL,
  HIGHLIGHT_BOOL_LITERAL,
  HIGHLIGHT_OPERATOR,
  HIGHLIGHT_PRECISION,
  HIGHLIGHT_COMMENT,
  HIGHLIGHT_CONTROL_FLOW,
  HIGHLIGHT_TYPE_QUALIFIER,
} Highlight;
#define HIGHLIGHT_COUNT (HIGHLIGHT_TYPE_QUALIFIER + 1)
static inline Highlight highlight_group(TokenType type) {
  switch (type) {
    case INVALID:
      return HIGHLIGHT_INVALID;
    case CONST:
    case BOOL:
    case FLOAT:
    case INT:
    case UINT:
    case DOUBLE:
      return HIGHLIGHT_PRIMITIVE;
    case BVEC2:
    case BVEC3:
    case BVEC4:
    case IVEC2:
    case IVEC3:
    case IVEC4:
    case UVEC2:
    case UVEC3:
    case UVEC4:
    case VEC2:
    case VEC3:
    case VEC4:
      return HIGHLIGHT_TYPE_VECTOR;
    case MAT2:
    case MAT3:
    case MAT4:
    case MAT2X2:
    case MAT2X3:
    case MAT2X4:
    case MAT3X2:
    case MAT3X3:
    case MAT3X4:
    case MAT4X2:
    case MAT4X3:
    case MAT4X4:
    case DVEC2:
    case DVEC3:
    case DVEC4:
    case DMAT2:
    case DMAT3:
    case DMAT4:
    case DMAT2X2:
    case DMAT2X3:
    case DMAT2X4:
    case DMAT3X2:
    case DMAT3X3:
    case DMAT3X4:
    case DMAT4X2:
    case DMAT4X3:
    case DMAT4X4:
      return HIGHLIGHT_TYPE_MATRIX;
    case CENTROID:
    case IN:
    case OUT:
    case INOUT:
    case UNIFORM:
    case PATCH:
    case SAMPLE:
    case BUFFER:
    case SHARED:
    case COHERENT:
    case VOLATILE:
    case RESTRICT:
    case READONLY:
    case WRITEONLY:
    case NOPERSPECTIVE:
    case FLAT:
    case SMOOTH:
    case LAYOUT:
      return HIGHLIGHT_TYPE_QUALIFIER;
    case ATOMIC_UINT:
      return HIGHLIGHT_HIGHLIGHT_ATOMIC_COUNTER;
    case SAMPLER2D:
    case SAMPLER3D:
    case SAMPLERCUBE:
    case SAMPLER2DSHADOW:
    case SAMPLERCUBESHADOW:
    case SAMPLER2DARRAY:
    case SAMPLER2DARRAYSHADOW:
    case ISAMPLER2D:
    case ISAMPLER3D:
    case ISAMPLERCUBE:
    case ISAMPLER2DARRAY:
    case USAMPLER2D:
    case USAMPLER3D:
    case USAMPLERCUBE:
    case USAMPLER2DARRAY:
    case SAMPLER1D:
    case SAMPLER1DSHADOW:
    case SAMPLER1DARRAY:
    case SAMPLER1DARRAYSHADOW:
    case ISAMPLER1D:
    case ISAMPLER1DARRAY:
    case USAMPLER1D:
    case USAMPLER1DARRAY:
    case SAMPLER2DRECT:
    case SAMPLER2DRECTSHADOW:
    case ISAMPLER2DRECT:
    case USAMPLER2DRECT:
    case SAMPLERBUFFER:
    case ISAMPLERBUFFER:
    case USAMPLERBUFFER:
    case SAMPLERCUBEARRAY:
    case SAMPLERCUBEARRAYSHADOW:
    case ISAMPLERCUBEARRAY:
    case USAMPLERCUBEARRAY:
    case SAMPLER2DMS:
    case ISAMPLER2DMS:
    case USAMPLER2DMS:
    case SAMPLER2DMSARRAY:
    case ISAMPLER2DMSARRAY:
    case USAMPLER2DMSARRAY:
      return HIGHLIGHT_TYPE_SAMPLER;
    case IMAGE2D:
    case IIMAGE2D:
    case UIMAGE2D:
    case IMAGE3D:
    case IIMAGE3D:
    case UIMAGE3D:
    case IMAGECUBE:
    case IIMAGECUBE:
    case UIMAGECUBE:
    case IMAGEBUFFER:
    case IIMAGEBUFFER:
    case UIMAGEBUFFER:
    case IMAGE2DARRAY:
    case IIMAGE2DARRAY:
    case UIMAGE2DARRAY:
    case IMAGECUBEARRAY:
    case IIMAGECUBEARRAY:
    case UIMAGECUBEARRAY:
    case IMAGE1D:
    case IIMAGE1D:
    case UIMAGE1D:
    case IMAGE1DARRAY:
    case IIMAGE1DARRAY:
    case UIMAGE1DARRAY:
    case IMAGE2DRECT:
    case IIMAGE2DRECT:
    case UIMAGE2DRECT:
    case IMAGE2DMS:
    case IIMAGE2DMS:
    case UIMAGE2DMS:
    case IMAGE2DMSARRAY:
    case IIMAGE2DMSARRAY:
    case UIMAGE2DMSARRAY:
      return HIGHLIGHT_TYPE_IMAGE;
    case STRUCT:
    case VOID:
      return HIGHLIGHT_KEYWORD;
    case WHILE:
    case BREAK:
    case CONTINUE:
    case DO:
    case ELSE:
    case FOR:
    case IF:
    case DISCARD:
    case RETURN:
    case SWITCH:
    case CASE:
    case DEFAULT:
    case SUBROUTINE:
      return HIGHLIGHT_CONTROL_FLOW;
    case IDENTIFIER:
      return HIGHLIGHT_IDENTIFIER;
    case BUILTIN_FUNCTION:
      return HIGHLIGHT_BUILTIN_FUNCTION;
    case TYPE_NAME:
      return HIGHLIGHT_TYPE;
    case FLOATCONSTANT:
    case DOUBLECONSTANT:
    case INTCONSTANT:
    case UINTCONSTANT:
      return HIGHLIGHT_NUMBER_LITERAL;
    case BOOLCONSTANT:
      return HIGHLIGHT_BOOL_LITERAL;
    case FIELD_SELECTION:
      return HIGHLIGHT_FIELD_SELECTION;
    case LEFT_OP:
    case RIGHT_OP:
    case INC_OP:
    case DEC_OP:
    case LE_OP:
    case GE_OP:
    case EQ_OP:
    case NE_OP:
    case AND_OP:
    case OR_OP:
    case XOR_OP:
    case MUL_ASSIGN:
    case DIV_ASSIGN:
    case ADD_ASSIGN:
    case MOD_ASSIGN:
    case LEFT_ASSIGN:
    case RIGHT_ASSIGN:
    case AND_ASSIGN:
    case XOR_ASSIGN:
    case OR_ASSIGN:
    case SUB_ASSIGN:
    case LEFT_PAREN:
    case RIGHT_PAREN:
    case LEFT_BRACKET:
    case RIGHT_BRACKET:
    case LEFT_BRACE:
    case RIGHT_BRACE:
    case DOT:
    case COMMA:
    case COLON:
    case EQUAL:
    case SEMICOLON:
    case BANG:
    case DASH:
    case TILDE:
    case PLUS:
    case STAR:
    case SLASH:
    case PERCENT:
    case LEFT_ANGLE:
    case RIGHT_ANGLE:
    case VERTICAL_BAR:
    case CARET:
    case AMPERSAND:
    case QUESTION:
      return HIGHLIGHT_OPERATOR;
    case INVARIANT:
    case PRECISE:
    case HIGH_PRECISION:
    case MEDIUM_PRECISION:
    case LOW_PRECISION:
    case PRECISION:
      return HIGHLIGHT_PRECISION;
    case BLOCK_COMMENT:
    case LINE_COMMENT:
      return HIGHLIGHT_COMMENT;
    case PREPROC_HASH:
    case PREPROC_IF:
    case PREPROC_IFDEF:
    case PREPROC_IFNDEF:
    case PREPROC_ELIF:
    case PREPROC_ELSE:
    case PREPROC_ENDIF:
    case PREPROC_INCLUDE:
    case PREPROC_DEFINE:
    case PREPROC_UNDEF:
    case PREPROC_LINE:
    case PREPROC_ERROR:
    case PREPROC_PRAGMA:
      return HIGHLIGHT_DIRECTIVE;
    case TOKEN_EOF:
      return HIGHLIGHT_INVALID;
  }
}
static int colors[HIGHLIGHT_COUNT];

static void set_colors(JNIEnv *env, jclass clazz, jintArray java_colors) {
  jint *color_view = (*env)->GetIntArrayElements(env, java_colors, NULL);
  memcpy(colors, color_view, sizeof(colors));
  (*env)->ReleaseIntArrayElements(env, java_colors, color_view, JNI_ABORT);
}

static jmethodID foreground_constructor;
static jclass foreground_class;
static jclass spanned_class;
static jmethodID set_span;
static jfieldID spanned_exclusive_exclusive_id;

static void highlight(JNIEnv *env, jclass clazz, jobject spannable,
                      jstring source) {
  const char *string = (*env)->GetStringUTFChars(env, source, NULL);
  Lexer lexer = create_lexer(string);
  jint exclusive_exclusive = (*env)->GetStaticIntField(
      env, spanned_class, spanned_exclusive_exclusive_id);
  // char buffer[64];
  for (Token current; (current = next_token(&lexer)).type != TOKEN_EOF;) {
    Highlight highlight = highlight_group(current.type);
    jobject span = (*env)->NewObject(env, foreground_class,
                                     foreground_constructor, colors[highlight]);
    // sprintf(buffer, "%d, %d %x", current.start, current.end, colors[highlight]);
    // __android_log_write(ANDROID_LOG_ERROR, "JNI", buffer);
    (*env)->CallVoidMethod(env, spannable, set_span, span, (jint)current.start,
                           (jint)current.end, exclusive_exclusive);
  }
  (*env)->ReleaseStringUTFChars(env, source, string);
}
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  lexer_init_lookup();
  JNIEnv *env;
  if ((*vm)->GetEnv(vm, (void **)(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }
  jclass spannable_class = (*env)->FindClass(env, "android/text/Spannable");
  spanned_class =
      (*env)->NewGlobalRef(env, (*env)->FindClass(env, "android/text/Spanned"));
  spanned_exclusive_exclusive_id = (*env)->GetStaticFieldID(
      env, spanned_class, "SPAN_EXCLUSIVE_EXCLUSIVE", "I");
  foreground_class = (*env)->NewGlobalRef(
      env, (*env)->FindClass(env, "android/text/style/ForegroundColorSpan"));
  foreground_constructor =
      (*env)->GetMethodID(env, foreground_class, "<init>", "(I)V");
  set_span = (*env)->GetMethodID(env, spannable_class, "setSpan",
                                 "(Ljava/lang/Object;III)V");
  // Find your class. JNI_OnLoad is called from the correct class loader context
  // for this to work.
  jclass c = (*env)->FindClass(
      env, "de/markusfisch/android/shadereditor/highlighter/Highlighter");
  if (c == NULL) return JNI_ERR;

  // Register your class' native methods.
  static const JNINativeMethod methods[] = {
      {"highlight", "(Landroid/text/Spannable;Ljava/lang/String;)V",
       (void *)highlight},
      {"set_colors", "([I)V", (void *)set_colors}};
  int rc = (*env)->RegisterNatives(env, c, methods,
                                   sizeof(methods) / sizeof(*methods));
  if (rc != JNI_OK) return rc;

  return JNI_VERSION_1_6;
}
