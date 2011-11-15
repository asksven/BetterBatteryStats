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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.BatteryStatsTypes;
import com.asksven.android.common.privateapiproxies.KernelWakelock;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.NetworkUsage;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.privateapiproxies.Wakelock;
import com.asksven.android.system.AndroidVersion;
import android.view.View;
import com.asksven.betterbatterystats.R;

public class StatsActivity extends ListActivity implements AdapterView.OnItemSelectedListener
{
    // dependent on arrays.xml
    private final static int STATS_CUSTOM 	= 4;
    
    private ArrayList<StatElement> m_refWakelocks = null;
    private ArrayList<StatElement> m_refKernelWakelocks = null;
    private ArrayList<StatElement> m_refProcesses = null;
    private ArrayList<StatElement> m_refNetwork	 = null;
    private ArrayList<StatElement> m_refOther	 = null;
    private long m_refBatteryRealtime = 0;

	/**
	 * The logging TAG
	 */
	private static final String TAG = "StatsActivity";

	/**
	 * The logfile TAG
	 */
	private static final String LOGFILE = "BetterBatteryStats_Dump.log";
	
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
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
	 * the selected sorting
	 */
	private int m_iSorting = 0;
	/**
	 * @see android.app.Activity#onCreate(Bundle@SuppressWarnings("rawtypes")
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);
		
		// check if we have a new release
		// if yes show release notes
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String strLastRelease	= sharedPrefs.getString("last_release", "0");
		String strCurrentRelease = "";
		try
		{
		PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		
    	strCurrentRelease = Integer.toString(pinfo.versionCode);
		}
		catch (Exception e)
		{
			// nop strCurrentRelease is set to ""
		}
    	if (!strLastRelease.equals(strCurrentRelease))
    	{
    		// show the readme
	    	Intent intentReleaseNotes = new Intent(this, HelpActivity.class);
	    	intentReleaseNotes.putExtra("filename", "readme.html");
	        this.startActivity(intentReleaseNotes);
	        
	        // save the current release to properties so that the dialog won't be shown till next version
	        SharedPreferences.Editor editor = sharedPrefs.edit();
	        editor.putString("last_release", strCurrentRelease);
	        editor.commit();
    	}
		// retrieve default selections for spinners
    	
    	m_iStat		= Integer.valueOf(sharedPrefs.getString("default_stat", "0"));
		m_iStatType	= Integer.valueOf(sharedPrefs.getString("default_stat_type", "0"));
		
		try
		{
			// recover any saved state
			if ( (savedInstanceState != null) && (!savedInstanceState.isEmpty()))
			{
				m_refWakelocks 			= (ArrayList<StatElement>) savedInstanceState.getSerializable("wakelockstate");
				m_refKernelWakelocks 	= (ArrayList<StatElement>) savedInstanceState.getSerializable("kernelwakelockstate");
				m_refProcesses 			= (ArrayList<StatElement>) savedInstanceState.getSerializable("processstate");
				m_refOther 				= (ArrayList<StatElement>) savedInstanceState.getSerializable("otherstate");
				m_iStat 				= (Integer) savedInstanceState.getSerializable("stat");
				m_iStatType 			= (Integer) savedInstanceState.getSerializable("stattype");
				m_refBatteryRealtime 	= (Long) savedInstanceState.getSerializable("batteryrealtime");
	 			
			}
		}
		catch (Exception e)
		{
			m_iStat		= Integer.valueOf(sharedPrefs.getString("default_stat", "0"));
			m_iStatType	= Integer.valueOf(sharedPrefs.getString("default_stat_type", "0"));
			m_refBatteryRealtime = 0;
			
    		Log.e(TAG, "Exception: " + e.getMessage());
    		DataStorage.LogToFile(LOGFILE, "Exception in onCreate restoring Bundle");
    		DataStorage.LogToFile(LOGFILE, e.getMessage());
    		DataStorage.LogToFile(LOGFILE, e.getStackTrace());
    		
    		Toast.makeText(this, "Wakelock Stats: an error occured while recovering the previous state", Toast.LENGTH_SHORT).show();
		}

		// Display the reference of the stat
        TextView tvSince = (TextView) findViewById(R.id.TextViewSince);
        if (tvSince != null)
        {
        	tvSince.setText("Since " + DateUtils.formatDuration(getBatteryRealtime(m_iStatType)));
        }
        
        if (sharedPrefs.getBoolean("hide_since", true))
        {
        	FrameLayout myLayout = (FrameLayout) findViewById(R.id.FrameLayoutSince);
        	myLayout.setVisibility(View.GONE);
        }
		

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
		
		ArrayAdapter spinnerAdapter = null;
		if (AndroidVersion.isFroyo())
		{
			spinnerAdapter = ArrayAdapter.createFromResource(
	            this, R.array.stat_types_froyo, android.R.layout.simple_spinner_item);
		}
		else
		{
			spinnerAdapter = ArrayAdapter.createFromResource(
		            this, R.array.stat_types, android.R.layout.simple_spinner_item);
			
		}
		
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    
		spinnerStatType.setAdapter(spinnerAdapter);
		// setSelection MUST be called after setAdapter
		spinnerStatType.setSelection(positionFromStatType(m_iStatType));
		spinnerStatType.setOnItemSelectedListener(this);

		this.setListViewAdapter();

		// sorting
		String strOrderBy = sharedPrefs.getString("default_orderby", "0");
		try
		{
			m_iSorting = Integer.valueOf(strOrderBy);
		}
		catch(Exception e)
		{
			// handle error here
			m_iSorting = 0;
			
		}

	}
    
	/* Request updates at startup */
	@Override
	protected void onResume()
	{
		super.onResume();
		
		// refresh 
		doRefresh();
	}

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause()
	{
		super.onPause();
	}

