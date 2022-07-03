/*
 * Copyright (C) 2011-18 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asksven.betterbatterystats.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.Wakelock;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;

/**
 * @author sven
 *
 */
public class WriteBootReferenceServicePre21 extends IntentService
{
    private static final String TAG = "WriteBootRefServiceP21";

    public WriteBootReferenceServicePre21()
    {
        super("WriteBootReferenceService");
    }

    @Override
    public void onHandleIntent(Intent intent)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        Log.i(TAG, "Called at " + DateUtils.now());
        try
        {

            Wakelock.aquireWakelock(this);
            StatsProvider.getInstance().setReferenceSinceBoot(0);

            // delete screen on time counters
            SharedPreferences.Editor updater = sharedPrefs.edit();
            long elapsedRealtime = SystemClock.elapsedRealtime();
            updater.putLong("time_screen_on", elapsedRealtime);
            updater.putLong("screen_on_counter", 0);

            updater.commit();


            Intent i = new Intent(ReferenceStore.REF_UPDATED).putExtra(Reference.EXTRA_REF_NAME, Reference.BOOT_REF_FILENAME);
            this.sendBroadcast(i);


        }
        catch (Exception e)
        {
            Log.e(TAG, "An error occured: " + e.getMessage());
        }
        finally
        {
            Wakelock.releaseWakelock();
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "Destroyed at" + DateUtils.now());
        Wakelock.releaseWakelock();
    }

}