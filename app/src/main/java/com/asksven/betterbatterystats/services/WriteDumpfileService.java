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

import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.appanalytics.Analytics;
import com.asksven.betterbatterystats.appanalytics.Events;
import com.asksven.betterbatterystats.data.Reading;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.Wakelock;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * @author sven
 *
 */
public class WriteDumpfileService extends IntentService
{
	private static final String TAG = "WriteDumpfileService";
	public static final String STAT_TYPE_FROM = "StatTypeFrom";
	public static final String STAT_TYPE_TO = "StatTypeTo";
	public static final String OUTPUT = "Output";

	public WriteDumpfileService()
	{
	    super("WriteDumpfileService");
	}
	
	@Override
	public void onHandleIntent(Intent intent)
	{
		Log.i(TAG, "Called at " + DateUtils.now());

		Analytics.getInstance(this).trackEvent(Events.EVENT_PERFORM_SAVEDUMPFILE);

		String refFrom = intent.getStringExtra(WriteDumpfileService.STAT_TYPE_FROM);
		String refTo = intent.getStringExtra(WriteDumpfileService.STAT_TYPE_TO);
		String output = intent.getStringExtra(WriteDumpfileService.OUTPUT);
		if (refTo == null)
		{
			refTo = Reference.CURRENT_REF_FILENAME;
		}
		
		// if we want a reading until "current" make sure to update that ref
		if (refTo == Reference.CURRENT_REF_FILENAME)
		{
			StatsProvider.getInstance(this).setCurrentReference(0);
		}
		
		
		Log.i(TAG, "Called with extra " + refFrom + " and " + refTo);

		if ((refTo != null) && (refFrom != null) && !refFrom.equals("") && !refTo.equals(""))
		{
			try
			{
				Wakelock.aquireWakelock(this);
				// restore any available references if required
	        	Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(refFrom, this);
	    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(refTo, this);
	    		Reading data = new Reading(this,myReferenceFrom, myReferenceTo);
	    		
	    		data.writeDumpfile(this, "");
	
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
		else
		{
			Log.i(TAG, "No dumpfile written: " + refFrom + " and " + refTo);
			
		}
		
		stopSelf();
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