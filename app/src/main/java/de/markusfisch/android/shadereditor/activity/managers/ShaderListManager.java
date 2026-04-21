package de.markusfisch.android.shadereditor.activity.managers;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.ShaderAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.DataRecords.ShaderInfo;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.view.SoftKeyboard;

public class ShaderListManager {
	public interface Listener {
		void onShaderSelected(long id);

		void onShaderRenamed(long id, @NonNull String name);

		void onAllShadersDeleted();

		void onShadersLoaded(@NonNull List<ShaderInfo> shaders);
	}

	private final Activity activity;
	private final DataSource dataSource;
	private final Listener listener;
	private final ShaderAdapter shaderAdapter;

	// Use an ExecutorService for background tasks and a Handler for the main thread.
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final CoalescingReloadGate reloadGate = new CoalescingReloadGate();

	public ShaderListManager(@NonNull Activity activity,
			@NonNull ListView listView,
			@NonNull DataSource dataSource,
			@NonNull Listener listener) {
		this.activity = activity;
		this.dataSource = dataSource;
		this.listener = listener;

		listView.setEmptyView(activity.findViewById(R.id.no_shaders));
		listView.setOnItemClickListener(
				(parent, view, position, id) ->
						listener.onShaderSelected(id));
		listView.setOnItemLongClickListener(
				(parent, view, position, id) -> {
					var title = ((ShaderAdapter) parent.getAdapter()).getItem(
							position).getTitle();
					editShaderName(id, title != null ? title : "");
					return true;
				});

		shaderAdapter = new ShaderAdapter(activity);
		listView.setAdapter(shaderAdapter);
	}

	/**
	 * Loads shaders from the database asynchronously.
	 */
	public void loadShadersAsync() {
		if (executor.isShutdown() ||
				!reloadGate.request()) {
			return;
		}
		submitLoad();
	}

	public void destroy() {
		executor.shutdownNow();
		handler.removeCallbacksAndMessages(null);
	}

	private void submitLoad() {
		try {
			executor.execute(() -> {
				List<ShaderInfo> shaders = dataSource.shader.getShaders(
						ShaderEditorApp.preferences.sortByLastModification());
				handler.post(() -> onShadersLoaded(shaders));
			});
		} catch (RejectedExecutionException ignored) {
			reloadGate.abort();
		}
	}

	private void onShadersLoaded(List<ShaderInfo> shaders) {
		if (reloadGate.finish()) {
			if (!executor.isShutdown() && isActivityAlive()) {
				submitLoad();
			} else {
				reloadGate.abort();
			}
			return;
		}
		if (!isActivityAlive()) {
			return;
		}
		List<ShaderInfo> shaderList = shaders != null
				? shaders
				: new ArrayList<>();
		listener.onShadersLoaded(shaderList);
		updateAdapter(shaderList);
	}

	private boolean isActivityAlive() {
		return !activity.isFinishing() && !activity.isDestroyed();
	}

	private void updateAdapter(List<ShaderInfo> shaders) {
		if (!isActivityAlive()) {
			return;
		}

		View progressBar = activity.findViewById(R.id.progress_bar);
		View noShadersMessage = activity.findViewById(R.id.no_shaders);

		if (progressBar != null) {
			progressBar.setVisibility(View.GONE);
		}

		List<ShaderInfo> data = shaders != null ? shaders : new ArrayList<>();

		if (data.isEmpty()) {
			if (noShadersMessage != null) {
				noShadersMessage.setVisibility(View.VISIBLE);
			}
			listener.onAllShadersDeleted();
		} else if (noShadersMessage != null) {
			noShadersMessage.setVisibility(View.GONE);
		}
		shaderAdapter.setData(data);
	}

	public void setSelectedShaderId(long id) {
		if (shaderAdapter != null) {
			shaderAdapter.setSelectedId(id);
		}
	}

	private void editShaderName(final long id, @NonNull String name) {
		View view = activity.getLayoutInflater().inflate(
				R.layout.dialog_rename_shader, null);
		final EditText nameView = view.findViewById(R.id.name);
		nameView.setText(name);

		new MaterialAlertDialogBuilder(activity)
				.setTitle(R.string.rename_shader)
				.setView(view)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					String newName = nameView.getText().toString();
					dataSource.shader.updateShaderName(id, newName);
					listener.onShaderRenamed(id, newName);
					SoftKeyboard.hide(activity, nameView);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}
}
