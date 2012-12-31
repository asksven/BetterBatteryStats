/*
 * Copyright (C) 2011-2012 asksven
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
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.asksven.android.common.AppRater;
import com.asksven.android.common.CommonLogSettings;
import com.asksven.android.common.ReadmeActivity;
import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.privateapiproxies.BatteryInfoUnavailableException;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.adapters.ReferencesAdapter;
import com.asksven.betterbatterystats.adapters.StatsAdapter;
import com.asksven.betterbatterystats.data.GoogleAnalytics;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceDBHelper;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.services.EventWatcherService;
import com.asksven.betterbatterystats.services.WriteCurrentReferenceService;
import com.asksven.betterbatterystats.services.WriteScreenOffReferenceService;


public class StatsActivity extends ListActivity implements AdapterView.OnItemSelectedListener, OnSharedPreferenceChangeListener
{    
	public static String STAT 				= "STAT";
	public static String STAT_TYPE_FROM		= "STAT_TYPE_FROM";
	public static String STAT_TYPE_TO		= "STAT_TYPE_TO";
	public static String FROM_NOTIFICATION 	= "FROM_NOTIFICATION";
	
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
	private StatsAdapter m_listViewAdapter;
	private ReferencesAdapter m_spinnerFromAdapter;
	private ReferencesAdapter m_spinnerToAdapter;
	/**
	 * The Type of Stat to be displayed (default is "Since charged")
	 */
