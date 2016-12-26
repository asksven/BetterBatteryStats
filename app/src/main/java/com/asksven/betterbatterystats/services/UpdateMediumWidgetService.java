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
import com.asksven.android.common.utils.StringUtils;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.StatsActivity;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgetproviders.MediumWidgetProvider;

import java.util.ArrayList;

/**
 * @author sven
 *
 */
public class UpdateMediumWidgetService extends Service
{
	private static final String TAG = "UpdateMWidgetService";
	/** must be unique for each widget */
	private static final int PI_CODE = 3;

	@Override
	public void onStart(Intent intent, int startId)
	{
		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Service started");
		}
		// Create some random data

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
				.getApplicationContext());

		int[] allWidgetIds = intent
				.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
				
		Log.w(TAG, "From Intent" + String.valueOf(allWidgetIds.length));

		StatsProvider stats = StatsProvider.getInstance(this);
		// make sure to flush cache
		BatteryStatsProxy.getInstance(this).invalidate();
		
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
			String refFrom	= sharedPrefs.getString("large_widget_default_stat_type", Reference.UNPLUGGED_REF_FILENAME);

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
				Reference currentRef = StatsProvider.getInstance(this).getUncachedPartialReference(0);
				Reference fromRef = ReferenceStore.getReferenceByName(refFrom, this);
				
				ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, currentRef);
				
				if ( (otherStats == null) || ( otherStats.size() == 1) )
				{
					// the desired stat type is unavailable, pick the alternate one and go on with that one
					refFrom	= sharedPrefs.getString("widget_fallback_stat_type", Reference.BOOT_REF_FILENAME);
					fromRef = ReferenceStore.getReferenceByName(refFrom, this);
					otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, currentRef);
				}

				if ( (otherStats != null) && ( otherStats.size() > 1) )
				{
					try
					{
						timeAwake = ((Misc) stats.getElementByKey(otherStats, StatsProvider.LABEL_MISC_AWAKE)).getTimeOn();
						timeScreenOn = ((Misc) stats.getElementByKey(otherStats, "Screen On")).getTimeOn();
					}
					catch (Exception e)
					{
						timeAwake 		= 0;
						timeScreenOn 	= 0;
					}
					timeSince = StatsProvider.getInstance(this).getSince(fromRef, currentRef);
					ArrayList<StatElement> pWakelockStats = stats.getWakelockStatList(true, fromRef, 0, 0, currentRef);
					sumPWakelocks = stats.sum(pWakelockStats);
	
					ArrayList<StatElement> kWakelockStats = stats.getKernelWakelockStatList(true, fromRef, 0, 0, currentRef);
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
					remoteViews.setTextViewText(R.id.stat_type, fromRef.getLabel());
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
					if (fromRef != null)
					{
						remoteViews.setTextViewText(R.id.stat_type, fromRef.m_fileName);
					}
					else
					{
						remoteViews.setTextViewText(R.id.stat_type, notAvailable);
					}
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
				Log.e(TAG, "Exception: "+Log.getStackTraceString(e));
			}
			finally
			{
				if (LogSettings.DEBUG)
				{
					Log.d(TAG, "Since: " + DateUtils.formatDurationShort(timeSince));
					Log.d(TAG, "Awake: " + DateUtils.formatDurationShort(timeAwake));
					Log.d(TAG, "Deep Sleep: " + DateUtils.formatDurationShort(timeDeepSleep));
					Log.d(TAG, "Screen on: " + DateUtils.formatDurationShort(timeScreenOn));
					Log.d(TAG, "PWL: " + DateUtils.formatDurationShort(sumPWakelocks));
					Log.d(TAG, "KWL: " + DateUtils.formatDurationShort(sumKWakelocks));
				}
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
				int stat = Integer.valueOf(sharedPrefs.getString("widget_default_stat", "0"));
				i.putExtra(StatsActivity.STAT, stat);
				i.putExtra(StatsActivity.STAT_TYPE_FROM, refFrom);
				i.putExtra(StatsActivity.STAT_TYPE_TO, Reference.CURRENT_REF_FILENAME);


				PendingIntent clickPI = PendingIntent.getActivity(
						this.getApplicationContext(), PI_CODE,
						i, PendingIntent.FLAG_UPDATE_CURRENT);
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