package com.noshufou.android.su;

import java.io.IOException;
import java.io.OutputStream;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class ResponseHelper 
{
	private static final String TAG = "SuRequest";

	public static void sendResult(Context context, AppStatus appStatus, String socketPath) {
        LocalSocket socket;
        try {
            socket = new LocalSocket();
            socket.connect(new LocalSocketAddress(socketPath,
                LocalSocketAddress.Namespace.FILESYSTEM));

            if (socket != null) {
                OutputStream os = socket.getOutputStream();
                String resultCode = appStatus.getPermissionCode();
                Log.d(TAG, "Sending result: " + resultCode);
                byte[] bytes = resultCode.getBytes("UTF-8");
                os.write(bytes);
                os.flush();
                os.close();
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        if (appStatus.permission == AppStatus.ALLOW && appStatus.dateAccess + 60*1000 < System.currentTimeMillis()) {
            sendNotification(context, appStatus.callerUid);
        }
    }
	
	private static void sendNotification(Context context, int callerUid) {
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.contains("preference_notification")) {
            Editor editor = prefs.edit();
            String newPref = "";
            if (prefs.getBoolean("preference_notification", false)) {
                Log.d(TAG, "Old notification setting = true. New notification setting = notification");
                newPref = "notification";
            } else {
                Log.d(TAG, "Old notification setting = false. new notification setting = none");
                newPref = "none";
            }
            editor.putString("preference_notification_type", newPref);
            editor.remove("preference_notification");
            editor.commit();
        }

        String notification_type = prefs.getString("preference_notification_type", "toast");
        if (notification_type.equals("none"))
            return;

        String notification_message = context.getString(R.string.notification_text,
            Su.getAppName(context, callerUid, false));

        if (notification_type.equals("notification")) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            Intent notificationIntent = new Intent(context, Su.class);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

            String title = context.getString(R.string.app_name_perms);

            Notification notification = new Notification(R.drawable.stat_su, notification_message, System.currentTimeMillis());
            notification.setLatestEventInfo(context, title, notification_message, contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL|Notification.FLAG_ONLY_ALERT_ONCE;

            nm.notify(callerUid, notification);
        } else if (notification_type.equals("toast")) {
            Toast.makeText(context, notification_message, Toast.LENGTH_SHORT).show();
        }
    }
}