	/**
	 * Handle the "back" button to make sure the user wants to
	 * quit the application and lose any custom ref 
	 */
	@Override 
    public boolean onKeyDown(int keyCode, KeyEvent event)
	{ 
        // if "back" was pressed. If a custom ref was saved ask if app should
		// still be closed
        if (keyCode == KeyEvent.KEYCODE_BACK) 
        { 
        	// do we have a custom ref
        	if (m_refOther != null)
        	{
        		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
        		{
        		    @Override
        		    public void onClick(DialogInterface dialog, int which)
        		    {
        		        switch (which)
        		        {
        		        case DialogInterface.BUTTON_POSITIVE:
        		            //Yes button clicked
        		        	finish();
        		            break;

        		        case DialogInterface.BUTTON_NEGATIVE:
        		            //No button clicked
        		            break;
        		        }
        		    }
        		};
        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        		builder.setMessage("By closing the custom reference will be lost. Are you sure?").setPositiveButton("Yes", dialogClickListener)
        		    .setNegativeButton("No", dialogClickListener).show();
        		return true;
        	}
        	else
        	{
        		return super.onKeyDown(keyCode, event);
        	}
        } 
        return super.onKeyDown(keyCode, event); 
    } 	
    /**
     * Save state, the application is going to get moved out of memory
     * @see http://stackoverflow.com/questions/151777/how-do-i-save-an-android-applications-state
     */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
    {
    	super.onSaveInstanceState(savedInstanceState);
        
    	savedInstanceState.putSerializable("stattype", m_iStatType); 
    	savedInstanceState.putSerializable("stat", m_iStat);
    	savedInstanceState.putSerializable("batteryrealtime", m_refBatteryRealtime);
		
    	if (m_refProcesses != null)
    	{		
    		savedInstanceState.putSerializable("wakelockstate", m_refWakelocks);
    		savedInstanceState.putSerializable("kernelwakelockstate", m_refKernelWakelocks);
    		savedInstanceState.putSerializable("processstate", m_refProcesses);
    		savedInstanceState.putSerializable("otherstate", m_refOther);
    		savedInstanceState.putSerializable("networkstate", m_refNetwork);
    		savedInstanceState.putSerializable("batteryrealtime", m_refBatteryRealtime);	
        }
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
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }  

