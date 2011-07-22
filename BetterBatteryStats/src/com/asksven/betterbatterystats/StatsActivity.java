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

/**
 * @author sven
 *
 */

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.BatteryStatsTypes;
import com.asksven.android.common.privateapiproxies.NetworkUsage;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.privateapiproxies.Wakelock;
import com.asksven.android.common.privateapiproxies.Process;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class StatsActivity extends ListActivity implements AdapterView.OnItemSelectedListener
{
    private final int MENU_ITEM_0 = 0;
    private final int MENU_ITEM_1 = 1;
    
	/**
	 * The logging TAG
	 */
	private static final String TAG = "StatsActivity";
	
	/**
	 * The ArrayAdpater for rendering the ListView
	 */
//	private ArrayAdapter<String> m_listViewAdapter;
	private StatsAdapter m_listViewAdapter;
	
	/**
	 * The Type of Stat to be displayed (default is "Since charged")
	 */
	private int m_iStatType = 0; 

	/**
	 * The Stat to be displayed (default is "Process")
	 */
	private int m_iStat = 0; 

	/**
	 * @see android.app.Activity#onCreate(Bundle@SuppressWarnings("rawtypes")
@SuppressWarnings("rawtypes")
)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);

		// retrieve default selections for spinners
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		m_iStat		= Integer.valueOf(sharedPrefs.getString("default_stat", "0"));
		m_iStatType	= Integer.valueOf(sharedPrefs.getString("default_stat_type", "0"));

		// Spinner for selecting the stat
		Spinner spinnerStat = (Spinner) findViewById(R.id.spinnerStat);
		
		ArrayAdapter spinnerStatAdapter = ArrayAdapter.createFromResource(
	            this, R.array.stats, android.R.layout.simple_spinner_item);
		spinnerStatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    
		spinnerStat.setAdapter(spinnerStatAdapter);
		// setSelection MUST be called after setAdapter
		spinnerStat.setSelection(m_iStat);
		spinnerStat.setOnItemSelectedListener(this);

		// Spinner for Selecting the Stat type
		Spinner spinnerStatType = (Spinner) findViewById(R.id.spinnerStatType);
		
		ArrayAdapter spinnerAdapter = ArrayAdapter.createFromResource(
	            this, R.array.stat_types, android.R.layout.simple_spinner_item);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    
		spinnerStatType.setAdapter(spinnerAdapter);
		// setSelection MUST be called after setAdapter
		spinnerStatType.setSelection(m_iStatType);
		spinnerStatType.setOnItemSelectedListener(this);

		this.setListViewAdapter();
	}

	/**
	 * In order to refresh the ListView we need to re-create the Adapter
	 * (should be the case but notifyDataSetChanged doesn't work so
	 * we recreate and set a new one)
	 */
	private void setListViewAdapter()
	{
		m_listViewAdapter = new StatsAdapter(this, getStatList());
        setListAdapter(m_listViewAdapter);
	}
	
    /** 
     * Add menu items
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {  
    	menu.add(0, MENU_ITEM_1, 0, "Preferences");
        menu.add(0, MENU_ITEM_0, 0, "About");  
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
            	Intent intentAbout = new Intent(this, AboutActivity.class);
                this.startActivity(intentAbout);
            	break;	
            case MENU_ITEM_1:  
            	Intent intentPrefs = new Intent(this, PreferencesActivity.class);
                this.startActivity(intentPrefs);
            	break;	
           	
        }  
        return false;  
    }    
	/**
	 * Take the change of selection from the spinners into account and refresh the ListView
	 * with the right data
	 */
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
	{
		// id is in the order of the spinners, 0 is stat, 1 is stat_type
		if (parent == (Spinner) findViewById(R.id.spinnerStatType))
		{
			m_iStatType = position;
		}
		else if (parent == (Spinner) findViewById(R.id.spinnerStat))
		{
			m_iStat = position;
		}
		else
		{
    		Log.e(TAG, "ProcessStatsActivity.onItemSelected error. ID could not be resolved");
    		Toast.makeText(this, "Error: could not resolve what changed", Toast.LENGTH_SHORT).show();

		}
		
		m_listViewAdapter.notifyDataSetChanged();
		this.setListViewAdapter();
	}

	public void onNothingSelected(AdapterView<?> parent)
	{
		// default
		m_iStatType = 0;
		m_listViewAdapter.notifyDataSetChanged();
		
	}
	/**
	 * Get the Stat to be displayed
	 * @return a List of StatElements sorted (descending)
	 */
	private List<StatElement> getStatList()
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		try
    	{
			BatteryStatsProxy mStats = new BatteryStatsProxy(this);
			
			switch (m_iStat)
			{
				// constants are related to arrays.xml string-array name="stats"
				case 0:
					return getProcessStatList(bFilterStats);
					
				case 1:
					return getWakelockStatList(bFilterStats);
				case 2:
					return getNetworkUsageStatList(bFilterStats);
						
			}
			
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    		Toast.makeText(this, "Wakelock Stats: an error occured while retrieving the statistics", Toast.LENGTH_SHORT).show();
    	}
		
		return new Vector<StatElement>();
	}
	
	/**
	 * Get the Process Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	List<StatElement> getProcessStatList(boolean bFilter) throws Exception
	{
		BatteryStatsProxy mStats = new BatteryStatsProxy(this);
		List<StatElement> myStats = new Vector<StatElement>();
		
   		List<Process> myProcesses = mStats.getProcessStats(this, m_iStatType);
		
		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myProcesses);
		
		for (int i = 0; i < myProcesses.size(); i++)
		{
			Process ps = myProcesses.get(i);
			if ( (!bFilter) || ((ps.getSystemTime() + ps.getUserTime()) > 0) )
			{
				myStats.add((StatElement) ps);
			}
		}
		
		return myStats;
		
	}

	/**
	 * Get the Process Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	List<StatElement> getWakelockStatList(boolean bFilter) throws Exception
	{
		List<StatElement> myStats = new Vector<StatElement>();
		
		BatteryStatsProxy mStats = new BatteryStatsProxy(this);

		List<Wakelock> myWakelocks = mStats.getWakelockStats(this, BatteryStatsTypes.WAKE_TYPE_PARTIAL, m_iStatType);
		
		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myWakelocks);
		
		for (int i = 0; i < myWakelocks.size(); i++)
		{
			Wakelock wl = myWakelocks.get(i);
			if ( (!bFilter) || ((wl.getDuration()/1000) > 0) )
			{
				myStats.add((StatElement) wl);
			}
		}
		return myStats;
	}

	/**
	 * Get the Network Usage Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	List<StatElement> getNetworkUsageStatList(boolean bFilter) throws Exception
	{
		List<StatElement> myStats = new Vector<StatElement>();
		
		BatteryStatsProxy mStats = new BatteryStatsProxy(this);

		List<NetworkUsage> myUsages = mStats.getNetworkUsageStats(this, m_iStatType);
		
		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myUsages);
		
		for (int i = 0; i < myUsages.size(); i++)
		{
			NetworkUsage usage = myUsages.get(i); 
			if ( (!bFilter) || ((usage.getBytesReceived() + usage.getBytesSent()) > 0) )
			{
				myStats.add((StatElement) usage);
			}
		}
		return myStats;
	}
	
	
}
