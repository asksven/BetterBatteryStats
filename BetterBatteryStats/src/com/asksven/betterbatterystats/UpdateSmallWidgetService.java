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
import com.asksven.betterbatterystats.widgets.WidgetBattery;

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
		Log.i(TAG, "Called");
		// Create some random data

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
				.getApplicationContext());

		int[] allWidgetIds = intent
				.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

		ComponentName thisWidget = new ComponentName(getApplicationContext(), SmallWidgetProvider.class);
		
		int[] allWidgetIds2 = appWidgetManager.getAppWidgetIds(thisWidget);
		
		Log.w(TAG, "From Intent" + String.valueOf(allWidgetIds.length));
		Log.w(TAG, "Direct" + String.valueOf(allWidgetIds2.length));

		for (int widgetId : allWidgetIds)
		{
			// Create some random data
			int number = (new Random().nextInt(100));
 
			RemoteViews remoteViews = new RemoteViews(this
					.getApplicationContext().getPackageName(),
					R.layout.small_widget_layout);
			

			// retrieve stats
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			int statType	= StatsProvider.statTypeFromPosition(
					Integer.valueOf(sharedPrefs.getString("small_widget_default_stat_type", "1")));
			
			long timeAwake 		= 0;
			long timeScreenOn 	= 0;
			
			StatsProvider stats = StatsProvider.getInstance(this);
			try
			{
				
				ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, statType);
				timeAwake = ((Misc) stats.getElementByKey(otherStats, "Awake")).getTimeOn();
				timeScreenOn = ((Misc) stats.getElementByKey(otherStats, "Screen On")).getTimeOn();
			}
			catch (Exception e)
			{
				Log.e(TAG,"An error occured: " + e.getMessage());
				
			}
			finally
			{
				Log.d(TAG, "Awake: " + DateUtils.formatDuration(timeAwake));
				Log.d(TAG, "Screen on: " + DateUtils.formatDuration(timeScreenOn));
			}

			// Set the text
//			remoteViews.setTextViewText(R.id.awake, DateUtils.formatDuration(timeAwake));
//			remoteViews.setTextViewText(R.id.screen_on, DateUtils.formatDuration(timeScreenOn));
			
			WidgetBattery graph = new WidgetBattery();
			ArrayList<Long> serie = new ArrayList<Long>();
			graph.setAwake(timeAwake);
			graph.setScreenOn(timeScreenOn);
			
			
			remoteViews.setImageViewBitmap(R.id.graph, graph.getBitmap());

//			remoteViews.setTextViewText(R.id.update,
//					"Random: " + String.valueOf(number));

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
		stopSelf();

		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
}