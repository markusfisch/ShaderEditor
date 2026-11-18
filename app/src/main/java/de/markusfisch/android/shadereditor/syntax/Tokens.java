package de.markusfisch.android.shadereditor.syntax;

import java.util.Arrays;

/**
 * The tokens of one text, in order and without overlap.
 */
public final class Tokens {
	static final int IN_DIRECTIVE = 1 << 8;

	private static final Style[] STYLES = Style.values();

	private final int[] starts;
	private final int[] ends;
	private final int[] kinds;

	Tokens(int[] starts, int[] ends, int[] kinds, int size) {
		this.starts = Arrays.copyOf(starts, size);
		this.ends = Arrays.copyOf(ends, size);
		this.kinds = Arrays.copyOf(kinds, size);
	}

	public int size() {
		return starts.length;
	}

	public int start(int index) {
		return starts[index];
	}

	public int end(int index) {
		return ends[index];
	}

	public Style style(int index) {
		return STYLES[kinds[index] & 0xff];
	}

	public boolean inDirective(int index) {
		return (kinds[index] & IN_DIRECTIVE) != 0;
	}

	/** Index of the token that contains offset, or -1 if there is none. */
	public int indexAt(int offset) {
		int low = 0;
		int high = starts.length - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			if (starts[mid] <= offset) {
				low = mid + 1;
			} else {
				high = mid - 1;
			}
		}
		return high >= 0 && offset < ends[high] ? high : -1;
	}
}
