/*
 * Copyright (C) 2015 asksven
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

import java.util.ArrayList;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.StringUtils;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgetproviders.AppWidget;
import com.asksven.betterbatterystats.widgets.WidgetSummary;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.StatsActivity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

/**
 * @author sven
 *
 */
public class UpdateWidgetService extends Service
{
	private static final String TAG = "UpdateWidgetService";
	/** must be unique for each widget */
	private static final int PI_CODE = 1;

	@SuppressLint("NewApi")
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
					R.layout.widget);
			
			final int cellSize = 40;
			int width = 3;
			int height = 2;
			int widthDim = 0;
			int heightDim = 0;
			
			if (Build.VERSION.SDK_INT >= 16)
			{
				Bundle widgetOptions = appWidgetManager.getAppWidgetOptions(widgetId);
//				width = (widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) / cellSize;
//				height = (widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)) / cellSize;
				width = AppWidget.sizeToCells(widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH ) - 10);
				height = AppWidget.sizeToCells(widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) + 10);
				widthDim = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
				heightDim = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
				
				Log.i(TAG, "[" + widgetId + "] height=" + height + " (" + widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) + ")");
				Log.i(TAG, "[" + widgetId + "] width=" + width + "(" + widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) + ")");
				remoteViews = new RemoteViews(this.getPackageName(), R.layout.widget_horz);
				if ((height <= 2) && (width <= 2)) 
				{
					// switch to image only
					Log.i(TAG, "[" + widgetId + "] using image-only layout");
					remoteViews = new RemoteViews(this.getPackageName(), R.layout.widget);
				}
				else if (height < width)
				{
					// switch to horizontal
					Log.i(TAG, "[" + widgetId + "] using horizontal layout");
					remoteViews = new RemoteViews(this.getPackageName(), R.layout.widget_horz);
				}
				else
				{
					// switch to vertical
					Log.i(TAG, "[" + widgetId + "] using vertical layout");
					remoteViews = new RemoteViews(this.getPackageName(), R.layout.widget_vert);
				}
			}
			
			
			// we change the bg color of the layout based on alpha from prefs
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			int opacity	= sharedPrefs.getInt("new_widget_bg_opacity", 20);
			opacity = (255 * opacity) / 100; 
			remoteViews.setInt(R.id.background, "setBackgroundColor", (opacity << 24) & android.graphics.Color.BLACK);
			//remoteViews.setInt(R.id.layoutBackground, "setImageAlpha", opacity);

			long timeAwake 		= 0;
			long timeSince		= 0;
			long timeScreenOn 	= 0;
			long timeDeepSleep 	= 0;
			long timePWL		= 0;
			long timeKWL		= 0;	

			String refFrom	= sharedPrefs.getString("new_widget_default_stat_type", Reference.UNPLUGGED_REF_FILENAME);
			try
			{
				// retrieve stats
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
				
				timeSince = StatsProvider.getInstance(this).getSince(fromRef, currentRef);

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
					
					ArrayList<StatElement> pWakelockStats = stats.getWakelockStatList(true, fromRef, 0, 0, currentRef);
					timePWL = stats.sum(pWakelockStats);
	
					ArrayList<StatElement> kWakelockStats = stats.getKernelWakelockStatList(true, fromRef, 0, 0, currentRef);
					timeKWL = stats.sum(kWakelockStats);
				}
				else
				{
					// no proper reference found
//			        remoteViews.setInt(R.id.graph, "setVisibility", View.GONE);					
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
					Log.d(TAG, "Reference: " + refFrom);
					Log.d(TAG, "Since: " + DateUtils.formatShort(timeSince) + " " + AppWidget.formatDuration(timeSince) + " " + timeSince);
					Log.d(TAG, "Awake: " + DateUtils.formatShort(timeAwake) + " " + AppWidget.formatDuration(timeAwake) +  " " + timeAwake);
					Log.d(TAG, "Screen on: " + DateUtils.formatShort(timeScreenOn) + " " + AppWidget.formatDuration(timeScreenOn) + " " + timeScreenOn);
					Log.d(TAG, "Deep sleep: " + DateUtils.formatShort(timeDeepSleep) + " " + AppWidget.formatDuration(timeDeepSleep) + " " + timeDeepSleep);
					Log.d(TAG, "KWL: " + DateUtils.formatShort(timeKWL) + " " + AppWidget.formatDuration(timeKWL) + " " + timeKWL);
					Log.d(TAG, "PWL: " + DateUtils.formatShort(timePWL) + " " + AppWidget.formatDuration(timePWL) + " " + timePWL);
				}
				WidgetSummary graph = new WidgetSummary();
						
				graph.setAwake(timeAwake);
				graph.setScreenOn(timeScreenOn);
				graph.setDeepSleep(timeDeepSleep);
				graph.setDuration(timeSince);
				graph.setKWL(timeKWL);
				graph.setPWL(timePWL);
				
				
		    	DisplayMetrics metrics = this.getResources().getDisplayMetrics();
		        //Float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Math.min(width, height) * cellSize, metrics);
		    	Log.i(TAG, "Widget Dimensions: height=" + heightDim + " width=" + widthDim);
		    	Float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Math.min(Math.max(Math.min(widthDim, heightDim),80),160), metrics);
		        Log.i(TAG, "BitmapDip=" + Math.min(Math.max(Math.min(widthDim, heightDim),80),160) + ", BitmapPx=" + px.intValue());
				graph.setBitmapSizePx(px.intValue());
	
				remoteViews.setImageViewBitmap(R.id.imageView1, graph.getBitmap(this));
				
				// Show % depending on width and if vertical or horz
				if ((width > height) && (width <= 4)) 
				{					
				remoteViews.setTextViewText(R.id.textViewAwakeVal, AppWidget.formatDuration(timeAwake-timeScreenOn));
				remoteViews.setTextViewText(R.id.textViewDeepSleepVal, AppWidget.formatDuration(timeDeepSleep));
				remoteViews.setTextViewText(R.id.textViewScreenOnVal, AppWidget.formatDuration(timeScreenOn));
				remoteViews.setTextViewText(R.id.textViewKWLVal, AppWidget.formatDuration(timeKWL));
				remoteViews.setTextViewText(R.id.textViewPWLVal, AppWidget.formatDuration(timePWL));
				}
				else
				{
					remoteViews.setTextViewText(R.id.textViewAwakeVal, AppWidget.formatDuration(timeAwake-timeScreenOn)
							+ " (" + StringUtils.formatRatio(timeAwake-timeScreenOn, timeSince) + ")");
					remoteViews.setTextViewText(R.id.textViewDeepSleepVal, AppWidget.formatDuration(timeDeepSleep)
					 		+ " (" + StringUtils.formatRatio(timeDeepSleep, timeSince) + ")");
					remoteViews.setTextViewText(R.id.textViewScreenOnVal, AppWidget.formatDuration(timeScreenOn)
							 + " (" + StringUtils.formatRatio(timeScreenOn, timeSince) + ")");
					remoteViews.setTextViewText(R.id.textViewKWLVal, AppWidget.formatDuration(timeKWL)
							+ " (" + StringUtils.formatRatio(timeKWL, timeSince) + ")");
					remoteViews.setTextViewText(R.id.textViewPWLVal, AppWidget.formatDuration(timePWL)
							 + " (" + StringUtils.formatRatio(timePWL, timeSince) + ")");
				}
				
				// tap zones

				// Register an onClickListener for the graph -> refresh
				Intent clickIntentRefresh = new Intent(this.getApplicationContext(),
						AppWidget.class);
	
				clickIntentRefresh.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				clickIntentRefresh.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
						allWidgetIds);
	
				PendingIntent pendingIntentRefresh = PendingIntent.getBroadcast(
						getApplicationContext(), 0, clickIntentRefresh,
						PendingIntent.FLAG_UPDATE_CURRENT);
				remoteViews.setOnClickPendingIntent(R.id.imageViewRefresh, pendingIntentRefresh);
				
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
				remoteViews.setOnClickPendingIntent(R.id.imageView1, clickPI);
				
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