package de.markusfisch.android.shadereditor.activity.managers;

final class CoalescingReloadGate {
	private boolean loading = false;
	private boolean reloadQueued = false;

	public synchronized boolean request() {
		if (loading) {
			reloadQueued = true;
			return false;
		}
		loading = true;
		return true;
	}

	public synchronized boolean finish() {
		if (reloadQueued) {
			reloadQueued = false;
			return true;
		}
		loading = false;
		return false;
	}

	public synchronized void abort() {
		loading = false;
		reloadQueued = false;
	}
}
