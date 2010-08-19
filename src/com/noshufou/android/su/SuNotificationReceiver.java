package com.noshufou.android.su;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SuNotificationReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    
	    int callerUid = intent.getIntExtra("caller_uid", 0);

	    String notification_type = prefs.getString("pref_notification_type", "toast");
	    int lastNotificationUid = prefs.getInt("last_notification_uid", 0);
	    long lastNotificationTime = prefs.getLong("last_notification_time", 0);

        String notification_message = context.getString(R.string.notification_text,
            Util.getAppName(context, callerUid, false));

        if (notification_type.equals("notification")) {
            NotificationManager nm = 
            	(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            Intent notificationIntent = new Intent(context, Su.class);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

            String title = context.getString(R.string.app_name);

            Notification notification = new Notification(R.drawable.stat_su, notification_message,
            		System.currentTimeMillis());
            notification.setLatestEventInfo(context, title, notification_message, contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL|Notification.FLAG_ONLY_ALERT_ONCE;

            nm.notify(callerUid, notification);
        } else if (notification_type.equals("toast")) {
        	if ((callerUid != lastNotificationUid || 
        			lastNotificationTime + (60 * 1000) < System.currentTimeMillis())) {
        		Toast.makeText(context, notification_message, Toast.LENGTH_SHORT).show();
        	}
        }
        Editor editor = prefs.edit();
        editor.putInt("last_notification_uid", callerUid);
        editor.putLong("last_notification_time", System.currentTimeMillis());
        editor.commit();
    }
}
