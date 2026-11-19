package de.markusfisch.android.shadereditor.syntax;

// The tokens of one line, with offsets into it. One instance is filled again
// for every line.
public final class LineTokens {
	int[] starts = new int[16];
	int[] ends = new int[16];
	int[] kinds = new int[16];
	private int size;

	public int size() {
		return size;
	}

	void set(int[] starts, int[] ends, int[] kinds, int size) {
		this.starts = starts;
		this.ends = ends;
		this.kinds = kinds;
		this.size = size;
	}
}
