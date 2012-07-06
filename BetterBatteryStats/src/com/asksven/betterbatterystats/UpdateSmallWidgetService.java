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

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
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
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.method.TimeKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;

/**
 * @author sven
 *
 */
public class UpdateSmallWidgetService extends Service
{
	private static final String TAG = "UpdateSmallWidgetService";
	/** must be unique for each widget */
	private static final int PI_CODE = 1;

	@Override
	public void onStart(Intent intent, int startId)
	{
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
				.getApplicationContext());

		int[] allWidgetIds = intent
				.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
		
		StatsProvider stats = StatsProvider.getInstance(this);
		// make sure to flush cache
		BatteryStatsProxy.getInstance(this).invalidate();
		
		if (!stats.hasSinceUnpluggedRef())
		{
			// restore any available custom reference
			StatsProvider.getInstance(this).deserializeFromFile();
		}


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
			int statType	= Integer.valueOf(sharedPrefs.getString("small_widget_default_stat_type", "1"));

			boolean showTitle	= sharedPrefs.getBoolean("widget_show_stat_type", true);

			long timeAwake 		= 0;
			long timeScreenOn 	= 0;
			long timeDeepSleep 	= 0;
			
			if (!showTitle)
			{
				remoteViews.setInt(R.id.stat_type, "setVisibility", View.GONE);
			}
			remoteViews.setTextViewText(R.id.stat_type, StatsProvider.statTypeToLabelShort(statType));
			

			try
			{
				
				ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, statType, false);
				
				if ( (otherStats != null) || ( otherStats.size() == 1) )
				{
					// the desired stat type is unavailable, pick the alternate one and go on with that one
					statType	= Integer.valueOf(sharedPrefs.getString("widget_fallback_stat_type", "1"));
					otherStats = stats.getOtherUsageStatList(true, statType, false);
				}

				if ( (otherStats != null) && ( otherStats.size() > 1) )
				{

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
				else
				{
					// no proper reference found
			        remoteViews.setInt(R.id.graph, "setVisibility", View.GONE);
//			        remoteViews.setInt(R.id.error, "setVisibility", View.VISIBLE);	
					
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
	
				// tap behavior depends on preferences
				boolean refreshOnTap = sharedPrefs.getBoolean("small_widget_refresh_on_tap", true);

				// Register an onClickListener for the graph -> refresh
				Intent clickIntent = new Intent(this.getApplicationContext(),
						SmallWidgetProvider.class);
	
				clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
						allWidgetIds);
	
				PendingIntent pendingIntent = PendingIntent.getBroadcast(
						getApplicationContext(), 0, clickIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);
				if (refreshOnTap)
				{
					remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
					
				}
				else
				{
					remoteViews.setOnClickPendingIntent(R.id.stat_type, pendingIntent);

					// Register an onClickListener for the widget -> call main activity
					Intent i = new Intent(Intent.ACTION_MAIN);
					PackageManager manager = getPackageManager();
					i = manager.getLaunchIntentForPackage(getPackageName());
					i.addCategory(Intent.CATEGORY_LAUNCHER);
				    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					int stat = Integer.valueOf(sharedPrefs.getString("widget_default_stat", "2"));
					i.putExtra(StatsActivity.STAT, stat);
					i.putExtra(StatsActivity.STAT_TYPE, statType);

					PendingIntent clickPI = PendingIntent.getActivity(
							this.getApplicationContext(), PI_CODE,
							i, PendingIntent.FLAG_UPDATE_CURRENT);
					remoteViews.setOnClickPendingIntent(R.id.layout, clickPI);
				}
				
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