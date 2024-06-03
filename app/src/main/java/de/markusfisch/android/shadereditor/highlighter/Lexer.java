package de.markusfisch.android.shadereditor.highlighter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Lexer implements Iterable<Token> {
	@NonNull
	public static CharSequence tokenSource(@NonNull Token token, @NonNull CharSequence source) {
		return source.subSequence(token.startOffset(), token.endOffset());
	}

	public static class Diff {
		public final @NonNull List<Token> original;
		public final @NonNull List<Token> edited;
		public final int start;
		public final int deleteEnd;

		@Override
		public @NonNull String toString() {
			return "Change{" +
					"start=" + start +
					", deleteEnd=" + deleteEnd +
					", insertEnd=" + insertEnd +
					", original='" + original + '\'' +
					", edited='" + edited + '\'' +
					'}';
		}

		public final int insertEnd;

		public Diff(@NonNull List<Token> original, @NonNull List<Token> edited, int start,
				int deleteEnd, int insertEnd) {
			this.original = original;
			this.edited = edited;
			this.start = start;
			this.deleteEnd = deleteEnd;
			this.insertEnd = insertEnd;
		}
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Identifier that = (Identifier) o;

			if (length != that.length) return false;
			int end = start + length;
			int thatEnd = that.start + that.length;
			for (
					int i = start, j = that.start;
					i < end && j < thatEnd;
					i = CharIterator.nextC(i, source), j = CharIterator.nextC(j, that.source)
			) {
				if (CharIterator.ch(i, source) != CharIterator.ch(j, that.source)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			int result = length;
			for (int i = start, end = start + result; i < end; i = CharIterator.nextC(i, source)) {
				result = 31 * result + CharIterator.ch(i, source);
			}
			return result;
		}

		@NonNull
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			for (int i = start, end = start + length; i < end; i = CharIterator.nextC(i, source)) {
				builder.append(CharIterator.ch(i, source));
			}
			return builder.toString();
		}
	}

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

	private static final TrieNode KEYWORDS_TRIE = new TrieNode();
	private static final TrieNode DIRECTIVES_TRIE = new TrieNode();
	private @NonNull Token previous = new Token();
	private @NonNull Token lastNormal = new Token();
	private final @NonNull String source;
	private int unicodePosition;  // codepoint index
	private int lineStart;
	private int position;       // current position byte offset
	private int readPosition;  // read position byte offset
	private int lineCount;

	public Lexer(@NonNull String input) {
		this.source = input;
		this.readPosition = 1;
		this.lineCount = 1;
		nextToken();
	}

	private @NonNull TokenType advance(@NonNull TokenType type) {
		readNext();
		return type;
	}

	@Override
	public @NonNull Iterator<Token> iterator() {
		return new TokenIterator();
	}

	public @NonNull Token nextToken() {
		boolean isFirstLine = this.position == 0;
		boolean isNewLogicLine = skipWhitespace() != 0 || isFirstLine;
		int start = this.unicodePosition;
		int startOffset = this.position;
		Token tok = new Token()
				.setType(TokenType.INVALID)
				.setStart(start)
				.setEnd(start)
				.setLine((short) this.lineCount)
				.setCategory(Token.Category.NORMAL)
				.setStartOffset(startOffset)
				.setColumn((short) (this.unicodePosition - this.lineStart));
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
						} while (CharIterator.isValid(this.position, this.source) && !(getCurrentChar() == '*' && peekNextChar() == '/'));
						if (CharIterator.isValid(this.position, this.source)) {
							readNext();
							readNext();
						}
						break;
					case '/':
						tok.setType(advance(TokenType.LINE_COMMENT));
						tok.setCategory(Token.Category.TRIVIA);
						do {
							readNext();
						} while (CharIterator.isValid(this.position, this.source) && getCurrentChar() != '\n');
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
						tok.setType(TokenType.values()[DIRECTIVES_TRIE.find(identifier.source(),
								identifier.start(), identifier.length())]);
					}
					// is not a preprocessor directive? -> is it a keyword?
					if (tok.type() == TokenType.INVALID) {
						tok.setType(TokenType.values()[KEYWORDS_TRIE.find(identifier.source(),
								identifier.start(), identifier.length())]);
					}
					if (tok.type() == TokenType.INVALID) {
						tok.setType(TokenType.IDENTIFIER);
					}
					if (tok.type() == TokenType.IDENTIFIER && tok.category() == Token.Category.NORMAL) {
						if (lastNormal.type() == TokenType.DOT) {
							tok.setType(TokenType.FIELD_SELECTION);
						} else if (lastNormal.type() == TokenType.STRUCT) {
							tok.setType(TokenType.TYPE_NAME);
						} else if (lastNormal.type() == TokenType.IDENTIFIER) {
							lastNormal.setType(TokenType.TYPE_NAME);
						}
					}
				} else {
					tok.setType(TokenType.INVALID);
					if (CharIterator.isSurrogate(this.getCurrentChar())) {
						++this.readPosition;
					}
					this.position = this.readPosition++;
					++this.unicodePosition;
				}
				break;
		}
		tok.setEndOffset(this.position)
				.setEnd(this.unicodePosition);

		this.previous = tok;
		if (tok.category() == Token.Category.NORMAL) {
			lastNormal = tok;
		}
		return previous;
	}

	/**
	 * Find an area of change between two token lists.
	 *
	 * @param original List of tokens before change.
	 * @param edited   List of tokens after change.
	 * @return A {@link Diff} object that describes the area of change.
	 */
	public static @NonNull Diff diff(List<Token> original, List<Token> edited) {
		int start = 0;
		int end = 1;
		int max = Math.min(original.size(), edited.size());

		// Find the longest common prefix
		while (start < max && original.get(start).equals(edited.get(start))) {
			start++;
		}

		// Find the longest common suffix
		while (end < max && original.get(original.size() - end).equals(edited.get(edited.size() - end))) {
			end++;
		}

		// Calculate the changes
		int deleteEnd = original.size() - end;
		int insertEnd = edited.size() - end;
		return new Diff(original, edited, start, deleteEnd, insertEnd);
	}

	@Contract(pure = true)
	private char peekNextChar() {
		return CharIterator.peekC(this.position, this.source);
	}

	@Contract(pure = true)
	private char getCurrentChar() {
		return CharIterator.ch(this.position, this.source);
	}

	private void handleLineColumn() {
		char ch = getCurrentChar();
		if (ch == '\n' || ch == '\r') {
			++this.lineCount;
			this.lineStart = this.unicodePosition + 1;
		}
	}

	private void readNext() {
		while (true) {
			++this.unicodePosition;
			this.position = this.readPosition++;
			int oldPosition = this.position;
			// if ch == \, then check if line continuation
			if (getCurrentChar() == '\\' && CharIterator.hasMoved(oldPosition, this.position =
					CharIterator.nextNewline(this.position, this.source))) {
				this.readPosition = this.position + 1;
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
		int start = this.position;
		while (getCurrentChar() == '_' || Character.isLetterOrDigit(getCurrentChar())) {
			readNext();
		}
		return new Identifier(this.source, start, this.position - start);
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

	public static List<String> completeKeyword(@NonNull String text,
			@NonNull Token.Category type) {
		List<String> result = new ArrayList<>();
		TrieNode root = tokenRoot(type);
		if (root != null) {
			root.findAll(text, (short) TokenType.INVALID.ordinal(), result);
		}

		if (type == Token.Category.PREPROC) {
			// Also add normal keywords to the list
			KEYWORDS_TRIE.findAll(text, (short) TokenType.INVALID.ordinal(), result);
		}

		return result;
	}

	/**
	 * Performs a binary search to find the token that includes the given position.
	 * Assumes tokens are non-overlapping and touch each other.
	 *
	 * @param tokens   List of tokens sorted by their start offsets.
	 * @param position The offset to search for.
	 * @return The token that contains the position, or null if no such token exists.
	 */
	@Nullable
	public static Token findToken(@NonNull List<Token> tokens, int position) {
		int low = 0;
		int high = tokens.size() - 1;

		while (low <= high) {
			int mid = low + (high - low) / 2;
			Token midToken = tokens.get(mid);

			if (position >= midToken.startOffset() && position <= midToken.endOffset()) {
				return midToken;
			} else if (position < midToken.startOffset()) {
				high = mid - 1;
			} else {
				low = mid + 1;
			}
		}

		return null; // No token contains the position
	}

	@Nullable
	private static TrieNode tokenRoot(@NonNull Token.Category type) {
		switch (type) {
			case NORMAL:
				return KEYWORDS_TRIE;
			case TRIVIA:
				return null;
			case PREPROC:
				return DIRECTIVES_TRIE;
		}
		return null;
	}

	// initialize lookup
	static {
		// Keyword lookup
		KEYWORDS_TRIE
				.insert("const", TokenType.CONST)
				.insert("bool", TokenType.BOOL)
				.insert("float", TokenType.FLOAT)
				.insert("int", TokenType.INT)
				.insert("uint", TokenType.UINT)
				.insert("double", TokenType.DOUBLE)
				.insert("bvec2", TokenType.BVEC2)
				.insert("bvec3", TokenType.BVEC3)
				.insert("bvec4", TokenType.BVEC4)
				.insert("ivec2", TokenType.IVEC2)
				.insert("ivec3", TokenType.IVEC3)
				.insert("ivec4", TokenType.IVEC4)
				.insert("uvec2", TokenType.UVEC2)
				.insert("uvec3", TokenType.UVEC3)
				.insert("uvec4", TokenType.UVEC4)
				.insert("vec2", TokenType.VEC2)
				.insert("vec3", TokenType.VEC3)
				.insert("vec4", TokenType.VEC4)
				.insert("mat2", TokenType.MAT2)
				.insert("mat3", TokenType.MAT3)
				.insert("mat4", TokenType.MAT4)
				.insert("mat2x2", TokenType.MAT2X2)
				.insert("mat2x3", TokenType.MAT2X3)
				.insert("mat2x4", TokenType.MAT2X4)
				.insert("mat3x2", TokenType.MAT3X2)
				.insert("mat3x3", TokenType.MAT3X3)
				.insert("mat3x4", TokenType.MAT3X4)
				.insert("mat4x2", TokenType.MAT4X2)
				.insert("mat4x3", TokenType.MAT4X3)
				.insert("mat4x4", TokenType.MAT4X4)
				.insert("dvec2", TokenType.DVEC2)
				.insert("dvec3", TokenType.DVEC3)
				.insert("dvec4", TokenType.DVEC4)
				.insert("dmat2", TokenType.DMAT2)
				.insert("dmat3", TokenType.DMAT3)
				.insert("dmat4", TokenType.DMAT4)
				.insert("dmat2x2", TokenType.DMAT2X2)
				.insert("dmat2x3", TokenType.DMAT2X3)
				.insert("dmat2x4", TokenType.DMAT2X4)
				.insert("dmat3x2", TokenType.DMAT3X2)
				.insert("dmat3x3", TokenType.DMAT3X3)
				.insert("dmat3x4", TokenType.DMAT3X4)
				.insert("dmat4x2", TokenType.DMAT4X2)
				.insert("dmat4x3", TokenType.DMAT4X3)
				.insert("dmat4x4", TokenType.DMAT4X4)
				.insert("centroid", TokenType.CENTROID)
				.insert("in", TokenType.IN)
				.insert("out", TokenType.OUT)
				.insert("inout", TokenType.INOUT)
				.insert("uniform", TokenType.UNIFORM)
				.insert("patch", TokenType.PATCH)
				.insert("sample", TokenType.SAMPLE)
				.insert("buffer", TokenType.BUFFER)
				.insert("shared", TokenType.SHARED)
				.insert("coherent", TokenType.COHERENT)
				.insert("volatile", TokenType.VOLATILE)
				.insert("restrict", TokenType.RESTRICT)
				.insert("readonly", TokenType.READONLY)
				.insert("writeonly", TokenType.WRITEONLY)
				.insert("noperspective", TokenType.NOPERSPECTIVE)
				.insert("flat", TokenType.FLAT)
				.insert("smooth", TokenType.SMOOTH)
				.insert("layout", TokenType.LAYOUT)
				.insert("atomic_uint", TokenType.ATOMIC_UINT)
				.insert("sampler2D", TokenType.SAMPLER2D)
				.insert("sampler3D", TokenType.SAMPLER3D)
				.insert("samplerCube", TokenType.SAMPLERCUBE)
				.insert("sampler2DShadow", TokenType.SAMPLER2DSHADOW)
				.insert("samplerCubeShadow", TokenType.SAMPLERCUBESHADOW)
				.insert("sampler2DArray", TokenType.SAMPLER2DARRAY)
				.insert("sampler2DArrayShadow", TokenType.SAMPLER2DARRAYSHADOW)
				.insert("isampler2D", TokenType.ISAMPLER2D)
				.insert("isampler3D", TokenType.ISAMPLER3D)
				.insert("isamplerCube", TokenType.ISAMPLERCUBE)
				.insert("isampler2DArray", TokenType.ISAMPLER2DARRAY)
				.insert("usampler2D", TokenType.USAMPLER2D)
				.insert("usampler3D", TokenType.USAMPLER3D)
				.insert("usamplerCube", TokenType.USAMPLERCUBE)
				.insert("usampler2DArray", TokenType.USAMPLER2DARRAY)
				.insert("sampler1D", TokenType.SAMPLER1D)
				.insert("sampler1DShadow", TokenType.SAMPLER1DSHADOW)
				.insert("sampler1DArray", TokenType.SAMPLER1DARRAY)
				.insert("sampler1DArrayShadow", TokenType.SAMPLER1DARRAYSHADOW)
				.insert("isampler1D", TokenType.ISAMPLER1D)
				.insert("isampler1DArray", TokenType.ISAMPLER1DARRAY)
				.insert("usampler1D", TokenType.USAMPLER1D)
				.insert("usampler1DArray", TokenType.USAMPLER1DARRAY)
				.insert("sampler2DRect", TokenType.SAMPLER2DRECT)
				.insert("sampler2DRectShadow", TokenType.SAMPLER2DRECTSHADOW)
				.insert("isampler2DRect", TokenType.ISAMPLER2DRECT)
				.insert("usampler2DRect", TokenType.USAMPLER2DRECT)
				.insert("samplerbuffer", TokenType.SAMPLERBUFFER)
				.insert("isamplerbuffer", TokenType.ISAMPLERBUFFER)
				.insert("usamplerbuffer", TokenType.USAMPLERBUFFER)
				.insert("samplerCubeArray", TokenType.SAMPLERCUBEARRAY)
				.insert("samplerCubeArrayShadow", TokenType.SAMPLERCUBEARRAYSHADOW)
				.insert("isamplerCubeArray", TokenType.ISAMPLERCUBEARRAY)
				.insert("usamplerCubeArray", TokenType.USAMPLERCUBEARRAY)
				.insert("sampler2DMS", TokenType.SAMPLER2DMS)
				.insert("isampler2DMS", TokenType.ISAMPLER2DMS)
				.insert("usampler2DMS", TokenType.USAMPLER2DMS)
				.insert("sampler2DMSArray", TokenType.SAMPLER2DMSARRAY)
				.insert("isampler2DMSArray", TokenType.ISAMPLER2DMSARRAY)
				.insert("usampler2DMSArray", TokenType.USAMPLER2DMSARRAY)
				.insert("image2D", TokenType.IMAGE2D)
				.insert("iimage2D", TokenType.IIMAGE2D)
				.insert("uimage2D", TokenType.UIMAGE2D)
				.insert("image3D", TokenType.IMAGE3D)
				.insert("iimage3D", TokenType.IIMAGE3D)
				.insert("uimage3D", TokenType.UIMAGE3D)
				.insert("imageCube", TokenType.IMAGECUBE)
				.insert("iimageCube", TokenType.IIMAGECUBE)
				.insert("uimageCube", TokenType.UIMAGECUBE)
				.insert("imagebuffer", TokenType.IMAGEBUFFER)
				.insert("iimagebuffer", TokenType.IIMAGEBUFFER)
				.insert("uimagebuffer", TokenType.UIMAGEBUFFER)
				.insert("image2DArray", TokenType.IMAGE2DARRAY)
				.insert("iimage2DArray", TokenType.IIMAGE2DARRAY)
				.insert("uimage2DArray", TokenType.UIMAGE2DARRAY)
				.insert("imageCubeArray", TokenType.IMAGECUBEARRAY)
				.insert("iimageCubeArray", TokenType.IIMAGECUBEARRAY)
				.insert("uimageCubeArray", TokenType.UIMAGECUBEARRAY)
				.insert("image1D", TokenType.IMAGE1D)
				.insert("iimage1D", TokenType.IIMAGE1D)
				.insert("uimage1D", TokenType.UIMAGE1D)
				.insert("image1DArray", TokenType.IMAGE1DARRAY)
				.insert("iimage1DArray", TokenType.IIMAGE1DARRAY)
				.insert("iimage1DArray", TokenType.UIMAGE1DARRAY)
				.insert("image2DRect", TokenType.IMAGE2DRECT)
				.insert("iimage2DRect", TokenType.IIMAGE2DRECT)
				.insert("uimage2DRect", TokenType.UIMAGE2DRECT)
				.insert("image2DMS", TokenType.IMAGE2DMS)
				.insert("iimage2DMS", TokenType.IIMAGE2DMS)
				.insert("uimage2DMS", TokenType.UIMAGE2DMS)
				.insert("image2DMSArray", TokenType.IMAGE2DMSARRAY)
				.insert("iimage2DMSArray", TokenType.IIMAGE2DMSARRAY)
				.insert("uimage2DMSArray", TokenType.UIMAGE2DMSARRAY)
				.insert("struct", TokenType.STRUCT)
				.insert("void", TokenType.VOID)
				.insert("while", TokenType.WHILE)
				.insert("break", TokenType.BREAK)
				.insert("continue", TokenType.CONTINUE)
				.insert("do", TokenType.DO)
				.insert("else", TokenType.ELSE)
				.insert("for", TokenType.FOR)
				.insert("if", TokenType.IF)
				.insert("discard", TokenType.DISCARD)
				.insert("return", TokenType.RETURN)
				.insert("switch", TokenType.SWITCH)
				.insert("case", TokenType.CASE)
				.insert("default", TokenType.DEFAULT)
				.insert("subroutine", TokenType.SUBROUTINE)
				.insert("invariant", TokenType.INVARIANT)
				.insert("precise", TokenType.PRECISE)
				.insert("highp", TokenType.HIGH_PRECISION)
				.insert("mediump", TokenType.MEDIUM_PRECISION)
				.insert("lowp", TokenType.LOW_PRECISION)
				.insert("precision", TokenType.PRECISION)
				// builtins taken from taken from
				// https://registry.khronos.org/OpenGL-Refpages/gl4/index.php
				.insert("abs", TokenType.BUILTIN_FUNCTION)
				.insert("acos", TokenType.BUILTIN_FUNCTION)
				.insert("acosh", TokenType.BUILTIN_FUNCTION)
				.insert("all", TokenType.BUILTIN_FUNCTION)
				.insert("any", TokenType.BUILTIN_FUNCTION)
				.insert("asin", TokenType.BUILTIN_FUNCTION)
				.insert("asinh", TokenType.BUILTIN_FUNCTION)
				.insert("atan", TokenType.BUILTIN_FUNCTION)
				.insert("atanh", TokenType.BUILTIN_FUNCTION)
				.insert("atomicAdd", TokenType.BUILTIN_FUNCTION)
				.insert("atomicAnd", TokenType.BUILTIN_FUNCTION)
				.insert("atomicCompSwap", TokenType.BUILTIN_FUNCTION)
				.insert("atomicCounter", TokenType.BUILTIN_FUNCTION)
				.insert("atomicCounterDecrement", TokenType.BUILTIN_FUNCTION)
				.insert("atomicCounterIncrement", TokenType.BUILTIN_FUNCTION)
				.insert("atomicExchange", TokenType.BUILTIN_FUNCTION)
				.insert("atomicMax", TokenType.BUILTIN_FUNCTION)
				.insert("atomicMin", TokenType.BUILTIN_FUNCTION)
				.insert("atomicOr", TokenType.BUILTIN_FUNCTION)
				.insert("atomicXor", TokenType.BUILTIN_FUNCTION)
				.insert("barrier", TokenType.BUILTIN_FUNCTION)
				.insert("bitCount", TokenType.BUILTIN_FUNCTION)
				.insert("bitfieldExtract", TokenType.BUILTIN_FUNCTION)
				.insert("bitfieldInsert", TokenType.BUILTIN_FUNCTION)
				.insert("bitfieldReverse", TokenType.BUILTIN_FUNCTION)
				.insert("ceil", TokenType.BUILTIN_FUNCTION)
				.insert("clamp", TokenType.BUILTIN_FUNCTION)
				.insert("cos", TokenType.BUILTIN_FUNCTION)
				.insert("cosh", TokenType.BUILTIN_FUNCTION)
				.insert("cross", TokenType.BUILTIN_FUNCTION)
				.insert("degrees", TokenType.BUILTIN_FUNCTION)
				.insert("determinant", TokenType.BUILTIN_FUNCTION)
				.insert("dFdx", TokenType.BUILTIN_FUNCTION)
				.insert("dFdxCoarse", TokenType.BUILTIN_FUNCTION)
				.insert("dFdxFine", TokenType.BUILTIN_FUNCTION)
				.insert("dFdy", TokenType.BUILTIN_FUNCTION)
				.insert("dFdyCoarse", TokenType.BUILTIN_FUNCTION)
				.insert("dFdyFine", TokenType.BUILTIN_FUNCTION)
				.insert("distance", TokenType.BUILTIN_FUNCTION)
				.insert("dot", TokenType.BUILTIN_FUNCTION)
				.insert("EmitStreamVertex", TokenType.BUILTIN_FUNCTION)
				.insert("EmitVertex", TokenType.BUILTIN_FUNCTION)
				.insert("EndPrimitive", TokenType.BUILTIN_FUNCTION)
				.insert("EndStreamPrimitive", TokenType.BUILTIN_FUNCTION)
				.insert("equal", TokenType.BUILTIN_FUNCTION)
				.insert("exp", TokenType.BUILTIN_FUNCTION)
				.insert("exp2", TokenType.BUILTIN_FUNCTION)
				.insert("faceforward", TokenType.BUILTIN_FUNCTION)
				.insert("findLSB", TokenType.BUILTIN_FUNCTION)
				.insert("findMSB", TokenType.BUILTIN_FUNCTION)
				.insert("floatBitsToInt", TokenType.BUILTIN_FUNCTION)
				.insert("floatBitsToUint", TokenType.BUILTIN_FUNCTION)
				.insert("floor", TokenType.BUILTIN_FUNCTION)
				.insert("fma", TokenType.BUILTIN_FUNCTION)
				.insert("fract", TokenType.BUILTIN_FUNCTION)
				.insert("frexp", TokenType.BUILTIN_FUNCTION)
				.insert("fwidth", TokenType.BUILTIN_FUNCTION)
				.insert("fwidthCoarse", TokenType.BUILTIN_FUNCTION)
				.insert("fwidthFine", TokenType.BUILTIN_FUNCTION)
				.insert("gl_ClipDistance", TokenType.BUILTIN_FUNCTION)
				.insert("gl_CullDistance", TokenType.BUILTIN_FUNCTION)
				.insert("gl_FragCoord", TokenType.BUILTIN_FUNCTION)
				.insert("gl_FragDepth", TokenType.BUILTIN_FUNCTION)
				.insert("gl_FrontFacing", TokenType.BUILTIN_FUNCTION)
				.insert("gl_GlobalInvocationID", TokenType.BUILTIN_FUNCTION)
				.insert("gl_HelperInvocation", TokenType.BUILTIN_FUNCTION)
				.insert("gl_InstanceID", TokenType.BUILTIN_FUNCTION)
				.insert("gl_InvocationID", TokenType.BUILTIN_FUNCTION)
				.insert("gl_Layer", TokenType.BUILTIN_FUNCTION)
				.insert("gl_LocalInvocationID", TokenType.BUILTIN_FUNCTION)
				.insert("gl_LocalInvocationIndex", TokenType.BUILTIN_FUNCTION)
				.insert("gl_NumSamples", TokenType.BUILTIN_FUNCTION)
				.insert("gl_NumWorkGroups", TokenType.BUILTIN_FUNCTION)
				.insert("gl_PatchVerticesIn", TokenType.BUILTIN_FUNCTION)
				.insert("gl_PointCoord", TokenType.BUILTIN_FUNCTION)
				.insert("gl_PointSize", TokenType.BUILTIN_FUNCTION)
				.insert("gl_Position", TokenType.BUILTIN_FUNCTION)
				.insert("gl_PrimitiveID", TokenType.BUILTIN_FUNCTION)
				.insert("gl_PrimitiveIDIn", TokenType.BUILTIN_FUNCTION)
				.insert("gl_SampleID", TokenType.BUILTIN_FUNCTION)
				.insert("gl_SampleMask", TokenType.BUILTIN_FUNCTION)
				.insert("gl_SampleMaskIn", TokenType.BUILTIN_FUNCTION)
				.insert("gl_SamplePosition", TokenType.BUILTIN_FUNCTION)
				.insert("gl_TessCoord", TokenType.BUILTIN_FUNCTION)
				.insert("gl_TessLevelInner", TokenType.BUILTIN_FUNCTION)
				.insert("gl_TessLevelOuter", TokenType.BUILTIN_FUNCTION)
				.insert("gl_VertexID", TokenType.BUILTIN_FUNCTION)
				.insert("gl_ViewportIndex", TokenType.BUILTIN_FUNCTION)
				.insert("gl_WorkGroupID", TokenType.BUILTIN_FUNCTION)
				.insert("gl_WorkGroupSize", TokenType.BUILTIN_FUNCTION)
				.insert("greaterThan", TokenType.BUILTIN_FUNCTION)
				.insert("greaterThanEqual", TokenType.BUILTIN_FUNCTION)
				.insert("groupMemoryBarrier", TokenType.BUILTIN_FUNCTION)
				.insert("imageAtomicAdd", TokenType.BUILTIN_FUNCTION)
				.insert("imageAtomicAnd", TokenType.BUILTIN_FUNCTION)
				.insert("imageAtomicCompSwap", TokenType.BUILTIN_FUNCTION)
				.insert("imageAtomicExchange", TokenType.BUILTIN_FUNCTION)
				.insert("imageAtomicMax", TokenType.BUILTIN_FUNCTION)
				.insert("imageAtomicMin", TokenType.BUILTIN_FUNCTION)
				.insert("imageAtomicOr", TokenType.BUILTIN_FUNCTION)
				.insert("imageAtomicXor", TokenType.BUILTIN_FUNCTION)
				.insert("imageLoad", TokenType.BUILTIN_FUNCTION)
				.insert("imageSamples", TokenType.BUILTIN_FUNCTION)
				.insert("imageSize", TokenType.BUILTIN_FUNCTION)
				.insert("imageStore", TokenType.BUILTIN_FUNCTION)
				.insert("imulExtended", TokenType.BUILTIN_FUNCTION)
				.insert("intBitsToFloat", TokenType.BUILTIN_FUNCTION)
				.insert("interpolateAtCentroid", TokenType.BUILTIN_FUNCTION)
				.insert("interpolateAtOffset", TokenType.BUILTIN_FUNCTION)
				.insert("interpolateAtSample", TokenType.BUILTIN_FUNCTION)
				.insert("inverse", TokenType.BUILTIN_FUNCTION)
				.insert("inversesqrt", TokenType.BUILTIN_FUNCTION)
				.insert("isinf", TokenType.BUILTIN_FUNCTION)
				.insert("isnan", TokenType.BUILTIN_FUNCTION)
				.insert("ldexp", TokenType.BUILTIN_FUNCTION)
				.insert("length", TokenType.BUILTIN_FUNCTION)
				.insert("lessThan", TokenType.BUILTIN_FUNCTION)
				.insert("lessThanEqual", TokenType.BUILTIN_FUNCTION)
				.insert("log", TokenType.BUILTIN_FUNCTION)
				.insert("log2", TokenType.BUILTIN_FUNCTION)
				.insert("matrixCompMult", TokenType.BUILTIN_FUNCTION)
				.insert("max", TokenType.BUILTIN_FUNCTION)
				.insert("memoryBarrier", TokenType.BUILTIN_FUNCTION)
				.insert("memoryBarrierAtomicCounter", TokenType.BUILTIN_FUNCTION)
				.insert("memoryBarrierBuffer", TokenType.BUILTIN_FUNCTION)
				.insert("memoryBarrierImage", TokenType.BUILTIN_FUNCTION)
				.insert("memoryBarrierShared", TokenType.BUILTIN_FUNCTION)
				.insert("min", TokenType.BUILTIN_FUNCTION)
				.insert("mix", TokenType.BUILTIN_FUNCTION)
				.insert("mod", TokenType.BUILTIN_FUNCTION)
				.insert("modf", TokenType.BUILTIN_FUNCTION)
				.insert("noise", TokenType.BUILTIN_FUNCTION)
				.insert("noise1", TokenType.BUILTIN_FUNCTION)
				.insert("noise2", TokenType.BUILTIN_FUNCTION)
				.insert("noise3", TokenType.BUILTIN_FUNCTION)
				.insert("noise4", TokenType.BUILTIN_FUNCTION)
				.insert("normalize", TokenType.BUILTIN_FUNCTION)
				.insert("not", TokenType.BUILTIN_FUNCTION)
				.insert("notEqual", TokenType.BUILTIN_FUNCTION)
				.insert("outerProduct", TokenType.BUILTIN_FUNCTION)
				.insert("packDouble2x32", TokenType.BUILTIN_FUNCTION)
				.insert("packHalf2x16", TokenType.BUILTIN_FUNCTION)
				.insert("packSnorm2x16", TokenType.BUILTIN_FUNCTION)
				.insert("packSnorm4x8", TokenType.BUILTIN_FUNCTION)
				.insert("packUnorm", TokenType.BUILTIN_FUNCTION)
				.insert("packUnorm2x16", TokenType.BUILTIN_FUNCTION)
				.insert("packUnorm4x8", TokenType.BUILTIN_FUNCTION)
				.insert("pow", TokenType.BUILTIN_FUNCTION)
				.insert("radians", TokenType.BUILTIN_FUNCTION)
				.insert("reflect", TokenType.BUILTIN_FUNCTION)
				.insert("refract", TokenType.BUILTIN_FUNCTION)
				.insert("round", TokenType.BUILTIN_FUNCTION)
				.insert("roundEven", TokenType.BUILTIN_FUNCTION)
				.insert("sign", TokenType.BUILTIN_FUNCTION)
				.insert("sin", TokenType.BUILTIN_FUNCTION)
				.insert("sinh", TokenType.BUILTIN_FUNCTION)
				.insert("smoothstep", TokenType.BUILTIN_FUNCTION)
				.insert("sqrt", TokenType.BUILTIN_FUNCTION)
				.insert("step", TokenType.BUILTIN_FUNCTION)
				.insert("tan", TokenType.BUILTIN_FUNCTION)
				.insert("tanh", TokenType.BUILTIN_FUNCTION)
				.insert("texelFetch", TokenType.BUILTIN_FUNCTION)
				.insert("texelFetchOffset", TokenType.BUILTIN_FUNCTION)
				.insert("texture", TokenType.BUILTIN_FUNCTION)
				.insert("textureGather", TokenType.BUILTIN_FUNCTION)
				.insert("textureGatherOffset", TokenType.BUILTIN_FUNCTION)
				.insert("textureGatherOffsets", TokenType.BUILTIN_FUNCTION)
				.insert("textureGrad", TokenType.BUILTIN_FUNCTION)
				.insert("textureGradOffset", TokenType.BUILTIN_FUNCTION)
				.insert("textureLod", TokenType.BUILTIN_FUNCTION)
				.insert("textureLodOffset", TokenType.BUILTIN_FUNCTION)
				.insert("textureOffset", TokenType.BUILTIN_FUNCTION)
				.insert("textureProj", TokenType.BUILTIN_FUNCTION)
				.insert("textureProjGrad", TokenType.BUILTIN_FUNCTION)
				.insert("textureProjGradOffset", TokenType.BUILTIN_FUNCTION)
				.insert("textureProjLod", TokenType.BUILTIN_FUNCTION)
				.insert("textureProjLodOffset", TokenType.BUILTIN_FUNCTION)
				.insert("textureProjOffset", TokenType.BUILTIN_FUNCTION)
				.insert("textureQueryLevels", TokenType.BUILTIN_FUNCTION)
				.insert("textureQueryLod", TokenType.BUILTIN_FUNCTION)
				.insert("textureSamples", TokenType.BUILTIN_FUNCTION)
				.insert("textureSize", TokenType.BUILTIN_FUNCTION)
				.insert("transpose", TokenType.BUILTIN_FUNCTION)
				.insert("trunc", TokenType.BUILTIN_FUNCTION)
				.insert("uaddCarry", TokenType.BUILTIN_FUNCTION)
				.insert("uintBitsToFloat", TokenType.BUILTIN_FUNCTION)
				.insert("umulExtended", TokenType.BUILTIN_FUNCTION)
				.insert("unpackDouble2x32", TokenType.BUILTIN_FUNCTION)
				.insert("unpackHalf2x16", TokenType.BUILTIN_FUNCTION)
				.insert("unpackSnorm2x16", TokenType.BUILTIN_FUNCTION)
				.insert("unpackSnorm4x8", TokenType.BUILTIN_FUNCTION)
				.insert("unpackUnorm", TokenType.BUILTIN_FUNCTION)
				.insert("unpackUnorm2x16", TokenType.BUILTIN_FUNCTION)
				.insert("unpackUnorm4x8", TokenType.BUILTIN_FUNCTION)
				.insert("usubBorrow", TokenType.BUILTIN_FUNCTION);
		// Preprocessor directives
		DIRECTIVES_TRIE
				.insert("if", TokenType.PREPROC_IF)
				.insert("ifdef", TokenType.PREPROC_IFDEF)
				.insert("ifndef", TokenType.PREPROC_IFNDEF)
				.insert("elif", TokenType.PREPROC_ELIF)
				.insert("else", TokenType.PREPROC_ELSE)
				.insert("endif", TokenType.PREPROC_ENDIF)
				.insert("include", TokenType.PREPROC_INCLUDE)
				.insert("define", TokenType.PREPROC_DEFINE)
				.insert("undef", TokenType.PREPROC_UNDEF)
				.insert("line", TokenType.PREPROC_LINE)
				.insert("error", TokenType.PREPROC_ERROR)
				.insert("pragma", TokenType.PREPROC_PRAGMA)
				.insert("version", TokenType.PREPROC_VERSION);
	}
}
