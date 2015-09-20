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

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.services.UpdateLargeWidgetService;

/**
 * @author sven
 *
 */
public class LargeWidgetProvider extends BbsWidgetProvider
{

	private static final String TAG = "LargeWidgetProvider";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{

		if (LogSettings.DEBUG)
		{
			Log.i(TAG, "onUpdate method called, starting service and setting alarm");
		}

		// Update the widgets via the service
		startService(context, this.getClass(), appWidgetManager, UpdateLargeWidgetService.class);
		
		setAlarm(context);
		
		super.onUpdate(context, appWidgetManager, appWidgetIds);

	}
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		super.onReceive(context, intent);

		if (LogSettings.DEBUG)
		{
			Log.i(TAG, "onReceive method called, action = '" + intent.getAction() + "' at " + DateUtils.now());
		}
		
		if ( (WIDGET_UPDATE.equals(intent.getAction())) ||
				intent.getAction().equals("android.appwidget.action.APPWIDGET_UPDATE") ||
				intent.getAction().equals("com.sec.android.widgetapp.APPWIDGET_RESIZE") ||
				intent.getAction().equals("android.appwidget.action.APPWIDGET_UPDATE_OPTIONS")
				)
		{
			if (LargeWidgetProvider.WIDGET_UPDATE.equals(intent.getAction()))
			{
				if (LogSettings.DEBUG)
				{
					Log.d(TAG, "Alarm called: updating");
				}
//				GenericLogger.i(WIDGET_LOG, TAG, "LargeWidgetProvider: Alarm to refresh widget was called");
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
			else
			{
				if (LogSettings.DEBUG)
				{
					Log.i(TAG, "No widget found to update");
				}
			}
		}
	}
	
	
}
