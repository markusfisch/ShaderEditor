package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

import java.util.Iterator;


public class Lexer implements Iterable<Token> {
	private class TokenIterator implements Iterator<Token> {
		@NonNull
		Token current = new Token();

		@Override
		public boolean hasNext() {
			return current.type() != TokenType.EOF;
		}

		@Override
		public @NonNull Token next() {
			current = nextToken();
			return current;
		}
	}

	@Override
	public @NonNull Iterator<Token> iterator() {
		return new TokenIterator();
	}

	private static class Identifier {
		@NonNull
		public String source() {
			return source;
		}

		public int start() {
			return start;
		}

		public int length() {
			return length;
		}

		private final @NonNull String source;
		private final int start;
		private final int length;

		public Identifier(@NonNull String source, int start, int length) {
			this.source = source;
			this.start = start;
			this.length = length;
		}
	}

	@NonNull
	Token previous = new Token();
	private final @NonNull String source;
	int iter;  // codepoint index
	int lineStart;
	int lineOffset;
	int position;       // current position byte offset
	int readPosition;  // read position byte offset
	int lineCount;
	int tabWidth;
	int lineTabCount;

	private static class KeywordToken {
		private final @NonNull TokenType type;
		private final @NonNull String name;

		public KeywordToken(@NonNull TokenType type, @NonNull String name) {
			this.type = type;
			this.name = name;
		}
	}

	private static final TrieNode KEYWORDS_TRIE = new TrieNode();
	private static final TrieNode DIRECTIVES_TRIE = new TrieNode();

	public Lexer(@NonNull String input, int tabWidth) {
		this.source = input;
		this.readPosition = 1;
		this.tabWidth = tabWidth;
		this.lineCount = 1;
		nextToken();
	}

	private @NonNull TokenType advance(@NonNull TokenType type) {
		readNext();
		return type;
	}

