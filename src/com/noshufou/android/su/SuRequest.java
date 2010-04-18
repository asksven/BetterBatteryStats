package com.noshufou.android.su;

import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
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
	
    String resultCode = "DENY";
    String socketPath;
    LocalSocket socket;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	if (getCallingPackage() != null) {
            Log.e(TAG, "SuRequest must be started from su");
            finish();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alert;
        
        Intent in = getIntent();
        socketPath = in.getStringExtra("socket");
        int callerUid = in.getIntExtra("caller_uid", 0);
        int desiredUid = in.getIntExtra("desired_uid", 0);
        String desiredCmd = in.getStringExtra("desired_cmd");

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

        builder.setTitle(R.string.app_name_request)
               .setIcon(R.drawable.icon)
               .setView(layout)
               .setPositiveButton(getString(R.string.allow), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialig, int id) {
                        String result = "ALLOW";
                        if (checkRemember.isChecked()) {
                            result = "ALWAYS_" + result;
                        }
                        sendResult(result);
                    }
                })
               .setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String result = "DENY";
                        if (checkRemember.isChecked()) {
                            result = "ALWAYS_" + result;
                        }
                        sendResult(result);
                    }
                })
               .setCancelable(false);
        alert = builder.create();
        alert.show();
    }

    private void sendResult(String resultCode) {
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
        finish();
    }
}
