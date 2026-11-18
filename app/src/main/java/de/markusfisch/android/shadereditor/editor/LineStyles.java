package de.markusfisch.android.shadereditor.editor;

import java.util.Arrays;

import de.markusfisch.android.shadereditor.syntax.LexedLine;
import de.markusfisch.android.shadereditor.syntax.LineTokens;
import de.markusfisch.android.shadereditor.syntax.Style;
import de.markusfisch.android.shadereditor.syntax.Syntax;

// The colours of a document's lines, kept current as it changes. A line's
// colours follow from its text and the lexer state it starts in, which is kept
// for every line in one byte. An edit tokenizes the changed lines and the lines
// below whose end state changed, up to budget lines, or as far as the shown
// lines and budget more; the lines below that are stale until a continuation,
// which may run on any thread, delivers their states.
public final class LineStyles {
	// How many changes a continuation may lag behind and still be used.
	private static final int HISTORY = 64;
	// How many states apply stores per call.
	private static final int ADOPT = 4096;
	private static final int CACHE = 256;
	private static final int BREAK_FOLLOWS = 0x100;
	private static final Style[] STYLES = Style.values();

	private final LineTokens tokens = new LineTokens();
	private final int[] changedLines = new int[HISTORY];
	private final Runs[] cache = new Runs[CACHE];
	private final Document document;
	private final Syntax syntax;
	private final int budget;
	private final Document.Listener listener = new Document.Listener() {
		@Override
		public void onChange(Document.Change change, Text before) {
			update(change, before);
		}

		@Override
		public void onUpdate() {
			if (walkPending) {
				walkPending = false;
				walk();
			}
		}
	};
	// The state every line ends in, with a gap at the last edit.
	private byte[] states;
	private int gapStart;
	private int gapEnd;
	private int lines;
	// Every line above staleFrom has its current state, and every line
	// from consistentFrom on has the state its text gives the state
	// stored above it. When staleFrom is the line count, all are current.
	private int staleFrom;
	private int consistentFrom;
	private boolean walkPending;
	private int shownFrom;
	private int shownTo;

	// budget: the lines on screen and a margin. The first budget lines are
	// tokenized now, the rest by continuations.
	public LineStyles(Document document, Syntax syntax, int budget) {
		this.document = document;
		this.syntax = syntax;
		this.budget = budget;
		lines = document.text().lineCount();
		states = new byte[lines + 64];
		gapStart = lines;
		gapEnd = states.length;
		consistentFrom = lines;
		walk();
		document.addListener(listener);
	}

	public Syntax syntax() {
		return syntax;
	}

	public void close() {
		document.removeListener(listener);
	}

	// Before the view draws [from, to): stale lines among them are tokenized
	// now. Returns whether a state changed.
	public boolean show(int from, int to) {
		shownFrom = from;
		shownTo = to;
		return Math.min(to, lines) > staleFrom && walk();
	}

	// A stale line has the colours of its old start state.
	public Runs runs(int n) {
		String line = document.text().line(n);
		int key = startState(n) | (n < lines - 1 ? BREAK_FOLLOWS : 0);
		int slot = (System.identityHashCode(line) * 31 + key) & (CACHE - 1);
		Runs runs = cache[slot];
		if (runs == null || runs.line.text() != line || runs.key != key) {
			runs = lex(line, key);
			cache[slot] = runs;
		}
		return runs;
	}

	// To be run on any thread and handed to apply; null if no line is stale.
	public Continuation continuation() {
		return staleFrom < lines
				? new Continuation(syntax, document.text(),
						document.revision(), staleFrom, startState(staleFrom))
				: null;
	}

	// Stores the states that still hold for the current text, at most ADOPT per
	// call; a result more than HISTORY changes old, or from a line that became
	// stale again, is dropped. Returns whether more of result remains.
	public boolean apply(Result result) {
		int revision = document.revision();
		if (revision - result.revision > HISTORY ||
				result.from > staleFrom ||
				startState(result.from) != result.state) {
			return false;
		}
		int end = result.from + result.states.length;
		for (int r = result.revision; r < revision; ) {
			end = Math.min(end, changedLines[++r % HISTORY]);
		}
		for (int stop = Math.min(end, staleFrom + ADOPT); staleFrom < stop; ) {
			settle(result.states[staleFrom - result.from] & 0xff);
		}
		return staleFrom < end;
	}

	int staleFrom() {
		return staleFrom;
	}

	int startState(int line) {
		return line == 0 ? Syntax.TEXT_START : get(line - 1);
	}

	private void update(Document.Change change, Text before) {
		int first = before.lineOf(change.start());
		int removed = change.removed().lineCount() - 1;
		int inserted = change.inserted().lineCount() - 1;
		int last = first + inserted;
		int oldEnd = get(first + removed);
		changedLines[document.revision() % HISTORY] = first;
		replace(first, removed + 1, inserted + 1);
		// Until they are tokenized, the new lines pass on what the old
		// ones did, so the lines below them keep their colours.
		Arrays.fill(states, gapStart - inserted - 1, gapStart, (byte) oldEnd);
		boolean current = staleFrom == lines;
		lines += inserted - removed;
		consistentFrom = !current && consistentFrom > first + removed
				? consistentFrom + inserted - removed
				: last + 1;
		if (first <= staleFrom) {
			staleFrom = first;
			walkPending = true;
		} else if (Math.min(shownTo, lines) > staleFrom) {
			walkPending = true;
		}
	}

