package de.markusfisch.android.shadereditor.highlighter;

import android.content.Context;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public final class Lexer {

	private Lexer() {
	}


	/**
	 * Lex the source code.
	 * @param source The source code to lex (MUST use LF line endings)
	 * @return An int[] that has following values: {type, start, end, line, column ...}.
	 */
	public static native @NonNull int[] runLexer(@NonNull String source, @NonNull int[] old, int tabWidth);

}
