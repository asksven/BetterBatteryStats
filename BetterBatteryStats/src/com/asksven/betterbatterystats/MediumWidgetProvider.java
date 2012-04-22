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

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.asksven.android.common.utils.GenericLogger;
import com.asksven.betterbatterystats.R;

/**
 * @author sven
 *
 */
public class MediumWidgetProvider extends BbsWidgetProvider
{

	private static final String TAG = "MediumWidgetProvider";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{

		Log.w(TAG, "onUpdate method called");

		// Update the widgets via the service
		startService(context, this.getClass(), appWidgetManager, UpdateMediumWidgetService.class);
		
		setAlarm(context);
		
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}	
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		super.onReceive(context, intent);

		if ( (WIDGET_UPDATE.equals(intent.getAction())) ||
				intent.getAction().equals("android.appwidget.action.APPWIDGET_UPDATE") )
		{
			if (WIDGET_UPDATE.equals(intent.getAction()))
			{
				Log.d(TAG, "Alarm called: updating");
				GenericLogger.i(LargeWidgetProvider.WIDGET_LOG, TAG, "LargeWidgetProvider: Alarm to refresh widget was called");
			}
			else
			{
				Log.d(TAG, "APPWIDGET_UPDATE called: updating");
			}

			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(context);
			ComponentName thisAppWidget = new ComponentName(
					context.getPackageName(),
					this.getClass().getName());
			int[] appWidgetIds = appWidgetManager
					.getAppWidgetIds(thisAppWidget);
			if (appWidgetIds.length > 0)
			{
				onUpdate(context, appWidgetManager, appWidgetIds);
			}
		}
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
		removeAlarm(context);

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
