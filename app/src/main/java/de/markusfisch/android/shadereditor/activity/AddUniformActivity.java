package de.markusfisch.android.shadereditor.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import de.markusfisch.android.shadereditor.fragment.UniformPagesFragment;
import de.markusfisch.android.shadereditor.widget.SearchMenu;

public class AddUniformActivity extends AbstractContentActivity {
	public static final String STATEMENT = "statement";
	public static final int PICK_IMAGE = 1;
	public static final int CROP_IMAGE = 2;
	public static final int PICK_TEXTURE = 3;

	private SearchMenu.OnSearchListener onSearchListener;

	@Nullable
	private String currentSearchQuery = null;

	@Nullable
	public String getCurrentSearchQuery() {
		return currentSearchQuery;
	}

	public static void setAddUniformResult(Activity activity, String name) {
		Bundle bundle = new Bundle();
		bundle.putString(STATEMENT, name);

		Intent data = new Intent();
		data.putExtras(bundle);

		activity.setResult(RESULT_OK, data);
	}

	public void setSearchListener(SearchMenu.OnSearchListener onSearchListener) {
		this.onSearchListener = onSearchListener;
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		addMenuProvider(new MenuProvider() {
			@Override
			public void onCreateMenu(@NonNull android.view.Menu menu,
					@NonNull MenuInflater menuInflater) {
				SearchMenu.addSearchMenu(menu, menuInflater,
						(value) -> {
							currentSearchQuery = value;
							onSearchListener.filter(value);
						});
			}

			@Override
			public boolean onMenuItemSelected(@NonNull android.view.MenuItem menuItem) {
				return false;
			}
		}, this, Lifecycle.State.RESUMED);
	}

	@Override
	public void onActivityResult(
			int requestCode,
			int resultCode,
			Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK) {
			return;
		}

		Uri imageUri;

		if (requestCode == PICK_IMAGE &&
				data != null &&
				(imageUri = data.getData()) != null) {
			startActivityForResult(
					CropImageActivity.getIntentForImage(
							this,
							imageUri),
					CROP_IMAGE);
		} else if (requestCode == CROP_IMAGE ||
				requestCode == PICK_TEXTURE) {
			setResult(RESULT_OK, data);
			finish();
		}
	}

	@Override
	protected Fragment defaultFragment() {
		startActivityForIntent(getIntent());
		return new UniformPagesFragment();
	}

	private void startActivityForIntent(Intent intent) {
		if (intent == null) {
			return;
		}

		String type;
		if (!Intent.ACTION_SEND.equals(intent.getAction()) ||
				(type = intent.getType()) == null ||
				!type.startsWith("image/")) {
			return;
		}

		Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
		if (imageUri == null) {
			return;
		}

		startActivity(CropImageActivity.getIntentForImage(this, imageUri));
	}
}
