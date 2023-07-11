package de.markusfisch.android.shadereditor.highlighter;

import android.content.Context;
import android.text.Spannable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import de.markusfisch.android.shadereditor.R;

public class Highlighter {
    // order has to match the one defined in `highlighter.c`
    enum Highlight {
        HIGHLIGHT_INVALID(R.color.syntax_invalid),
        HIGHLIGHT_KEYWORD(R.color.syntax_keyword),
        HIGHLIGHT_DIRECTIVE(R.color.syntax_directive),
        HIGHLIGHT_PRIMITIVE(R.color.syntax_type_primitive),
        HIGHLIGHT_TYPE_VECTOR(R.color.syntax_type_vector),
        HIGHLIGHT_TYPE_MATRIX(R.color.syntax_type_matrix),
        HIGHLIGHT_HIGHLIGHT_ATOMIC_COUNTER(R.color.syntax_atomic_counter),
        HIGHLIGHT_TYPE_SAMPLER(R.color.syntax_type_sampler),
        HIGHLIGHT_TYPE_IMAGE(R.color.syntax_type_image),
        HIGHLIGHT_TYPE(R.color.syntax_type),
        HIGHLIGHT_IDENTIFIER(R.color.syntax_identifier),
        HIGHLIGHT_BUILTIN_FUNCTION(R.color.syntax_builtin_function),

        HIGHLIGHT_FIELD_SELECTION(R.color.syntax_field_selection),
        HIGHLIGHT_NUMBER_LITERAL(R.color.syntax_number_literal),
        HIGHLIGHT_BOOL_LITERAL(R.color.syntax_bool_literal),
        HIGHLIGHT_OPERATOR(R.color.syntax_operator),
        HIGHLIGHT_PRECISION(R.color.syntax_precision),
        HIGHLIGHT_COMMENT(R.color.syntax_comment),
        HIGHLIGHT_CONTROL_FLOW(R.color.syntax_control_flow),
        HIGHLIGHT_TYPE_QUALIFIER(R.color.syntax_type_qualifier);
        final int id;

        Highlight(int id) {
            this.id = id;
        }
    }

    private Highlighter() {
    }

    public static native void highlight(@NonNull Spannable spannable, @NonNull String source);

    public static void init_colors(@NonNull Context context) {
        int[] colors = new int[Highlight.values().length];
        for (Highlight highlight : Highlight.values()) {
            colors[highlight.ordinal()] = ContextCompat.getColor(context, highlight.id);
        }
        set_colors(colors);
    }

    private static native void set_colors(@NonNull int[] colors);
}