//	private int m_iStatType = 0; 
	private String m_refFromName = "";
	private String m_refToName = Reference.CURRENT_REF_FILENAME;
	/**
	 * The Stat to be displayed (default is "Process")
	 */
	private int m_iStat = 0; 

	/**
	 * the selected sorting
	 */
	private int m_iSorting = 0;
	
	private BroadcastReceiver m_referenceSavedReceiver = null;
	
	/**
	 * @see android.app.Activity#onCreate(Bundle@SuppressWarnings("rawtypes")
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);	
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// set debugging
		if (sharedPrefs.getBoolean("debug_logging", false))
		{
			LogSettings.DEBUG=true;
			CommonLogSettings.DEBUG=true;
		}
		else
		{
			LogSettings.DEBUG=false;
			CommonLogSettings.DEBUG=false;
		}

		// Check if the stats are accessible and warn if not
		BatteryStatsProxy stats = BatteryStatsProxy.getInstance(this);
				
		if (stats.initFailed())
		{
			Toast.makeText(this, "The 'batteryinfo' service could not be accessed. If this error persists after a reboot please contact the dev and provide your ROM/Kernel versions.", Toast.LENGTH_SHORT).show();			
		}
		
		///////////////////////////////////////////////
		// check if we have a new release
		///////////////////////////////////////////////
		// if yes show release notes
		// migrate the default stat and stat type preferences for pre-1.9 releases if not migrated already
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
    		//////////////////////////////////////////////////////////////////////////
    		// Migration from 1.11.x to 1.12.x : preferences for default stat types
    		//////////////////////////////////////////////////////////////////////////
    		if (strLastRelease.startsWith("1.11"))
    		{
    			// 1.12 changes the way stat type prefs are stored:
    			// 1.11 used to have index number for constants
    			// 1.12 has the reference name
    			// prefs to be migrated are:
    			// default_stat_type, small_widget_default_stat_type, widget_fallback_stat_type, large_widget_default_stat_type
    			// all are migrated to "unplugged"
    	        SharedPreferences.Editor editor = sharedPrefs.edit();
    	        editor.putString("default_stat_type", Reference.UNPLUGGED_REF_FILENAME);
    	        editor.putString("small_widget_default_stat_type", Reference.UNPLUGGED_REF_FILENAME);
    	        editor.putString("widget_fallback_stat_type", Reference.UNPLUGGED_REF_FILENAME);
    	        editor.putString("large_widget_default_stat_type", Reference.UNPLUGGED_REF_FILENAME);

    	        editor.commit();
    			
    			
    		}
    		// show the readme
	    	Intent intentReleaseNotes = new Intent(this, ReadmeActivity.class);
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
			else
			{
		    	// show "rate" dialog
		    	// for testing: AppRater.showRateDialog(this, null);
		    	AppRater.app_launched(this);

			}
    	}
    	
		///////////////////////////////////////////////
    	// retrieve default selections for spinners
    	// if none were passed
		///////////////////////////////////////////////
    	
    	m_iStat		= Integer.valueOf(sharedPrefs.getString("default_stat", "0"));
		m_refFromName	= sharedPrefs.getString("default_stat_type", Reference.UNPLUGGED_REF_FILENAME);

		if (!ReferenceStore.hasReferenceByName(m_refFromName, this))
		{
			if (sharedPrefs.getBoolean("fallback_to_since_boot", false))
			{
				m_refFromName = Reference.BOOT_REF_FILENAME;
	    		Toast.makeText(this, "Fallback to 'Since Boot'", Toast.LENGTH_SHORT).show();
			}
		}
		
		try
		{
			// recover any saved state
			if ( (savedInstanceState != null) && (!savedInstanceState.isEmpty()))
			{
				m_iStat 				= (Integer) savedInstanceState.getSerializable("stat");
				m_refFromName 			= (String) savedInstanceState.getSerializable("stattypeFrom");
				m_refToName 			= (String) savedInstanceState.getSerializable("stattypeTo");
	 			
			}			
		}
		catch (Exception e)
		{
			m_iStat		= Integer.valueOf(sharedPrefs.getString("default_stat", "0"));
			m_refFromName	= sharedPrefs.getString("default_stat_type", Reference.UNPLUGGED_REF_FILENAME);
			
    		Log.e(TAG, "Exception: " + e.getMessage());
    		DataStorage.LogToFile(LOGFILE, "Exception in onCreate restoring Bundle");
    		DataStorage.LogToFile(LOGFILE, e.getMessage());
    		DataStorage.LogToFile(LOGFILE, e.getStackTrace());
    		
    		Toast.makeText(this, "An error occured while recovering the previous state", Toast.LENGTH_SHORT).show();
		}

		// Handle the case the Activity was called from an intent with paramaters
		Bundle extras = getIntent().getExtras();
		if (extras != null)
		{
			// Override if some values were passed to the intent
			m_iStat = extras.getInt(StatsActivity.STAT);
			m_refFromName = extras.getString(StatsActivity.STAT_TYPE_FROM);
			m_refToName = extras.getString(StatsActivity.STAT_TYPE_TO);
			boolean bCalledFromNotification = extras.getBoolean(StatsActivity.FROM_NOTIFICATION, false);
			
			// Clear the notifications that was clicked to call the activity
			if (bCalledFromNotification)
			{
		    	NotificationManager nM = (NotificationManager)getSystemService(Service.NOTIFICATION_SERVICE);
		    	nM.cancel(EventWatcherService.NOTFICATION_ID);
			}
		}

		// Display the reference of the stat
        TextView tvSince = (TextView) findViewById(R.id.TextViewSince);
        if (tvSince != null)
        {
    		Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, this);
    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, this);

            long sinceMs = StatsProvider.getInstance(this).getSince(myReferenceFrom, myReferenceTo);
            if (sinceMs != -1)
            {
    	        String sinceText = "Since " + DateUtils.formatDuration(sinceMs);
    			boolean bShowBatteryLevels = sharedPrefs.getBoolean("show_batt", true);
    	        if (bShowBatteryLevels)
    	        {
    	        		sinceText += " " + StatsProvider.getInstance(this).getBatteryLevelFromTo(myReferenceFrom, myReferenceTo);
    	        }
    	        tvSince.setText(sinceText);
    	    	Log.i(TAG, "Since " + sinceText);
            }
            else
            {
    	        tvSince.setText("Since: n/a ");
    	    	Log.i(TAG, "Since: n/a ");
            	
            }
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
		m_spinnerFromAdapter = new ReferencesAdapter(this, android.R.layout.simple_spinner_item);
		m_spinnerFromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerStatType.setAdapter(m_spinnerFromAdapter);

		try
		{
			this.setListViewAdapter();
		}
		catch (BatteryInfoUnavailableException e)
		{
//			Log.e(TAG, e.getMessage(), e.fillInStackTrace());
			Log.e(TAG, "Exception: "+Log.getStackTraceString(e));
			Toast.makeText(this,
					"BatteryInfo Service could not be contacted.",
					Toast.LENGTH_LONG).show();
			
		}
		catch (Exception e)
		{
			//Log.e(TAG, e.getMessage(), e.fillInStackTrace());
			Log.e(TAG, "Exception: "+Log.getStackTraceString(e));
			Toast.makeText(this,
					"An unhandled error occured. Please check your logcat",
					Toast.LENGTH_LONG).show();
		}
		// setSelection MUST be called after setAdapter
		spinnerStatType.setSelection(m_spinnerFromAdapter.getPosition(m_refFromName));
		spinnerStatType.setOnItemSelectedListener(this);
		
		///////////////////////////////////////////////
		// Spinner for Selecting the end sample
		///////////////////////////////////////////////
		Spinner spinnerStatSampleEnd = (Spinner) findViewById(R.id.spinnerStatSampleEnd);
		m_spinnerToAdapter = new ReferencesAdapter(this, android.R.layout.simple_spinner_item);
		m_spinnerToAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		
		boolean bShowSpinner = sharedPrefs.getBoolean("show_to_ref", false);
        if (bShowSpinner)
        {
        	spinnerStatSampleEnd.setVisibility(View.VISIBLE);
    		spinnerStatSampleEnd.setAdapter(m_spinnerToAdapter);
    		// setSelection must be called after setAdapter
    		if ((m_refToName != null) && !m_refToName.equals("") )
    		{
    			int pos = m_spinnerToAdapter.getPosition(m_refToName);
    			spinnerStatSampleEnd.setSelection(pos);
    			
    		}
    		else
    		{
    			spinnerStatSampleEnd.setSelection(m_spinnerToAdapter.getPosition(Reference.CURRENT_REF_FILENAME));
    		}

        }
        else
        {
        	spinnerStatSampleEnd.setVisibility(View.GONE);
    		spinnerStatSampleEnd.setAdapter(m_spinnerToAdapter);
    		// setSelection must be called after setAdapter
    		spinnerStatSampleEnd.setSelection(m_spinnerToAdapter.getPosition(Reference.CURRENT_REF_FILENAME));

        }
		
			

		spinnerStatSampleEnd.setOnItemSelectedListener(this);

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
		GoogleAnalytics.getInstance(this).trackStats(this, GoogleAnalytics.ACTIVITY_STATS, m_iStat, m_refFromName, m_refToName, m_iSorting);

        // Set up a listener whenever a key changes
    	PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
		
    	// log reference store
    	ReferenceStore.logReferences(this);
		
	}
    
	/* Request updates at startup */
	@Override
	protected void onResume()
	{
		super.onResume();

		
		// register the broadcast receiver
		IntentFilter intentFilter = new IntentFilter(ReferenceStore.REF_UPDATED);
        m_referenceSavedReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                //extract our message from intent
                String refName = intent.getStringExtra(Reference.EXTRA_REF_NAME);
                //log our message value
                Log.i("Reference was updated;", refName);
                
                // reload the spinners to make sure all refs are in the right sequence when current gets refreshed
                if (refName.equals(Reference.CURRENT_REF_FILENAME))
                {
                	refreshSpinners();
                }
            }
        };
        
        //registering our receiver
        this.registerReceiver(m_referenceSavedReceiver, intentFilter);
        
		// the service is always started as it handles the widget updates too
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean serviceShouldBeRunning = sharedPrefs.getBoolean("ref_for_screen_off", false);
		if (serviceShouldBeRunning)
		{
			if (!EventWatcherService.isServiceRunning(this))
			{
				Intent i = new Intent(this, EventWatcherService.class);
				this.startService(i);
			}    				
		}
		
		// make sure to create a valid "current" stat if none exists
		// or if prefs re set to auto refresh
		boolean bAutoRefresh = sharedPrefs.getBoolean("auto_refresh", true);

		if ((bAutoRefresh) || (!ReferenceStore.hasReferenceByName(Reference.CURRENT_REF_FILENAME, this)))
		{
			Intent serviceIntent = new Intent(this, WriteCurrentReferenceService.class);
			this.startService(serviceIntent);
			doRefresh(true);

		}
		else
		{	
			refreshSpinners();
			doRefresh(false);
			
		}

		

		
		
	}

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause()
	{
		super.onPause();
		
		// unregister boradcast receiver for saved references
		this.unregisterReceiver(this.m_referenceSavedReceiver);
		
		// make sure to dispose any running dialog
		if (m_progressDialog != null)
		{
			m_progressDialog.dismiss();
			m_progressDialog = null;
		}
//		this.unregisterReceiver(m_batteryHandler);

	}

    /**
     * Save state, the application is going to get moved out of memory
     * @see http://stackoverflow.com/questions/151777/how-do-i-save-an-android-applications-state
     */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
    {
    	super.onSaveInstanceState(savedInstanceState);
        
    	savedInstanceState.putSerializable("stattypeFrom", m_refFromName);
    	savedInstanceState.putSerializable("stattypeTo", m_refToName); 

    	savedInstanceState.putSerializable("stat", m_iStat);
		
    	//StatsProvider.getInstance(this).writeToBundle(savedInstanceState);
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

	        case R.id.rawalarms:  
	        	Intent intentAlarms = new Intent(this, AlarmsActivity.class);
	        	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_ALARMS);
	            this.startActivity(intentAlarms);
	        	break;	

	        case R.id.rawkwl:  
	        	Intent intentKwl = new Intent(this, KernelWakelocksActivity.class);
	        	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_KWL);
	            this.startActivity(intentKwl);
	        	break;	

	        case R.id.rawnetwork:  
	        	Intent intentNet = new Intent(this, NetworkStatsActivity.class);
	        	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_NET);
	            this.startActivity(intentNet);
	        	break;	

	        case R.id.rawcpustates:  
	        	Intent intentCpu = new Intent(this, CpuStatesActivity.class);
	        	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_CPU);
	            this.startActivity(intentCpu);
	        	break;	

	        case R.id.refresh:
            	// Refresh
	        	doRefresh(true);
            	break;
            case R.id.dump:
            	// Dump to File
            	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTION_DUMP);
            	new WriteDumpFile().execute("");
            	//this.writeDumpToFile();
            	break;
            case R.id.logcat:
            	// Dump to File
            	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTION_DUMP);
            	new WriteLogcatFile().execute("");
            	break;
            case R.id.dmesg:
            	// Dump to File
            	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTION_DUMP);
            	new WriteDmesgFile().execute("");
            	break;
	
            case R.id.custom_ref:
            	// Set custom reference
            	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTION_SET_CUSTOM_REF);
            	new SetCustomRef().execute(this);
            	break;
            case R.id.by_time_desc:
            	// Enable "count" option
            	m_iSorting = 0;            	
            	doRefresh(false);
            	break;	
            case R.id.by_count_desc:
            	// Enable "count" option
            	m_iSorting = 1;            	
            	doRefresh(false);
            	break;	

