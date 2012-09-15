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
import com.asksven.betterbatterystats.R.drawable;
import com.asksven.betterbatterystats.R.string;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgets.WidgetBars;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.StatsActivity;

import android.app.Notification;
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
import android.widget.Toast;

/**
 * @author sven
 *
 */
public class WatchdogProcessingService extends Service
{
	private static final String TAG = "WatchdogProcessingService";

	@Override
	public void onStart(Intent intent, int startId)
	{
		Log.i(TAG, "Called at " + DateUtils.now());
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		try
		{
			boolean bIssueWarnings = sharedPrefs.getBoolean("watchdog_issue_warnings", false);
			if (bIssueWarnings)
			{
				boolean bShowToast = sharedPrefs.getBoolean("watchdog_show_toasts", false);

				int minScreenOffDurationMin 	= sharedPrefs.getInt("watchdog_duration_threshold", 10);
				int awakeThresholdPct		= sharedPrefs.getInt("watchdog_awake_threshold", 30);
				long now = System.currentTimeMillis();
				Long screenOffTime 			= sharedPrefs.getLong("screen_went_off_at", now );
				
				Long screenOffDurationMs 		= now - screenOffTime;
				
				// we process only if screenOffDuration is >= minScreenOffDuration
				if (screenOffDurationMs >= ((long)minScreenOffDurationMin*60*1000))
				{
					if (bShowToast)
					{
						Toast.makeText(this, "BBS: Watchdog processing...", Toast.LENGTH_SHORT).show();
					}

					int awakePct = 0;
					StatsProvider stats = StatsProvider.getInstance(this);
					// make sure to flush cache
					BatteryStatsProxy.getInstance(this).invalidate();
					
					if (stats.hasScreenOffRef())
					{
						// restore any available since screen reference
						StatsProvider.getInstance(this).deserializeFromFile();
						ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, StatsProvider.STATS_SCREEN_OFF, false, false);

						long timeAwake = 0;
						long timeSince = 0;

						if ( (otherStats != null) && ( otherStats.size() > 1) )
						{
							timeAwake = ((Misc) stats.getElementByKey(otherStats, "Awake")).getTimeOn();
							timeSince = stats.getBatteryRealtime(StatsProvider.STATS_SCREEN_OFF);
							
						}
						else
						{
							// no stats means the phone was awake
							timeSince = stats.getBatteryRealtime(StatsProvider.STATS_SCREEN_OFF);
							timeAwake = timeSince;
						}
						
						if (timeSince > 0)
						{
							awakePct = ((int) (timeAwake / timeSince)) * 100;
						}
						else
						{
							awakePct = 0;
						}

						// we issue a warning if awakePct > awakeThresholdPct
						if (awakePct >= awakeThresholdPct)
						{
							if (bShowToast)
							{
								Toast.makeText(this, "BBS: Awake alert: " + awakePct + "% awake", Toast.LENGTH_SHORT).show();
							}

							// notify the user of the situation
					    	Notification notification = new Notification(
					    			R.drawable.icon_notext, "Awake alert", System.currentTimeMillis());
					    	
							Intent i = new Intent(Intent.ACTION_MAIN);
							PackageManager manager = this.getPackageManager();
							
							i = manager.getLaunchIntentForPackage(this.getPackageName());
							i.addCategory(Intent.CATEGORY_LAUNCHER);
						    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							i.putExtra(StatsActivity.STAT, 0);
							i.putExtra(StatsActivity.STAT_TYPE, StatsProvider.STATS_SCREEN_OFF);
							i.putExtra(StatsActivity.FROM_NOTIFICATION, true);

					    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

					    	notification.setLatestEventInfo(
					    			this, this.getText(R.string.app_name), 
					    			awakePct + "% awake while screen off", contentIntent);
					    	NotificationManager nM = (NotificationManager)this.getSystemService(Service.NOTIFICATION_SERVICE);
					    	nM.notify(EventWatcherService.NOTFICATION_ID, notification);
						}
					}
				}
				
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