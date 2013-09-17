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
package com.asksven.betterbatterystats.fragments;

/**
 * Shows alarms in a list
 * @author sven
 */

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.asksven.android.common.privateapiproxies.BatteryInfoUnavailableException;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.adapters.StatsAdapter;
import com.asksven.betterbatterystats.data.StatsProvider;

public class RawStatsFragment extends SherlockListFragment 
{
	/**
	 * The logging TAG
	 */
	private static final String TAG = "KernelWakelocksActivity";
	
	/**
	 * The Stat to be displayed
	 */
	private int m_iStat = 0; 
	
	private static final String STAT = "com.asksven.betterbatterystats.STAT";

	
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
	/**
	 * The ArrayAdpater for rendering the ListView
	 */
	private StatsAdapter m_listViewAdapter;
	
	public static RawStatsFragment newInstance(int position)
	{
		RawStatsFragment fragment = new RawStatsFragment();
	    Bundle args=new Bundle();

	    args.putInt(STAT, position);
	    fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		m_iStat = getArguments().getInt(STAT, 0);
		
		// Load the data once
		new LoadStatData().execute(this);

	}
	
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.new_raw_stats, container, false);
		
		    
	//	new LoadStatData().execute(this);

		return rootView;

	}
	
	@Override
	public void onResume()
	{
		Log.i(TAG, "OnResume called");
		super.onResume();
		
	}
	

	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {  
		super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.kernelwakelocks_menu, menu);
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
		BatteryStatsProxy.getInstance(getActivity()).invalidate();
		new LoadStatData().execute(this);
    	m_listViewAdapter.notifyDataSetChanged();
	}
	

	// @see http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
	// for more details
	private class LoadStatData extends AsyncTask<Object, Integer, StatsAdapter>
	{
		private Exception m_exception = null;
		@Override
	    protected StatsAdapter doInBackground(Object... refresh)
	    {
			m_listViewAdapter = null;
			try
			{
				Log.i(TAG, "LoadStatData: refreshing display for raw stats");
				
				ArrayList<StatElement> stats = null;
				StatsProvider provider = StatsProvider.getInstance(getActivity());
				// constants are related to arrays.xml string-array name="stats"
				switch (m_iStat)
				{
					case 0:
						stats = provider.getCurrentOtherUsageStatList(true, false, false);
						break;
					case 1:
						stats = provider.getCurrentNativeKernelWakelockStatList(false, 0, 0);
						break;
					case 2:
						stats = provider.getCurrentWakelockStatList(false, 0, 0);
						break;
					case 3:
						stats = provider.getCurrentAlarmsStatList(false);
						break;
					case 4:
						stats = provider.getCurrentNativeNetworkUsageStatList(false);
						break;
					case 5:
						stats = provider.getCurrentCpuStateList(false);
						break;
					case 6:
						stats = provider.getCurrentProcessStatList(false, 0);
						break;
				}
				m_listViewAdapter = new StatsAdapter(getActivity(), stats);
			}
			catch (BatteryInfoUnavailableException e)
			{
				//Log.e(TAG, e.getMessage(), e.fillInStackTrace());
				Log.e(TAG, "Exception: "+Log.getStackTraceString(e));
				m_exception = e;

			}
			catch (Exception e)
			{
				//Log.e(TAG, e.getMessage(), e.fillInStackTrace());
				Log.e(TAG, "Exception: "+Log.getStackTraceString(e));
				m_exception = e;

			}

	    	//StatsActivity.this.setListAdapter(m_listViewAdapter);
	        // getStatList();
	        return m_listViewAdapter;
	    }
		
//		@Override
		protected void onPostExecute(StatsAdapter o)
	    {
//			super.onPostExecute(o);
	        // update hourglass
			try
			{
		    	if (m_progressDialog != null)
		    	{
		    		m_progressDialog.dismiss(); //hide();
		    		m_progressDialog = null;
		    	}
			}
			catch (Exception e)
			{
				// nop
			}
			finally 
			{
				m_progressDialog = null;
			}
			
	    	if (m_exception != null)
	    	{
	    		if (m_exception instanceof BatteryInfoUnavailableException)
	    		{
	    			Toast.makeText(getActivity(),
	    					"BatteryInfo Service could not be contacted.",
	    					Toast.LENGTH_LONG).show();

	    		}
	    		else
	    		{
	    			Toast.makeText(getActivity(),
	    					"An unknown error occured while retrieving stats.",
	    					Toast.LENGTH_LONG).show();
	    			
	    		}
	    	}

	    	RawStatsFragment.this.setListAdapter(o);
	    }
//	    @Override
	    protected void onPreExecute()
	    {
	        // update hourglass
	    	// @todo this code is only there because onItemSelected is called twice
	    	if (m_progressDialog == null)
	    	{
	    		try
	    		{
			    	m_progressDialog = new ProgressDialog(getActivity());
			    	m_progressDialog.setMessage("Computing...");
			    	m_progressDialog.setIndeterminate(true);
			    	m_progressDialog.setCancelable(false);
			    	m_progressDialog.show();
	    		}
	    		catch (Exception e)
	    		{
	    			m_progressDialog = null;
	    		}
	    	}
	    }
	}
	
}
