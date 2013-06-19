package de.markusfisch.android.shadereditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.widget.ListAdapter;

import java.util.ArrayList;

public class ShaderListPreference
	extends ListPreference
{
	private ShaderDataSource dataSource;

	public ShaderListPreference( Context context, AttributeSet attrs )
	{
		super( context, attrs );
	}

	public static void saveShader( SharedPreferences p, long id )
	{
		SharedPreferences.Editor e = p.edit();

		e.putString(
			ShaderPreferenceActivity.SHADER,
			String.valueOf( id ) );
		e.commit();
	}

	@Override
	protected void onPrepareDialogBuilder( AlertDialog.Builder builder )
	{
		// don't call super.onPrepareDialogBuilder() because it'll check
		// for Entries and set up a setSingleChoiceItems() for them that
		// will never be used

		dataSource = new ShaderDataSource( getContext() );
		dataSource.open();

		final ListAdapter adapter = (ListAdapter)new ShaderAdapter(
			getContext(),
			dataSource.queryAll(),
			R.layout.shader_spinner_dropdown );

		builder.setSingleChoiceItems(
			adapter,
			0,
			new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialog, int which )
				{
					ShaderListPreference.saveShader(
						getSharedPreferences(),
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
		super.onDialogClosed( positiveResult );

		dataSource.close();
	}
}
