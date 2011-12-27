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
import android.content.IntentFilter;
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
import com.asksven.android.common.kernelutils.Alarm;
import com.asksven.android.common.kernelutils.AlarmsDumpsys;
import com.asksven.android.common.kernelutils.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.Wakelocks;
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
import com.asksven.betterbatterystats.data.GoogleAnalytics;
import com.asksven.betterbatterystats.data.StatsProvider;

public class StatsActivity extends ListActivity implements AdapterView.OnItemSelectedListener
{    
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
	
	BatteryChangedHandler m_batteryHandler = null;
	/**
	 * @see android.app.Activity#onCreate(Bundle@SuppressWarnings("rawtypes")
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);				
		
		// register battery changed events
		m_batteryHandler = new BatteryChangedHandler();
        this.registerReceiver(m_batteryHandler, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		// Check if the stats are accessible and warn if not
		BatteryStatsProxy stats = new BatteryStatsProxy(this);
		
		if (stats.initFailed())
		{
			Toast.makeText(this, "The 'batteryinfo' service could not be accessed. Known reasons: MIUI settings allow to turn them off, making MIUI incompatible with android standards", Toast.LENGTH_SHORT).show();			
		}
		
		///////////////////////////////////////////////
		// check if we have a new release
		///////////////////////////////////////////////
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
    	else
    	{
    		// can't do this at the same time as the popup dialog would be masked by the readme
			///////////////////////////////////////////////
			// check if we have shown the opt-out from analytics
			///////////////////////////////////////////////
			boolean bWarningShown	= sharedPrefs.getBoolean("analytics_opt_out", false);
			boolean bAnalyticsEnabled = sharedPrefs.getBoolean("use_analytics", true);
			if (bAnalyticsEnabled && !bWarningShown)
			{
				// prepare the alert box
	            AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
	 
	            // set the message to display
	            alertbox.setMessage("BetterBatteryStats makes use of Google Analytics to collect usage statitics. If you disagree or do not want to participate you can opt-out by disabling \"Google Analytics\" in the \"Advanced Preferences\"");
	 
	            // add a neutral button to the alert box and assign a click listener
	            alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener()
	            {
	 
	                // click listener on the alert box
	                public void onClick(DialogInterface arg0, int arg1)
	                {
	        	        // opt out info was displayed
	            		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StatsActivity.this);
	        	        SharedPreferences.Editor editor = prefs.edit();
	        	        editor.putBoolean("analytics_opt_out", true);
	        	        editor.commit();
	
	                }
	            });
	 
	            // show it
	            alertbox.show();
	
			}
    	}
    	
		///////////////////////////////////////////////
    	// retrieve default selections for spinners
		///////////////////////////////////////////////
    	
    	m_iStat		= Integer.valueOf(sharedPrefs.getString("default_stat", "0"));
		m_iStatType	= Integer.valueOf(sharedPrefs.getString("default_stat_type", "0"));
		
		try
		{
			// recover any saved state
			if ( (savedInstanceState != null) && (!savedInstanceState.isEmpty()))
			{
//				StatsProvider.getInstance(this).restoreFromBundle(savedInstanceState);
				
				m_iStat 				= (Integer) savedInstanceState.getSerializable("stat");
				m_iStatType 			= (Integer) savedInstanceState.getSerializable("stattype");
	 			
			}
			
			// restore any available custom reference
			StatsProvider.getInstance(this).deserializeFromFile();
		}
		catch (Exception e)
		{
			m_iStat		= Integer.valueOf(sharedPrefs.getString("default_stat", "0"));
			m_iStatType	= Integer.valueOf(sharedPrefs.getString("default_stat_type", "0"));
			
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
        	tvSince.setText("Since " + DateUtils.formatDuration(StatsProvider.getInstance(this).getBatteryRealtime(m_iStatType)));
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
		
		///////////////////////////////////////////////
		// Spinner for Selecting the Stat type
		///////////////////////////////////////////////
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

		///////////////////////////////////////////////
		// sorting
		///////////////////////////////////////////////
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
		GoogleAnalytics.getInstance(this).trackStats(this, GoogleAnalytics.ACTIVITY_STATS, m_iStat, m_iStatType, m_iSorting); 

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
//		this.unregisterReceiver(m_batteryHandler);

	}

//	/**
//	 * Handle the "back" button to make sure the user wants to
//	 * quit the application and lose any custom ref 
//	 */
//	@Override 
//    public boolean onKeyDown(int keyCode, KeyEvent event)
//	{ 
//        // if "back" was pressed. If a custom ref was saved ask if app should
//		// still be closed
//        if (keyCode == KeyEvent.KEYCODE_BACK) 
//        { 
//        	// do we have a custom ref
//        	if (StatsProvider.getInstance(this).hasCustomRef())
//        	{
//        		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
//        		{
//        		    @Override
//        		    public void onClick(DialogInterface dialog, int which)
//        		    {
//        		        switch (which)
//        		        {
//        		        case DialogInterface.BUTTON_POSITIVE:
//        		            //Yes button clicked
//        		        	finish();
//        		            break;
//
//        		        case DialogInterface.BUTTON_NEGATIVE:
//        		            //No button clicked
//        		            break;
//        		        }
//        		    }
//        		};
//        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        		builder.setMessage("By closing the custom reference will be lost. Are you sure?").setPositiveButton("Yes", dialogClickListener)
//        		    .setNegativeButton("No", dialogClickListener).show();
//        		return true;
//        	}
//        	else
//        	{
//        		return super.onKeyDown(keyCode, event);
//        	}
//        } 
//        return super.onKeyDown(keyCode, event); 
//    } 	
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
		
