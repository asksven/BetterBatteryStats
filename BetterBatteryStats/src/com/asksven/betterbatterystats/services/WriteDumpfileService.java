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
package com.asksven.betterbatterystats.services;

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
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgets.WidgetBars;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.StatsActivity;
import com.asksven.betterbatterystats.Wakelock;

import android.app.IntentService;
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
public class WriteDumpfileService extends IntentService
{
	private static final String TAG = "WriteDumpfileService";
	public static final String STAT_TYPE_FROM = "StatTypeFrom";
	public static final String STAT_TYPE_TO = "StatTypeTo";

	public WriteDumpfileService()
	{
	    super("WriteDumpfileService");
	}
	
	@Override
	public void onHandleIntent(Intent intent)
	{
		Log.i(TAG, "Called at " + DateUtils.now());
		
		
		String refFrom = intent.getStringExtra(WriteDumpfileService.STAT_TYPE_FROM);
		String refTo = intent.getStringExtra(WriteDumpfileService.STAT_TYPE_TO);
		
		
		Log.i(TAG, "Called with extra " + refFrom + " and " + refTo);

		if (!refFrom.equals("") && !refTo.equals(""))
		{
			try
			{
				Wakelock.aquireWakelock(this);
				// restore any available references if required
	        	Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(refFrom, this);
	    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(refTo, this);
	        	StatsProvider.getInstance(this).writeDumpToFile(myReferenceFrom, 0, myReferenceTo);
	
			}
			catch (Exception e)
			{
				Log.e(TAG, "An error occured: " + e.getMessage());
			}
			finally
			{
				Wakelock.releaseWakelock();
			}
		}
		else
		{
			Log.i(TAG, "No dumpfile written: " + refFrom + " and " + refTo);
			
		}
		
		stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	@Override
	public void onDestroy()
	{
		Log.e(TAG, "Destroyed at" + DateUtils.now());
		Wakelock.releaseWakelock();
	}

}