	public @NonNull Token nextToken() {
		boolean isFirstLine = this.position == 0;
		boolean isNewLogicLine = skipWhitespace() != 0 || isFirstLine;
		int start = this.position;
		int startOffset = this.iter;
		Token tok = new Token()
			.setType(TokenType.INVALID)
			.setStart(start)
			.setEnd(start)
			.setLine((short) this.lineCount)
			.setCategory(Token.Category.NORMAL)
			.setStartOffset(startOffset)
			.setColumn((short) (this.position - this.lineStart - this.lineTabCount + this.lineTabCount * this.tabWidth));
		Token previous = this.previous;

		if (!isNewLogicLine && previous.category() == Token.Category.PREPROC) {
			tok.setCategory(Token.Category.PREPROC);
		}
		char ch = getCurrentChar();
		char peek = peekNextChar();
		switch (ch) {
			case '#':
				if (isNewLogicLine) {
					tok.setCategory(Token.Category.PREPROC);
					tok.setType(advance(TokenType.PREPROC_HASH));
				} else {
					tok.setType(advance(TokenType.INVALID));
				}
				break;
			// 1 - 3 char cases + comments
			// Comparison operations
			case '<':
				switch (peek) {
					case '=':
						readNext();
						tok.setType(advance(TokenType.LE_OP));
						break;
					case '<':
						readNext();
						if (peekNextChar() == '=') {
							readNext();
							tok.setType(advance(TokenType.LEFT_ASSIGN));
						} else {
							tok.setType(advance(TokenType.LEFT_OP));
						}
						break;
					default:
						tok.setType(advance(TokenType.LEFT_ANGLE));
						break;
				}
				break;
			case '>':
				switch (peek) {
					case '=':
						readNext();
						tok.setType(advance(TokenType.GE_OP));
						break;
					case '>':
						readNext();
						if (peekNextChar() == '=') {
							readNext();
							tok.setType(advance(TokenType.RIGHT_ASSIGN));
						} else {
							tok.setType(advance(TokenType.RIGHT_OP));
						}
						break;
					default:
						tok.setType(advance(TokenType.RIGHT_ANGLE));
						break;
				}
				break;
			// Arithmetic operations + comments
			case '+':
				switch (peek) {
					case '+':
						readNext();
						tok.setType(advance(TokenType.INC_OP));
						break;
					case '=':
						readNext();
						tok.setType(advance(TokenType.ADD_ASSIGN));
						break;
					default:
						tok.setType(advance(TokenType.PLUS));
						break;
				}
				break;
			case '-':
				switch (peek) {
					case '-':
						readNext();
						tok.setType(advance(TokenType.DEC_OP));
						break;
					case '=':
						readNext();
						tok.setType(advance(TokenType.SUB_ASSIGN));
						break;
					default:
						tok.setType(advance(TokenType.DASH));
						break;
				}
				break;
			case '*':
				if (peek == '=') {
					readNext();
					tok.setType(advance(TokenType.MUL_ASSIGN));
				} else {
					tok.setType(advance(TokenType.STAR));
				}
				break;
			case '/':
				switch (peek) {
					case '=':
						readNext();
						tok.setType(advance(TokenType.DIV_ASSIGN));
						break;
					case '*':
						tok.setType(advance(TokenType.BLOCK_COMMENT));
						tok.setCategory(Token.Category.TRIVIA);
						do {
							readNext();
							handleLineColumn();
						} while (CharIterator.isValid(this.iter, this.source) && !(getCurrentChar() == '*' && peekNextChar() == '/'));
						if (CharIterator.isValid(this.iter, this.source)) {
							readNext();
							readNext();
						}
						break;
					case '/':
						tok.setType(advance(TokenType.LINE_COMMENT));
						tok.setCategory(Token.Category.TRIVIA);
						do {
							readNext();
						} while (CharIterator.isValid(this.iter, this.source) && getCurrentChar() != '\n');
						break;
					default:
						tok.setType(advance(TokenType.SLASH));
				}
				break;
			case '%':
				if (peek == '=') {
					readNext();
					tok.setType(advance(TokenType.MOD_ASSIGN));
				} else {
					tok.setType(advance(TokenType.PERCENT));
				}
				break;
			// Logical and binary operations
			case '&':
				switch (peek) {
					case '&':
						readNext();
						tok.setType(advance(TokenType.AND_OP));
						break;
					case '=':
						readNext();
						tok.setType(advance(TokenType.AND_ASSIGN));
						break;
					default:
						tok.setType(advance(TokenType.AMPERSAND));
						break;
				}
				break;
			case '|':
				switch (peek) {
					case '|':
						readNext();
						tok.setType(advance(TokenType.OR_OP));
						break;
					case '=':
						readNext();
						tok.setType(advance(TokenType.OR_ASSIGN));
						break;
					default:
						tok.setType(advance(TokenType.VERTICAL_BAR));
						break;
				}
				break;
			case '^':
				switch (peek) {
					case '^':
						readNext();
						tok.setType(advance(TokenType.XOR_OP));
						break;
					case '=':
						readNext();
						tok.setType(advance(TokenType.XOR_ASSIGN));
						break;
					default:
						tok.setType(advance(TokenType.CARET));
						break;
				}
				break;
			// cases =, ==, !, !=
			case '=':
				if (peek == '=') {
					readNext();
					tok.setType(advance(TokenType.EQ_OP));
				} else {
					tok.setType(advance(TokenType.EQUAL));
				}
				break;
			case '!':
				if (peek == '=') {
					readNext();
					tok.setType(advance(TokenType.NE_OP));
				} else {
					tok.setType(advance(TokenType.BANG));
				}
				break;
			// Simple cases
			case ',':
				tok.setType(advance(TokenType.COMMA));
				break;
			case '~':
				tok.setType(advance(TokenType.TILDE));
				break;
			case ';':
				tok.setType(advance(TokenType.SEMICOLON));
				break;
			case ':':
				tok.setType(advance(TokenType.COLON));
				break;
			case '?':
				tok.setType(advance(TokenType.QUESTION));
				break;
			case '(':
				tok.setType(advance(TokenType.LEFT_PAREN));
				break;
			case ')':
				tok.setType(advance(TokenType.RIGHT_PAREN));
				break;
			case '{':
				tok.setType(advance(TokenType.LEFT_BRACE));
				break;
			case '}':
				tok.setType(advance(TokenType.RIGHT_BRACE));
				break;
			case '[':
				tok.setType(advance(TokenType.LEFT_BRACKET));
				break;
			case ']':
				tok.setType(advance(TokenType.RIGHT_BRACKET));
				break;
			case CharIterator.INVALID:
				tok.setType(TokenType.EOF);
				break;
			case '.':
				if (Character.isDigit(peek)) {
					tok.setType(readNumber());
				} else {
					tok.setType(advance(TokenType.DOT));
				}
				break;
			default:
				if (Character.isDigit(ch)) {
					tok.setType(readNumber());
				} else if (Character.isLetter(ch) || ch == '_') {
					Identifier identifier = readIdentifier();
					// first check if previous token indicates a preprocessor directive
					if (previous.type() == TokenType.PREPROC_HASH) {
						tok.setType(TokenType.values()[DIRECTIVES_TRIE.find(identifier.source(), identifier.start(), identifier.length())]);
					}
					// is not a preprocessor directive? -> is it a keyword?
					if (tok.type() == TokenType.INVALID) {
						tok.setType(TokenType.values()[KEYWORDS_TRIE.find(identifier.source(), identifier.start(), identifier.length())]);
					}
					if (tok.type() == TokenType.INVALID) {
						tok.setType(TokenType.IDENTIFIER);
					}
					// is not a keyword? -> check if in same category as previous token
					if (tok.type() == TokenType.IDENTIFIER && previous.category() == tok.category()) {
						if (previous.type() == TokenType.DOT)
							tok.setType(TokenType.FIELD_SELECTION);
						else if (previous.type() == TokenType.STRUCT)
							tok.setType(TokenType.TYPE_NAME);
						else if (previous.type() == TokenType.IDENTIFIER)
							previous.setType(TokenType.TYPE_NAME);
					}
				} else {
					tok.setType(TokenType.INVALID);
					this.position = this.readPosition;
					++this.readPosition;

					this.iter += CharIterator.isSurrogate(this.getCurrentChar()) ? 2 : 1;
				}
				break;
		}
		tok.setEndOffset(this.iter)
			.setEnd(this.position);

		this.previous = tok;
		return previous;
	}

