/*
 * Copyright (C) 2011 asksven
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
 * @author sven
 *
 */

import java.util.ArrayList;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.HistoryItem;
import com.asksven.betterbatterystats.R;

public class HistActivity extends ListActivity
{
	/**
	 * The logging TAG
	 */
	private static final String TAG = "HistActivity";
	
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
	/**
	 * The ArrayAdpater for rendering the ListView
	 */
	private HistAdapter m_listViewAdapter;
	
	/**
	 * @see android.app.Activity#onCreate(Bundle@SuppressWarnings("rawtypes")
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history);
//		m_listViewAdapter = new HistAdapter(this, getHistList());
//        setListAdapter(m_listViewAdapter);


	}
	
	/* Request updates at startup */
	@Override
	protected void onResume()
	{
		super.onResume();
		
		new LoadStatData().execute(this);
	}

	/**
	 * In order to refresh the ListView we need to re-create the Adapter
	 * (should be the case but notifyDataSetChanged doesn't work so
	 * we recreate and set a new one)
	 */
//	private void setListViewAdapter()
//	{
//		m_listViewAdapter = new HistAdapter(this, getHistList());
//		
//        setListAdapter(m_listViewAdapter);
//	}
	
    /** 
     * Add menu items
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {  
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.serie_menu, menu);
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
//    	new LoadStatData().execute(this);
		// Display the reference of the stat
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
//    	this.setListViewAdapter();
    	m_listViewAdapter.notifyDataSetChanged();

	}

	// @see http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
	// for more details
	private class LoadStatData extends AsyncTask<Context, Integer, HistAdapter>
	{
		@Override
	    protected HistAdapter doInBackground(Context... params)
	    {
			//super.doInBackground(params);
			m_listViewAdapter = new HistAdapter(HistActivity.this, getHistList());
	    	//StatsActivity.this.setListAdapter(m_listViewAdapter);
	        // getStatList();
	        return m_listViewAdapter;
	    }
		
		@Override
		protected void onPostExecute(HistAdapter o)
	    {
			super.onPostExecute(o);
	        // update hourglass
	    	if (m_progressDialog != null)
	    	{
	    		m_progressDialog.hide();
	    		m_progressDialog = null;
	    	}
	    	HistActivity.this.setListAdapter(o);
	    }
	    @Override
	    protected void onPreExecute()
	    {
	        // update hourglass
	    	// @todo this code is only there because onItemSelected is called twice
	    	if (m_progressDialog == null)
	    	{
		    	m_progressDialog = new ProgressDialog(HistActivity.this);
		    	m_progressDialog.setMessage("Computing...");
		    	m_progressDialog.setIndeterminate(true);
		    	m_progressDialog.setCancelable(false);
		    	m_progressDialog.show();
	    	}
	    }
	}
	
	/**
	 * Get the Stat to be displayed
	 * @return a List of StatElements sorted (descending)
	 */
	private ArrayList<HistoryItem> getHistList()
	{
		ArrayList<HistoryItem> myRet = new ArrayList<HistoryItem>();
		
//		HistoryItem myItem = new HistoryItem(Long.valueOf(32545550), 
//				Byte.valueOf("0"), Byte.valueOf("12"), Byte.valueOf("1"),
//				Byte.valueOf("12"), Byte.valueOf("1"),"", "", 120);
//		myRet.add(myItem);
		
		BatteryStatsProxy mStats = new BatteryStatsProxy(this);
		try
		{
			myRet = mStats.getHistory(this);
		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured while retrieving history. No result");
		}
		return myRet;
	}
	

}
