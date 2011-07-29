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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.noshufou.android.su.util.Util;

public class InstallReceiver extends BroadcastReceiver {
    private static final String TAG = "Su.InstallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        String packageName = intent.getDataString().split(":")[1];
        PackageInfo packageInfo = null;

        try {
            packageInfo  = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e) {
            // This won't happen, but if it does, we don't continue
            Log.e(TAG, "PackageManager divided by zero...", e);
            return;
        }
        
        if (Util.isPackageMalicious(context, packageInfo) != 0) {
            NotificationManager nm = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification(R.drawable.stat_su,
                    context.getString(R.string.malicious_app_notification_ticker),
                    System.currentTimeMillis());
            CharSequence contentTitle = context.getString(R.string.app_name);
            CharSequence contentText = context.getString(R.string.malicious_app_notification_text,
                    pm.getApplicationLabel(packageInfo.applicationInfo));
            Intent notificationIntent = new Intent(Intent.ACTION_DELETE, intent.getData());
            PendingIntent contentIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            nm.notify(0, notification);
        }
    }

}
