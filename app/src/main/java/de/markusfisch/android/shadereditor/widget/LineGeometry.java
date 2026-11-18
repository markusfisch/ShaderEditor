package de.markusfisch.android.shadereditor.widget;

// Where the lines of a CodeView are, from the top of line 0: every line has the
// same height.
final class LineGeometry {
	private int lineHeight = 1;

	void setLineHeight(int lineHeight) {
		this.lineHeight = Math.max(1, lineHeight);
	}

	float top(int n) {
		return n * (float) lineHeight;
	}

	// The first line above all lines, the last below them.
	int lineAt(float y, int lineCount) {
		int line = (int) Math.floor(y / lineHeight);
		return Math.max(0, Math.min(lineCount - 1, line));
	}

	// Nearest scrollY that shows line n between visibleTop and visibleBottom
	// from the view's top; the top of the line wins when it does not fit.
	int reveal(int scrollY, int n, float visibleTop, float visibleBottom) {
		float top = top(n);
		float bottom = top + lineHeight;
		if (bottom > scrollY + visibleBottom) {
			scrollY = (int) Math.ceil(bottom - visibleBottom);
		}
		if (top < scrollY + visibleTop) {
			scrollY = (int) Math.floor(top - visibleTop);
		}
		return scrollY;
	}

	// above and below: padding and what covers the view's bottom. The last
	// line rests at the bottom; 0 when everything fits.
	int maxScroll(int lineCount, float above, float below, int viewHeight) {
		return Math.max(0, (int) Math.ceil(above + top(lineCount) + below -
				viewHeight));
	}
}
