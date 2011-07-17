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
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.BatteryStatsTypes;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.privateapiproxies.Wakelock;

public class AboutActivity extends Activity
{

    private static final String TAG = "AboutStatsActivity";

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // retrieve the version name and display it
        try
        {
        	PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        	TextView versionTextView = (TextView) findViewById(R.id.textViewVersion);
        	versionTextView.setText(pinfo.versionName);
        }
        catch (Exception e)
        {
        	Log.e(TAG, "An error occured retrieveing the version info: " + e.getMessage());
        }
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