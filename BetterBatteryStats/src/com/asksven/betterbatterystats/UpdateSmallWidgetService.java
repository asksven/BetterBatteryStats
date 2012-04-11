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

import java.util.ArrayList;
import java.util.Random;

import org.achartengine.chart.TimeChart;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.GenericLogger;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgets.WidgetBars;
import com.asksven.betterbatterystats.widgets.WidgetBattery;
import com.asksven.betterbatterystats.R;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.method.TimeKeyListener;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * @author sven
 *
 */
public class UpdateSmallWidgetService extends Service
{
	private static final String TAG = "UpdateSmallWidgetService";

	@Override
	public void onStart(Intent intent, int startId)
	{
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
				.getApplicationContext());

		int[] allWidgetIds = intent
				.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

		ComponentName thisWidget = new ComponentName(getApplicationContext(), SmallWidgetProvider.class);
		
		int[] allWidgetIds2 = appWidgetManager.getAppWidgetIds(thisWidget);
		
		for (int widgetId : allWidgetIds)
		{
 
			RemoteViews remoteViews = new RemoteViews(this
					.getApplicationContext().getPackageName(),
					R.layout.small_widget_layout);
			// we change the bg color of the layout based on alpha from prefs
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			int opacity	= sharedPrefs.getInt("small_widget_bg_opacity", 20);
			opacity = (255 * opacity) / 100; 
			remoteViews.setInt(R.id.layout, "setBackgroundColor", (opacity << 24) & android.graphics.Color.BLACK);

			// retrieve stats
			int statType	= StatsProvider.statTypeFromPosition(
					Integer.valueOf(sharedPrefs.getString("small_widget_default_stat_type", "1")));
			
			long timeAwake 		= 0;
			long timeScreenOn 	= 0;
			long timeDeepSleep 	= 0;

			try
			{
				
				
				StatsProvider stats = StatsProvider.getInstance(this);
				
				ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, statType);

				Misc timeAwakeStat = (Misc) stats.getElementByKey(otherStats, "Awake");
				if (timeAwakeStat != null)
				{
					timeAwake = timeAwakeStat.getTimeOn();
				}
				else
				{
					timeAwake = 0;
				}
				
				Misc timeScreenOnStat = (Misc) stats.getElementByKey(otherStats, "Screen On");
				if (timeScreenOnStat != null)
				{
					timeScreenOn = timeScreenOnStat.getTimeOn();
				}
				else
				{
					timeScreenOn = 0;
				}

				Misc deepSleepStat = ((Misc) stats.getElementByKey(otherStats, "Deep Sleep"));
				if (deepSleepStat != null)
				{
					timeDeepSleep = deepSleepStat.getTimeOn();
				}
				else
				{
					timeDeepSleep = 0;
				}
			}
			catch (Exception e)
			{
				Log.e(TAG,"An error occured: " + e.getMessage());
				GenericLogger.stackTrace(TAG, e.getStackTrace());
				
			}
			finally
			{
				Log.d(TAG, "Awake: " + DateUtils.formatDuration(timeAwake));
				Log.d(TAG, "Screen on: " + DateUtils.formatDuration(timeScreenOn));
				Log.d(TAG, "Deep sleep: " + DateUtils.formatDuration(timeDeepSleep));

				WidgetBattery graph = new WidgetBattery();
				graph.setAwake(timeAwake);
				graph.setScreenOn(timeScreenOn);
				graph.setDeepSleep(timeDeepSleep);
	
				remoteViews.setImageViewBitmap(R.id.graph, graph.getBitmap(this));
	
				// Register an onClickListener
				Intent clickIntent = new Intent(this.getApplicationContext(),
						SmallWidgetProvider.class);
	
				clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
						allWidgetIds);
	
				PendingIntent pendingIntent = PendingIntent.getBroadcast(
						getApplicationContext(), 0, clickIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);
				remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
				appWidgetManager.updateAppWidget(widgetId, remoteViews);
			}
		}
		stopSelf();

		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
}