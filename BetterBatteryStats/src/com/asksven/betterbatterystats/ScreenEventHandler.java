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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author sven
 *
 */
public class ScreenEventHandler extends BroadcastReceiver
{

	private static final String TAG = "ScreenEventHandler";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
		{
			Log.i(TAG, "Received Broadcast ACTION_SCREEN_OFF");
			
			// todo: store the "since screen off" refs here
			try
			{
				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
				
				boolean bRefForScreenOff = sharedPrefs.getBoolean("ref_for_screen_off", false);

				if (bRefForScreenOff)
				{
					// Store the "since screen off" ref
					StatsProvider.getInstance(context).setReferenceSinceScreenOff(0);
				}
				
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
        Intent i = new Intent(context, EventWatcherService.class);
        context.startService(i);
    }

}