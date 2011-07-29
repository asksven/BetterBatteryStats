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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.preference.PreferenceManager;
import android.util.Log;

import com.noshufou.android.su.preferences.Preferences;

public class SuRequestReceiver extends BroadcastReceiver {
    private static final String TAG = "Su.SuRequestReceiver";
    
    public static final String EXTRA_CALLERUID = "caller_uid";
    public static final String EXTRA_UID = "desired_uid";
    public static final String EXTRA_CMD = "desired_cmd";
    public static final String EXTRA_SOCKET = "socket";
    public static final String EXTRA_ALLOW = "allow";
    public static final String EXTRA_VERSION_CODE = "version_code";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String automaticAction = prefs.getString(Preferences.AUTOMATIC_ACTION, "prompt");
        if (automaticAction.equals("deny")) {
            sendResult(context, intent, false);
            return;
        } else if (automaticAction.equals("allow")) {
            sendResult(context, intent, true);
            return;
        }
        int sysTimeout = prefs.getInt(Preferences.TIMEOUT, 0);
        if ( sysTimeout > 0) {
            String key = "active_" + intent.getIntExtra(EXTRA_CALLERUID, 0);
            long timeout = prefs.getLong(key, 0);
            if (System.currentTimeMillis() < timeout) {
                sendResult(context, intent, true);
                return;
            } else {
                showPrompt(context, intent);
                return;
            }
        } else {
            showPrompt(context, intent);
            return;
        }
    }
    
    private void showPrompt(Context context, Intent intent) {
        Intent prompt = new Intent(context, SuRequestActivity.class);
        prompt.putExtras(intent);
        prompt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(prompt);
    }
    
    private void sendResult(Context context, Intent intent, boolean allow) {
        LocalSocket socket = new LocalSocket();
        OutputStream os = null;;
        try {
            socket.connect(new LocalSocketAddress(
                    intent.getStringExtra(EXTRA_SOCKET), LocalSocketAddress.Namespace.FILESYSTEM));
            os = socket.getOutputStream();
            os.write((allow?"ALLOW":"DENY").getBytes());
            os.flush();
            os.close();
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to socket", e);
        }
    }

}