	private char peekNextChar() {
		return CharIterator.peekC(this.iter, this.source);
	}

	@Contract(pure = true)
	private char getCurrentChar() {
		return CharIterator.ch(this.iter, this.source);
	}

	private void handleLineColumn() {
		if (getCurrentChar() == '\n') {
			++this.lineCount;
			this.lineStart = this.position + 1;
			this.lineOffset = this.iter + 1;
			this.lineTabCount = 0;
		} else if (getCurrentChar() == '\t') {
			++this.lineTabCount;
		}
	}

	private void readNext() {
		while (true) {
			++this.iter;
			this.position = this.readPosition++;
			int oldPosition = this.position;
			// if ch == \, then check if line continuation
			if (getCurrentChar() == '\\' && CharIterator.hasMoved(oldPosition, this.position = CharIterator.nextNewline(this.position, this.source))) {
				handleLineColumn();
				continue;
			}
			return;
		}
	}

	private int skipWhitespace() {
		int lineCountBefore = this.lineCount;
		while (Character.isWhitespace(getCurrentChar())) {
			handleLineColumn();
			readNext();
		}
		return this.lineCount - lineCountBefore;
	}

	// Read an identifier
	private @NonNull Identifier readIdentifier() {
		int start = this.iter;
		while (getCurrentChar() == '_' || Character.isLetterOrDigit(getCurrentChar())) {
			readNext();
		}
		return new Identifier(this.source, start, this.iter - start);
	}

