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
	}

	public static void saveBattery( boolean low )
	{
		if( !ShaderEditorApplication
				.preferences
				.saveBattery() )
			// set batteryLow and render mode because
			// the setting may have changed while in
			// battery mode
			low = false;

		ShaderEditorApplication.batteryLow = low;

		ShaderWallpaperService.setRenderMode( low ?
			GLSurfaceView.RENDERMODE_WHEN_DIRTY :
			GLSurfaceView.RENDERMODE_CONTINUOUSLY );
	}
}