    	//StatsProvider.getInstance(this).writeToBundle(savedInstanceState);
    }
        
	/**
	 * In order to refresh the ListView we need to re-create the Adapter
	 * (should be the case but notifyDataSetChanged doesn't work so
	 * we recreate and set a new one)
	 */
	private void setListViewAdapter()
	{
		// make sure we only instanciate when the reference does not exist
		if (m_listViewAdapter == null)
		{
			m_listViewAdapter = new StatsAdapter(this, 
					StatsProvider.getInstance(this).getStatList(m_iStat, m_iStatType, m_iSorting));
		
			setListAdapter(m_listViewAdapter);
		}
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
	        	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_PREFERENCES);
	            this.startActivity(intentPrefs);
	        	break;	

	        case R.id.graph:  
	        	Intent intentGraph = new Intent(this, BatteryGraphActivity.class);
	        	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_BATTERY_GRAPH);
	            this.startActivity(intentGraph);
	        	break;	

	        case R.id.alarms:  
	        	Intent intentAlarms = new Intent(this, AlarmsActivity.class);
	        	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_ALARMS);
	            this.startActivity(intentAlarms);
	        	break;	

	        case R.id.refresh:
            	// Refresh
	        	doRefresh();
            	break;
            case R.id.dump:
            	// Dump to File
            	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTION_DUMP);
            	new WriteDumpFile().execute("");
            	//this.writeDumpToFile();
            	break;
            case R.id.custom_ref:
            	// Set custom reference
            	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTION_SET_CUSTOM_REF);
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

//            case R.id.test:
//            	// Test
//            	StatsProvider.getInstance(this).setReferenceSinceCharged(m_iSorting);
//            	StatsProvider.getInstance(this).setReferenceSinceUnplugged(m_iSorting);
//            	doRefresh();
//            	break;	

            case R.id.about:
            	// About
            	Intent intentAbout = new Intent(this, AboutActivity.class);
            	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_ABOUT);       	
                this.startActivity(intentAbout);
            	break;
            case R.id.getting_started:
            	// Help
            	Intent intentHelp = new Intent(this, HelpActivity.class);
            	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_HELP);
            	intentHelp.putExtra("filename", "help.html");
                this.startActivity(intentHelp);
            	break;	

            case R.id.howto:
            	// How To
            	Intent intentHowTo = new Intent(this, HelpActivity.class);
            	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_HOWTO);
            	intentHowTo.putExtra("filename", "howto.html");
                this.startActivity(intentHowTo);
            	break;	

            case R.id.releasenotes:
            	// Release notes
            	Intent intentReleaseNotes = new Intent(this, HelpActivity.class);
            	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_README);
            	intentReleaseNotes.putExtra("filename", "readme.html");
                this.startActivity(intentReleaseNotes);
            	break;	

