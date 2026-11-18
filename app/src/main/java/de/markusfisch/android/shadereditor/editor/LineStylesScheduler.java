package de.markusfisch.android.shadereditor.editor;

import java.util.concurrent.Executor;

// Runs the continuations of a LineStyles on a background executor and applies
// each result on the main thread, one apply call per message, so the main
// thread never stores more than one call's states at a time.
public final class LineStylesScheduler {
	public interface Callback {
		// Lines may need drawing again.
		void onApplied();
	}
	// The most lines one continuation tokenizes before its result is
	// stored, so the lines near the edit get their colours early.
	private static final int CHUNK = 16384;

	private final LineStyles styles;
	private final Document document;
	private final Executor background;
	private final Executor main;
	private final Callback callback;
	private final Document.Listener listener = new Document.Listener() {
		@Override
		public void onUpdate() {
			schedule();
		}
	};
	private boolean running;
	private boolean closed;

	public LineStylesScheduler(Document document, LineStyles styles,
			Executor background, Executor main,
			Callback callback) {
		this.document = document;
		this.styles = styles;
		this.background = background;
		this.main = main;
		this.callback = callback;
		document.addListener(listener);
		schedule();
	}

	// Results still on their way are dropped.
	public void close() {
		closed = true;
		document.removeListener(listener);
	}

	private void schedule() {
		if (closed || running) {
			return;
		}
		LineStyles.Continuation continuation = styles.continuation();
		if (continuation == null) {
			return;
		}
		running = true;
		background.execute(() -> {
			LineStyles.Result result = continuation.run(CHUNK);
			main.execute(() -> deliver(result));
		});
	}

	private void deliver(LineStyles.Result result) {
		if (closed) {
			return;
		}
		boolean more = styles.apply(result);
		callback.onApplied();
		if (more) {
			main.execute(() -> deliver(result));
			return;
		}
		running = false;
		schedule();
	}
}