    @Override
	public boolean onPrepareOptionsMenu(Menu menu)
    {
    	boolean bSortingEnabled = true;
    	    	
    	MenuItem sortCount = menu.findItem(R.id.by_count_desc);
    	MenuItem sortTime = menu.findItem(R.id.by_time_desc);
    	
    	if (m_iSorting == 0)
    	{
    		// sorting is by time
    		sortTime.setEnabled(false);
    		sortCount.setEnabled(true);
    	}
    	else
    	{
    		// sorting is by count
    		sortTime.setEnabled(true);
    		sortCount.setEnabled(false);
    	}
    	
		if (m_iStat == 2) // @see arrays.xml, dependency to string-array name="stats"
		{
			// disable menu group
			bSortingEnabled = true;
		}
		else
		{
			// enable menu group
			bSortingEnabled = true;
		}
		menu.setGroupEnabled(R.id.sorting_group, bSortingEnabled);
		
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
	        case R.id.preferences:  
	        	Intent intentPrefs = new Intent(this, PreferencesActivity.class);
	            this.startActivity(intentPrefs);
	        	break;	

	        case R.id.graph:  
	        	Intent intentGraph = new Intent(this, BatteryGraphActivity.class);
	            this.startActivity(intentGraph);
	        	break;	

	        case R.id.refresh:
            	// Refresh
	        	doRefresh();
            	break;
            case R.id.dump:
            	// Dump to File
            	new WriteDumpFile().execute("");
            	//this.writeDumpToFile();
            	break;
            case R.id.custom_ref:
            	// Set custom reference
            	new SetCustomRef().execute(this);
            	break;
            case R.id.by_time_desc:
            	// Enable "count" option
            	m_iSorting = 0;            	
            	doRefresh();
            	break;	
            case R.id.by_count_desc:
            	// Enable "count" option
            	m_iSorting = 1;            	
            	doRefresh();
            	break;	

            case R.id.about:
            	// About
            	Intent intentAbout = new Intent(this, AboutActivity.class);
                this.startActivity(intentAbout);
            	break;
            case R.id.getting_started:
            	// Help
            	Intent intentHelp = new Intent(this, HelpActivity.class);
            	intentHelp.putExtra("filename", "help.html");
                this.startActivity(intentHelp);
            	break;	

            case R.id.howto:
            	// How To
            	Intent intentHowTo = new Intent(this, HelpActivity.class);
            	intentHowTo.putExtra("filename", "howto.html");
                this.startActivity(intentHowTo);
            	break;	

            case R.id.releasenotes:
            	// Release notes
            	Intent intentReleaseNotes = new Intent(this, HelpActivity.class);
            	intentReleaseNotes.putExtra("filename", "readme.html");
                this.startActivity(intentReleaseNotes);
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
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean bAlternateMethod = sharedPrefs.getBoolean("alternate_kwl", true);

		// id is in the order of the spinners, 0 is stat, 1 is stat_type
		if (parent == (Spinner) findViewById(R.id.spinnerStatType))
		{
			// The Spinner does not show all available stats so it must be translated
			m_iStatType = statTypeFromPosition(position);
			// Display the reference of the stat
	
			// warn the user if custom ref was chosen without having selected a ref first
			if ( (m_iStatType == STATS_CUSTOM) && (m_refBatteryRealtime == 0))
			{
				Toast.makeText(this, "Warning: there is no custom reference set.", Toast.LENGTH_SHORT).show();
			}
		}
		else if (parent == (Spinner) findViewById(R.id.spinnerStat))
		{
			m_iStat = position;
			
			// check if Kernel Wakelocks: if so disable stat type except the prefs say otherwise
			if ( (m_iStat == 3) && (bAlternateMethod) ) // array.xml
			{
				((Spinner) findViewById(R.id.spinnerStatType)).setVisibility(View.INVISIBLE);
				((Spinner) findViewById(R.id.spinnerStatType)).setEnabled(false);
				m_iStatType = BatteryStatsTypes.STATS_SINCE_CHARGED;
//				((Spinner) findViewById(R.id.spinnerStatType)).setSelection(positionFromStatType(m_iStatType));
			}
			else
			{
				((Spinner) findViewById(R.id.spinnerStatType)).setVisibility(View.VISIBLE);
				((Spinner) findViewById(R.id.spinnerStatType)).setEnabled(true);
			}
		}
		else
		{
    		Log.e(TAG, "ProcessStatsActivity.onItemSelected error. ID could not be resolved");
    		Toast.makeText(this, "Error: could not resolve what changed", Toast.LENGTH_SHORT).show();

		}

        TextView tvSince = (TextView) findViewById(R.id.TextViewSince);
		long timeSinceBoot = SystemClock.elapsedRealtime();

        if ( (m_iStat != 3) || (!bAlternateMethod) )
        {
        	tvSince.setText("Since " + DateUtils.formatDuration(getBatteryRealtime(m_iStatType)));
        }
        else
        {
        	tvSince.setText("Since boot " + DateUtils.formatDuration(timeSinceBoot));
        }
		
		// @todo fix this: this method is called twice
		//m_listViewAdapter.notifyDataSetChanged();
		new LoadStatData().execute(this);
		//this.setListViewAdapter();
	}

	public void onNothingSelected(AdapterView<?> parent)
	{
		// default
		m_iStatType = 0;
		//m_listViewAdapter.notifyDataSetChanged();
		
	}
	
	private void doRefresh()
	{
    	new LoadStatData().execute(this);
		// Display the reference of the stat
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        TextView tvSince = (TextView) findViewById(R.id.TextViewSince);
        tvSince.setText("Since " + DateUtils.formatDuration(getBatteryRealtime(m_iStatType)));
    	
        FrameLayout myLayout = (FrameLayout) findViewById(R.id.FrameLayoutSince);
		if (sharedPrefs.getBoolean("hide_since", true))
        {
        	myLayout.setVisibility(View.GONE);
        }
		else
		{
			myLayout.setVisibility(View.VISIBLE);
		}
		
    	this.setListViewAdapter();

	}
	private class WriteDumpFile extends AsyncTask
	{
		@Override
	    protected Object doInBackground(Object... params)
	    {
			writeDumpToFile();
	    	return true;
	    }

		@Override
		protected void onPostExecute(Object o)
	    {
			super.onPostExecute(o);
	        // update hourglass
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
			m_listViewAdapter = new StatsAdapter(StatsActivity.this, getStatList());
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
	    	StatsActivity.this.setListAdapter(o);
	    }
	    @Override
	    protected void onPreExecute()
	    {
	        // update hourglass
	    	// @todo this code is only there because onItemSelected is called twice
	    	if (m_progressDialog == null)
	    	{
		    	m_progressDialog = new ProgressDialog(StatsActivity.this);
		    	m_progressDialog.setMessage("Computing...");
		    	m_progressDialog.setIndeterminate(true);
		    	m_progressDialog.setCancelable(false);
		    	m_progressDialog.show();
	    	}
	    }
	}
	