//            case R.id.test:
//            	// Test something
//            	AlarmsDumpsys.getAlarms();
//            	break;	

        }  
        return false;  
    }    
	/**
	 * Take the change of selection from the spinners into account and refresh the ListView
	 * with the right data
	 */
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
	{
		// this method is fired even if nothing has changed so we nee to find that out
		boolean bChanged = false;
		
		// id is in the order of the spinners, 0 is stat, 1 is stat_type
		if (parent == (Spinner) findViewById(R.id.spinnerStatType))
		{
			// The Spinner does not show all available stats so it must be translated
			int iNewStatType = statTypeFromPosition(position);
			
			// detect if something changed
			if (m_iStatType != iNewStatType)
			{
				m_iStatType = iNewStatType;
				bChanged = true;
			}
			else
			{
				return;
			}
	
			// warn the user if custom ref was chosen without having selected a ref first
			if ( (m_iStatType == StatsProvider.STATS_CUSTOM) && (!StatsProvider.getInstance(this).hasCustomRef()))
			{
				Toast.makeText(this, "Warning: there is no custom reference set.", Toast.LENGTH_SHORT).show();
			}
			// warn the user if custom ref was chosen without a ref being present yet
			if ( (m_iStat == 3)
					&& (m_iStatType == StatsProvider.STATS_CHARGED) 
					&& (!StatsProvider.getInstance(this).hasSinceChargedRef()))
			{
				Toast.makeText(this, "Warning: there is no reference for kernel wakelocks since charged yet.", Toast.LENGTH_SHORT).show();
			}

			if ( (m_iStat == 3)
					&& (m_iStatType == StatsProvider.STATS_UNPLUGGED) 
					&& (!StatsProvider.getInstance(this).hasSinceUnpluggedRef()))
			{
				Toast.makeText(this, "Warning: there is no reference for kernel wakelocks since unplugged yet.", Toast.LENGTH_SHORT).show();
			}

		}
		else if (parent == (Spinner) findViewById(R.id.spinnerStat))
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
			// check if Kernel Wakelocks: if so disable stat type
			if (false && ( m_iStat == 3)) // array.xml
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

//        if ( m_iStat != 3 )
//        {
        	tvSince.setText("Since " + DateUtils.formatDuration(
        			StatsProvider.getInstance(this).getBatteryRealtime(m_iStatType)));
        	Log.i(TAG, "Since " + DateUtils.formatDuration(
        			StatsProvider.getInstance(this).getBatteryRealtime(m_iStatType)));
//        }
//        else
//        {
//        	tvSince.setText("Since boot " + DateUtils.formatDuration(timeSinceBoot));
//        }
		
		// @todo fix this: this method is called twice
		//m_listViewAdapter.notifyDataSetChanged();
        if (bChanged)
        {
        	GoogleAnalytics.getInstance(this).trackStats(this, GoogleAnalytics.ACTIVITY_STATS, m_iStat, m_iStatType, m_iSorting);
        	new LoadStatData().execute(this);
        }
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
        tvSince.setText("Since " + DateUtils.formatDuration(
        		StatsProvider.getInstance(this).getBatteryRealtime(m_iStatType)));
    	
        FrameLayout myLayout = (FrameLayout) findViewById(R.id.FrameLayoutSince);
		if (sharedPrefs.getBoolean("hide_since", true))
        {
        	myLayout.setVisibility(View.GONE);
        }
		else
		{
			myLayout.setVisibility(View.VISIBLE);
		}
		
//    	this.setListViewAdapter();

	}
	private class WriteDumpFile extends AsyncTask
	{
		@Override
	    protected Object doInBackground(Object... params)
	    {
			StatsProvider.getInstance(StatsActivity.this).writeDumpToFile(m_iStat, m_iStatType, m_iSorting);
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
			m_listViewAdapter = new StatsAdapter(
					StatsActivity.this,
					StatsProvider.getInstance(StatsActivity.this).getStatList(m_iStat, m_iStatType, m_iSorting));
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
			StatsProvider.getInstance(StatsActivity.this).setCustomReference(m_iSorting);
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


	
}
