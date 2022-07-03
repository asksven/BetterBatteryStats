/*
 * Copyright (C) 2011-2018 asksven
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

package com.asksven.betterbatterystats.handlers;


import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.services.AppWidgetJobService;
import com.asksven.betterbatterystats.services.EventWatcherService;
import com.asksven.betterbatterystats.services.WriteBootReferenceService;
import com.asksven.betterbatterystats.services.WriteBootReferenceServicePre21;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;


/**
 * General broadcast handler: handles event as registered on Manifest
 * @author sven
 *
 */
public class OnBootHandler extends BroadcastReceiver
{	
	private static final String TAG = "OnBootHandler";
	private static int JOB_ID=777;
	
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

 
		Log.i(TAG, "Received Broadcast " + intent.getAction());
		
		// delete whatever references we have saved here
		ReferenceStore.deleteAllRefs(context);
		
		
		// start service to persist boot reference
		if (Build.VERSION.SDK_INT < 23)
		{
			Intent serviceIntent = new Intent(context, WriteBootReferenceServicePre21.class);
			context.startService(serviceIntent);
		}
		else
		{
			WriteBootReferenceService.scheduleJob(context);
		}

        // start the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            context.startForegroundService(new Intent(context, EventWatcherService.class));
        }
        else
        {
            Intent i = new Intent(context, EventWatcherService.class);
            context.startService(i);
        }


		// if active monitoring enabled schedule the next alarm 
		if (sharedPrefs.getBoolean("active_mon_enabled", false))
		{
			// reschedule next timer
			StatsProvider.scheduleActiveMonAlarm(context);
		}

		if (Build.VERSION.SDK_INT >= 23)
        {
            // start the job refreshing widgets
            OnBootHandler.scheduleAppWidgetsJob(context);
        }
	}

    // schedule the start of the service every 10 to 15 minutes
    @TargetApi(21)
    public static void scheduleAppWidgetsJob(Context context)
    {
        OnBootHandler.deletePendingAppWidgetsJobs(context);
        OnBootHandler.scheduleJob(context, 5 * 60 * 1000, 1 * 60 * 1000);
    }

    // schedule an immediate refresh job
    @TargetApi(21)
    public static void scheduleAppWidgetsJobImmediate(Context context)
    {
        OnBootHandler.deletePendingAppWidgetsJobs(context);
        OnBootHandler.scheduleJob(context, 5 * 1000, 10 * 1000);
    }

    @TargetApi(21)
    static void scheduleJob(Context context, long minLatencyMs, long maxDelayMs)
    {
        ComponentName serviceComponent = new ComponentName(context, AppWidgetJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent);
        builder.setMinimumLatency(minLatencyMs); // wait at least
        builder.setOverrideDeadline(maxDelayMs); // maximum delay
        if (Build.VERSION.SDK_INT >= 28)
        {
            //builder.setImportantWhileForeground(true);
        }
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());

    }

    @TargetApi(21)
    public static boolean isAppWidgetsJobOn(Context context)
    {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : scheduler.getAllPendingJobs())
        {
            if (jobInfo.getId() == JOB_ID)
            {
                hasBeenScheduled = true;
                break;
            }
        }

        return hasBeenScheduled;
    }

    @TargetApi(21)
    public static void deletePendingAppWidgetsJobs(Context context)
    {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(JOB_ID);
    }

}