	// @see http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
	// for more details
	private class SetCustomRef extends AsyncTask<Context, Integer, Boolean>
	{
		@Override
	    protected Boolean doInBackground(Context... params)
	    {
			//super.doInBackground(params);
			StatsActivity.this.setCustomReference();
			return true;
	    }
		
		@Override
		protected void onPostExecute(Boolean b)
	    {
			super.onPostExecute(b);
	        // update hourglass
	    	if (m_progressDialog != null)
	    	{
	    		m_progressDialog.hide();
	    		m_progressDialog = null;
	    	}
	    	
	    }
	    @Override
	    protected void onPreExecute()
	    {
	        // update hourglass
	    	// @todo this code is only there because onItemSelected is called twice
	    	if (m_progressDialog == null)
	    	{
		    	m_progressDialog = new ProgressDialog(StatsActivity.this);
		    	m_progressDialog.setMessage("Saving...");
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
	private ArrayList<StatElement> getStatList()
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref", "0"));
		
		try
    	{			
			switch (m_iStat)
			{
				// constants are related to arrays.xml string-array name="stats"
				case 0:
					return getProcessStatList(bFilterStats, m_iStatType);
				case 1:
					return getWakelockStatList(bFilterStats, m_iStatType, iPctType);
				case 2:
					return getOtherUsageStatList(bFilterStats, m_iStatType);	
				case 3:
					return getKernelWakelockStatList(bFilterStats, m_iStatType, iPctType);
			}
			
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    	}
		
		return new ArrayList<StatElement>();
	}
	
	/**
	 * Get the Process Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	ArrayList<StatElement> getProcessStatList(boolean bFilter, int iStatType) throws Exception
	{
		BatteryStatsProxy mStats = new BatteryStatsProxy(this);
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		ArrayList<Process> myProcesses = null;
		ArrayList<Process> myRetProcesses = new ArrayList<Process>();
		
		// if we are using custom ref. always retrieve "stats current"
		if (iStatType == STATS_CUSTOM)
		{
			myProcesses = mStats.getProcessStats(this, BatteryStatsTypes.STATS_CURRENT);
		}
		else
		{
			myProcesses = mStats.getProcessStats(this, iStatType);
		}
		
		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		//Collections.sort(myProcesses);
		
		for (int i = 0; i < myProcesses.size(); i++)
		{
			Process ps = myProcesses.get(i);
			if ( (!bFilter) || ((ps.getSystemTime() + ps.getUserTime()) > 0) )
			{
				// we must distinguish two situations
				// a) we use custom stat type
				// b) we use regular stat type
				
				if (iStatType == STATS_CUSTOM)
				{
					// case a)
					// we need t return a delta containing
					//   if a process is in the new list but not in the custom ref
					//	   the full time is returned
					//   if a process is in the reference return the delta
					//	 a process can not have disapeared in btwn so we don't need
					//	 to test the reverse case
					ps.substractFromRef(m_refProcesses);
					
					// we must recheck if the delta process is still above threshold
					if ( (!bFilter) || ((ps.getSystemTime() + ps.getUserTime()) > 0) )
					{
						myRetProcesses.add(ps);
					}
				}
				else
				{
					// case b) nothing special
					myRetProcesses.add(ps);
				}
			}
		}
		
		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		switch (m_iSorting)
		{
			case 0:
				// by Duration
				Comparator<Process> myCompTime = new Process.ProcessTimeComparator();
				Collections.sort(myRetProcesses, myCompTime);
				break;
			case 1:
				// by Count
				Comparator<Process> myCompCount = new Process.ProcessCountComparator();
				Collections.sort(myRetProcesses, myCompCount);
				break;
		}
		
		for (int i=0; i < myRetProcesses.size(); i++)
		{
			myStats.add((StatElement) myRetProcesses.get(i));
		}
		
		return myStats;
		
	}

	/**
	 * Get the Wakelock Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	ArrayList<StatElement> getWakelockStatList(boolean bFilter, int iStatType, int iPctType) throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		BatteryStatsProxy mStats = new BatteryStatsProxy(this);
		
		ArrayList<Wakelock> myWakelocks = null;
		ArrayList<Wakelock> myRetWakelocks = new ArrayList<Wakelock>();
		// if we are using custom ref. always retrieve "stats current"
		if (iStatType == STATS_CUSTOM)
		{
			myWakelocks = mStats.getWakelockStats(this, BatteryStatsTypes.WAKE_TYPE_PARTIAL, BatteryStatsTypes.STATS_CURRENT, iPctType);
		}
		else
		{
			myWakelocks = mStats.getWakelockStats(this, BatteryStatsTypes.WAKE_TYPE_PARTIAL, iStatType, iPctType);
		}

		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myWakelocks);
		
		for (int i = 0; i < myWakelocks.size(); i++)
		{
			Wakelock wl = myWakelocks.get(i);
			if ( (!bFilter) || ((wl.getDuration()/1000) > 0) )
			{
				// we must distinguish two situations
				// a) we use custom stat type
				// b) we use regular stat type
				
				if (iStatType == STATS_CUSTOM)
				{
					// case a)
					// we need t return a delta containing
					//   if a process is in the new list but not in the custom ref
					//	   the full time is returned
					//   if a process is in the reference return the delta
					//	 a process can not have disapeared in btwn so we don't need
					//	 to test the reverse case
					wl.substractFromRef(m_refWakelocks);
					
					// we must recheck if the delta process is still above threshold
					if ( (!bFilter) || ((wl.getDuration()/1000) > 0) )
					{
						myRetWakelocks.add( wl);
					}
				}
				else
				{
					// case b) nothing special
					myRetWakelocks.add(wl);
				}

			}
		}

		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		switch (m_iSorting)
		{
			case 0:
				// by Duration
				Comparator<Wakelock> myCompTime = new Wakelock.WakelockTimeComparator();
				Collections.sort(myRetWakelocks, myCompTime);
				break;
			case 1:
				// by Count
				Comparator<Wakelock> myCompCount = new Wakelock.WakelockCountComparator();
				Collections.sort(myRetWakelocks, myCompCount);
				break;
		}

		
		
		for (int i=0; i < myRetWakelocks.size(); i++)
		{
			myStats.add((StatElement) myRetWakelocks.get(i));
		}

		// @todo add sorting by settings here: Collections.sort......
		return myStats;
	}

	/**
	 * Get the Kernel Wakelock Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	ArrayList<StatElement> getKernelWakelockStatList(boolean bFilter, int iStatType, int iPctType) throws Exception
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean bAlternateMethod = sharedPrefs.getBoolean("alternate_kwl", true);

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		BatteryStatsProxy mStats = new BatteryStatsProxy(this);
		
		ArrayList<KernelWakelock> myKernelWakelocks = null;
		ArrayList<KernelWakelock> myRetKernelWakelocks = new ArrayList<KernelWakelock>();
		// if we are using custom ref. always retrieve "stats current"

		if (iStatType == STATS_CUSTOM)
		{
			myKernelWakelocks = mStats.getKernelWakelockStats(this, BatteryStatsTypes.STATS_CURRENT, iPctType, bAlternateMethod);
		}
		else
		{
			myKernelWakelocks = mStats.getKernelWakelockStats(this, iStatType, iPctType, bAlternateMethod);
		}

		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myKernelWakelocks);
		
		for (int i = 0; i < myKernelWakelocks.size(); i++)
		{
			KernelWakelock wl = myKernelWakelocks.get(i);
			if ( (!bFilter) || ((wl.getDuration()/1000) > 0) )
			{
				// we must distinguish two situations
				// a) we use custom stat type
				// b) we use regular stat type
				
				if (iStatType == STATS_CUSTOM)
				{
					// case a)
					// we need t return a delta containing
					//   if a process is in the new list but not in the custom ref
					//	   the full time is returned
					//   if a process is in the reference return the delta
					//	 a process can not have disapeared in btwn so we don't need
					//	 to test the reverse case
					wl.substractFromRef(m_refKernelWakelocks);


					// we must recheck if the delta process is still above threshold
					if ( (!bFilter) || ((wl.getDuration()/1000) > 0) )
					{
						myRetKernelWakelocks.add( wl);
					}
				}
				else
				{
					// case b) nothing special
					myRetKernelWakelocks.add(wl);
				}

			}
		}

		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		switch (m_iSorting)
		{
			case 0:
				// by Duration
				Comparator<KernelWakelock> myCompTime = new KernelWakelock.TimeComparator();
				Collections.sort(myRetKernelWakelocks, myCompTime);
				break;
			case 1:
				// by Count
				Comparator<KernelWakelock> myCompCount = new KernelWakelock.CountComparator();
				Collections.sort(myRetKernelWakelocks, myCompCount);
				break;
		}

		
		
		for (int i=0; i < myRetKernelWakelocks.size(); i++)
		{
			myStats.add((StatElement) myRetKernelWakelocks.get(i));
		}

		// @todo add sorting by settings here: Collections.sort......
		return myStats;
	}

	/**
	 * Get the Network Usage Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Network usages sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	ArrayList<StatElement> getNetworkUsageStatList(boolean bFilter, int iStatType) throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		BatteryStatsProxy mStats = new BatteryStatsProxy(this);

		ArrayList<NetworkUsage> myUsages = null;
		
		
		// if we are using custom ref. always retrieve "stats current"
		if (iStatType == STATS_CUSTOM)
		{
			myUsages = mStats.getNetworkUsageStats(this, BatteryStatsTypes.STATS_CURRENT);
		}
		else
		{
			myUsages = mStats.getNetworkUsageStats(this, iStatType);
		}

		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myUsages);
		
		for (int i = 0; i < myUsages.size(); i++)
		{
			NetworkUsage usage = myUsages.get(i); 
			if ( (!bFilter) || ((usage.getBytesReceived() + usage.getBytesSent()) > 0) )
			{
				// we must distinguish two situations
				// a) we use custom stat type
				// b) we use regular stat type
				
				if (iStatType == STATS_CUSTOM)
				{
					// case a)
					// we need t return a delta containing
					//   if a process is in the new list but not in the custom ref
					//	   the full time is returned
					//   if a process is in the reference return the delta
					//	 a process can not have disapeared in btwn so we don't need
					//	 to test the reverse case
					usage.substractFromRef(m_refNetwork);
					
					// we must recheck if the delta process is still above threshold
					if ( (!bFilter) || ((usage.getBytesReceived() + usage.getBytesSent()) > 0) )
					{
						myStats.add((StatElement) usage);
					}
				}
				else
				{
					// case b) nothing special
					myStats.add((StatElement) usage);
				}

			}
		}
		
		return myStats;
	}

	/**
	 * Get the Other Usage Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Other usages sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	ArrayList<StatElement> getOtherUsageStatList(boolean bFilter, int iStatType) throws Exception
	{
		BatteryStatsProxy mStats = new BatteryStatsProxy(this);

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		// List to store the other usages to
		ArrayList<Misc> myUsages = new ArrayList<Misc>();

		long rawRealtime = SystemClock.elapsedRealtime() * 1000;
        long batteryRealtime = mStats.getBatteryRealtime(rawRealtime);

        long whichRealtime 		= 0;
        long timeBatteryUp 		= 0;
        long timeScreenOn		= 0;
        long timePhoneOn		= 0;
        long timeWifiOn			= 0;
        long timeWifiRunning	= 0;
        long timeWifiMulticast	= 0;
        long timeWifiLocked		= 0;
        long timeWifiScan		= 0;
        long timeAudioOn		= 0;
        long timeVideoOn		= 0;
        long timeBluetoothOn	= 0;
        
		// if we are using custom ref. always retrieve "stats current"
		if (iStatType == STATS_CUSTOM)
		{
	        whichRealtime 		= mStats.computeBatteryRealtime(rawRealtime, BatteryStatsTypes.STATS_CURRENT)  / 1000;      
	        timeBatteryUp 		= mStats.computeBatteryUptime(SystemClock.uptimeMillis() * 1000, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeScreenOn 		= mStats.getScreenOnTime(batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timePhoneOn 		= mStats.getPhoneOnTime(batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeWifiOn 			= mStats.getWifiOnTime(batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeWifiRunning		= mStats.getWifiRunningTime(this, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeWifiMulticast	= mStats.getWifiMulticastTime(this, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeWifiLocked		= mStats.getFullWifiLockTime(this, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeWifiScan		= mStats.getScanWifiLockTime(this, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeAudioOn			= mStats.getAudioTurnedOnTime(this, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeVideoOn			= mStats.getVideoTurnedOnTime(this, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;

	        timeBluetoothOn 	= mStats.getBluetoothOnTime(batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
		}
		else
		{
	        whichRealtime 		= mStats.computeBatteryRealtime(rawRealtime, iStatType)  / 1000;      
	        timeBatteryUp 		= mStats.computeBatteryUptime(SystemClock.uptimeMillis() * 1000, iStatType) / 1000;		
	        timeScreenOn 		= mStats.getScreenOnTime(batteryRealtime, iStatType) / 1000;
	        timePhoneOn 		= mStats.getPhoneOnTime(batteryRealtime, iStatType) / 1000;
	        timeWifiOn 			= mStats.getWifiOnTime(batteryRealtime, iStatType) / 1000;
	        timeWifiRunning 	= mStats.getWifiRunningTime(this, batteryRealtime, iStatType) / 1000;
	        timeWifiMulticast	= mStats.getWifiMulticastTime(this, batteryRealtime, iStatType) / 1000;
	        timeWifiLocked		= mStats.getFullWifiLockTime(this, batteryRealtime, iStatType) / 1000;
	        timeWifiScan		= mStats.getScanWifiLockTime(this, batteryRealtime, iStatType) / 1000;
	        timeAudioOn			= mStats.getAudioTurnedOnTime(this, batteryRealtime, iStatType) / 1000;
	        timeVideoOn			= mStats.getVideoTurnedOnTime(this, batteryRealtime, iStatType) / 1000;

	        timeBluetoothOn = mStats.getBluetoothOnTime(batteryRealtime, iStatType) / 1000;
		}

		if (timeBatteryUp > 0)
		{
            myUsages.add(new Misc("Awake", timeBatteryUp, whichRealtime));
        }
        
        if (timeScreenOn > 0)
        {
        	myUsages.add(new Misc("Screen On", timeScreenOn, whichRealtime));  
        }
                
        if (timePhoneOn > 0)
        {
        	myUsages.add(new Misc("Phone On", timePhoneOn, whichRealtime));
        }
        
        if (timeWifiOn > 0)
        {
        	myUsages.add(new Misc("Wifi On", timeWifiOn, whichRealtime));
        }
        
        if (timeWifiRunning > 0)
        {
        	myUsages.add(new Misc("Wifi Running", timeWifiRunning, whichRealtime));
        }
        
        if (timeBluetoothOn > 0)
        {
        	myUsages.add(new Misc("Bluetooth On", timeBluetoothOn, whichRealtime)); 
        }

        if (timeWifiMulticast > 0)
        {
        	myUsages.add(new Misc("Wifi Multicast On", timeWifiMulticast, whichRealtime)); 
        }

        if (timeWifiLocked > 0)
        {
        	myUsages.add(new Misc("Wifi Locked", timeWifiLocked, whichRealtime)); 
        }

        if (timeWifiScan > 0)
        {
        	myUsages.add(new Misc("Wifi Scan", timeWifiScan, whichRealtime)); 
        }

        if (timeAudioOn > 0)
        {
        	myUsages.add(new Misc("Video On", timeAudioOn, whichRealtime)); 
        }

        if (timeVideoOn > 0)
        {
        	myUsages.add(new Misc("Video On", timeVideoOn, whichRealtime)); 
        }

        // sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myUsages);

		for (int i = 0; i < myUsages.size(); i++)
		{
			Misc usage = myUsages.get(i); 
			if ( (!bFilter) || (usage.getTimeOn() > 0) )
			{
				if (iStatType == STATS_CUSTOM)
				{
					// case a)
					// we need t return a delta containing
					//   if a process is in the new list but not in the custom ref
					//	   the full time is returned
					//   if a process is in the reference return the delta
					//	 a process can not have disapeared in btwn so we don't need
					//	 to test the reverse case
					usage.substractFromRef(m_refOther);
					if ( (!bFilter) || (usage.getTimeOn() > 0) )
					{
						myStats.add((StatElement) usage);
					}
				}
				else
				{
					// case b)
					// nothing special
					myStats.add((StatElement) usage);
				}
			}
		}
		return myStats;
	}

	private long getBatteryRealtime(int iStatType)
	{
        BatteryStatsProxy mStats = new BatteryStatsProxy(this);
        long whichRealtime = 0;
		long rawRealtime = SystemClock.elapsedRealtime() * 1000;
		if (iStatType == STATS_CUSTOM)
		{
			whichRealtime 	= mStats.computeBatteryRealtime(rawRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
			whichRealtime -= m_refBatteryRealtime;	
		}
		else
		{
			whichRealtime 	= mStats.computeBatteryRealtime(rawRealtime, iStatType) / 1000;
		}
		return whichRealtime;
	}
	
	/** 
	 * Dumps relevant data to an output file
	 * 
	 */
	void writeDumpToFile()
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref", "0"));
		
		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
    		Toast.makeText(this, "External Storage can not be written", Toast.LENGTH_SHORT).show();
		}
		try
    	{		
			// open file for writing
			File root = Environment.getExternalStorageDirectory();
		    if (root.canWrite())
		    {
		    	File dumpFile = new File(root, "BetterBatteryStats.txt");
		        FileWriter fw = new FileWriter(dumpFile);
		        BufferedWriter out = new BufferedWriter(fw);
			  
				// write header
		        out.write("===================\n");
				out.write("General Information\n");
				out.write("===================\n");
				PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
				out.write("BetterBatteryStats version: " + pinfo.versionName + "\n");
				out.write("Creation Date: " + DateUtils.now() + "\n");
				out.write("Statistic Type: (" + m_iStatType + ") " + statTypeToLabel(m_iStatType) + "\n");
				out.write("Since " + DateUtils.formatDuration(getBatteryRealtime(m_iStatType)) + "\n");
				out.write("VERSION.RELEASE: " + Build.VERSION.RELEASE+"\n");
				out.write("BRAND: "+Build.BRAND+"\n");
				out.write("DEVICE: "+Build.DEVICE+"\n");
				out.write("MANUFACTURER: "+Build.MANUFACTURER+"\n");
				out.write("MODEL: "+Build.MODEL+"\n");
				// write timing info
				out.write("===========\n");
				out.write("Other Usage\n");
				out.write("===========\n");
				dumpList(getOtherUsageStatList(bFilterStats, m_iStatType), out);
				// write wakelock info
				out.write("=========\n");
				out.write("Wakelocks\n");
				out.write("=========\n");
				dumpList(getWakelockStatList(bFilterStats, m_iStatType, iPctType), out);
				// write kernel wakelock info
				out.write("================\n");
				out.write("Kernel Wakelocks\n");
				out.write("================\n");
				dumpList(getKernelWakelockStatList(bFilterStats, m_iStatType, iPctType), out);
				// write process info
				out.write("=========\n");
				out.write("Processes\n");
				out.write("=========\n");
				dumpList(getProcessStatList(bFilterStats, m_iStatType), out);
				// write network info
				//out.write("=======\n");
				//out.write("Network\n");
				//out.write("=======\n");
				//dumpList(getNetworkUsageStatList(bFilterStats, m_iStatType), out);
		
				out.write("========\n");
				out.write("Services\n");
				out.write("========\n");
				out.write("Active since: The time when the service was first made active, either by someone starting or binding to it.\n");
				out.write("Last activity: The time when there was last activity in the service (either explicit requests to start it or clients binding to it)\n");
				out.write("See http://developer.android.com/reference/android/app/ActivityManager.RunningServiceInfo.html\n");
				ActivityManager am = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
				List<ActivityManager.RunningServiceInfo> rs = am.getRunningServices(50);
				         
				for (int i=0; i < rs.size(); i++) {
				  ActivityManager.RunningServiceInfo  rsi = rs.get(i);
				  out.write(rsi.process + " (" + rsi.service.getClassName() + ")\n");
				  out.write("  Active since: " + DateUtils.formatDuration(rsi.activeSince) + "\n");
				  out.write("  Last activity: " + DateUtils.formatDuration(rsi.lastActivityTime) + "\n");
				  out.write("  Crash count:" + rsi.crashCount + "\n");
				}
				
				// see http://androidsnippets.com/show-all-running-services
				// close file
				out.close();
		    }
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    		Toast.makeText(this, "an error occured while dumping the statistics", Toast.LENGTH_SHORT).show();
    	}		
	}
	
