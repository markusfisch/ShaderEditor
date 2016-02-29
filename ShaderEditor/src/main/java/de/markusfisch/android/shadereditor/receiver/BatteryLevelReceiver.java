package de.markusfisch.android.shadereditor.receiver;

import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.service.ShaderWallpaperService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;

public class BatteryLevelReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive( Context context, Intent intent )
	{
		String action = intent.getAction();

		if( Intent.ACTION_BATTERY_LOW.equals( action ) )
			saveBattery( true );
		else if( Intent.ACTION_BATTERY_OKAY.equals( action ) )
			saveBattery( false );
		else if( Intent.ACTION_POWER_CONNECTED.equals( action ) )
			saveBattery( false );
	}

	public static void saveBattery( boolean low )
	{
		if( !ShaderEditorApplication
				.preferences
				.saveBattery() )
			low = false;
			// fall through to update batteryLow and
			// render mode because the preference may
			// have changed while battery is low

		ShaderEditorApplication
			.preferences
			.setBatteryLow( low );

		ShaderWallpaperService.setRenderMode( low ?
			GLSurfaceView.RENDERMODE_WHEN_DIRTY :
			GLSurfaceView.RENDERMODE_CONTINUOUSLY );
	}
}
