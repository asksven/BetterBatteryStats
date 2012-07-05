/*
 * Copyright (C) 2011 asksven
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * @author sven
 *
 */
public class ScreenEventHandler extends BroadcastReceiver
{

	private static final String TAG = "ScreenEventHandler";

    @Override
    public void onReceive(Context context, Intent intent)
    {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
		{
			Log.i(TAG, "Received Broadcast ACTION_SCREEN_OFF");
			
			// todo: store the "since screen off" refs here
			try
			{
				
				// Clear any notifications taht may still be shown as the reference in going to be overwritten
		    	NotificationManager nM = (NotificationManager)context.getSystemService(Service.NOTIFICATION_SERVICE);
		    	nM.cancel(EventWatcherService.NOTFICATION_ID);

				boolean bRefForScreenOff = sharedPrefs.getBoolean("ref_for_screen_off", false);

				if (bRefForScreenOff)
				{
					// Store the "since screen off" ref
					StatsProvider.getInstance(context).setReferenceSinceScreenOff(0);
					
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

		}

        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
		{
			Log.i(TAG, "Received Broadcast ACTION_SCREEN_ON");
			boolean bRunOnUnlock = sharedPrefs.getBoolean("watchdog_on_unlock", false);
			
			if (!bRunOnUnlock)
			{
				processScreenOn(context);
			}

			
		}
        
        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT))
		{
			Log.i(TAG, "Received Broadcast ACTION_USER_PRESENT");
			boolean bRunOnUnlock = sharedPrefs.getBoolean("watchdog_on_unlock", false);
			
			if (bRunOnUnlock)
			{
				processScreenOn(context);
			}
			

		}

        Intent i = new Intent(context, EventWatcherService.class);
        context.startService(i);
    }
    
    void processScreenOn(Context context)
    {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

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
						Toast.makeText(context, "BBS: Watchdog processing...", Toast.LENGTH_SHORT).show();
					}

					int awakePct = 0;
					StatsProvider stats = StatsProvider.getInstance(context);
					// make sure to flush cache
					BatteryStatsProxy.getInstance(context).invalidate();
					
					if (stats.hasScreenOffRef())
					{
						// restore any available since screen reference
						StatsProvider.getInstance(context).deserializeFromFile();
						ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, StatsProvider.STATS_SCREEN_OFF, false);

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
								Toast.makeText(context, "BBS: Awake alert: " + awakePct + "% awake", Toast.LENGTH_SHORT).show();
							}

							// notify the user of the situation
					    	Notification notification = new Notification(
					    			R.drawable.icon_notext, "Awake alert", System.currentTimeMillis());
					    	
							Intent i = new Intent(Intent.ACTION_MAIN);
							PackageManager manager = context.getPackageManager();
							
							i = manager.getLaunchIntentForPackage(context.getPackageName());
							i.addCategory(Intent.CATEGORY_LAUNCHER);
						    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							i.putExtra(StatsActivity.STAT, 0);
							i.putExtra(StatsActivity.STAT_TYPE, StatsProvider.STATS_SCREEN_OFF);
							i.putExtra(StatsActivity.FROM_NOTIFICATION, true);

					    	PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i, 0);

					    	notification.setLatestEventInfo(
					    			context, context.getText(R.string.app_name), 
					    			awakePct + "% awake while screen off", contentIntent);
					    	NotificationManager nM = (NotificationManager)context.getSystemService(Service.NOTIFICATION_SERVICE);
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

    }

}