package de.markusfisch.android.shadereditor.service;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import de.markusfisch.android.shadereditor.R;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {
	private static int counter = 0;
	private static Long lastNotificationTime = null;

	@Override
	public synchronized void onNotificationPosted(StatusBarNotification sbn) {
		counter = getActiveNotifications().length;
		lastNotificationTime = sbn.getPostTime();
	}

	@Override
	public synchronized void onNotificationRemoved(StatusBarNotification sbn) {
		counter = getActiveNotifications().length;
	}

	@Override
	public void onListenerConnected() {
		invalidate();
	}

	@Override
	public void onListenerDisconnected() {
		counter = 0;
		lastNotificationTime = null;
	}

	private void invalidate() {
		requirePermissions(this);
		StatusBarNotification[] notifications = getActiveNotifications();
		Long lastTime;
		synchronized (this) {
			counter = notifications.length;
			lastTime = lastNotificationTime;
		}
		for (StatusBarNotification notification : notifications) {
			long time = notification.getPostTime();
			if (lastTime == null || time > lastTime) {
				lastTime = time;
			}
		}
		synchronized (this) {
			lastNotificationTime = lastTime;
		}
	}

	public static void requirePermissions(@NonNull Context context) {
		if (isPermissionRequired(context)) {
			requestNotificationPermission(context);
		}
	}

	public static boolean isPermissionRequired(@NonNull Context context) {
		ComponentName cn = new ComponentName(context, NotificationService.class);
		String flat = Settings.Secure.getString(context.getContentResolver(),
				"enabled_notification_listeners");
		final boolean enabled = flat != null && flat.contains(cn.flattenToString());
		return !enabled;
	}

	private static void requestNotificationPermission(@NonNull Context context) {
		new Handler(Looper.getMainLooper()).post(() -> new AlertDialog.Builder(context)
				.setTitle(R.string.enable_notification_listener_title)
				.setMessage(R.string.enable_notification_listener)
				.setPositiveButton(R.string.go_to_settings, (dialog, id) -> {
					Intent intent = new Intent("android.settings" +
							".ACTION_NOTIFICATION_LISTENER_SETTINGS");
					context.startActivity(intent);
				})
				.setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss())
				.show());
	}


	public static int getCount() {
		return counter;
	}

	@Nullable
	public static Long getLastNotificationTime() {
		return lastNotificationTime;
	}
}
