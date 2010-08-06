package com.noshufou.android.su;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

public class SuRequest extends Activity {
    private static final String TAG = "SuRequest";

    private String socketPath;
    private int callerUid = 0;
    private int desiredUid = 0;
    private String desiredCmd = "";

    SharedPreferences prefs;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
    	if (getCallingPackage() != null) {
            Log.e(TAG, "SuRequest must be started from su");
            finish();
            return;
        }
    	
    	Intent in = getIntent();
    	socketPath = in.getStringExtra(SuRequestReceiver.EXTRA_SOCKET);
    	callerUid = in.getIntExtra(SuRequestReceiver.EXTRA_CALLERUID, 0);
    	desiredUid = in.getIntExtra(SuRequestReceiver.EXTRA_UID, 0);
    	desiredCmd = in.getStringExtra(SuRequestReceiver.EXTRA_CMD);
    	
    	prompt();
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

        appNameView.setText(Util.getAppName(this, callerUid, true));
        packageNameView.setText(Util.getAppPackage(this, callerUid));
        requestDetailView.setText(Util.getUidName(this, desiredUid, true));
        commandView.setText(desiredCmd);
        checkRemember.setChecked(prefs.getBoolean("last_remember_value", true));
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Context context = getApplicationContext();
                boolean remember = checkRemember.isChecked();
                AppStatus appStatus = new AppStatus(0, callerUid, System.currentTimeMillis());
                if (id == DialogInterface.BUTTON_POSITIVE) {
                    appStatus.permission = AppStatus.ALLOW;
                } else if (id == DialogInterface.BUTTON_NEGATIVE) {
                    appStatus.permission = AppStatus.DENY;
                }
                if (remember) {
                    DBHelper db = new DBHelper(context);
                   	db.insert(callerUid, desiredUid, desiredCmd, appStatus.permission);
                   	db.close();
                }
                ResponseHelper.sendResult(context, appStatus, socketPath);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("last_remember_value", checkRemember.isChecked());
                editor.commit();
                finish();
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
}
