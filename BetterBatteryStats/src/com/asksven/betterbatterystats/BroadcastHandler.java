/*
 * Copyright (C) 2011 asksven
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


import java.util.logging.Logger;

import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;


/**
 * General broadcast handler: handles event as registered on Manifest
 * @author sven
 *
 */
public class BroadcastHandler extends BroadcastReceiver
{	
	private static final String TAG = "BroadcastHandler";
	
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

 
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
		{
        	// start the service
//        	context.startService(new Intent(context, BetterBatteryStatsService.class));
        	
			Log.i(TAG, "Received Broadcast ACTION_BOOT_COMPLETED");
			// delete whatever references we have saved here
			StatsProvider.getInstance(context).deletedSerializedRefs();
			
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

		}


        if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED))
		{
			Log.i(TAG, "Received Broadcast ACTION_POWER_DISCONNECTED, serializing 'since unplugged'");
			
			// start service to persist reference
			Intent serviceIntent = new Intent(context, WriteUnpluggedReferenceService.class);
			context.startService(serviceIntent);
		}


	}
}
