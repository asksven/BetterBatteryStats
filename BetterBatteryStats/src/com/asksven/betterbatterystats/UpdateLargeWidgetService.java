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
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgets.WidgetBars;
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
import android.widget.LinearLayout;
import android.widget.RemoteViews;

/**
 * @author sven
 *
 */
public class UpdateLargeWidgetService extends Service
{
	private static final String TAG = "UpdateLargeWidgetService";

	@Override
	public void onStart(Intent intent, int startId)
	{
		Log.i(TAG, "Called");
		// Create some random data

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
				.getApplicationContext());

		int[] allWidgetIds = intent
				.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

		ComponentName thisWidget = new ComponentName(getApplicationContext(), LargeWidgetProvider.class);
		
		int[] allWidgetIds2 = appWidgetManager.getAppWidgetIds(thisWidget);
		
		Log.w(TAG, "From Intent" + String.valueOf(allWidgetIds.length));
		Log.w(TAG, "Direct" + String.valueOf(allWidgetIds2.length));

		for (int widgetId : allWidgetIds)
		{ 
			RemoteViews remoteViews = new RemoteViews(this
					.getApplicationContext().getPackageName(),
					R.layout.large_widget_layout);
			
			// we change the bg color of the layout based on alpha from prefs
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			int opacity	= sharedPrefs.getInt("large_widget_opacity", 80);
			opacity = (255 * opacity) / 100; 
			remoteViews.setInt(R.id.layout, "setBackgroundColor", (opacity << 24) & android.graphics.Color.BLACK);
			
			// retrieve stats
			int statType	= StatsProvider.statTypeFromPosition(
					Integer.valueOf(sharedPrefs.getString("large_widget_default_stat_type", "1")));
			
			long timeAwake 		= 0;
			long timeScreenOn 	= 0;
			long timeSince 		= 0;
			long sumPWakelocks	= 0;
			long sumKWakelocks	= 0;
			
			StatsProvider stats = StatsProvider.getInstance(this);
			try
			{
				
				ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, statType);
				timeAwake = ((Misc) stats.getElementByKey(otherStats, "Awake")).getTimeOn();
				timeScreenOn = ((Misc) stats.getElementByKey(otherStats, "Screen On")).getTimeOn();
				timeSince = stats.getBatteryRealtime(statType);
				ArrayList<StatElement> pWakelockStats = stats.getWakelockStatList(true, statType, 0, 0);
				sumPWakelocks = stats.sum(pWakelockStats);

				ArrayList<StatElement> kWakelockStats = stats.getNativeKernelWakelockStatList(true, statType, 0, 0);
				sumKWakelocks = stats.sum(kWakelockStats);

	
			}
			catch (Exception e)
			{
				Log.e(TAG,"An error occured: " + e.getMessage());
				
			}
			finally
			{
				Log.d(TAG, "Since: " + DateUtils.formatDuration(timeSince));
				Log.d(TAG, "Awake: " + DateUtils.formatDuration(timeAwake));
				Log.d(TAG, "Screen on: " + DateUtils.formatDuration(timeScreenOn));
				Log.d(TAG, "P. Wl.: " + DateUtils.formatDuration(sumPWakelocks));
				Log.d(TAG, "K. Wl.: " + DateUtils.formatDuration(sumKWakelocks));
			}

			// Set the text
			remoteViews.setTextViewText(R.id.stat_type, StatsProvider.statTypeToLabel(statType));
			remoteViews.setTextViewText(R.id.since, DateUtils.formatDuration(timeSince));
			remoteViews.setTextViewText(R.id.awake, DateUtils.formatDuration(timeAwake));
			remoteViews.setTextViewText(R.id.screen_on, DateUtils.formatDuration(timeScreenOn));
			remoteViews.setTextViewText(R.id.wl, DateUtils.formatDuration(sumPWakelocks));
			remoteViews.setTextViewText(R.id.kwl, DateUtils.formatDuration(sumKWakelocks));
			
			WidgetBars graph = new WidgetBars();
			ArrayList<Long> serie = new ArrayList<Long>();
			serie.add(timeSince);
			serie.add(timeAwake);
			serie.add(timeScreenOn);
			serie.add(sumKWakelocks);
			serie.add(sumPWakelocks);
			
			
			remoteViews.setImageViewBitmap(R.id.graph, graph.getBitmap(this, serie));

			// Register an onClickListener
			Intent clickIntent = new Intent(this.getApplicationContext(),
					LargeWidgetProvider.class);

			clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
					allWidgetIds);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(
					getApplicationContext(), 0, clickIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
			appWidgetManager.updateAppWidget(widgetId, remoteViews);
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