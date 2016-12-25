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

package com.asksven.betterbatterystats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.services.WriteTimerReferenceService;

/**
 * Handles alarms to turn off Wifi is a connection could not be established
 * @author sven
 *
 */
public class ActiveMonAlarmReceiver extends BroadcastReceiver
{		 
	private static String TAG = "ActiveMonAlarmReceiver";
	public static int ACTIVE_MON_ALARM = 323;
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.d(TAG, "Alarm received: processing");
		

		try
		{
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    	
			// if enabled write the ref and schedule the next alarm 
			if (prefs.getBoolean("active_mon_enabled", false))
			{
				// reschedule next timer
				StatsProvider.scheduleActiveMonAlarm(context);
				
				// write the reference
				Intent serviceIntent = new Intent(context, WriteTimerReferenceService.class);
				context.startService(serviceIntent);				
			}
			else
			{
				StatsProvider.cancelActiveMonAlarm(context);
				return;
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured receiving the alarm" + e.getMessage());
		}
	}
}
