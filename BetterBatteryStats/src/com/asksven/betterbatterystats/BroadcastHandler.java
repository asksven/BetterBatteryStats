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
import android.content.BroadcastReceiver;
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
	private BatteryChangedHandler m_batteryHandler = null;
	
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{

 
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
		{
        	// start the service
        	context.startService(new Intent(context, BetterBatteryStatsService.class));
        	
			Log.i(TAG, "Received Broadcast ACTION_BOOT_COMPLETED");
			// delete whatever references we have saved here
			StatsProvider.getInstance(context).deletedSerializedRefs();
		}


        if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED))
		{
			Log.i(TAG, "Received Broadcast ACTION_POWER_DISCONNECTED, seralizing 'since unplugged'");
			// todo: store the "since unplugged" refs here
			try
			{
				// Store the "since unplugged ref
				StatsProvider.getInstance(context).setReferenceSinceUnplugged(0);
				
				// check the battery level and if 100% the store "since charged" ref
				Intent batteryIntent = context.getApplicationContext().registerReceiver(null,
	                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
				int status = batteryIntent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
	            
				if (status == BatteryManager.BATTERY_STATUS_FULL)
	            {
	                // save the references for "since charged" 
					try
					{
						Log.i(TAG, "Received Broadcast BATTERY_STATUS_FULL, serializing 'since charged'");
						StatsProvider.getInstance(context).setReferenceSinceCharged(0);
					}
					catch (Exception e)
					{
						Log.e(TAG, "An error occured: " + e.getMessage());
					}

	            }
			}
			catch (Exception e)
			{
				Log.e(TAG, "An error occured: " + e.getMessage());
			}

		}

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
		{
			Log.i(TAG, "Received Broadcast ACTION_SCREEN_OFF");
			
			// todo: store the "since screen off" refs here
			try
			{
				
			}
			catch (Exception e)
			{
				Log.e(TAG, "An error occured: " + e.getMessage());
			}

		}

        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
		{
			Log.i(TAG, "Received Broadcast ACTION_SCREEN_ON");
			// todo: evaluate what hapened while screen was off here
			try
			{
				
			}
			catch (Exception e)
			{
				Log.e(TAG, "An error occured: " + e.getMessage());
			}

		}

	}
}
