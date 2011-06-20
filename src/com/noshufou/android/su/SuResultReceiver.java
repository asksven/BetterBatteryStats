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
package com.noshufou.android.su;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.noshufou.android.su.preferences.Preferences;
import com.noshufou.android.su.service.LogService;
import com.noshufou.android.su.service.NotificationService;
import com.noshufou.android.su.util.Util;

public class SuResultReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        int callerUid = intent.getIntExtra("caller_uid", 0);
        Long currentTime = System.currentTimeMillis();
        
        // Notify the user if their su binary is outdated. Note this doesn't
        // check for the absolute latest binary, just the latest required
        // to work properly
        if (intent.getIntExtra(SuRequestReceiver.EXTRA_VERSION_CODE, 0) < 6) {
            NotificationManager nm = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification(R.drawable.stat_su,
                    context.getString(R.string.notif_outdated_ticker), System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                    new Intent(context, UpdaterActivity.class), 0);
            notification.setLatestEventInfo(context, context.getString(R.string.notif_outdated_title),
                    context.getString(R.string.notif_outdated_text), contentIntent);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            nm.notify(0, notification);
            // The rest of this receiver probably won't work properly, so just finish now
            return;
        }
        
        boolean notificationsEnabled = prefs.getBoolean(Preferences.NOTIFICATIONS, true);
        boolean loggingEnabled = prefs.getBoolean(Preferences.LOGGING, true);
        
        // If elite is installed, pass control to that. Elite has licensing checks in it,
        // so it's not necessary to check for a valid license here.
        if (Util.elitePresent(context, true, 2)) {
            Intent eliteIntent = new Intent();
            eliteIntent.setClassName("com.noshufou.android.su.elite",
                    "com.noshufou.android.su.elite.service.ResultService");
            eliteIntent.putExtras(intent);
            eliteIntent.putExtra("notifications_enabled", notificationsEnabled);
            eliteIntent.putExtra("logging_enabled", loggingEnabled);
            eliteIntent.putExtra("current_time", currentTime);
            context.startService(eliteIntent);
            return;
        }
        
        // Add log if applicable
        if (loggingEnabled) {
            Intent serviceIntent = new Intent(context, LogService.class);
            serviceIntent.putExtras(intent);
            serviceIntent.putExtra(LogService.EXTRA_ACTION, LogService.ADD_LOG);
            context.startService(serviceIntent);
        }
        
        // Send notification if applicable
        if (notificationsEnabled) {
            Intent serviceIntent = new Intent(context, NotificationService.class);
            serviceIntent.putExtras(intent);
            context.startService(serviceIntent);
        }
    }

}
