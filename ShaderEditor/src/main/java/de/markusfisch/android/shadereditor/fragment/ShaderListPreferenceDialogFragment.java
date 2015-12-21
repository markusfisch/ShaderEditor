package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.adapter.ShaderSpinnerAdapter;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreferenceDialogFragmentCompat;

public class ShaderListPreferenceDialogFragment
	extends ListPreferenceDialogFragmentCompat
{
	private ShaderSpinnerAdapter adapter;

	public static ShaderListPreferenceDialogFragment newInstance(
		String key )
	{
		Bundle bundle = new Bundle();
		bundle.putString( ARG_KEY, key );

		ShaderListPreferenceDialogFragment fragment =
			new ShaderListPreferenceDialogFragment();
		fragment.setArguments( bundle );

		return fragment;
	}

	@Override
	public void onDialogClosed( boolean positiveResult )
	{
		// close last cursor
		if( adapter != null )
			adapter.changeCursor( null );

		super.onDialogClosed( positiveResult );
	}

	@Override
	protected void onPrepareDialogBuilder( AlertDialog.Builder builder )
	{
		// don't call super.onPrepareDialogBuilder() because it'll check
		// for Entries and set up a setSingleChoiceItems() for them that
		// will never be used

		adapter = new ShaderSpinnerAdapter(
			getContext(),
			ShaderEditorApplication.dataSource.getShaders() );

		builder.setSingleChoiceItems(
			adapter,
			0,
			new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(
					DialogInterface dialog,
					int which )
				{
					ShaderEditorApplication
						.preferences
						.setWallpaperShader(
							adapter.getItemId( which ) );

					ShaderListPreferenceDialogFragment
						.this
						.onClick(
							dialog,
							DialogInterface.BUTTON_POSITIVE );

					dialog.dismiss();
				}
			} );

		builder.setPositiveButton( null, null );
	}
}
