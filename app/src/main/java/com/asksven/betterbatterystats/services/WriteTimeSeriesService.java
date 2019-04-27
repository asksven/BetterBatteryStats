/*
 * Copyright (C) 2011-14 asksven
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
import android.app.IntentService;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.Wakelock;
import com.asksven.betterbatterystats.appanalytics.Analytics;
import com.asksven.betterbatterystats.appanalytics.Events;
import com.asksven.betterbatterystats.data.Reading;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;

/**
 * @author sven
 *
 */
@TargetApi(21)
public class WriteTimeSeriesService extends JobService
{
	private static final String TAG = "WriteTSeriesService";
	public static final String STAT_TYPE_FROM = "StatTypeFrom";
	public static final String STAT_TYPE_TO = "StatTypeTo";
	public static final String OUTPUT = "Output";

	@Override
	public boolean onStartJob(JobParameters params)
	{
		Log.i(TAG, "Called at " + DateUtils.now());

		Analytics.getInstance(this).trackEvent(Events.EVENT_PERFORM_SAVETIMESERIES);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		String refFrom = sharedPrefs.getString("default_stat_type", Reference.UNPLUGGED_REF_FILENAME);
		String refTo = Reference.CURRENT_REF_FILENAME;

		if ((refTo != null) && (refFrom != null) && !refFrom.equals("") && !refTo.equals(""))
		{
            // @todo: we want to separate-out the shipping of the data
            // 1. we want the collection (this job) to happen fast
            // 2. for the shipping we can afford to wait until more data has been collected
            //    and for ideal network connectivity (unmetered)
            // 3. atm we need to run a parallel thread as the processing uses network connectivity but this can be removed after we split things-up
            startWorkOnNewThread(params, refFrom, refTo); // Services do NOT run on a separate thread
		}
		else
		{
			Log.i(TAG, "No time series shipped written: " + refFrom + " and " + refTo);
			
		}

		return true;
	}

	private void startWorkOnNewThread(final JobParameters jobParameters, final String refFrom, final String refTo)
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				doWork(jobParameters, refFrom, refTo);
			}
		}).start();
	}

	private void doWork(JobParameters jobParameters, String refFrom, String refTo)
	{

        try
        {
            Wakelock.aquireWakelock(this);
            // restore any available references if required
            Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(refFrom, this);
            Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(refTo, this);

            if (myReferenceFrom!=null && myReferenceTo != null)
            {
                Reading data = new Reading(this, myReferenceFrom, myReferenceTo);

                // @todo: we want to separate-out the shipping of the data
                // 1. we want the collection (this job) to happen fast
                // 2. for the shipping we can afford to wait until more data has been collected
                //    and for ideal network connectivity (unmetered)
//                data.writeTimeSeries(this);
            }
            else
            {
                Log.i(TAG, "No time series were collected as one of the references was null");
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "An error occured: " + e.getMessage());
        }
        finally
        {
            Wakelock.releaseWakelock();

        }

        jobFinished(jobParameters, false);
	}
	@Override
	public boolean onStopJob(JobParameters params)
	{
		return true;
	}

	// schedule the start of the service every 10 - 30 seconds
	public static void scheduleJob(Context context)
	{
		ComponentName serviceComponent = new ComponentName(context, WriteTimeSeriesService.class);
		JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
		builder.setMinimumLatency(1 * 1000); // wait at least 10 second
		builder.setOverrideDeadline(10 * 1000); // maximum delay 10 seconds
		//builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
		//builder.setRequiresDeviceIdle(true); // device should be idle
		//builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

		jobScheduler.schedule(builder.build());
	}

}