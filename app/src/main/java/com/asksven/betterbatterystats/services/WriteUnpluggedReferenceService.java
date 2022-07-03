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

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.Wakelock;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgetproviders.AppWidget;

/**
 * @author sven
 *
 */
public class WriteUnpluggedReferenceService extends IntentService
{
	private static final String TAG = "WriteUnplRefService";

	public WriteUnpluggedReferenceService()
	{
	    super("WriteUnpluggedReferenceService");
	}
	
	@Override
	public void onHandleIntent(Intent intent)
	{
		Log.i(TAG, "Called at " + DateUtils.now());
		try
		{
			// Store the "since unplugged ref
			Wakelock.aquireWakelock(this);
			StatsProvider.getInstance().setReferenceSinceUnplugged(0);

			Intent i = new Intent(ReferenceStore.REF_UPDATED).putExtra(Reference.EXTRA_REF_NAME, Reference.UNPLUGGED_REF_FILENAME);
		    this.sendBroadcast(i);

//			// save a new current ref
//			StatsProvider.getInstance(this).setCurrentReference(0);
//
//			i = new Intent(ReferenceStore.REF_UPDATED).putExtra(Reference.EXTRA_REF_NAME, Reference.CURRENT_REF_FILENAME);
//		    this.sendBroadcast(i);

			// check the battery level and if 100% the store "since charged" ref
			Intent batteryIntent = this.getApplicationContext().registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

			int rawlevel = batteryIntent.getIntExtra("level", -1);
			double scale = batteryIntent.getIntExtra("scale", -1);
			double level = -1;
			if (rawlevel >= 0 && scale > 0)
			{
				// normalize level to [0..1]
			    level = rawlevel / scale;
			}

			Log.i(TAG, "Bettery level on uplug is " + level );

			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			double level_threshold = sharedPrefs.getInt("battery_charged_minimum_threshold", 100) / 100.0;

			if (level >= level_threshold)
			{
				try
				{
					Log.i(TAG, "Level was 100% at unplug, serializing 'since charged'");
					StatsProvider.getInstance().setReferenceSinceCharged(0);

					i = new Intent(ReferenceStore.REF_UPDATED).putExtra(Reference.EXTRA_REF_NAME, Reference.CHARGED_REF_FILENAME);
				    this.sendBroadcast(i);

					// save a new current ref
					StatsProvider.getInstance().setCurrentReference(0);

					i = new Intent(ReferenceStore.REF_UPDATED).putExtra(Reference.EXTRA_REF_NAME, Reference.CURRENT_REF_FILENAME);
				    this.sendBroadcast(i);

				}
				catch (Exception e)
				{
					Log.e(TAG, "An error occured: " + e.getMessage());
				}
				
			}
			// Build the intent to update the widget
			Intent intentRefreshWidgets = new Intent(AppWidget.WIDGET_UPDATE);
			this.sendBroadcast(intentRefreshWidgets);
			

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