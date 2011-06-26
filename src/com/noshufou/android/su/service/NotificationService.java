package com.noshufou.android.su.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.noshufou.android.su.AppDetailsActivity;
import com.noshufou.android.su.R;
import com.noshufou.android.su.SuRequestReceiver;
import com.noshufou.android.su.preferences.Preferences;
import com.noshufou.android.su.util.Util;

public class NotificationService extends IntentService {
    private static final String TAG = "NotificationService";

    private SharedPreferences mPrefs;

    final String LAST_NOTIFICATION_UID = "last_notification_uid";
    final String LAST_NOTIFICATION_TIME = "last_notification_time";
    
    private Handler mHandler;

    public NotificationService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        mHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "NotificationService handling intent");
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        int callerUid = intent.getIntExtra(SuRequestReceiver.EXTRA_CALLERUID, 0);
        Long currentTime = System.currentTimeMillis();

        String notificationType = mPrefs.getString(Preferences.NOTIFICATION_TYPE, "toast");
        int allow = intent.getIntExtra(SuRequestReceiver.EXTRA_ALLOW, 0);
        String notificationMessage = getString(
                allow==1?R.string.notification_text_allow:R.string.notification_text_deny,
                Util.getAppName(this, callerUid, false));
        if (notificationType.equals("status")) {
            showNotification(callerUid, notificationMessage, currentTime);
        } else if (notificationType.equals("toast")) {
            showToast(callerUid, notificationMessage, currentTime);
        }
    }
    
    private void showNotification(int callerUid, String notificationMessage, long currentTime) {
        NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, AppDetailsActivity.class);
        notificationIntent.putExtra("uid", callerUid);
        PendingIntent contentIntent = PendingIntent
            .getActivity(this, 0, notificationIntent, 0);

        String title = getString(R.string.app_name);

        Notification notification = new Notification (R.drawable.stat_su, notificationMessage,
                currentTime);
        notification.setLatestEventInfo(this, title, notificationMessage, contentIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL|Notification.FLAG_ONLY_ALERT_ONCE;

        nm.notify(callerUid, notification);
    }
    
    private void showToast(int callerUid, final String notificationMessage, long currentTime) {
        Log.d(TAG, "show toast");
        int lastNotificationUid = mPrefs.getInt(LAST_NOTIFICATION_UID, 0);
        long lastNotificationTime = mPrefs.getLong(LAST_NOTIFICATION_TIME, 0);
        Log.d(TAG, "callerUid = " + callerUid + ", currentTime = " + currentTime);
        Log.d(TAG, "lastNotificationUid = " + lastNotificationUid + ", lastNotificationTime = " + lastNotificationTime);
        if (callerUid != lastNotificationUid ||
                lastNotificationTime + (5 * 1000) < currentTime) {
            Log.d(TAG, "checks passed, display the toast now");
            final int gravity = Integer.parseInt(mPrefs.getString(Preferences.TOAST_LOCATION, "0"));
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    Log.d(TAG, "showing toast from handler");
                    Toast toast = Toast.makeText(getApplicationContext(), notificationMessage, Toast.LENGTH_SHORT);
                    if (gravity > 0) {
                        toast.setGravity(gravity, 0, 0);
                    }
                    toast.show();
                }
                
            });
            Editor editor = mPrefs.edit();
            editor.putInt(LAST_NOTIFICATION_UID, callerUid);
            editor.putLong(LAST_NOTIFICATION_TIME, currentTime);
            editor.commit();
        }
    }
}
