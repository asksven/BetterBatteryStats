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
package com.asksven.betterbatterystats.widgetproviders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.LogSettings;

/**
 * @author sven
 *
 */
public class BbsWidgetProvider extends AppWidgetProvider
{

	private static final String TAG = "BbsWidgetProvider";
	public static final String WIDGET_UPDATE = "BBS_WIDGET_UPDATE";
	public static final String WIDGET_PREFS_REFRESH = "BBS_WIDGET_PREFS_REFRESH";
	
	protected void setAlarm(Context context)
	{
		// set the alarm for next round
		//prepare Alarm Service to trigger Widget
		Intent intent = new Intent(BbsWidgetProvider.WIDGET_UPDATE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
				1234567, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		int freqMinutes = Integer.valueOf(sharedPrefs.getString("widget_refresh_freq", "30"));
		
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
		if (freqMinutes != 0)
		{
			if (LogSettings.DEBUG)
			{
				Log.i(TAG, "It is now " + DateUtils.now() + ", Scheduling alarm in " + freqMinutes + " minutes");
			}
			alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + (freqMinutes * 60 * 1000),
					pendingIntent);
		}
		else
		{
			if (LogSettings.DEBUG)
			{
				Log.i(TAG, "No alarm scheduled, freq is 0");
			}
		}
		
	}
	
	protected void removeAlarm(Context context)
	{
		Intent intent = new Intent(LargeWidgetProvider.WIDGET_UPDATE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
				1234567, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);		
	}
	
	protected void startService(Context context, Class callerClass, AppWidgetManager appWidgetManager, Class serviceClass)
	{
		// Get all ids
		ComponentName thisWidget = new ComponentName(context, callerClass);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		if (LogSettings.DEBUG)
		{
			Log.i(TAG, "Starting Widget Service " + serviceClass.getName());
		}
		// Build the intent to call the service
		Intent intent = new Intent(context.getApplicationContext(), serviceClass);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
		
		// Update the widgets via the service
		context.startService(intent);

	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds)
	{
		// called when widgets are deleted
		// see that you get an array of widgetIds which are deleted
		// so handle the delete of multiple widgets in an iteration
		super.onDeleted(context, appWidgetIds);
	}

	@Override
	public void onDisabled(Context context)
	{
		super.onDisabled(context);
		// runs when all of the instances of the widget are deleted from
		// the home screen
		
		// remove the alarms

	}

	@Override
	public void onEnabled(Context context)
	{
		super.onEnabled(context);
		// runs when all of the first instance of the widget are placed
		// on the home screen
		setAlarm(context);
	}
	
	
}
