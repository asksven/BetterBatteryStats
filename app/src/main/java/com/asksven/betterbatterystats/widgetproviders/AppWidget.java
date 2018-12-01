/*
 * Copyright (C) 2011-2018 asksven
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

import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.services.UpdateWidgetService;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * @author android
 * 
 */
public class AppWidget extends AppWidgetProvider
{
	static String TAG = "AppWidget";
    public static final String WIDGET_UPDATE = "BBS_WIDGET_UPDATE";
    public static final String WIDGET_PREFS_REFRESH = "BBS_WIDGET_PREFS_REFRESH";

	// based on http://stackoverflow.com/a/18552461/115145
	@SuppressLint("NewApi")
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{
		
		if (LogSettings.DEBUG)
		{
			Log.i(TAG, "onUpdate method called, starting service and setting alarm");
		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		// Update the widgets via the service
		UpdateWidgetService.enqueueWork(context, new Intent());
		
		for (int appWidgetId : appWidgetIds)
		{
			if (Build.VERSION.SDK_INT >= 16)
			{
				Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
				onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, options);
			}
		}
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
			if (WIDGET_UPDATE.equals(intent.getAction()))
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

	@Override
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr, int appWidgetId, Bundle newOptions)
	{
		super.onAppWidgetOptionsChanged(ctx, mgr, appWidgetId, newOptions);
		drawWidget(ctx, appWidgetId);
	}
	
	@SuppressLint("NewApi")
	private void drawWidget(Context context, int appWidgetId)
	{
		RemoteViews updateViews;
		final int cellSize = 40;
		
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		Resources res = context.getResources();
		updateViews = null; //new RemoteViews(context.getPackageName(), R.layout.widget);

		if (Build.VERSION.SDK_INT >= 16)
		{
			Bundle widgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
	
			
			int width = AppWidget.sizeToCells(widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
			int height = AppWidget.sizeToCells(widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
			
			//int width = AppWidget.sizeToCells(widgetOptions.getInt("widgetspanx", 0));
	        //int height = AppWidget.sizeToCells(widgetOptions.getInt("widgetspany", 0)) - 1;
			Log.i(TAG, "[" + appWidgetId + "] height=" + height + " (" + widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) + ")");
			Log.i(TAG, "[" + appWidgetId + "] width=" + width + "(" + widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) + ")");
			//Log.i(TAG, "[" + appWidgetId + "] spanHeight=" + spanHeight);
			//Log.i(TAG, "[" + appWidgetId + "] spanWidth=" + spanWidth);
			
			// responsive rules
			// if (height > width) -> vertial layout
			// else -> horizontal layout
					
			// if (1 x 1, 1 x 2, 2 x 1, 2 x 2) -> Only the graph is visible
			// if (higher than 2 and wider than 2) -> show the graph
			if ((height <= 2) && (width <= 2)) 
			{
				// switch to image-only
				Log.i(TAG, "[" + appWidgetId + "] using image-only layout");
				updateViews = new RemoteViews(context.getPackageName(), R.layout.widget);
			}
			else if (height < width)
			{
				// switch to horizontal
				Log.i(TAG, "[" + appWidgetId + "] using horizontal layout");
				updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_horz);
			}
			else
			{
				// switch to normal
				Log.i(TAG, "[" + appWidgetId + "] using vertical layout");
				updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_vert);
			}

			// check for legend text size
			if ((width < 4))
			{
				// set the Labels
				Log.i(TAG, "[" + appWidgetId + "] using short labels");
                UpdateWidgetService.setShortLabels(updateViews, context, true);
			}
			else
			{
				// set the Labels
				Log.i(TAG, "[" + appWidgetId + "] using long labels");
                UpdateWidgetService.setShortLabels(updateViews, context, false);
			}

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean showColor = sharedPrefs.getBoolean("text_widget_color", true);
            UpdateWidgetService.setTextColor(updateViews, showColor, context);

        }
		
		appWidgetManager.updateAppWidget(appWidgetId, updateViews);

		ComponentName thisWidget = new ComponentName(context, this.getClass());
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        Log.i(TAG, "trigger widget update");
        Intent intentWidget = new Intent(AppWidget.WIDGET_UPDATE);
        Log.i(TAG, "Widget ids: " + allWidgetIds.toString());
        intentWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

        UpdateWidgetService.enqueueWork(context.getApplicationContext(), intentWidget);

    }
	
	public static String formatDuration(long timeMs)
	{
		return DateUtils.formatDurationCompressed(timeMs);
	}
	
	public static int sizeToCells(int size)
	{
		// width = 70 × n − 30
		// n = (width + 30) / 70
		int ratio = (size + 30) / 70;
		//int cells = (int) (Math.floor(ratio));
		Log.i(TAG, "size=" + size);
		Log.i(TAG, "Ratio: " + ratio);//, rounded:" + cells);
		
		return (ratio); 
	}
}
