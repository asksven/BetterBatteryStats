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

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


/**
 * The BetterBatteryStats keeps running even if the main Activity is not displayed/never called
 * The Service takes care of holding the ACTION_BATTERY_CHANGED listener
 * @author sven
 *
 */
/**
 * @author sven
 *
 */
public class BetterBatteryStatsService extends Service 
{
		
	private static final String TAG = "BetterBatteryStatsService";
	public static String SERVICE_NAME = "com.asksven.betterbatteryStats.BetterBatteryStatsService";

	BatteryChangedHandler m_batteryHandler = null;


    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder
    {
        BetterBatteryStatsService getService()
        {
            return BetterBatteryStatsService.this;
        }
    }

    @Override
    public void onCreate()
    {
    	super.onCreate();
    	
		// register battery changed events
		m_batteryHandler = new BatteryChangedHandler();
        this.registerReceiver(m_batteryHandler, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

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
    
    @Override
    /**
     * Called when Service is terminated
     */
    public void onDestroy()
    {        
    	// unregister the receiver
    	this.unregisterReceiver(m_batteryHandler);

    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
	


}

