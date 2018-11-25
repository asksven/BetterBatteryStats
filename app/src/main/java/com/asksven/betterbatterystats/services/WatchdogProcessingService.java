/*
 * Copyright (C) 2011-2018 asksven
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

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.StatsActivity;
import com.asksven.betterbatterystats.appanalytics.Analytics;
import com.asksven.betterbatterystats.appanalytics.Events;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgetproviders.AppWidget;

import java.util.ArrayList;

/**
 * @author sven
 *
 */
public class WatchdogProcessingService extends IntentService
{
	private static final String TAG = "WatchdogProcService";

	public WatchdogProcessingService()
	{
	    super("WatchdogProcessingService");
	}
	
	@Override
	public void onHandleIntent(Intent intent)
	{
		Log.i(TAG, "Called at " + DateUtils.now());
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		try
		{

			if (true)
			{
				Analytics.getInstance(this).trackEvent(Events.EVENT_PROCESS_WATCHDOG);

				int minScreenOffDurationMin 	= sharedPrefs.getInt("watchdog_duration_threshold", 10);
				int awakeThresholdPct		= sharedPrefs.getInt("watchdog_awake_threshold", 30);
				long now = System.currentTimeMillis();
				Long screenOffTime 			= sharedPrefs.getLong("screen_went_off_at", now );
				
				Long screenOffDurationMs 		= now - screenOffTime;
				
				// we process only if screenOffDuration is >= minScreenOffDuration
				if (screenOffDurationMs >= ((long)minScreenOffDurationMin*60*1000))
				{

					//Toast.makeText(this, getString(R.string.message_watchdog_processing), Toast.LENGTH_SHORT).show();

					int awakePct = 0;
					StatsProvider stats = StatsProvider.getInstance();
					// make sure to flush cache
					BatteryStatsProxy.getInstance(this).invalidate();


					// save screen on reference
					Intent serviceIntent = new Intent(this.getApplicationContext(), WriteScreenOnReferenceService.class);
					this.startService(serviceIntent);

					if (stats.hasScreenOffRef())
					{
						// restore any available since screen reference
						Reference refFrom = ReferenceStore.getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, this);
						StatsProvider.getInstance().setCurrentReference(0);
						//Reference refTo = StatsProvider.getInstance(this).getUncachedPartialReference(0);
						Reference refTo = ReferenceStore.getReferenceByName(Reference.CURRENT_REF_FILENAME, this);
						ArrayList<StatElement> otherStats = null;
						
						// only process if both references are in the right order
						if (refFrom.getCreationTime() < refTo.getCreationTime())
						{		
							otherStats = stats.getOtherUsageStatList(true, refFrom, false, false, refTo);
						}
						else
						{
							otherStats = null;
						}

						long timeAwake = 0;
						long timeSince = 0;

						if ( (otherStats != null) && ( otherStats.size() > 1) )
						{
							
							timeAwake = ((Misc) stats.getElementByKey(otherStats, StatsProvider.LABEL_MISC_AWAKE)).getTimeOn();
							timeSince = stats.getBatteryRealtime(StatsProvider.STATS_SCREEN_OFF);
							Log.i(TAG, "Other stats found. Since=" + timeSince + ", Awake=" + timeAwake);
						}
						else
						{
							// no stats means the phone was awake
							timeSince = stats.getBatteryRealtime(StatsProvider.STATS_SCREEN_OFF);
							timeAwake = timeSince;
							Log.i(TAG, "Other stats do not have any data. Since=" + timeSince + ", Awake=" + timeAwake);
						}
						
						if (timeSince > 0)
						{
							awakePct = (int) ((timeAwake *100 / timeSince));
						}
						else
						{
							awakePct = 0;
						}

						Log.i(TAG, "Awake %=" + awakePct);
						// we issue a warning if awakePct > awakeThresholdPct
						if (awakePct >= awakeThresholdPct)
						{
					    	// Instantiate a Builder object.
					    	NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
					    	
					    	String alertText = "";
					    	try
					    	{
					    		alertText = getString(R.string.message_awake_info, awakePct);
					    	}
					    	catch (Exception e)
					    	{
					    		alertText = getString(R.string.message_awake_alert);
					    	}
					    	
					    	builder.setSmallIcon(R.drawable.ic_stat_notification);
					        builder.setContentTitle(this.getText(R.string.app_name));
					        builder.setContentText(alertText);
					    	
					    	// Creates an Intent for the Activity
					    	Intent i = new Intent(Intent.ACTION_MAIN);
							PackageManager manager = this.getPackageManager();
							
							i = manager.getLaunchIntentForPackage(this.getPackageName());
							i.addCategory(Intent.CATEGORY_LAUNCHER);
						    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							i.putExtra(StatsActivity.STAT, 0);
							i.putExtra(StatsActivity.STAT_TYPE_FROM, Reference.SCREEN_OFF_REF_FILENAME);
							i.putExtra(StatsActivity.STAT_TYPE_TO, Reference.SCREEN_ON_REF_FILENAME);
							i.putExtra(StatsActivity.FROM_NOTIFICATION, true);

					    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

					    	// Puts the PendingIntent into the notification builder
					    	builder.setContentIntent(contentIntent);
					    	// Notifications are issued by sending them to the
					    	// NotificationManager system service.
					    	NotificationManager mNotificationManager =
					    	    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					    	// Builds an anonymous Notification object from the builder, and
					    	// passes it to the NotificationManager
					    	mNotificationManager.notify(EventWatcherService.NOTIFICATION_ID, builder.build());

						}
					}
				}
				else
				{
					// delete screen on ref
					ReferenceStore.invalidate(Reference.SCREEN_ON_REF_FILENAME, this);

				}
			
				// Build the intent to update the widget
				Intent intentRefreshWidgets = new Intent(AppWidget.WIDGET_UPDATE);
				this.sendBroadcast(intentRefreshWidgets);
			}
			
		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured: " + e.getMessage());
		}

	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
}