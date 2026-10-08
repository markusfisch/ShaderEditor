package de.markusfisch.android.shadereditor.opengl;

/** Tracks rendered time without including pauses or depending on a GL context. */
final class RuntimeClock {
	private long elapsedNanos;
	private long lastFrameNanos;
	private boolean hasLastFrame;

	synchronized long advance(long now) {
		if (hasLastFrame) {
			elapsedNanos += now - lastFrameNanos;
		}
		lastFrameNanos = now;
		hasLastFrame = true;
		return elapsedNanos;
	}

	synchronized void pause() {
		hasLastFrame = false;
	}

	synchronized long getElapsedNanos() {
		return elapsedNanos;
	}

	synchronized void setElapsedNanos(long nanos) {
		elapsedNanos = Math.max(0, nanos);
		hasLastFrame = false;
	}
}