//            case R.id.test:
//    			Intent serviceIntent = new Intent(this, WriteUnpluggedReferenceService.class);
//    			this.startService(serviceIntent);
//    			break;	

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
            	Intent intentReleaseNotes = new Intent(this, ReadmeActivity.class);
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
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		boolean bChanged = false;
		
		// id is in the order of the spinners, 0 is stat, 1 is stat_type
		if (parent == (Spinner) findViewById(R.id.spinnerStatType))
		{
			// detect if something changed
			String newStat = (String) ( (ReferencesAdapter) parent.getAdapter()).getItemName(position);
			if ( !m_refFromName.equals(newStat) )
			{
				m_refFromName = newStat;
				bChanged = true;
				// we need to update the second spinner
				m_spinnerToAdapter.filter(newStat, this);
				m_spinnerToAdapter.notifyDataSetChanged();
				
				// select the right element
				Spinner spinnerStatSampleEnd = (Spinner) findViewById(R.id.spinnerStatSampleEnd);
				if (spinnerStatSampleEnd.isShown())
				{
					spinnerStatSampleEnd.setSelection(m_spinnerToAdapter.getPosition(m_refToName));
				}
				else
				{
					spinnerStatSampleEnd.setSelection(m_spinnerToAdapter.getPosition(Reference.CURRENT_REF_FILENAME));
				}

			}
			else
			{
				return;
			}

		}
		else if (parent == (Spinner) findViewById(R.id.spinnerStatSampleEnd))
		{
			String newStat = (String) ( (ReferencesAdapter) parent.getAdapter()).getItemName(position);
			if ( !m_refToName.equals(newStat) )
			{
				m_refToName = newStat;
				bChanged = true;
			}
			else
			{
				return;
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

			// inform the user when he tries to use functions requiring root and he doesn't have root enabled
			boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);
			
			if (!rootEnabled)
			{
				if ((m_iStat == 4) || (m_iStat == 3)) 
				{
					Toast.makeText(this,
							"This function requires root access. Check \"Advanced\" preferences",
							Toast.LENGTH_LONG).show();
				}
			}

		}
		else
		{
    		Log.e(TAG, "ProcessStatsActivity.onItemSelected error. ID could not be resolved");
    		Toast.makeText(this, "Error: could not resolve what changed", Toast.LENGTH_SHORT).show();

		}

    	Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, this);
		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, this);

        TextView tvSince = (TextView) findViewById(R.id.TextViewSince);
