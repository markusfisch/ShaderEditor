package de.markusfisch.android.shadereditor.activity.managers;

import android.app.Activity;
import android.os.AsyncTask;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
	private final ListView listView;
	private final ShaderAdapter shaderAdapter;

	public ShaderListManager(@NonNull Activity activity, @NonNull ListView listView,
			@NonNull DataSource dataSource, @NonNull Listener listener) {
		this.activity = activity;
		this.dataSource = dataSource;
		this.listener = listener;
		this.listView = listView;

		listView.setEmptyView(activity.findViewById(R.id.no_shaders));
		listView.setOnItemClickListener((parent, view, position, id) -> listener.onShaderSelected(id));
		listView.setOnItemLongClickListener((parent, view, position, id) -> {
			Object item = parent.getAdapter().getItem(position);
			if (item instanceof ShaderInfo) {
				// Use the name from the data record, or an empty string as a fallback.
				String name = ((ShaderInfo) item).name() != null ? ((ShaderInfo) item).name() : "";
				editShaderName(id, name);
			}
			return true;
		});

		shaderAdapter = new ShaderAdapter(activity);
		listView.setAdapter(shaderAdapter);
	}

	public void loadShadersAsync() {
		new LoadShadersTask(this).execute();
	}

	public String getShaderTitle(long id) {
		for (int i = 0; i < shaderAdapter.getCount(); ++i) {
			if (shaderAdapter.getItemId(i) == id) {
				Object item = shaderAdapter.getItem(i);
				if (item instanceof ShaderInfo info) {
					// Return the shader name, or its modification date as a fallback.
					return info.name() != null && !info.name().isEmpty()
							? info.name()
							: info.modified();
				}
			}
		}
		return "";
	}

	private void updateAdapter(List<ShaderInfo> shaders) {
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

	private long getSelectedShaderId() {
		return ((ShaderManager.Provider) activity).getShaderManager().getSelectedShaderId();
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
					dataSource.updateShaderName(id, newName);
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
	}

	private static class LoadShadersTask extends AsyncTask<Void, Void, List<ShaderInfo>> {
		private final WeakReference<ShaderListManager> managerRef;

		LoadShadersTask(ShaderListManager manager) {
			this.managerRef = new WeakReference<>(manager);
		}

		@Override
		protected List<ShaderInfo> doInBackground(Void... params) {
			ShaderListManager manager = managerRef.get();
			if (manager == null) {
				return null;
			}
			// Read preference when the task executes to get the latest setting.
			boolean sortByModification = ShaderEditorApp.preferences.sortByLastModification();
			return manager.dataSource.getShaders(sortByModification);
		}

		@Override
		protected void onPostExecute(List<ShaderInfo> shaders) {
			ShaderListManager manager = managerRef.get();
			if (manager != null) {
				manager.updateAdapter(shaders);
			}
		}
	}
}