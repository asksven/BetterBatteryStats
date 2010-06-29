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
import android.util.Log;
import android.widget.Toast;

public class SendResponseHelper 
{
	private static final String TAG = "SuRequest";
	public static final String ALLOW = "ALLOW";
	public static final String DENY = "DENY";

	private DBHelper db;
    private DBHelper.AppStatus app_status;
    
    Context context;
    SharedPreferences prefs;
    
    public SendResponseHelper(Context context)
    {
    	this.context = context;
    	db = new DBHelper(this.context);
    }
	
	public void sendResult(String resultCode, boolean remember, int callerUid, int desiredUid, String desiredCmd, String socketPath) {
        LocalSocket socket;
        if (remember) {
            db.insert(callerUid, desiredUid, desiredCmd, (resultCode.equals(ALLOW)) ? 1 : 0);
        }
        try {
            socket = new LocalSocket();
            socket.connect(new LocalSocketAddress(socketPath,
                LocalSocketAddress.Namespace.FILESYSTEM));

            Log.d(TAG, "Sending result: " + resultCode);
            if (socket != null) {
                OutputStream os = socket.getOutputStream();
                byte[] bytes = resultCode.getBytes("UTF-8");
                os.write(bytes);
                os.flush();
                os.close();
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        if (resultCode.equals(ALLOW) && app_status.dateAccess + 60*1000 < System.currentTimeMillis()) {
            sendNotification(callerUid);
        }
    }
	
	private void sendNotification(int callerUid) {
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
