/*******************************************************************************
 * Copyright (c) 2011 Adam Shanks (ChainsDD)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/**
 ** Copyright (C) 2010 Adam Shanks (chainsdd@gmail.com)
 **
 ** This program is free software; you can redistribute it and/or
 ** modify it under the terms of the GNU General Public License,
 ** version 2 as published by the Free Software Foundation.
 **
 ** This program is distributed in the hope that it will be useful,
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 ** GNU General Public License for more details.
 **
 ** You should have received a copy of the GNU General Public License
 ** along with this program; if not, write to the Free Software
 ** Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 **/

package com.noshufou.android.su;

import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.noshufou.android.su.util.AppDetails;
import com.noshufou.android.su.util.DBHelper;
import com.noshufou.android.su.util.Util;
import com.noshufou.android.su.util.DBHelper.LogType;

public class SuRequestActivity extends Activity implements OnClickListener {
    private static final String TAG = "SuRequest";

    private LocalSocket mSocket;
    private SharedPreferences mPrefs;

    private int mCallerUid = 0;
    private int mDesiredUid = 0;
    private String mDesiredCmd = "";

    private boolean mDbEnabled = true;

    private CheckBox mRememberCheckBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String socketPath;
        super.onCreate(savedInstanceState);

        if (this.getCallingPackage() != null) {
            Log.e(TAG, "SuRequest must be started from su");
            finish();
            return;
        }

        this.setContentView(R.layout.activity_request);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mDbEnabled = mPrefs.getBoolean("db_enabled", true);

        Intent intent = this.getIntent();
        mCallerUid = intent.getIntExtra(SuRequestReceiver.EXTRA_CALLERUID, 0);
        mDesiredUid = intent.getIntExtra(SuRequestReceiver.EXTRA_UID, 0);
        mDesiredCmd = intent.getStringExtra(SuRequestReceiver.EXTRA_CMD);
        socketPath = intent.getStringExtra(SuRequestReceiver.EXTRA_SOCKET);

        try {
            mSocket = new LocalSocket();
            mSocket.connect(new LocalSocketAddress(socketPath,
                    LocalSocketAddress.Namespace.FILESYSTEM));
        } catch (IOException e) {
            // If we can't connect to the socket, there's no point in
            // being here. Log it and quit
            Log.e(TAG, "Failed to connect to socket", e);
            finish();
        }

        TextView appNameView = (TextView) findViewById(R.id.appName);
        appNameView.setText(Util.getAppName(this, mCallerUid, true));

        TextView packageNameView = (TextView) findViewById(R.id.packageName);
        packageNameView.setText(Util.getAppPackage(this, mCallerUid));

        TextView requestDetailView = (TextView) findViewById(R.id.requestDetail);
        requestDetailView.setText(Util.getUidName(this, mDesiredUid, true));

        TextView commandView = (TextView)findViewById(R.id.command);
        commandView.setText(mDesiredCmd);

        mRememberCheckBox = (CheckBox) findViewById(R.id.checkRemember);
        mRememberCheckBox.setChecked(mPrefs.getBoolean("last_remember_value", true));
        mRememberCheckBox.setEnabled(mDbEnabled);

        ((Button)findViewById(R.id.allow)).setOnClickListener(this);
        ((Button)findViewById(R.id.deny)).setOnClickListener(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            sendResult(false);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
        case R.id.allow:
            sendResult(true);
            break;
        case R.id.deny:
            sendResult(false);
            break;
        }
    }

    private void sendResult(boolean allow) {
        String resultCode = allow ? AppDetails.ALLOW_CODE : AppDetails.DENY_CODE;

        if (mDbEnabled) {
            DBHelper db = null;
            try {
                db = new DBHelper(this);
                String appName = Util.getAppName(this, mCallerUid, false);
                long time = System.currentTimeMillis();

                if (mRememberCheckBox.isChecked()) {
                    db.insert(mCallerUid, mDesiredUid, mDesiredCmd, allow, time);
                } else if (mPrefs.getBoolean("pref_log_enabled", true)) {
                    db.addLog(appName, allow ? LogType.ALLOW : LogType.DENY, time);
                }
            } catch (SQLiteException e) {
                Log.e(TAG, "Opening database failed", e);

                // Disable the database until su is updated
                mPrefs.edit().putBoolean("remember_enabled", false)
                    .putBoolean("last_remember_value", false).commit();

                // Notify the user of the problem
                NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification = new Notification(R.drawable.stat_su,
                        getString(R.string.notif_disable_remember_ticker),
                        System.currentTimeMillis());
                Intent notificationIntent = new Intent(this, UpdaterActivity.class);
                PendingIntent contentIntent = PendingIntent
                    .getActivity(this, 0, notificationIntent, 0);
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                notification.setLatestEventInfo(this,
                        getString(R.string.notif_disable_remember_title),
                        getString(R.string.notif_disable_remember_text), contentIntent);
                nm.notify(0, notification);
            } finally {
                if (db != null) {
                    db.close();
                }
            }
        }
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean("last_remember_value", mRememberCheckBox.isChecked());
        editor.commit();

        try {
            OutputStream os = mSocket.getOutputStream();
            Log.d(TAG, "Sending result: " + resultCode + " for UID: " + mCallerUid);
            os.write(resultCode.getBytes("UTF-8"));
            os.flush();
            os.close();
            mSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to socket", e);
        }
        finish();
    }
}
