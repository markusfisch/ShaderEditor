package de.markusfisch.android.shadereditor.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import de.markusfisch.android.shadereditor.R;

public class CodexAuthActivity extends AppCompatActivity {
	public static final String EXTRA_CODEX_API_KEY = "codex_api_key";

	private static final String ISSUER = "https://auth.openai.com";
	private static final String CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann";
	private static final String REDIRECT_URI = "http://localhost:1455/auth/callback";

	private String state;
	private String codeVerifier;
	private TextView statusView;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_codex_auth);

		statusView = findViewById(R.id.codex_auth_status);
		WebView webView = findViewById(R.id.codex_auth_webview);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				return handleUrl(request.getUrl().toString());
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				handleUrl(url);
			}
		});

		state = randomUrlSafe(32);
		codeVerifier = randomUrlSafe(64);
		String codeChallenge = sha256UrlSafe(codeVerifier);
		String authUrl = ISSUER + "/oauth/authorize?" +
				"response_type=code" +
				"&client_id=" + urlEncode(CLIENT_ID) +
				"&redirect_uri=" + urlEncode(REDIRECT_URI) +
				"&scope=" + urlEncode(
						"openid profile email offline_access api.connectors.read api.connectors.invoke") +
				"&code_challenge=" + urlEncode(codeChallenge) +
				"&code_challenge_method=S256" +
				"&id_token_add_organizations=true" +
				"&codex_cli_simplified_flow=true" +
				"&state=" + urlEncode(state);
		webView.loadUrl(authUrl);
	}

	private boolean handleUrl(@NonNull String url) {
		if (!url.startsWith(REDIRECT_URI)) {
			return false;
		}
		statusView.setText(R.string.codex_exchanging_token);

		Map<String, String> params = parseQuery(url);
		String returnedState = params.get("state");
		String code = params.get("code");
		if (returnedState == null || !returnedState.equals(state) || code == null) {
			setResult(Activity.RESULT_CANCELED);
			finish();
			return true;
		}

		new Thread(() -> {
			try {
				JSONObject tokenResponse = exchangeAuthorizationCode(code);
				String idToken = tokenResponse.getString("id_token");
				String apiKey = exchangeForApiKey(idToken);
				Intent data = new Intent().putExtra(EXTRA_CODEX_API_KEY, apiKey);
				runOnUiThread(() -> {
					setResult(Activity.RESULT_OK, data);
					finish();
				});
			} catch (Exception e) {
				runOnUiThread(() -> {
					statusView.setText(getString(R.string.codex_auth_failed, e.getMessage()));
					setResult(Activity.RESULT_CANCELED);
					finish();
				});
			}
		}).start();

		return true;
	}

	@NonNull
	private JSONObject exchangeAuthorizationCode(@NonNull String code) throws Exception {
		String body = "grant_type=authorization_code" +
				"&code=" + urlEncode(code) +
				"&redirect_uri=" + urlEncode(REDIRECT_URI) +
				"&client_id=" + urlEncode(CLIENT_ID) +
				"&code_verifier=" + urlEncode(codeVerifier);
		return postForm(ISSUER + "/oauth/token", body);
	}

	@NonNull
	private String exchangeForApiKey(@NonNull String idToken) throws Exception {
		String body = "grant_type=" + urlEncode("urn:ietf:params:oauth:grant-type:token-exchange") +
				"&client_id=" + urlEncode(CLIENT_ID) +
				"&requested_token=" + urlEncode("openai-api-key") +
				"&subject_token=" + urlEncode(idToken) +
				"&subject_token_type=" + urlEncode("urn:ietf:params:oauth:token-type:id_token");
		JSONObject response = postForm(ISSUER + "/oauth/token", body);
		return response.getString("access_token");
	}

	@NonNull
	private JSONObject postForm(@NonNull String endpoint, @NonNull String body) throws Exception {
		HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
		connection.setRequestMethod("POST");
		connection.setConnectTimeout(30000);
		connection.setReadTimeout(60000);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		try (OutputStream os = connection.getOutputStream()) {
			os.write(bytes);
		}
		int status = connection.getResponseCode();
		InputStream in = status >= 200 && status < 300
				? connection.getInputStream()
				: connection.getErrorStream();
		String responseText = readFully(in);
		if (status < 200 || status >= 300) {
			throw new IllegalStateException("HTTP " + status + ": " + responseText);
		}
		return new JSONObject(responseText);
	}

	@NonNull
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

	@NonNull
	private static Map<String, String> parseQuery(@NonNull String url) {
		Map<String, String> map = new HashMap<>();
		int i = url.indexOf('?');
		if (i < 0 || i + 1 >= url.length()) {
			return map;
		}
		String query = url.substring(i + 1);
		for (String pair : query.split("&")) {
			int e = pair.indexOf('=');
			if (e < 0) {
				continue;
			}
			String key = urlDecode(pair.substring(0, e));
			String value = urlDecode(pair.substring(e + 1));
			map.put(key, value);
		}
		return map;
	}

	@NonNull
	private static String randomUrlSafe(int bytes) {
		byte[] data = new byte[bytes];
		new SecureRandom().nextBytes(data);
		return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
	}

	@NonNull
	private static String sha256UrlSafe(@NonNull String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return Base64.encodeToString(hashed,
					Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@NonNull
	private static String urlEncode(@NonNull String value) {
		try {
			return java.net.URLEncoder.encode(value, "UTF-8");
		} catch (Exception e) {
			return value;
		}
	}

	@NonNull
	private static String urlDecode(@NonNull String value) {
		try {
			return java.net.URLDecoder.decode(value, "UTF-8");
		} catch (Exception e) {
			return value;
		}
	}
}
