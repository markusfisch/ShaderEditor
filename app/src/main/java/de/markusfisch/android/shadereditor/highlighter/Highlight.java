package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.syntax.Style;

public final class Highlight {
	private Highlight() {
	}

	@ColorRes
	public static int colorOf(@NonNull Style style) {
		switch (style) {
			case KEYWORD:
				return R.color.syntax_keyword;
			case DIRECTIVE:
				return R.color.syntax_directive;
			case PRIMITIVE:
				return R.color.syntax_type_primitive;
			case TYPE_VECTOR:
				return R.color.syntax_type_vector;
			case TYPE_MATRIX:
				return R.color.syntax_type_matrix;
			case ATOMIC_COUNTER:
				return R.color.syntax_atomic_counter;
			case TYPE_SAMPLER:
				return R.color.syntax_type_sampler;
			case TYPE_IMAGE:
				return R.color.syntax_type_image;
			case TYPE:
				return R.color.syntax_type;
			case IDENTIFIER:
				return R.color.syntax_identifier;
			case BUILTIN_FUNCTION:
				return R.color.syntax_builtin_function;
			case FIELD_SELECTION:
				return R.color.syntax_field_selection;
			case NUMBER:
				return R.color.syntax_number_literal;
			case BOOL:
				return R.color.syntax_bool_literal;
			case OPERATOR:
				return R.color.syntax_operator;
			case PRECISION:
				return R.color.syntax_precision;
			case COMMENT:
				return R.color.syntax_comment;
			case CONTROL_FLOW:
				return R.color.syntax_control_flow;
			case TYPE_QUALIFIER:
				return R.color.syntax_type_qualifier;
			default:
				return R.color.syntax_invalid;
		}
	}
}
