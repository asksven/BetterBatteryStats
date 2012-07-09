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
import android.content.IntentFilter;
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
public class WriteUnpluggedReferenceService extends Service
{
	private static final String TAG = "WriteUnpluggedReferenceService";

	@Override
	public void onStart(Intent intent, int startId)
	{
		Log.i(TAG, "Called");
		try
		{
			// Store the "since unplugged ref
			StatsProvider.getInstance(this).setReferenceSinceUnplugged(0);
			
			// check the battery level and if 100% the store "since charged" ref
			Intent batteryIntent = this.getApplicationContext().registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

			int rawlevel = batteryIntent.getIntExtra("level", -1);
			double scale = batteryIntent.getIntExtra("scale", -1);
			double level = -1;
			if (rawlevel >= 0 && scale > 0)
			{
				// normalize level to [0..1]
			    level = rawlevel / scale;
			}

			Log.i(TAG, "Bettery level on uplug is " + level );

			if (level == 1)
			{
				try
				{
					Log.i(TAG, "Level was 100% at unplug, serializing 'since charged'");
					StatsProvider.getInstance(this).setReferenceSinceCharged(0);
				}
				catch (Exception e)
				{
					Log.e(TAG, "An error occured: " + e.getMessage());
				}
				
			}
			// Build the intent to call the service
			Intent intentRefreshWidgets = new Intent(LargeWidgetProvider.WIDGET_UPDATE);
			this.sendBroadcast(intentRefreshWidgets);

		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured: " + e.getMessage());
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