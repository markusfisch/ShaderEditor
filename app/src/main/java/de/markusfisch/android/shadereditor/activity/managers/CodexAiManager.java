package de.markusfisch.android.shadereditor.activity.managers;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.activity.PreferencesActivity;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.fragment.EditorFragment;

public class CodexAiManager {
	private static final String RESPONSES_URL = "https://api.openai.com/v1/responses";
	private static final List<String> MODELS = Arrays.asList(
			"gpt-5.4",
			"gpt-5.4-pro",
			"gpt-5.4-mini",
			"gpt-5.4-nano",
			"gpt-5-mini",
			"gpt-5-nano",
			"gpt-5.3-codex",
			"gpt-5.3-codex-spark",
			"gpt-5.2");
	private static final List<String> REASONING_EFFORTS = Arrays.asList(
			"none",
			"minimal",
			"low",
			"medium",
			"high",
			"xhigh");

	private final AppCompatActivity activity;
	private final EditorFragment editorFragment;
	private final ShaderManager shaderManager;
	private final ShaderViewManager shaderViewManager;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private String previousResponseId;

	public CodexAiManager(@NonNull AppCompatActivity activity,
			@NonNull EditorFragment editorFragment,
			@NonNull ShaderManager shaderManager,
			@NonNull ShaderViewManager shaderViewManager) {
		this.activity = activity;
		this.editorFragment = editorFragment;
		this.shaderManager = shaderManager;
		this.shaderViewManager = shaderViewManager;
	}

	public void show() {
		if (!ShaderEditorApp.preferences.hasCodexApiKey()) {
			Toast.makeText(activity, R.string.codex_sign_in_required, Toast.LENGTH_LONG).show();
			activity.startActivity(new Intent(activity, PreferencesActivity.class));
			return;
		}

		View view = LayoutInflater.from(activity).inflate(R.layout.dialog_ai_assistant, null);
		Spinner modelSpinner = view.findViewById(R.id.ai_model);
		Spinner effortSpinner = view.findViewById(R.id.ai_reasoning);
		EditText promptView = view.findViewById(R.id.ai_prompt);
		ProgressBar progressBar = view.findViewById(R.id.ai_progress);
		TextView statusView = view.findViewById(R.id.ai_status);
		Button sendBtn = view.findViewById(R.id.ai_send);
		Button newChatBtn = view.findViewById(R.id.ai_new_chat);

		modelSpinner.setAdapter(new ArrayAdapter<>(activity,
				android.R.layout.simple_spinner_dropdown_item,
				MODELS));
		effortSpinner.setAdapter(new ArrayAdapter<>(activity,
				android.R.layout.simple_spinner_dropdown_item,
				REASONING_EFFORTS));
		effortSpinner.setSelection(REASONING_EFFORTS.indexOf("low"));

		sendBtn.setOnClickListener(v -> {
			String prompt = promptView.getText().toString().trim();
			if (prompt.isEmpty()) {
				return;
			}
			sendPrompt(
					modelSpinner.getSelectedItem().toString(),
					effortSpinner.getSelectedItem().toString(),
					prompt,
					progressBar,
					statusView,
					sendBtn);
		});

		newChatBtn.setOnClickListener(v -> {
			previousResponseId = null;
			statusView.setText(R.string.ai_new_chat_started);
		});

		new MaterialAlertDialogBuilder(activity)
				.setTitle(R.string.ai_assistant)
				.setView(view)
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	public void destroy() {
		executor.shutdownNow();
	}

	private void sendPrompt(@NonNull String model,
			@NonNull String effort,
			@NonNull String prompt,
			@NonNull ProgressBar progressBar,
			@NonNull TextView statusView,
			@NonNull Button sendBtn) {
		statusView.setText(R.string.ai_generating);
		progressBar.setVisibility(View.VISIBLE);
		sendBtn.setEnabled(false);

		executor.execute(() -> {
			try {
				JSONObject response = callResponsesApi(model, effort, prompt);
				String updatedShader = extractTextOutput(response);
				if (updatedShader == null || updatedShader.isEmpty()) {
					throw new IllegalStateException("No shader output returned");
				}
				previousResponseId = response.optString("id", previousResponseId);
				activity.runOnUiThread(() -> {
					editorFragment.setText(updatedShader);
					shaderManager.setModified(true);
					shaderViewManager.setFragmentShader(updatedShader);
					statusView.setText(R.string.ai_done);
					progressBar.setVisibility(View.GONE);
					sendBtn.setEnabled(true);
				});
			} catch (Exception e) {
				activity.runOnUiThread(() -> {
					statusView.setText(activity.getString(R.string.ai_failed, e.getMessage()));
					progressBar.setVisibility(View.GONE);
					sendBtn.setEnabled(true);
				});
			}
		});
	}

	@NonNull
	private JSONObject callResponsesApi(@NonNull String model,
			@NonNull String effort,
			@NonNull String prompt) throws Exception {
		JSONObject body = new JSONObject();
		body.put("model", model);
		body.put("reasoning", new JSONObject().put("effort", effort));

		JSONArray input = new JSONArray();
		input.put(new JSONObject()
				.put("role", "system")
				.put("content", "You are a shader assistant. Return only valid fragment shader source code."));
		input.put(new JSONObject()
				.put("role", "user")
				.put("content", "Current shader:\n" + editorFragment.getText()
						+ "\n\nTask:\n" + prompt
						+ "\n\nReturn only the full updated shader source."));
		body.put("input", input);

		if (previousResponseId != null && !previousResponseId.isEmpty()) {
			body.put("previous_response_id", previousResponseId);
		}

		HttpURLConnection connection = (HttpURLConnection) new URL(RESPONSES_URL).openConnection();
		connection.setRequestMethod("POST");
		connection.setConnectTimeout(30000);
		connection.setReadTimeout(120000);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization",
				"Bearer " + ShaderEditorApp.preferences.getCodexApiKey());

		byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
		try (OutputStream os = connection.getOutputStream()) {
			os.write(bytes);
		}

		int status = connection.getResponseCode();
		InputStream in = status >= 200 && status < 300
				? connection.getInputStream()
				: connection.getErrorStream();
		String text = readFully(in);
		if (status < 200 || status >= 300) {
			throw new IllegalStateException("HTTP " + status + ": " + text);
		}
		return new JSONObject(text);
	}

	private static String readFully(InputStream in) throws Exception {
		if (in == null) {
			return "";
		}
		try (InputStream input = in;
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[4096];
			int read;
			while ((read = input.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			return out.toString("UTF-8");
		}
	}

	private static String extractTextOutput(@NonNull JSONObject response) {
		String outputText = response.optString("output_text", null);
		if (outputText != null && !outputText.isEmpty()) {
			return outputText;
		}
		JSONArray output = response.optJSONArray("output");
		if (output == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < output.length(); ++i) {
			JSONObject item = output.optJSONObject(i);
			if (item == null || !"message".equals(item.optString("type"))) {
				continue;
			}
			JSONArray content = item.optJSONArray("content");
			if (content == null) {
				continue;
			}
			for (int j = 0; j < content.length(); ++j) {
				JSONObject part = content.optJSONObject(j);
				if (part == null) {
					continue;
				}
				String text = part.optString("text", null);
				if (text != null && !text.isEmpty()) {
					sb.append(text);
				}
			}
		}
		return sb.toString();
	}
}
