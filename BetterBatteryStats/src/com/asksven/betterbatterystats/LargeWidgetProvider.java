/*
 * Copyright (C) 2011-12 asksven
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

import java.util.Calendar;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.asksven.betterbatterystats.R;

/**
 * @author sven
 *
 */
public class LargeWidgetProvider extends AppWidgetProvider
{

	private static final String TAG = "LargeWidgetProvider";
	public static final String WIDGET_UPDATE = "BBS_WIDGET_UPDATE";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{

		Log.w(TAG, "onUpdate method called");
		// Get all ids
		ComponentName thisWidget = new ComponentName(context, LargeWidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

		// Build the intent to call the service
		Intent intent = new Intent(context.getApplicationContext(),
				UpdateLargeWidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

		// Update the widgets via the service
		context.startService(intent);
		
		// set the alarm for next round
		//prepare Alarm Service to trigger Widget
		intent = new Intent(LargeWidgetProvider.WIDGET_UPDATE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
				1234567, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		int freqMinutes = Integer.valueOf(sharedPrefs.getString("widget_refresh_freq", "30"));
//		freqMinutes = 1;
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.MINUTE, freqMinutes);

		alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
				pendingIntent);
		

	}
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		super.onReceive(context, intent);

		if ( (WIDGET_UPDATE.equals(intent.getAction())) ||
				intent.getAction().equals("android.appwidget.action.APPWIDGET_UPDATE") )
		{
			Log.d(TAG, "Alarm called: updating");
			Bundle extras = intent.getExtras();
			if (extras != null)
			{
				AppWidgetManager appWidgetManager = AppWidgetManager
						.getInstance(context);
				ComponentName thisAppWidget = new ComponentName(
						context.getPackageName(),
						LargeWidgetProvider.class.getName());
				int[] appWidgetIds = appWidgetManager
						.getAppWidgetIds(thisAppWidget);

				onUpdate(context, appWidgetManager, appWidgetIds);
			}
		}
	}
	
}
