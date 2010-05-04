package com.noshufou.android.su;

import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.CheckBox;

public class SuRequest extends Activity {
    private static final String TAG = "SuRequest";
	
    private DBHelper db;
    private String socketPath;
    private int callerUid = 0;
    private int desiredUid = 0;
    private String desiredCmd = "";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	if (getCallingPackage() != null) {
            Log.e(TAG, "SuRequest must be started from su");
            finish();
            return;
        }

        db = new DBHelper(this);

        Intent in = getIntent();
        socketPath = in.getStringExtra("socket");
        callerUid = in.getIntExtra("caller_uid", 0);
        desiredUid = in.getIntExtra("desired_uid", 0);
        desiredCmd = in.getStringExtra("desired_cmd");

        switch (db.checkApp(callerUid, desiredUid, desiredCmd)) {
            case DBHelper.ALLOW: sendResult("ALLOW", false); break;
            case DBHelper.DENY:  sendResult("DENY",  false); break;
            case DBHelper.ASK:   prompt(); break;
            default: Log.e(TAG, "Bad response from database"); break;
        }
    }

    private void prompt() {
        LayoutInflater inflater = LayoutInflater.from(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alert;
        
        View layout = inflater.inflate(R.layout.request, (ViewGroup) findViewById(R.id.requestLayout));
        TextView appNameView = (TextView) layout.findViewById(R.id.appName);
        TextView packageNameView = (TextView) layout.findViewById(R.id.packageName);
        TextView requestDetailView = (TextView) layout.findViewById(R.id.requestDetail);
        TextView commandView = (TextView) layout.findViewById(R.id.command);
        final CheckBox checkRemember = (CheckBox) layout.findViewById(R.id.checkRemember);

        appNameView.setText(Su.getAppName(this, callerUid, true));
        packageNameView.setText(Su.getAppPackage(this, callerUid));
        requestDetailView.setText(Su.getUidName(this, desiredUid, true));
        commandView.setText(desiredCmd);
        checkRemember.setChecked(true);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                int result;
                boolean remember = checkRemember.isChecked();
                if (id == DialogInterface.BUTTON_POSITIVE) {
                    sendResult("ALLOW", remember);
                    result = DBHelper.ALLOW;
                } else if (id == DialogInterface.BUTTON_NEGATIVE) {
                    sendResult("DENY", remember);
                    result = DBHelper.DENY;
                }
            }
        };

        builder.setTitle(R.string.app_name_request)
               .setIcon(R.drawable.icon)
               .setView(layout)
               .setPositiveButton(R.string.allow, listener)
               .setNegativeButton(R.string.deny, listener)
               .setCancelable(false);
        alert = builder.create();
        alert.show();
    }

    private void sendNotification() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("preference_notification", false)) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            Context context = getApplicationContext();
            Intent notificationIntent = new Intent(this, Su.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            String text = Su.getAppName(this, callerUid, false) + " has been granted Superuser permissions";
            String title = "Superuser permissions";

            Notification notification = new Notification(R.drawable.stat_su, text, System.currentTimeMillis());
            notification.setLatestEventInfo(context, title, text, contentIntent);
            notification.flags = Notification.FLAG_AUTO_CANCEL|Notification.FLAG_ONLY_ALERT_ONCE;

            nm.notify(TAG, callerUid, notification);
        }
    }

    private void sendResult(String resultCode, boolean remember) {
        LocalSocket socket;
        if (remember) {
            db.insert(callerUid, desiredUid, desiredCmd, (resultCode.equals("ALLOW")) ? 1 : 0);
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
        sendNotification();
        finish();
    }
}
