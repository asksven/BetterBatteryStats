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

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.BatteryStatsTypes;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.privateapiproxies.Wakelock;

public class BetterBatteryStatsActivity extends Activity
{

    private final int MENU_ITEM_0 = 0;  
    private final int MENU_ITEM_1 = 1;
    private final int MENU_ITEM_2 = 2;
    private final int MENU_ITEM_3 = 3;
    private final int MENU_ITEM_4 = 4;
    
    private static final String TAG = "BetterBatteryStatsActivity";

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    /** 
     * Add menu items
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {  
        menu.add(0, MENU_ITEM_0, 0, "Battery State");  
        menu.add(0, MENU_ITEM_1, 0, "Battery Remaining");
        menu.add(0, MENU_ITEM_2, 0, "Process Stats");
        menu.add(0, MENU_ITEM_3, 0, "Wakelock Stats");
        menu.add(0, MENU_ITEM_4, 0, "Show Wakelock Stats");
        return true;  
    }  
     
    /** 
     * Define menu action
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {  
        switch (item.getItemId())
        {  
            case MENU_ITEM_0:  
            	doIsOnBattery(); 
            	break;
            case MENU_ITEM_1: 
            	doGetDischargeCurrentLevel(); 
            	break;	  
            case MENU_ITEM_2: 
            	getProcessStats(); 
            	break;	
            case MENU_ITEM_3: 
            	getWakelockStats(); 
            	break;	
            case MENU_ITEM_4: 
            	Intent intent = new Intent(this, WakelockStatsActivity.class);
                this.startActivity(intent);
            	break;	
            	
        }  
        return false;  
    }
    
    /**
     * Handle getIsOnBattery
     */
    private void doIsOnBattery()
    {
    	BatteryStatsProxy batteryStats = new BatteryStatsProxy(this);
    	Log.d("BetterBatteryStats","Calling BatteryStatsProxy.getIsOnBattery()");
    	boolean bOnBatt = batteryStats.getIsOnBattery(this);
    	Toast.makeText(this, "Battery State: " + Boolean.toString(bOnBatt), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Handle getDischargeCurrentLevel
     */
    private void doGetDischargeCurrentLevel()
    {
    	BatteryStatsProxy batteryStats = new BatteryStatsProxy(this);
    	Log.d("BetterBatteryStats","Calling BatteryStatsProxy.getDischargeCurrentLevel()");
    	int iBatRemain = batteryStats.getDischargeCurrentLevel();
    	Toast.makeText(this, "Battery Remaining: " + Integer.toString(iBatRemain), Toast.LENGTH_SHORT).show();
    }

    /**
     * Get Wakelock Stats
     */
    private void getWakelockStats()
    {
    	BatteryStatsProxy mStats = new BatteryStatsProxy(this);
    	try
    	{
    		List<Wakelock> myWakelocks = mStats.getWakelockStats(this, BatteryStatsTypes.WAKE_TYPE_PARTIAL, BatteryStatsTypes.STATS_SINCE_CHARGED);
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    	}
    }
    
    /**
     * Get Process Stats
     */
    private void getProcessStats()
    {
    	BatteryStatsProxy mStats = new BatteryStatsProxy(this);
    	try
    	{
    		List<Process> mystats = mStats.getProcessStats(this, BatteryStatsTypes.STATS_SINCE_CHARGED);
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    	}
    }
    
}