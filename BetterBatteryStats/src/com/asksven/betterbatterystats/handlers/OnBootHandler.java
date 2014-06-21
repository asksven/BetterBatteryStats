/*
 * Copyright (C) 2012 asksven
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
import com.asksven.betterbatterystats.services.EventWatcherService;
import com.asksven.betterbatterystats.services.WriteBootReferenceService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
		Intent serviceIntent = new Intent(context, WriteBootReferenceService.class);
		context.startService(serviceIntent);

		
		boolean activeMonitoring	= sharedPrefs.getBoolean("ref_for_screen_off", false);
		if (activeMonitoring)
		{
			// start the service
			Intent i = new Intent(context, EventWatcherService.class);
	        context.startService(i);
		}
		
		// if active monitoring enabled schedule the next alarm 
		if (sharedPrefs.getBoolean("active_mon_enabled", false))
		{
			// reschedule next timer
			StatsProvider.scheduleActiveMonAlarm(context);
		}

	}
}
