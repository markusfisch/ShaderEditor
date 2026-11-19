package de.markusfisch.android.shadereditor.syntax;

// One tokenized line, with UTF-16 offsets into it. Immutable, so it may be
// handed to any thread.
public final class LexedLine {
	private static final Style[] STYLES = Style.values();

	private final String text;
	private final int state;
	// Start, end and kind of each token.
	private final int[] tokens;

	private LexedLine(String text, int state, int[] tokens) {
		this.text = text;
		this.state = state;
		this.tokens = tokens;
	}

	// scratch is the lexer's working space.
	public static LexedLine of(Syntax syntax, String line,
			boolean breakFollows, int state, LineTokens scratch) {
		syntax.lexLine(line, breakFollows, state, scratch);
		int size = scratch.size();
		int[] tokens = new int[size * 3];
		for (int i = 0; i < size; ++i) {
			tokens[i * 3] = scratch.starts[i];
			tokens[i * 3 + 1] = scratch.ends[i];
			tokens[i * 3 + 2] = scratch.kinds[i];
		}
		return new LexedLine(line, state, tokens);
	}

	public String text() {
		return text;
	}

	public int state() {
		return state;
	}

	public int size() {
		return tokens.length / 3;
	}

	public int start(int index) {
		return tokens[index * 3];
	}

	public int end(int index) {
		return tokens[index * 3 + 1];
	}

	public Style style(int index) {
		return STYLES[tokens[index * 3 + 2] & 0xff];
	}

	public boolean inDirective(int index) {
		return (tokens[index * 3 + 2] & Tokens.IN_DIRECTIVE) != 0;
	}

	// -1 if there is none.
	public int indexAt(int offset) {
		int low = 0;
		int high = size() - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			if (start(mid) <= offset) {
				low = mid + 1;
			} else {
				high = mid - 1;
			}
		}
		return high >= 0 && offset < end(high) ? high : -1;
	}
}