	/**
	 * Dump the elements on one list
	 * @param myList a list of StatElement
	 */
	private void dumpList(List<StatElement> myList, BufferedWriter out) throws IOException
	{
		if (myList != null)
		{
			for (int i = 0; i < myList.size(); i++)
			{
				out.write(myList.get(i).getDumpData(this) + "\n");
				
				
			}
		}
	}
	
	/**
	 * Saves all data to a point in time defined by user
	 * This data will be used in a custom "since..." stat type
	 */
	void setCustomReference()
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref", "0"));
		
		try
    	{			
			m_refOther 				= null;
			m_refWakelocks 			= null;
			m_refKernelWakelocks 	= null;
			m_refProcesses 			= null;
			m_refNetwork 			= null;			
    	
			// create a copy of each list for further reference
			m_refOther 				= getOtherUsageStatList(bFilterStats, BatteryStatsTypes.STATS_CURRENT);
			m_refWakelocks 			= getWakelockStatList(bFilterStats, BatteryStatsTypes.STATS_CURRENT, iPctType);
			m_refKernelWakelocks 	= getKernelWakelockStatList(bFilterStats, BatteryStatsTypes.STATS_CURRENT, iPctType);

			m_refProcesses 			= getProcessStatList(bFilterStats, BatteryStatsTypes.STATS_CURRENT);
			m_refNetwork 			= getNetworkUsageStatList(bFilterStats, BatteryStatsTypes.STATS_CURRENT);
			m_refBatteryRealtime 	= getBatteryRealtime(BatteryStatsTypes.STATS_CURRENT);
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    		Toast.makeText(this, "an error occured while creating the custom reference", Toast.LENGTH_SHORT).show();
    		m_refOther 				= null;
			m_refWakelocks 			= null;
			m_refKernelWakelocks 	= null;
			m_refProcesses 			= null;
			m_refNetwork 			= null;
			
			m_refBatteryRealtime 	= 0;
    	}			
	}
	
	/**
	 * translate the spinner position (see arrays.xml) to the stat type
	 * @param position the spinner position
	 * @return the stat type
	 */
	private int statTypeFromPosition(int position)
	{
		int iRet = 0;
		switch (position)
		{
			case 0:
				iRet = 0;
				break;
			case 1:
				iRet = 3;
				break;
			case 2:
				iRet = 4;
				break;
				
		}
		return iRet;
	}
	
	/**
	 * translate the stat type to the spinner position (see arrays.xml)
	 * @param iStatType the stat type
	 * @return the spinner position
	 */
	private int positionFromStatType(int iStatType)
	{
		int iRet = 0;
		switch (iStatType)
		{
			case 0:
				iRet = 0;
				break;
			case 1:
				iRet = 1;
				break;
			case 2:
				iRet = 2;
				break;
				
		}
		return iRet;
	}

	/**
	 * translate the stat type (see arrays.xml) to the corresponding label
	 * @param position the spinner position
	 * @return the stat type
	 */
	private String statTypeToLabel(int statType)
	{
		String strRet = "";
		switch (statType)
		{
			case 0:
				strRet = "Since Charged";
				break;
			case 1:
				strRet = "Since Unplugged";
				break;
			case 2:
				strRet = "Custom Reference";
				break;	
		}
		return strRet;
	}

	
}
