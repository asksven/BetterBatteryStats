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
package com.asksven.betterbatterystats.services;

import android.app.ActivityManager;
import android.app.Service;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.asksven.betterbatterystats.handlers.ScreenEventHandler;

/**
 * @author sven
 *
 */
public class EventWatcherService extends Service
{
	
	static final String TAG = "EventWatcherService";
	public static String SERVICE_NAME = "com.asksven.betterbatterystats.services.EventWatcherService";
	public static final int NOTFICATION_ID = 1002;


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
        BroadcastReceiver mReceiver = new ScreenEventHandler();
        registerReceiver(mReceiver, filter);
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
		
	    ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
	    {
	        if (EventWatcherService.SERVICE_NAME.equals(service.service.getClassName()))
	        {
	            return true;
	        }
	    }
	    return false;
	}


}