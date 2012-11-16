/*
 * Copyright (C) 2011-2012 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *et
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
 * Shows alarms in a list
 * @author sven
 */

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.BatteryStatsTypes;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.adapters.StatsAdapter;
import com.asksven.betterbatterystats.data.StatsProvider;

public class AlarmsActivity extends ListActivity
{
	/**
	 * The logging TAG
	 */
	private static final String TAG = "AlarmsActivity";
	
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
	/**
	 * The ArrayAdpater for rendering the ListView
	 */
	private StatsAdapter m_listViewAdapter;
	
	/**
	 * @see android.app.Activity#onCreate(Bundle@SuppressWarnings("rawtypes")
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarms);
	}
	
	/* Request updates at startup */
	@Override
	protected void onResume()
	{
		super.onResume();
			new LoadStatData().execute(this);
	}

    /** 
     * Add menu items
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {  
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.alarms_menu, menu);
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
	        case R.id.refresh:
            	// Refresh
	        	doRefresh();
            	break;

        }  
        return false;  
    }    

	
	private void doRefresh()
	{
		BatteryStatsProxy.getInstance(this).invalidate();
    	new LoadStatData().execute(this);
		// Display the reference of the stat
		
//    	this.setListViewAdapter();
    	if (m_listViewAdapter != null)
    	{
    		m_listViewAdapter.notifyDataSetChanged();
    	}

	}

	// @see http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
	// for more details
	private class LoadStatData extends AsyncTask<Context, Integer, StatsAdapter>
	{
		@Override
	    protected StatsAdapter doInBackground(Context... params)
	    {
			//super.doInBackground(params);
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AlarmsActivity.this);
			boolean bFilter = sharedPrefs.getBoolean("filter_data", true);

			try
			{
				m_listViewAdapter = new StatsAdapter(AlarmsActivity.this,
						StatsProvider.getInstance(AlarmsActivity.this).getAlarmsStatList(bFilter, BatteryStatsTypes.STATS_CURRENT, null));
			}
			catch (Exception e)
			{
				Log.e(TAG, "Loading of alarm stats failed");
				m_listViewAdapter = null;
			}
	    	//StatsActivity.this.setListAdapter(m_listViewAdapter);
	        // getStatList();
	        return m_listViewAdapter;
	    }
		
		@Override
		protected void onPostExecute(StatsAdapter o)
	    {
			super.onPostExecute(o);
	        // update hourglass
	    	if (m_progressDialog != null)
	    	{
	    		m_progressDialog.hide();
	    		m_progressDialog = null;
	    	}
	    	AlarmsActivity.this.setListAdapter(o);
	    }
	    @Override
	    protected void onPreExecute()
	    {
	        // update hourglass
	    	// @todo this code is only there because onItemSelected is called twice
	    	if (m_progressDialog == null)
	    	{
		    	m_progressDialog = new ProgressDialog(AlarmsActivity.this);
		    	m_progressDialog.setMessage("Computing...");
		    	m_progressDialog.setIndeterminate(true);
		    	m_progressDialog.setCancelable(false);
		    	m_progressDialog.show();
	    	}
	    }
	}
	
//	/**
//	 * Get the Stat to be displayed
//	 * @return a List of StatElements sorted (descending)
//	 */
//	private ArrayList<Alarm> getAlarms()
//	{
//		ArrayList<Alarm> myRet = new ArrayList<Alarm>();
//		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
//		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
//
//		try
//		{
//			ArrayList<Alarm> myAlarms = AlarmsDumpsys.getAlarms();
//			Collections.sort(myAlarms);
//
//			for (int i = 0; i < myAlarms.size(); i++)
//			{
//				Alarm usage = myAlarms.get(i); 
//				if ( (!bFilterStats) || (usage.getWakeups() > 0) )
//				{
//						myRet.add(usage);
//				}
//			}
//
//		}
//		catch (Exception e)
//		{
//			Log.e(TAG, "An exception occured: " + e.getMessage());
//		}
//		return myRet;
//	}
}
