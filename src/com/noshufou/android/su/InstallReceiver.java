package com.noshufou.android.su;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class InstallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        String packageName = intent.getDataString().split(":")[1];
        Log.d("InstallReceiver", packageName);
        
        if (pm.checkPermission("com.noshufou.android.su.RESPOND", packageName) ==
                PackageManager.PERMISSION_GRANTED) {
            CharSequence appName = "";
            try {
                appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0));
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            NotificationManager nm = 
                    (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification(R.drawable.stat_su,
                    context.getString(R.string.malicious_app_notification_ticker),
                    System.currentTimeMillis());
            
            CharSequence contentTitle = context.getString(R.string.app_name);
            CharSequence contentText = context.getString(R.string.malicious_app_notification_text, appName);
            Intent notificationIntent = new Intent(context, Su.class);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            nm.notify(0, notification);
        }
    }

}
