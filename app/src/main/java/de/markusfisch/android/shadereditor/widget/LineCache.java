package de.markusfisch.android.shadereditor.widget;

import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.os.Build;

import androidx.annotation.RequiresApi;

import de.markusfisch.android.shadereditor.editor.LineStyles;

// The lines a CodeView laid out, found by the identity of their string, so a
// line that only moved or came back is not laid out again. From Android 10 on,
// a line's drawing is recorded once and replayed until its string or colours
// change.
final class LineCache {
	private static final int SIZE = 512;

	private final CodeAppearance appearance;
	private final Line[] lines = new Line[SIZE];
	private int frame;
	private float widest;

	LineCache(CodeAppearance appearance) {
		this.appearance = appearance;
	}

	// Lines got for drawing from now on are this frame's.
	void nextFrame() {
		++frame;
	}

	// runs may be null. A line got for drawing whose place another line of this
	// frame took is laid out for once and not kept.
	Line get(String text, LineStyles.Runs runs, boolean drawing) {
		int slot = (System.identityHashCode(text) * 0x9e3779b9 >>> 16) &
				(SIZE - 1);
		Line line = lines[slot];
		if (line != null && line.shows(text, runs)) {
			line.runs = runs;
		} else if (drawing && line != null && line.frame == frame) {
			return new Line(text, runs, layout(text, runs), false);
		} else if (line == null) {
			line = lines[slot] = new Line(text, runs, layout(text, runs),
					true);
		} else {
			line.set(text, runs, layout(text, runs));
		}
		if (drawing) {
			line.frame = frame;
		}
		return line;
	}

	float widest() {
		return widest;
	}

	// As after a change of the font.
	void clear() {
		for (int i = 0; i < SIZE; ++i) {
			if (lines[i] != null) {
				lines[i].discard();
				lines[i] = null;
			}
		}
		widest = 0;
	}

	private LineLayout layout(String text, LineStyles.Runs runs) {
		LineLayout layout = appearance.layout(text, runs);
		widest = Math.max(widest, layout.width());
		return layout;
	}

	static boolean sameRuns(LineStyles.Runs a, LineStyles.Runs b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null || a.size() != b.size()) {
			return false;
		}
		for (int i = a.size(); i-- > 0; ) {
			if (a.start(i) != b.start(i) || a.end(i) != b.end(i) ||
					a.style(i) != b.style(i)) {
				return false;
			}
		}
		return true;
	}

	static final class Line {
		private final boolean kept;
		private String text;
		private LineStyles.Runs runs;
		private LineLayout layout;
		private Object recording;
		private int frame;

		private Line(String text, LineStyles.Runs runs, LineLayout layout,
				boolean kept) {
			this.kept = kept;
			set(text, runs, layout);
		}

		LineLayout layout() {
			return layout;
		}

		boolean shows(String text, LineStyles.Runs runs) {
			return this.text == text && sameRuns(this.runs, runs);
		}

		boolean needsRecording(Canvas canvas) {
			return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
					recordable(canvas) && (recording == null ||
					!((Recording) recording).isCurrent());
		}

		// At least the part from left to right; recorded first if it needs
		// recording and record is true, else drawn directly.
		void draw(Canvas canvas, float left, float right, int height,
				boolean record) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
					recordable(canvas) && (record || !needsRecording(canvas))) {
				if (recording == null) {
					recording = new Recording();
				}
				((Recording) recording).draw(canvas, layout, height);
			} else {
				layout.draw(canvas, left, right);
			}
		}

		private boolean recordable(Canvas canvas) {
			return kept && layout.isWhole() && canvas.isHardwareAccelerated();
		}

		private void set(String text, LineStyles.Runs runs,
				LineLayout layout) {
			this.text = text;
			this.runs = runs;
			this.layout = layout;
			if (recording != null &&
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				((Recording) recording).valid = false;
			}
		}

		private void discard() {
			if (recording != null &&
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				((Recording) recording).node.discardDisplayList();
			}
		}
	}

	@RequiresApi(Build.VERSION_CODES.Q)
	private static final class Recording {
		private final RenderNode node = new RenderNode("line");
		private boolean valid;

		private boolean isCurrent() {
			return valid && node.hasDisplayList();
		}

		private void draw(Canvas canvas, LineLayout layout, int height) {
			if (!isCurrent()) {
				node.setPosition(0, 0, (int) Math.ceil(layout.width()) + 1,
						height);
				node.setClipToBounds(false);
				RecordingCanvas recording = node.beginRecording();
				try {
					layout.draw(recording, 0, Float.MAX_VALUE);
				} finally {
					node.endRecording();
				}
				valid = true;
			}
			canvas.drawRenderNode(node);
		}
	}
}
