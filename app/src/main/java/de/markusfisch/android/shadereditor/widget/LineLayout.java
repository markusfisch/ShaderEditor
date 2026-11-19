package de.markusfisch.android.shadereditor.widget;

import android.graphics.Canvas;
import android.graphics.Paint;

// One line laid out with its colours. Offsets count UTF-16 units into the line;
// x is measured from the start of the line, y from its top.
interface LineLayout extends TouchSelection.Line {
	// Longer lines are drawn without colours, one column per character, and
	// only where they are visible.
	int LONG = 10_000;

	float xOf(int offset);

	int offsetAt(float x);

	float width();

	// Whether draw draws the whole line whatever left and right are, so its
	// drawing may be recorded and replayed.
	boolean isWhole();

	// At least the part from left to right.
	void draw(Canvas canvas, float left, float right);

	void drawSelection(Canvas canvas, int from, int to, Paint paint);
}
