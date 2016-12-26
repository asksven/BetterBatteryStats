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

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.StatsActivity;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgetproviders.SmallWidgetProvider;
import com.asksven.betterbatterystats.widgets.WidgetBattery;

import java.util.ArrayList;

/**
 * @author sven
 *
 */
public class UpdateSmallWidgetService extends Service
{
	private static final String TAG = "UpdateSWidgetService";
	/** must be unique for each widget */
	private static final int PI_CODE = 1;

	@Override
	public void onStart(Intent intent, int startId)
	{
		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Service started");
		}
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
				.getApplicationContext());

		int[] allWidgetIds = intent
				.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
		
		StatsProvider stats = StatsProvider.getInstance(this);
		// make sure to flush cache
		BatteryStatsProxy.getInstance(this).invalidate();
		
		for (int widgetId : allWidgetIds)
		{
 
			RemoteViews remoteViews = new RemoteViews(this
					.getApplicationContext().getPackageName(),
					R.layout.small_widget_layout);
			
			// make sure to make the widget visible as it may have been previously hidden 
			remoteViews.setInt(R.id.graph, "setVisibility", View.VISIBLE);
			// we change the bg color of the layout based on alpha from prefs
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			int opacity	= sharedPrefs.getInt("small_widget_bg_opacity", 20);
			opacity = (255 * opacity) / 100; 
			remoteViews.setInt(R.id.layout, "setBackgroundColor", (opacity << 24) & android.graphics.Color.BLACK);

			// retrieve stats
			String refFrom	= sharedPrefs.getString("small_widget_default_stat_type", Reference.UNPLUGGED_REF_FILENAME);

			boolean showTitle	= sharedPrefs.getBoolean("widget_show_stat_type", true);

			long timeAwake 		= 0;
			long timeScreenOn 	= 0;
			long timeDeepSleep 	= 0;
			
			if (!showTitle)
			{
				remoteViews.setInt(R.id.stat_type, "setVisibility", View.GONE);
			}
			remoteViews.setTextViewText(R.id.stat_type, Reference.getLabel(refFrom));
			

			try
			{
				
				Reference currentRef = StatsProvider.getInstance(this).getUncachedPartialReference(0);
				Reference fromRef = ReferenceStore.getReferenceByName(refFrom, this);


				ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, currentRef);
				
				if ( (otherStats == null) || ( otherStats.size() == 1) )
				{
					// the desired stat type is unavailable, pick the alternate one and go on with that one
					refFrom	= sharedPrefs.getString("widget_fallback_stat_type", Reference.UNPLUGGED_REF_FILENAME);
					fromRef = ReferenceStore.getReferenceByName(refFrom, this);
					otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, currentRef);
				}

				if ( (otherStats != null) && ( otherStats.size() > 1) )
				{

					Misc timeAwakeStat = (Misc) stats.getElementByKey(otherStats, StatsProvider.LABEL_MISC_AWAKE);
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
				}
			}
			catch (Exception e)
			{
				Log.e(TAG, "Exception: "+Log.getStackTraceString(e));
			}
			finally
			{
				if (LogSettings.DEBUG)
				{
					Log.d(TAG, "Awake: " + DateUtils.formatDuration(timeAwake));
					Log.d(TAG, "Screen on: " + DateUtils.formatDuration(timeScreenOn));
					Log.d(TAG, "Deep sleep: " + DateUtils.formatDuration(timeDeepSleep));
				}
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
					int stat = Integer.valueOf(sharedPrefs.getString("widget_default_stat", "0"));
					i.putExtra(StatsActivity.STAT, stat);
					i.putExtra(StatsActivity.STAT_TYPE_FROM, refFrom);
					i.putExtra(StatsActivity.STAT_TYPE_TO, Reference.CURRENT_REF_FILENAME);

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