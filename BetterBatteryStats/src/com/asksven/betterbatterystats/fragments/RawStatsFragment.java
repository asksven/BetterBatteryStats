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

public class RawStatsFragment extends SherlockListFragment implements AdapterView.OnItemSelectedListener
{
	/**
	 * The logging TAG
	 */
	private static final String TAG = "KernelWakelocksActivity";
	
	/**
	 * The Stat to be displayed
	 */
	private int m_iStat = 0; 

	
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
	/**
	 * The ArrayAdpater for rendering the ListView
	 */
	private StatsAdapter m_listViewAdapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.raw_stats, container, false);
		
		// Spinner for selecting the stat
		Spinner spinnerStat = (Spinner) rootView.findViewById(R.id.spinnerStat);
		
		ArrayAdapter spinnerStatAdapter = ArrayAdapter.createFromResource(
	            getActivity(), R.array.stats, android.R.layout.simple_spinner_item);
		spinnerStatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    
		spinnerStat.setAdapter(spinnerStatAdapter);
		// setSelection MUST be called after setAdapter
		spinnerStat.setSelection(m_iStat);
		spinnerStat.setOnItemSelectedListener(this);
		
		new LoadStatData().execute(this);

		return rootView;

	}
	
    /** 
     * Add menu items
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {  
    	MenuInflater inflater = getSherlockActivity().getSupportMenuInflater();
        inflater.inflate(R.menu.kernelwakelocks_menu, menu);
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
		BatteryStatsProxy.getInstance(getActivity()).invalidate();
		new LoadStatData().execute(this);
    	m_listViewAdapter.notifyDataSetChanged();
	}
	
	/**
	 * Take the change of selection from the spinners into account and refresh the ListView
	 * with the right data
	 */
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
	{
		// this method is fired even if nothing has changed so we nee to find that out
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		boolean bChanged = false;
		
		// id is in the order of the spinners, 0 is stat, 1 is stat_type
		if (parent == (Spinner) getView().findViewById(R.id.spinnerStat))
		{
			int iNewStat = position;
			if ( m_iStat != iNewStat )
			{
				m_iStat = iNewStat;
				bChanged = true;
			}
			else
			{
				return;
			}

			// inform the user when he tries to use functions requiring root and he doesn't have root enabled
			boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);
			
			if (!rootEnabled)
			{
				if ((m_iStat == 4) || (m_iStat == 3)) 
				{
					Toast.makeText(getActivity(),
							"This function requires root access. Check \"Advanced\" preferences",
							Toast.LENGTH_LONG).show();
				}
			}

		}
		else
		{
    		Log.e(TAG, "RawStatsActivity.onItemSelected error. ID could not be resolved");
    		Toast.makeText(getActivity(), "Error: could not resolve what changed", Toast.LENGTH_SHORT).show();

		}

        if (bChanged)
        {
        	doRefresh();
        }
	}

	public void onNothingSelected(AdapterView<?> parent)
	{
		// do nothing
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
