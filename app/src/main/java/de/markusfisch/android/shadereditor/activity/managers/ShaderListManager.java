package de.markusfisch.android.shadereditor.activity.managers;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.ShaderAdapter;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.database.DataRecords.ShaderInfo;
import de.markusfisch.android.shadereditor.database.DataSource;
import de.markusfisch.android.shadereditor.view.SoftKeyboard;

public class ShaderListManager {

	private final Activity activity;
	private final DataSource dataSource;
	private final Listener listener;
	private final ShaderAdapter shaderAdapter;

	// Use an ExecutorService for background tasks and a Handler for the main thread.
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final Handler handler = new Handler(Looper.getMainLooper());

	public ShaderListManager(@NonNull Activity activity, @NonNull ListView listView,
			@NonNull DataSource dataSource, @NonNull Listener listener) {
		this.activity = activity;
		this.dataSource = dataSource;
		this.listener = listener;

		listView.setEmptyView(activity.findViewById(R.id.no_shaders));
		listView.setOnItemClickListener((parent, view, position, id) -> listener.onShaderSelected(id));
		listView.setOnItemLongClickListener((parent, view, position, id) -> {
			var title = ((ShaderAdapter) parent.getAdapter()).getItem(position).getTitle();
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
		// Use a WeakReference to avoid leaking the context if the activity is destroyed.
		WeakReference<ShaderListManager> managerRef = new WeakReference<>(this);

		executor.execute(() -> {
			ShaderListManager manager = managerRef.get();
			if (manager == null) {
				return;
			}

			// Perform the long-running database query on a background thread.
			boolean sortByModification = ShaderEditorApp.preferences.sortByLastModification();
			List<ShaderInfo> shaders = manager.dataSource.shader.getShaders(sortByModification);

			// Post the result back to the main thread.
			manager.handler.post(() -> {
				ShaderListManager finalManager = managerRef.get();
				// Ensure the manager and its activity are still valid.
				if (finalManager != null && !finalManager.activity.isFinishing()) {
					List<ShaderInfo> shaderList = shaders != null ? shaders : new ArrayList<>();
					finalManager.listener.onShadersLoaded(shaderList);
					finalManager.updateAdapter(shaderList);
				}
			});
		});
	}

	private void updateAdapter(List<ShaderInfo> shaders) {
		// The isFinishing() check is already here, making it robust.
		if (activity.isFinishing()) {
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
		View view = activity.getLayoutInflater().inflate(R.layout.dialog_rename_shader, null);
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

	public interface Listener {
		void onShaderSelected(long id);

		void onShaderRenamed(long id, @NonNull String name);

		void onAllShadersDeleted();

		void onShadersLoaded(@NonNull List<ShaderInfo> shaders);
	}

}