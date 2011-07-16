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

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.BatteryStatsTypes;
import com.asksven.android.common.privateapiproxies.Wakelock;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView;
import android.view.View;

public class WakelockStatsActivity extends ListActivity implements AdapterView.OnItemSelectedListener
{
	/**
	 * The logging TAG
	 */
	private static final String TAG = "WakelockStatsActivity";
	
	/**
	 * The ArrayAdpater for rendering the ListView
	 */
	private ArrayAdapter<String> m_listViewAdapter;
	
	/**
	 * The Type of Stat to be displayed (default is "Since charged")
	 */
	private int m_iStatType = 0; 
	
	/**
	 * @see android.app.Activity#onCreate(Bundle@SuppressWarnings("rawtypes")
@SuppressWarnings("rawtypes")
)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wakelock_stats);
		
		Spinner spinnerStatType = (Spinner) findViewById(R.id.spinnerStatType);
		ArrayAdapter spinnerAdapter = ArrayAdapter.createFromResource(
	            this, R.array.stat_types, android.R.layout.simple_spinner_item);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    
		spinnerStatType.setAdapter(spinnerAdapter);
		spinnerStatType.setOnItemSelectedListener(this);

		m_listViewAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,  
                getStatList());
        setListAdapter(m_listViewAdapter);
	}
	
	/**
	 * Take the change of selection from the spinner into account and refresh the ListView
	 */
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
	{
		m_iStatType = position;
		m_listViewAdapter.notifyDataSetChanged();
		m_listViewAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,  
                getStatList());
        setListAdapter(m_listViewAdapter);
	}

	public void onNothingSelected(AdapterView<?> parent)
	{
		// default
		m_iStatType = 0;
		m_listViewAdapter.notifyDataSetChanged();
		
	}
	/**
	 * Get the Wakelock Stats to be displayed
	 * @return a List of Wakelocks sorted by duration (descending)
	 */
	List<String> getStatList()
	{
		List<String> myStats = new Vector<String>();
		
		BatteryStatsProxy mStats = new BatteryStatsProxy(this);
    	try
    	{
    		List<Wakelock> myWakelocks = mStats.getWakelockStats(this, BatteryStatsTypes.WAKE_TYPE_PARTIAL, m_iStatType);
    		
    		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
    		Collections.sort(myWakelocks);
    		
    		for (int i = 0; i < myWakelocks.size(); i++)
    		{
    			Wakelock wl = myWakelocks.get(i); 
    			if ((wl.getDuration()/1000) > 0)
    			{
    				myStats.add(wl.getName() + " " + wl.getDuration()/1000 + "s");
    			}
    		}
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    		Toast.makeText(this, "Wakelock Stats: an error occured while retrieving the statistics", Toast.LENGTH_SHORT).show();
    	}
		
		return myStats;
	}
}
