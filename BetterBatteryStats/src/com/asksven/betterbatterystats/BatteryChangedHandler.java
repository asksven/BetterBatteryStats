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

import com.asksven.betterbatterystats.data.StatsProvider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

/**
 * @author sven
 *
 */
public class BatteryChangedHandler extends BroadcastReceiver
{
	private static final String TAG = "BatteryChangedHandler";
	
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{

        if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED))
		{
        	try
        	{
        		
				Log.i(TAG, "Received Broadcast ACTION_BATTERY_CHANGED");
				
				int status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
	            
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
	}
}