//        long sinceMs = getSince();
        long sinceMs = StatsProvider.getInstance(this).getSince(myReferenceFrom, myReferenceTo);

        if (sinceMs != -1)
        {
	        String sinceText = "Since " + DateUtils.formatDuration(sinceMs);
			boolean bShowBatteryLevels = sharedPrefs.getBoolean("show_batt", true);
	        if (bShowBatteryLevels)
	        {

        		sinceText += " " + StatsProvider.getInstance(this).getBatteryLevelFromTo(myReferenceFrom, myReferenceTo);
	        }
	        tvSince.setText(sinceText);
	    	Log.i(TAG, "Since " + sinceText);
        }
        else
        {
	        tvSince.setText("Since: n/a ");
	    	Log.i(TAG, "Since: n/a ");
        	
        }
		// @todo fix this: this method is called twice
		//m_listViewAdapter.notifyDataSetChanged();
        if (bChanged)
        {
        	GoogleAnalytics.getInstance(this).trackStats(this, GoogleAnalytics.ACTIVITY_STATS, m_iStat, m_refFromName, m_refToName, m_iSorting);
        	//new LoadStatData().execute(this);
        	// as the source changed fetch the data
        	doRefresh(false);
        }
	}

	public void onNothingSelected(AdapterView<?> parent)
	{
		// default
		m_refFromName = "";
		m_refToName = "";
		//m_listViewAdapter.notifyDataSetChanged();
		
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
    	if (key.equals("show_to_ref"))
    	{
    		Spinner spinnerStatSampleEnd = (Spinner) findViewById(R.id.spinnerStatSampleEnd);	
    		boolean bShowSpinner = prefs.getBoolean("show_to_ref", false);
            if (bShowSpinner)
            {
            	spinnerStatSampleEnd.setVisibility(View.VISIBLE);
            }
            else
            {
            	spinnerStatSampleEnd.setVisibility(View.GONE);
            }
    	}
    }

	private void refreshSpinners()
	{
		// reload the spinners to make sure all refs are in the right sequence
		m_spinnerFromAdapter.refresh(this);
		m_spinnerToAdapter.filter(m_refFromName, this);
		// after we reloaded the spinners we need to reset the selections
		Spinner spinnerStatTypeFrom = (Spinner) findViewById(R.id.spinnerStatType);
		Spinner spinnerStatTypeTo = (Spinner) findViewById(R.id.spinnerStatSampleEnd);
		spinnerStatTypeFrom.setSelection(m_spinnerFromAdapter.getPosition(m_refFromName));
		if (spinnerStatTypeTo.isShown())
		{
			spinnerStatTypeTo.setSelection(m_spinnerFromAdapter.getPosition(m_refToName));
		}
		else
		{
			spinnerStatTypeTo.setSelection(m_spinnerFromAdapter.getPosition(Reference.CURRENT_REF_FILENAME));
		}
	}
	
    /**
	 * In order to refresh the ListView we need to re-create the Adapter
	 * (should be the case but notifyDataSetChanged doesn't work so
	 * we recreate and set a new one)
	 */
	private void setListViewAdapter() throws Exception
	{
		// make sure we only instanciate when the reference does not exist
		if (m_listViewAdapter == null)
		{
			m_listViewAdapter = new StatsAdapter(this, 
					StatsProvider.getInstance(this).getStatList(m_iStat, m_refFromName, m_iSorting, m_refToName));
		
			setListAdapter(m_listViewAdapter);
		}
	}

	private void doRefresh(boolean updateCurrent)
		{
	
			BatteryStatsProxy.getInstance(this).invalidate();
			new LoadStatData().execute(updateCurrent);	
		}

	private class WriteDumpFile extends AsyncTask
	{
		@Override
	    protected Object doInBackground(Object... params)
	    {
        	Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, StatsActivity.this);
    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, StatsActivity.this);

			StatsProvider.getInstance(StatsActivity.this).writeDumpToFile(myReferenceFrom, m_iSorting, myReferenceTo);
	    	return true;
	    }

		@Override
		protected void onPostExecute(Object o)
	    {
			super.onPostExecute(o);
	        // update hourglass
	    }
	 }

	private class WriteLogcatFile extends AsyncTask
	{
		@Override
	    protected Object doInBackground(Object... params)
	    {
			StatsProvider.getInstance(StatsActivity.this).writeLogcatToFile();
	    	return true;
	    }

		@Override
		protected void onPostExecute(Object o)
	    {
			super.onPostExecute(o);
	        // update hourglass
	    }
	 }

	private class WriteDmesgFile extends AsyncTask
	{
		@Override
	    protected Object doInBackground(Object... params)
	    {
			StatsProvider.getInstance(StatsActivity.this).writeDmesgToFile();
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
	private class LoadStatData extends AsyncTask<Boolean, Integer, StatsAdapter>
	{
		private Exception m_exception = null;
		@Override
	    protected StatsAdapter doInBackground(Boolean... refresh)
	    {
			// do we need to refresh current
			if (refresh[0])
			{
				// make sure to create a valid "current" stat
				StatsProvider.getInstance(StatsActivity.this).setCurrentReference(m_iSorting);		
			}
			//super.doInBackground(params);
			m_listViewAdapter = null;
			try
			{
				m_listViewAdapter = new StatsAdapter(
						StatsActivity.this,
						StatsProvider.getInstance(StatsActivity.this).getStatList(m_iStat, m_refFromName, m_iSorting, m_refToName));
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
	    			Toast.makeText(StatsActivity.this,
	    					"BatteryInfo Service could not be contacted.",
	    					Toast.LENGTH_LONG).show();

	    		}
	    		else
	    		{
	    			Toast.makeText(StatsActivity.this,
	    					"An unknown error occured while retrieving stats.",
	    					Toast.LENGTH_LONG).show();
	    			
	    		}
	    	}
	        TextView tvSince = (TextView) findViewById(R.id.TextViewSince);
    		Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, StatsActivity.this);
    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, StatsActivity.this);

	        long sinceMs = StatsProvider.getInstance(StatsActivity.this).getSince(myReferenceFrom, myReferenceTo);

	        if (sinceMs != -1)
	        {
		        String sinceText = "Since " + DateUtils.formatDuration(sinceMs);
		        
				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(StatsActivity.this);
				boolean bShowBatteryLevels = sharedPrefs.getBoolean("show_batt", true);
		        if (bShowBatteryLevels)
		        {
		        		sinceText += " " + StatsProvider.getInstance(StatsActivity.this).getBatteryLevelFromTo(myReferenceFrom, myReferenceTo);
		        }
		        tvSince.setText(sinceText);
		    	Log.i(TAG, "Since " + sinceText);
	        }
	        else
	        {
		        tvSince.setText("Since: n/a ");
		    	Log.i(TAG, "Since: n/a ");
	        	
	        }

	    	StatsActivity.this.setListAdapter(o);
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
			    	m_progressDialog = new ProgressDialog(StatsActivity.this);
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
		
//		@Override
		protected void onPostExecute(Boolean b)
	    {
//			super.onPostExecute(b);
	        // update hourglass
			try
			{
		    	if (m_progressDialog != null)
		    	{
		    		m_progressDialog.dismiss(); // hide();
		    	
		    	}
			}
			catch (Exception e)
			{
				// nop
			}
			finally
			{
				m_progressDialog = null;
				m_spinnerFromAdapter.refresh(StatsActivity.this);
				m_spinnerToAdapter.refresh(StatsActivity.this);
			}
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
			    	m_progressDialog = new ProgressDialog(StatsActivity.this);
			    	m_progressDialog.setMessage("Saving...");
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
