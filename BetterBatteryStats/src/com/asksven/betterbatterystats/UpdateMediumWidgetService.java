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
import com.asksven.android.common.utils.StringUtils;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgets.WidgetBars;
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
import android.widget.LinearLayout;
import android.widget.RemoteViews;

/**
 * @author sven
 *
 */
public class UpdateMediumWidgetService extends Service
{
	private static final String TAG = "UpdateMediumWidgetService";

	@Override
	public void onStart(Intent intent, int startId)
	{
		Log.i(TAG, "Called");
		// Create some random data

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
				.getApplicationContext());

		int[] allWidgetIds = intent
				.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
				
		Log.w(TAG, "From Intent" + String.valueOf(allWidgetIds.length));

		StatsProvider stats = StatsProvider.getInstance(this);
		// make sure to flush cache
		BatteryStatsProxy.getInstance(this).invalidate();
		
		if (!stats.hasCustomRef())
		{
			// restore any available custom reference
			StatsProvider.getInstance(this).deserializeFromFile();
		}

		for (int widgetId : allWidgetIds)
		{ 
			RemoteViews remoteViews = new RemoteViews(this
					.getApplicationContext().getPackageName(),
					R.layout.medium_widget_layout);
			
			// we change the bg color of the layout based on alpha from prefs
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			int opacity	= sharedPrefs.getInt("large_widget_bg_opacity", 20);
			opacity = (255 * opacity) / 100; 
			remoteViews.setInt(R.id.layout, "setBackgroundColor", (opacity << 24) & android.graphics.Color.BLACK);

			
			// retrieve stats
			int statType	= StatsProvider.statTypeFromPosition(
					Integer.valueOf(sharedPrefs.getString("large_widget_default_stat_type", "1")));

			boolean showPct	= sharedPrefs.getBoolean("large_widget_show_pct", false);
			boolean showTitle	= sharedPrefs.getBoolean("widget_show_stat_type", true);


			long timeAwake 		= 0;
			long timeDeepSleep	= 0;
			long timeScreenOn 	= 0;
			long timeSince 		= 0;
			long sumPWakelocks	= 0;
			long sumKWakelocks	= 0;
			
			try
			{
				
				ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, statType, false);
				if ( (otherStats != null) && ( otherStats.size() > 1) )
				{
					timeAwake = ((Misc) stats.getElementByKey(otherStats, "Awake")).getTimeOn();
					timeScreenOn = ((Misc) stats.getElementByKey(otherStats, "Screen On")).getTimeOn();
					timeSince = stats.getBatteryRealtime(statType);
					ArrayList<StatElement> pWakelockStats = stats.getWakelockStatList(true, statType, 0, 0);
					sumPWakelocks = stats.sum(pWakelockStats);
	
					ArrayList<StatElement> kWakelockStats = stats.getNativeKernelWakelockStatList(true, statType, 0, 0);
					sumKWakelocks = stats.sum(kWakelockStats);
	
					Misc deepSleepStat = ((Misc) stats.getElementByKey(otherStats, "Deep Sleep"));
					if (deepSleepStat != null)
					{
						timeDeepSleep = deepSleepStat.getTimeOn();
					}
					else
					{
						timeDeepSleep = 0;
					}

					if (!showTitle)
					{
						remoteViews.setInt(R.id.stat_type, "setVisibility", View.GONE);
					}

					// Set the text
					remoteViews.setTextViewText(R.id.stat_type, StatsProvider.statTypeToLabel(statType));
					remoteViews.setTextViewText(R.id.since, DateUtils.formatDurationShort(timeSince));
					
					if (showPct)
					{
						remoteViews.setTextViewText(R.id.awake, StringUtils.formatRatio(timeAwake, timeSince));
						remoteViews.setTextViewText(R.id.deep_sleep, StringUtils.formatRatio(timeDeepSleep, timeSince));
						remoteViews.setTextViewText(R.id.screen_on, StringUtils.formatRatio(timeScreenOn, timeSince));
					}
					else
					{
						remoteViews.setTextViewText(R.id.awake, DateUtils.formatDurationShort(timeAwake));
						remoteViews.setTextViewText(R.id.deep_sleep, DateUtils.formatDurationShort(timeDeepSleep));
						remoteViews.setTextViewText(R.id.screen_on, DateUtils.formatDurationShort(timeScreenOn));
					}
					
					// and the font size
					float fontSize	= Float.valueOf(sharedPrefs.getString("large_widget_font_size", "10"));
					remoteViews.setFloat(R.id.staticSince, "setTextSize", fontSize);
					remoteViews.setFloat(R.id.staticAwake, "setTextSize", fontSize);
					remoteViews.setFloat(R.id.staticDeepSleep, "setTextSize", fontSize);
					remoteViews.setFloat(R.id.staticScreenOn, "setTextSize", fontSize);
					remoteViews.setFloat(R.id.staticKWL, "setTextSize", fontSize);
					remoteViews.setFloat(R.id.staticPWL, "setTextSize", fontSize);

					remoteViews.setFloat(R.id.stat_type, "setTextSize", fontSize);
					remoteViews.setFloat(R.id.since, "setTextSize", fontSize);
					remoteViews.setFloat(R.id.awake, "setTextSize", fontSize);
					remoteViews.setFloat(R.id.deep_sleep, "setTextSize", fontSize);
					remoteViews.setFloat(R.id.screen_on, "setTextSize", fontSize);
					remoteViews.setFloat(R.id.kwl, "setTextSize", fontSize);
					remoteViews.setFloat(R.id.wl, "setTextSize", fontSize);

					if ( (sumPWakelocks == 1) && (pWakelockStats.size()==1) )
					{
						// there was no reference
						remoteViews.setTextViewText(R.id.wl, "n/a");
					}
					else
					{
						if (showPct)
						{
							remoteViews.setTextViewText(R.id.wl, StringUtils.formatRatio(sumPWakelocks, timeSince));
						}
						else
						{
							remoteViews.setTextViewText(R.id.wl, DateUtils.formatDurationShort(sumPWakelocks));
						}
					}
					
					if ( (sumKWakelocks == 1) && (kWakelockStats.size()==1) )
					{
						// there was no reference
						remoteViews.setTextViewText(R.id.kwl, "n/a");
					}
					else
					{
						if (showPct)
						{
							remoteViews.setTextViewText(R.id.kwl, StringUtils.formatRatio(sumKWakelocks, timeSince));
						}
						else
						{
							remoteViews.setTextViewText(R.id.kwl, DateUtils.formatDurationShort(sumKWakelocks));
						}
					}
					
				}
				else
				{
					// no stat available
					// Set the text
					String notAvailable = "n/a";
					remoteViews.setTextViewText(R.id.stat_type, StatsProvider.statTypeToLabel(statType));
					remoteViews.setTextViewText(R.id.since, notAvailable);
					remoteViews.setTextViewText(R.id.awake, notAvailable);
					remoteViews.setTextViewText(R.id.deep_sleep, notAvailable);
					remoteViews.setTextViewText(R.id.screen_on, notAvailable);
					remoteViews.setTextViewText(R.id.wl, notAvailable);
					remoteViews.setTextViewText(R.id.kwl, notAvailable);
				}
			}
			catch (Exception e)
			{
				Log.e(TAG,"An error occured: " + e.getMessage());
				GenericLogger.stackTrace(TAG, e.getStackTrace());
				
			}
			finally
			{
				Log.d(TAG, "Since: " + DateUtils.formatDurationShort(timeSince));
				Log.d(TAG, "Awake: " + DateUtils.formatDurationShort(timeAwake));
				Log.d(TAG, "Deep Sleep: " + DateUtils.formatDurationShort(timeDeepSleep));
				Log.d(TAG, "Screen on: " + DateUtils.formatDurationShort(timeScreenOn));
				Log.d(TAG, "PWL: " + DateUtils.formatDurationShort(sumPWakelocks));
				Log.d(TAG, "KWL: " + DateUtils.formatDurationShort(sumKWakelocks));

				// Register an onClickListener for the graph -> refresh
				Intent clickIntent = new Intent(this.getApplicationContext(),
						MediumWidgetProvider.class);
	
				clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
						allWidgetIds);
	
				PendingIntent pendingIntent = PendingIntent.getBroadcast(
						getApplicationContext(), 0, clickIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);
				remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
				
				// Register an onClickListener for the widget -> call main activity
				Intent i = new Intent(Intent.ACTION_MAIN);
				PackageManager manager = getPackageManager();
				i = manager.getLaunchIntentForPackage(getPackageName());
				i.addCategory(Intent.CATEGORY_LAUNCHER);
			    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				PendingIntent clickPI = PendingIntent.getActivity(
						this.getApplicationContext(), 0,
						i, 0);
				remoteViews.setOnClickPendingIntent(R.id.layout2, clickPI);

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