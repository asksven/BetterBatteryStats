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

import android.app.NotificationManager;
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
public class WriteScreenOffReferenceService extends Service
{
	private static final String TAG = "WriteScreenOffReferenceService";

	@Override
	public void onStart(Intent intent, int startId)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		Log.i(TAG, "Called");
		try
		{
			
			// Clear any notifications taht may still be shown as the reference in going to be overwritten
	    	NotificationManager nM = (NotificationManager)this.getSystemService(Service.NOTIFICATION_SERVICE);
	    	nM.cancel(EventWatcherService.NOTFICATION_ID);

			boolean bRefForScreenOff = sharedPrefs.getBoolean("ref_for_screen_off", false);

			if (bRefForScreenOff)
			{
				// Store the "since screen off" ref
				StatsProvider.getInstance(this).setReferenceSinceScreenOff(0);
				
				long now = System.currentTimeMillis();
				// Save current time to prefs 
		        SharedPreferences.Editor editor = sharedPrefs.edit();
		        editor.putLong("screen_went_off_at", now);
		        editor.commit();
			}
			
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