	// Read a number token
	private @NonNull TokenType readNumber() {
		TokenType type = TokenType.INTCONSTANT;
		boolean possibleOctal = false;
		boolean incorrectOctal = false;
		if (getCurrentChar() == '0') {
			possibleOctal = true;
			readNext();
			// Check for hexadecimal number
			if (getCurrentChar() == 'x' || getCurrentChar() == 'X') {
				readNext();

				// Read hexadecimal part
				while (Character.digit(getCurrentChar(), 16) != -1) {
					readNext();
				}

				return TokenType.INTCONSTANT;
			}
		}
		// Read integer part
		while (Character.isDigit(getCurrentChar())) {
			if (getCurrentChar() > '7') incorrectOctal = true;
			readNext();
		}

		// Check for decimal part
		if (getCurrentChar() == '.') {
			type = TokenType.FLOATCONSTANT;
			readNext();
			// Read decimal part
			while (Character.isDigit(getCurrentChar())) {
				readNext();
			}
		}

		// Check for exponent part
		if ((getCurrentChar() == 'e' || getCurrentChar() == 'E')) {
			type = TokenType.FLOATCONSTANT;
			readNext();

			// Check for sign of exponent
			if (getCurrentChar() == '+' || getCurrentChar() == '-') {
				readNext();
			}
			if (!Character.isDigit(getCurrentChar())) return TokenType.INVALID;
			// Read exponent part
			do {
				readNext();
			} while ((Character.isDigit(getCurrentChar())));
		}
		if (type == TokenType.FLOATCONSTANT) {
			if (getCurrentChar() == 'f' || getCurrentChar() == 'F') readNext();
			return TokenType.FLOATCONSTANT;
		}
		// Check for unsigned suffix
		if (getCurrentChar() == 'u' || getCurrentChar() == 'U') {
			readNext();
			type = TokenType.UINTCONSTANT;
		}

		// Integer without suffix
		return possibleOctal && incorrectOctal ? TokenType.INVALID : type;
	}

