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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.asksven.betterbatterystats.R;

/**
 * Activity for managing preferences using Android's preferences framework
 * @see http://www.javacodegeeks.com/2011/01/android-quick-preferences-tutorial.html
 * 
 * Access prefs goes like this:
 *   SharedPreferences sharedPrefs = 
 *   	PreferenceManager.getDefaultSharedPreferences(this);
 *   sharedPrefs.getBoolean("perform_updates", false));
 *   
 * @author sven
 *
 */
public class PreferencesActivity extends PreferenceActivity
{
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		// refresh widgets
		Intent intent = new Intent(this.getApplicationContext(),
				LargeWidgetProvider.class);
		intent.setAction(BbsWidgetProvider.WIDGET_PREFS_REFRESH);
		this.sendBroadcast(intent);

		intent = new Intent(this.getApplicationContext(),
				MediumWidgetProvider.class);
		intent.setAction(BbsWidgetProvider.WIDGET_PREFS_REFRESH);
		this.sendBroadcast(intent);

		intent = new Intent(this.getApplicationContext(),
				SmallWidgetProvider.class);
		intent.setAction(BbsWidgetProvider.WIDGET_PREFS_REFRESH);
		this.sendBroadcast(intent);
		
		// check the state of the service and start / stop ot depending on prefs
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		boolean serviceShouldBeRunning = sharedPrefs.getBoolean("ref_for_screen_off", false);
	
		if (serviceShouldBeRunning)
		{
			if (!EventWatcherService.isServiceRunning(this))
			{
				Intent i = new Intent(this, EventWatcherService.class);
		        this.startService(i);
			}
				
		}
		else
		{
			if (EventWatcherService.isServiceRunning(this))
			{
				Intent i = new Intent(this, EventWatcherService.class);
		        this.stopService(i);
			}
			
		}

	}

}