	// Tokenizes budget lines from staleFrom on, or as many more as reach
	// the end of the shown lines, but no more than budget and the shown
	// lines. Returns whether a state was stored.
	private boolean walk() {
		int limit = budget;
		int to = Math.min(shownTo, lines);
		if (to > staleFrom) {
			limit = Math.max(limit, Math.min(to - staleFrom,
					budget + Math.max(0, to - shownFrom)));
		}
		Text text = document.text();
		int from = staleFrom;
		int state = startState(staleFrom);
		for (int n = limit; staleFrom < lines && n-- > 0; ) {
			state = syntax.lexLine(text.line(staleFrom),
					staleFrom < lines - 1, state, tokens);
			settle(state);
		}
		return staleFrom != from;
	}

	private void settle(int state) {
		int line = staleFrom;
		int old = get(line);
		set(line, state);
		if (state == old && line + 1 >= consistentFrom) {
			staleFrom = lines;
		} else {
			staleFrom = line + 1;
			consistentFrom = Math.max(consistentFrom, line + 2);
		}
	}

	private Runs lex(String line, int key) {
		LexedLine lexed = LexedLine.of(syntax, line,
				(key & BREAK_FOLLOWS) != 0, key & 0xff, tokens);
		int[] runs = new int[lexed.size() * 3];
		int size = 0;
		for (int i = 0, n = lexed.size(); i < n; ++i) {
			int start = lexed.start(i);
			int end = lexed.end(i);
			Style style = lexed.style(i);
			// What lies between two tokens of one style joins their run.
			if (size > 0 && runs[size - 1] == style.ordinal()) {
				runs[size - 2] = end;
			} else {
				runs[size++] = start;
				runs[size++] = end;
				runs[size++] = style.ordinal();
			}
		}
		return new Runs(lexed, key, Arrays.copyOf(runs, size));
	}

	private int get(int line) {
		return states[line < gapStart ? line : line + gapEnd - gapStart] &
				0xff;
	}

	private void set(int line, int state) {
		states[line < gapStart ? line : line + gapEnd - gapStart] =
				(byte) state;
	}

	// Replaces the states of remove lines at line by insert states; the
	// inserted ones end right before the gap.
	private void replace(int line, int remove, int insert) {
		moveGap(line);
		gapEnd += remove;
		if (gapEnd - gapStart < insert) {
			int tail = states.length - gapEnd;
			byte[] grown = new byte[(gapStart + tail + insert) * 2 + 64];
			System.arraycopy(states, 0, grown, 0, gapStart);
			System.arraycopy(states, gapEnd, grown, grown.length - tail, tail);
			gapEnd = grown.length - tail;
			states = grown;
		}
		gapStart += insert;
	}

	private void moveGap(int to) {
		if (to < gapStart) {
			int n = gapStart - to;
			System.arraycopy(states, to, states, gapEnd - n, n);
			gapStart -= n;
			gapEnd -= n;
		} else if (to > gapStart) {
			int n = to - gapStart;
			System.arraycopy(states, gapEnd, states, gapStart, n);
			gapStart += n;
			gapEnd += n;
		}
	}

	// What lies between two runs is white space.
	public static final class Runs {
		private final LexedLine line;
		private final int key;
		private final int[] runs;

		private Runs(LexedLine line, int key, int[] runs) {
			this.line = line;
			this.key = key;
			this.runs = runs;
		}

		public LexedLine line() {
			return line;
		}

		public int size() {
			return runs.length / 3;
		}

		public int start(int index) {
			return runs[index * 3];
		}

		public int end(int index) {
			return runs[index * 3 + 1];
		}

		public Style style(int index) {
			return STYLES[runs[index * 3 + 2]];
		}
	}

	// Holds only immutable values, so it may run on any thread.
	public static final class Continuation {
		private final Syntax syntax;
		private final Text text;
		private final int revision;
		private final int from;
		private final int state;

		private Continuation(Syntax syntax, Text text, int revision,
				int from, int state) {
			this.syntax = syntax;
			this.text = text;
			this.revision = revision;
			this.from = from;
			this.state = state;
		}

		public Result run(int maxLines) {
			int lines = text.lineCount();
			int end = (int) Math.min(lines, (long) from + maxLines);
			byte[] states = new byte[end - from];
			LineTokens tokens = new LineTokens();
			for (int line = from, s = state; line < end; ++line) {
				s = syntax.lexLine(text.line(line), line < lines - 1, s,
						tokens);
				states[line - from] = (byte) s;
			}
			return new Result(revision, from, state, states);
		}
	}

	public static final class Result {
		private final int revision;
		private final int from;
		private final int state;
		private final byte[] states;

		private Result(int revision, int from, int state, byte[] states) {
			this.revision = revision;
			this.from = from;
			this.state = state;
			this.states = states;
		}
	}
}