	// initialize lookup
	static {
		// Keyword lookup
		KeywordToken[] KEYWORDS = {new KeywordToken(TokenType.CONST, "const"), new KeywordToken(TokenType.BOOL, "bool"), new KeywordToken(TokenType.FLOAT, "float"), new KeywordToken(TokenType.INT, "int"), new KeywordToken(TokenType.UINT, "uint"), new KeywordToken(TokenType.DOUBLE, "double"), new KeywordToken(TokenType.BVEC2, "bvec2"), new KeywordToken(TokenType.BVEC3, "bvec3"), new KeywordToken(TokenType.BVEC4, "bvec4"), new KeywordToken(TokenType.IVEC2, "ivec2"), new KeywordToken(TokenType.IVEC3, "ivec3"), new KeywordToken(TokenType.IVEC4, "ivec4"), new KeywordToken(TokenType.UVEC2, "uvec2"), new KeywordToken(TokenType.UVEC3, "uvec3"), new KeywordToken(TokenType.UVEC4, "uvec4"), new KeywordToken(TokenType.VEC2, "vec2"), new KeywordToken(TokenType.VEC3, "vec3"), new KeywordToken(TokenType.VEC4, "vec4"), new KeywordToken(TokenType.MAT2, "mat2"), new KeywordToken(TokenType.MAT3, "mat3"), new KeywordToken(TokenType.MAT4, "mat4"), new KeywordToken(TokenType.MAT2X2, "mat2x2"), new KeywordToken(TokenType.MAT2X3, "mat2x3"), new KeywordToken(TokenType.MAT2X4, "mat2x4"), new KeywordToken(TokenType.MAT3X2, "mat3x2"), new KeywordToken(TokenType.MAT3X3, "mat3x3"), new KeywordToken(TokenType.MAT3X4, "mat3x4"), new KeywordToken(TokenType.MAT4X2, "mat4x2"), new KeywordToken(TokenType.MAT4X3, "mat4x3"), new KeywordToken(TokenType.MAT4X4, "mat4x4"), new KeywordToken(TokenType.DVEC2, "dvec2"), new KeywordToken(TokenType.DVEC3, "dvec3"), new KeywordToken(TokenType.DVEC4, "dvec4"), new KeywordToken(TokenType.DMAT2, "dmat2"), new KeywordToken(TokenType.DMAT3, "dmat3"), new KeywordToken(TokenType.DMAT4, "dmat4"), new KeywordToken(TokenType.DMAT2X2, "dmat2x2"), new KeywordToken(TokenType.DMAT2X3, "dmat2x3"), new KeywordToken(TokenType.DMAT2X4, "dmat2x4"), new KeywordToken(TokenType.DMAT3X2, "dmat3x2"), new KeywordToken(TokenType.DMAT3X3, "dmat3x3"), new KeywordToken(TokenType.DMAT3X4, "dmat3x4"), new KeywordToken(TokenType.DMAT4X2, "dmat4x2"), new KeywordToken(TokenType.DMAT4X3, "dmat4x3"), new KeywordToken(TokenType.DMAT4X4, "dmat4x4"), new KeywordToken(TokenType.CENTROID, "centroid"), new KeywordToken(TokenType.IN, "in"), new KeywordToken(TokenType.OUT, "out"), new KeywordToken(TokenType.INOUT, "inout"), new KeywordToken(TokenType.UNIFORM, "uniform"), new KeywordToken(TokenType.PATCH, "patch"), new KeywordToken(TokenType.SAMPLE, "sample"), new KeywordToken(TokenType.BUFFER, "buffer"), new KeywordToken(TokenType.SHARED, "shared"), new KeywordToken(TokenType.COHERENT, "coherent"), new KeywordToken(TokenType.VOLATILE, "volatile"), new KeywordToken(TokenType.RESTRICT, "restrict"), new KeywordToken(TokenType.READONLY, "readonly"), new KeywordToken(TokenType.WRITEONLY, "writeonly"), new KeywordToken(TokenType.NOPERSPECTIVE, "noperspective"), new KeywordToken(TokenType.FLAT, "flat"), new KeywordToken(TokenType.SMOOTH, "smooth"), new KeywordToken(TokenType.LAYOUT, "layout"), new KeywordToken(TokenType.ATOMIC_UINT, "atomic_uint"), new KeywordToken(TokenType.SAMPLER2D, "sampler2D"), new KeywordToken(TokenType.SAMPLER3D, "sampler3D"), new KeywordToken(TokenType.SAMPLERCUBE, "samplerCube"), new KeywordToken(TokenType.SAMPLER2DSHADOW, "sampler2DShadow"), new KeywordToken(TokenType.SAMPLERCUBESHADOW, "samplerCubeShadow"), new KeywordToken(TokenType.SAMPLER2DARRAY, "sampler2DArray"), new KeywordToken(TokenType.SAMPLER2DARRAYSHADOW, "sampler2DArrayShadow"), new KeywordToken(TokenType.ISAMPLER2D, "isampler2D"), new KeywordToken(TokenType.ISAMPLER3D, "isampler3D"), new KeywordToken(TokenType.ISAMPLERCUBE, "isamplerCube"), new KeywordToken(TokenType.ISAMPLER2DARRAY, "isampler2DArray"), new KeywordToken(TokenType.USAMPLER2D, "usampler2D"), new KeywordToken(TokenType.USAMPLER3D, "usampler3D"), new KeywordToken(TokenType.USAMPLERCUBE, "usamplerCube"), new KeywordToken(TokenType.USAMPLER2DARRAY, "usampler2DArray"), new KeywordToken(TokenType.SAMPLER1D, "sampler1D"), new KeywordToken(TokenType.SAMPLER1DSHADOW, "sampler1DShadow"), new KeywordToken(TokenType.SAMPLER1DARRAY, "sampler1DArray"), new KeywordToken(TokenType.SAMPLER1DARRAYSHADOW, "sampler1DArrayShadow"), new KeywordToken(TokenType.ISAMPLER1D, "isampler1D"), new KeywordToken(TokenType.ISAMPLER1DARRAY, "isampler1DArray"), new KeywordToken(TokenType.USAMPLER1D, "usampler1D"), new KeywordToken(TokenType.USAMPLER1DARRAY, "usampler1DArray"), new KeywordToken(TokenType.SAMPLER2DRECT, "sampler2DRect"), new KeywordToken(TokenType.SAMPLER2DRECTSHADOW, "sampler2DRectShadow"), new KeywordToken(TokenType.ISAMPLER2DRECT, "isampler2DRect"), new KeywordToken(TokenType.USAMPLER2DRECT, "usampler2DRect"), new KeywordToken(TokenType.SAMPLERBUFFER, "samplerbuffer"), new KeywordToken(TokenType.ISAMPLERBUFFER, "isamplerbuffer"), new KeywordToken(TokenType.USAMPLERBUFFER, "usamplerbuffer"), new KeywordToken(TokenType.SAMPLERCUBEARRAY, "samplerCubeArray"), new KeywordToken(TokenType.SAMPLERCUBEARRAYSHADOW, "samplerCubeArrayShadow"), new KeywordToken(TokenType.ISAMPLERCUBEARRAY, "isamplerCubeArray"), new KeywordToken(TokenType.USAMPLERCUBEARRAY, "usamplerCubeArray"), new KeywordToken(TokenType.SAMPLER2DMS, "sampler2DMS"), new KeywordToken(TokenType.ISAMPLER2DMS, "isampler2DMS"), new KeywordToken(TokenType.USAMPLER2DMS, "usampler2DMS"), new KeywordToken(TokenType.SAMPLER2DMSARRAY, "sampler2DMSArray"), new KeywordToken(TokenType.ISAMPLER2DMSARRAY, "isampler2DMSArray"), new KeywordToken(TokenType.USAMPLER2DMSARRAY, "usampler2DMSArray"), new KeywordToken(TokenType.IMAGE2D, "image2D"), new KeywordToken(TokenType.IIMAGE2D, "iimage2D"), new KeywordToken(TokenType.UIMAGE2D, "uimage2D"), new KeywordToken(TokenType.IMAGE3D, "image3D"), new KeywordToken(TokenType.IIMAGE3D, "iimage3D"), new KeywordToken(TokenType.UIMAGE3D, "uimage3D"), new KeywordToken(TokenType.IMAGECUBE, "imageCube"), new KeywordToken(TokenType.IIMAGECUBE, "iimageCube"), new KeywordToken(TokenType.UIMAGECUBE, "uimageCube"), new KeywordToken(TokenType.IMAGEBUFFER, "imagebuffer"), new KeywordToken(TokenType.IIMAGEBUFFER, "iimagebuffer"), new KeywordToken(TokenType.UIMAGEBUFFER, "uimagebuffer"), new KeywordToken(TokenType.IMAGE2DARRAY, "image2DArray"), new KeywordToken(TokenType.IIMAGE2DARRAY, "iimage2DArray"), new KeywordToken(TokenType.UIMAGE2DARRAY, "uimage2DArray"), new KeywordToken(TokenType.IMAGECUBEARRAY, "imageCubeArray"), new KeywordToken(TokenType.IIMAGECUBEARRAY, "iimageCubeArray"), new KeywordToken(TokenType.UIMAGECUBEARRAY, "uimageCubeArray"), new KeywordToken(TokenType.IMAGE1D, "image1D"), new KeywordToken(TokenType.IIMAGE1D, "iimage1D"), new KeywordToken(TokenType.UIMAGE1D, "uimage1D"), new KeywordToken(TokenType.IMAGE1DARRAY, "image1DArray"), new KeywordToken(TokenType.IIMAGE1DARRAY, "iimage1DArray"), new KeywordToken(TokenType.UIMAGE1DARRAY, "iimage1DArray"), new KeywordToken(TokenType.IMAGE2DRECT, "image2DRect"), new KeywordToken(TokenType.IIMAGE2DRECT, "iimage2DRect"), new KeywordToken(TokenType.UIMAGE2DRECT, "uimage2DRect"), new KeywordToken(TokenType.IMAGE2DMS, "image2DMS"), new KeywordToken(TokenType.IIMAGE2DMS, "iimage2DMS"), new KeywordToken(TokenType.UIMAGE2DMS, "uimage2DMS"), new KeywordToken(TokenType.IMAGE2DMSARRAY, "image2DMSArray"), new KeywordToken(TokenType.IIMAGE2DMSARRAY, "iimage2DMSArray"), new KeywordToken(TokenType.UIMAGE2DMSARRAY, "uimage2DMSArray"), new KeywordToken(TokenType.STRUCT, "struct"), new KeywordToken(TokenType.VOID, "void"), new KeywordToken(TokenType.WHILE, "while"), new KeywordToken(TokenType.BREAK, "break"), new KeywordToken(TokenType.CONTINUE, "continue"), new KeywordToken(TokenType.DO, "do"), new KeywordToken(TokenType.ELSE, "else"), new KeywordToken(TokenType.FOR, "for"), new KeywordToken(TokenType.IF, "if"), new KeywordToken(TokenType.DISCARD, "discard"), new KeywordToken(TokenType.RETURN, "return"), new KeywordToken(TokenType.SWITCH, "switch"), new KeywordToken(TokenType.CASE, "case"), new KeywordToken(TokenType.DEFAULT, "default"), new KeywordToken(TokenType.SUBROUTINE, "subroutine"), new KeywordToken(TokenType.INVARIANT, "invariant"), new KeywordToken(TokenType.PRECISE, "precise"), new KeywordToken(TokenType.HIGH_PRECISION, "highp"), new KeywordToken(TokenType.MEDIUM_PRECISION, "mediump"), new KeywordToken(TokenType.LOW_PRECISION, "lowp"), new KeywordToken(TokenType.PRECISION, "precision"),
				// builtins taken from taken from
				// https://registry.khronos.org/OpenGL-Refpages/gl4/index.php
				new KeywordToken(TokenType.BUILTIN_FUNCTION, "abs"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "acos"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "acosh"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "all"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "any"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "asin"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "asinh"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atan"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atanh"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atomicAdd"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atomicAnd"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atomicCompSwap"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atomicCounter"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atomicCounterDecrement"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atomicCounterIncrement"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atomicExchange"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atomicMax"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atomicMin"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atomicOr"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "atomicXor"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "barrier"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "bitCount"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "bitfieldExtract"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "bitfieldInsert"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "bitfieldReverse"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "ceil"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "clamp"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "cos"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "cosh"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "cross"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "degrees"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "determinant"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "dFdx"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "dFdxCoarse"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "dFdxFine"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "dFdy"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "dFdyCoarse"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "dFdyFine"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "distance"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "dot"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "EmitStreamVertex"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "EmitVertex"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "EndPrimitive"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "EndStreamPrimitive"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "equal"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "exp"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "exp2"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "faceforward"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "findLSB"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "findMSB"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "floatBitsToInt"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "floatBitsToUint"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "floor"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "fma"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "fract"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "frexp"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "fwidth"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "fwidthCoarse"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "fwidthFine"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_ClipDistance"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_CullDistance"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_FragCoord"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_FragDepth"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_FrontFacing"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_GlobalInvocationID"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_HelperInvocation"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_InstanceID"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_InvocationID"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_Layer"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_LocalInvocationID"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_LocalInvocationIndex"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_NumSamples"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_NumWorkGroups"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_PatchVerticesIn"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_PointCoord"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_PointSize"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_Position"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_PrimitiveID"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_PrimitiveIDIn"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_SampleID"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_SampleMask"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_SampleMaskIn"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_SamplePosition"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_TessCoord"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_TessLevelInner"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_TessLevelOuter"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_VertexID"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_ViewportIndex"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_WorkGroupID"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "gl_WorkGroupSize"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "greaterThan"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "greaterThanEqual"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "groupMemoryBarrier"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageAtomicAdd"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageAtomicAnd"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageAtomicCompSwap"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageAtomicExchange"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageAtomicMax"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageAtomicMin"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageAtomicOr"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageAtomicXor"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageLoad"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageSamples"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageSize"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imageStore"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "imulExtended"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "intBitsToFloat"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "interpolateAtCentroid"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "interpolateAtOffset"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "interpolateAtSample"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "inverse"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "inversesqrt"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "isinf"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "isnan"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "ldexp"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "length"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "lessThan"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "lessThanEqual"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "log"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "log2"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "matrixCompMult"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "max"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "memoryBarrier"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "memoryBarrierAtomicCounter"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "memoryBarrierBuffer"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "memoryBarrierImage"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "memoryBarrierShared"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "min"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "mix"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "mod"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "modf"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "noise"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "noise1"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "noise2"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "noise3"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "noise4"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "normalize"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "not"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "notEqual"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "outerProduct"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "packDouble2x32"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "packHalf2x16"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "packSnorm2x16"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "packSnorm4x8"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "packUnorm"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "packUnorm2x16"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "packUnorm4x8"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "pow"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "radians"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "reflect"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "refract"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "round"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "roundEven"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "sign"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "sin"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "sinh"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "smoothstep"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "sqrt"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "step"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "tan"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "tanh"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "texelFetch"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "texelFetchOffset"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "texture"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureGather"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureGatherOffset"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureGatherOffsets"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureGrad"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureGradOffset"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureLod"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureLodOffset"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureOffset"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureProj"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureProjGrad"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureProjGradOffset"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureProjLod"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureProjLodOffset"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureProjOffset"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureQueryLevels"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureQueryLod"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureSamples"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "textureSize"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "transpose"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "trunc"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "uaddCarry"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "uintBitsToFloat"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "umulExtended"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "unpackDouble2x32"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "unpackHalf2x16"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "unpackSnorm2x16"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "unpackSnorm4x8"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "unpackUnorm"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "unpackUnorm2x16"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "unpackUnorm4x8"), new KeywordToken(TokenType.BUILTIN_FUNCTION, "usubBorrow"),};
		// Preprocessor directives
		KeywordToken[] PREPROC_DIRECTIVES = {new KeywordToken(TokenType.PREPROC_IF, "if"), new KeywordToken(TokenType.PREPROC_IFDEF, "ifdef"), new KeywordToken(TokenType.PREPROC_IFNDEF, "ifndef"), new KeywordToken(TokenType.PREPROC_ELIF, "elif"), new KeywordToken(TokenType.PREPROC_ELSE, "else"), new KeywordToken(TokenType.PREPROC_ENDIF, "endif"), new KeywordToken(TokenType.PREPROC_INCLUDE, "include"), new KeywordToken(TokenType.PREPROC_DEFINE, "define"), new KeywordToken(TokenType.PREPROC_UNDEF, "undef"), new KeywordToken(TokenType.PREPROC_LINE, "line"), new KeywordToken(TokenType.PREPROC_ERROR, "error"), new KeywordToken(TokenType.PREPROC_PRAGMA, "pragma"), new KeywordToken(TokenType.PREPROC_VERSION, "version")};
		for (KeywordToken keyword : KEYWORDS) {
			KEYWORDS_TRIE.insert(keyword.name, (short) keyword.type.ordinal());
		}
		for (KeywordToken directive : PREPROC_DIRECTIVES) {
			DIRECTIVES_TRIE.insert(directive.name, (short) directive.type.ordinal());
		}
	}

}