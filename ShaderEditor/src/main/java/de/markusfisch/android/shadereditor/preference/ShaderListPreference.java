package de.markusfisch.android.shadereditor.preference;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.adapter.ShaderSpinnerAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class ShaderListPreference extends ListPreference
{
	private ShaderSpinnerAdapter adapter;

	public ShaderListPreference( Context context, AttributeSet attrs )
	{
		super( context, attrs );
	}

	@Override
	protected void onPrepareDialogBuilder( AlertDialog.Builder builder )
	{
		// don't call super.onPrepareDialogBuilder() because it'll check
		// for Entries and set up a setSingleChoiceItems() for them that
		// will never be used

		adapter = new ShaderSpinnerAdapter(
			getContext(),
			ShaderEditorApplication.dataSource.queryShaders() );

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

					ShaderListPreference.this.onClick(
						dialog,
						DialogInterface.BUTTON_POSITIVE );

					dialog.dismiss();
				}
			} );

		builder.setPositiveButton( null, null );
	}

	@Override
	protected void onDialogClosed( boolean positiveResult )
	{
		// close last cursor
		if( adapter != null )
			adapter.changeCursor( null );

		super.onDialogClosed( positiveResult );
	}
}
