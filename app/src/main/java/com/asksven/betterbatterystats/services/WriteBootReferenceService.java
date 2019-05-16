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
package com.asksven.betterbatterystats.services;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
@TargetApi(21)
public class WriteBootReferenceService extends JobService
{
	private static final String TAG = "WriteBootRefService";



    @Override
    public boolean onStartJob(JobParameters params)
    {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		Log.i(TAG, "Called at " + DateUtils.now());
		try
		{
			
			Wakelock.aquireWakelock(this);
			StatsProvider.getInstance().setReferenceSinceBoot(0);
			
			Intent i = new Intent(ReferenceStore.REF_UPDATED).putExtra(Reference.EXTRA_REF_NAME, Reference.BOOT_REF_FILENAME);
		    this.sendBroadcast(i);
			jobFinished(params, false);

			
		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured: " + e.getMessage());
            return false;
		}
		finally
		{
			Wakelock.releaseWakelock();
		}
        return true;
	}

    @Override
    public boolean onStopJob(JobParameters params)
    {
		Wakelock.releaseWakelock();
		// Job was finished by system: make sure to re-schedule it
        return true;
    }

    // Schedule the start right after boot
    public static void scheduleJob(Context context)
    {
        ComponentName serviceComponent = new ComponentName(context, WriteBootReferenceService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(1 * 1000); // wait at least 1 second
        builder.setOverrideDeadline(5 * 1000); // maximum delay 5 seconds
		JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }

}