package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;

import de.markusfisch.android.shadereditor.R;


public enum Highlight {
	HIGHLIGHT_INVALID(() -> R.color.syntax_invalid),
	HIGHLIGHT_KEYWORD(() -> R.color.syntax_keyword),
	HIGHLIGHT_DIRECTIVE(() -> R.color.syntax_directive),
	HIGHLIGHT_PRIMITIVE(() -> R.color.syntax_type_primitive),
	HIGHLIGHT_TYPE_VECTOR(() -> R.color.syntax_type_vector),
	HIGHLIGHT_TYPE_MATRIX(() -> R.color.syntax_type_matrix),
	HIGHLIGHT_HIGHLIGHT_ATOMIC_COUNTER(() -> R.color.syntax_atomic_counter),
	HIGHLIGHT_TYPE_SAMPLER(() -> R.color.syntax_type_sampler),
	HIGHLIGHT_TYPE_IMAGE(() -> R.color.syntax_type_image),
	HIGHLIGHT_TYPE(() -> R.color.syntax_type),
	HIGHLIGHT_IDENTIFIER(() -> R.color.syntax_identifier),
	HIGHLIGHT_BUILTIN_FUNCTION(() -> R.color.syntax_builtin_function),

	HIGHLIGHT_FIELD_SELECTION(() -> R.color.syntax_field_selection),
	HIGHLIGHT_NUMBER_LITERAL(() -> R.color.syntax_number_literal),
	HIGHLIGHT_BOOL_LITERAL(() -> R.color.syntax_bool_literal),
	HIGHLIGHT_OPERATOR(() -> R.color.syntax_operator),
	HIGHLIGHT_PRECISION(() -> R.color.syntax_precision),
	HIGHLIGHT_COMMENT(() -> R.color.syntax_comment),
	HIGHLIGHT_CONTROL_FLOW(() -> R.color.syntax_control_flow),
	HIGHLIGHT_TYPE_QUALIFIER(() -> R.color.syntax_type_qualifier);

	private final @NonNull IdSupplier id;

	Highlight(@NonNull IdSupplier id) {
		this.id = id;
	}

	public static Highlight from(@NonNull TokenType type) {
		switch (type) {
			case INVALID:
			case EOF:
				return Highlight.HIGHLIGHT_INVALID;
			case CONST:
			case BOOL:
			case FLOAT:
			case INT:
			case UINT:
			case DOUBLE:
				return Highlight.HIGHLIGHT_PRIMITIVE;
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
				return Highlight.HIGHLIGHT_TYPE_VECTOR;
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
				return Highlight.HIGHLIGHT_TYPE_MATRIX;
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
				return Highlight.HIGHLIGHT_TYPE_QUALIFIER;
			case ATOMIC_UINT:
				return Highlight.HIGHLIGHT_HIGHLIGHT_ATOMIC_COUNTER;
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
				return Highlight.HIGHLIGHT_TYPE_SAMPLER;
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
				return Highlight.HIGHLIGHT_TYPE_IMAGE;
			case STRUCT:
			case VOID:
				return Highlight.HIGHLIGHT_KEYWORD;
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
				return Highlight.HIGHLIGHT_CONTROL_FLOW;
			case IDENTIFIER:
				return Highlight.HIGHLIGHT_IDENTIFIER;
			case BUILTIN_FUNCTION:
				return Highlight.HIGHLIGHT_BUILTIN_FUNCTION;
			case TYPE_NAME:
				return Highlight.HIGHLIGHT_TYPE;
			case FLOATCONSTANT:
			case DOUBLECONSTANT:
			case INTCONSTANT:
			case UINTCONSTANT:
				return Highlight.HIGHLIGHT_NUMBER_LITERAL;
			case BOOLCONSTANT:
				return Highlight.HIGHLIGHT_BOOL_LITERAL;
			case FIELD_SELECTION:
				return Highlight.HIGHLIGHT_FIELD_SELECTION;
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
				return Highlight.HIGHLIGHT_OPERATOR;
			case INVARIANT:
			case PRECISE:
			case HIGH_PRECISION:
			case MEDIUM_PRECISION:
			case LOW_PRECISION:
			case PRECISION:
				return Highlight.HIGHLIGHT_PRECISION;
			case BLOCK_COMMENT:
			case LINE_COMMENT:
				return Highlight.HIGHLIGHT_COMMENT;
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
			case PREPROC_VERSION:
				return Highlight.HIGHLIGHT_DIRECTIVE;
		}
		return Highlight.HIGHLIGHT_INVALID;
	}

	public int id() {
		return id.getId();
	}

	@FunctionalInterface
	private interface IdSupplier {
		int getId();
	}
}
