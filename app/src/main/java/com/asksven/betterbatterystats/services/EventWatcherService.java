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

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.StatsActivity;
import com.asksven.betterbatterystats.handlers.OnUnplugHandler;
import com.asksven.betterbatterystats.handlers.ScreenEventHandler;

/**
 * @author sven
 *
 */
public class EventWatcherService extends Service
{
	
	static final String TAG = "EventWatcherService";
	public static String SERVICE_NAME = "com.asksven.betterbatterystats.services.EventWatcherService";
	public static final int NOTIFICATION_ID = 1002;
    public static final int FOREGROUND_ID = 1003;

    BroadcastReceiver mReceiver = null;
    BroadcastReceiver mReceiver2 = null;
    BroadcastReceiver mReceiver3 = null;

    String CHANNEL_ID = "bbs_channel_event_processing";

    // The user-visible description of the channel.
    CharSequence CHANNEL_NAME = "BBS Event Processing";
    String CHANNEL_DESCRIPTION = "BBS Notification for Backgroud Service";


	// This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder
    {
        public EventWatcherService getService()
        {
        	Log.i(TAG, "getService called");
            return EventWatcherService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        // register receiver that handles screen on and screen off logic
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new ScreenEventHandler();
        registerReceiver(mReceiver, filter);

        IntentFilter filter2 = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        mReceiver2 = new OnUnplugHandler();
        registerReceiver(mReceiver2, filter2);

        if (Build.VERSION.SDK_INT >= 26)
        {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            CharSequence channelName = getString(R.string.event_processing_channel_name);
            String channelDescription = getString(R.string.event_processing_channel_description);



            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, channelName, importance);

            // Configure the notification channel.
            mChannel.setDescription(channelDescription);

            mChannel.enableLights(false);
            mChannel.enableVibration(false);

            mNotificationManager.createNotificationChannel(mChannel);
        }

        Intent notificationIntent = new Intent(this, StatsActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(getString(R.string.plugin_name))
                .setContentText(getString(R.string.foreground_service_text))
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setChannelId(CHANNEL_ID)
                .setContentIntent(pendingIntent).build();

        startForeground(FOREGROUND_ID, notification);
    }


    @Override
    public void onDestroy()
    {
        // The service is no longer used and is being destroyed
        if (mReceiver != null)
        {
            unregisterReceiver(mReceiver);
        }

        if (mReceiver2 != null)
        {
            unregisterReceiver(mReceiver2);
        }
    }
    
    /** 
     * Called when service is started
     */
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i(getClass().getSimpleName(), "Received start id " + startId + ": " + intent);

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.

        return Service.START_STICKY;
    }

    
	public static boolean isServiceRunning(Context context)
	{
		if (context == null) return false;

		boolean ret = false;

		try
        {

            ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
            {
                if (EventWatcherService.SERVICE_NAME.equals(service.service.getClassName()))
                {
                    ret = true;
                }
            }
            ret = false;
        }
        catch (NullPointerException e)
        {
            ret = false;
        }

        return ret;
	}


}