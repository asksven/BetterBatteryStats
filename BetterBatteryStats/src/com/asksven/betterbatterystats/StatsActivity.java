/*
 * Copyright (C) 2011-2014 asksven
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
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
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
import com.asksven.android.common.RootShell;
import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.SysUtils;
import com.asksven.android.common.privateapiproxies.BatteryInfoUnavailableException;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.adapters.ReferencesAdapter;
import com.asksven.betterbatterystats.adapters.StatsAdapter;
import com.asksven.betterbatterystats.data.GoogleAnalytics;
import com.asksven.betterbatterystats.data.Reading;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceDBHelper;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.services.EventWatcherService;
import com.asksven.betterbatterystats.services.WriteCurrentReferenceService;
import com.asksven.betterbatterystats.services.WriteCustomReferenceService;
import com.asksven.betterbatterystats.services.WriteScreenOffReferenceService;
import com.asksven.betterbatterystats.services.WriteUnpluggedReferenceService;
import com.asksven.betterbatterystats.services.WriteBootReferenceService;
import com.asksven.betterbatterystats.contrib.ObservableScrollView;

public class StatsActivity extends ActionBarListActivity 
		implements AdapterView.OnItemSelectedListener, OnSharedPreferenceChangeListener, ObservableScrollView.Callbacks
{    
	public static String STAT 				= "STAT";
	public static String STAT_TYPE_FROM		= "STAT_TYPE_FROM";
	public static String STAT_TYPE_TO		= "STAT_TYPE_TO";
	public static String FROM_NOTIFICATION 	= "FROM_NOTIFICATION";
	
	private static final int STATE_ONSCREEN = 0;
    private static final int STATE_OFFSCREEN = 1;
    private static final int STATE_RETURNING = 2;
    
    private TextView mQuickReturnView;
    private View mPlaceholderView;
    private ObservableScrollView mObservableScrollView;
    private ScrollSettleHandler mScrollSettleHandler = new ScrollSettleHandler();
    private int mMinRawY = 0;
    private int mState = STATE_ONSCREEN;
    private int mQuickReturnHeight;
    private int mMaxScrollY;
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
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(new ColorDrawable(0xff2ecc71));
		
		Log.i(TAG, "OnCreated called");
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
		
		///////////////////////////////////////////////
		// check if we have a new release
		///////////////////////////////////////////////
		// if yes do some migration (if required) and show release notes
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
		
		// if root is available use it
		boolean hasRoot = sharedPrefs.getBoolean("root_features", false);
		boolean ignoreSystemApp = sharedPrefs.getBoolean("ignore_system_app", false);
		if (!hasRoot && (RootShell.getInstance().rooted()))
		{
	        SharedPreferences.Editor updater = sharedPrefs.edit();
	        updater.putBoolean("root_features", true);
	        updater.commit();
			hasRoot = sharedPrefs.getBoolean("root_features", false);
		}
			
		// show install as system app screen if root available but perms missing
		if (!ignoreSystemApp && hasRoot && !SysUtils.hasBatteryStatsPermission(this))
		{
        	Intent intentSystemApp = new Intent(this, SystemAppActivity.class);
        	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_PREFERENCES);
            this.startActivity(intentSystemApp);
		}
		
		// first start
		if (strLastRelease.equals("0"))
		{
			// show the initial run screen
			FirstLaunch.app_launched(this);
	        SharedPreferences.Editor updater = sharedPrefs.edit();
	        updater.putString("last_release", strCurrentRelease);
	        updater.commit();
		}
		else if (!strLastRelease.equals(strCurrentRelease))
    	{
	        // save the current release to properties so that the dialog won't be shown till next version
	        SharedPreferences.Editor updater = sharedPrefs.edit();
	        updater.putString("last_release", strCurrentRelease);
	        updater.commit();

			Toast.makeText(this, "Deleting and re-creating references", Toast.LENGTH_SHORT).show();
			ReferenceStore.deleteAllRefs(this);
			Intent i = new Intent(this, WriteBootReferenceService.class);
			this.startService(i);
			i = new Intent(this, WriteUnpluggedReferenceService.class);
			this.startService(i);
    			
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
		
		Log.i(TAG, "onCreate state from preferences: refFrom=" + m_refFromName + " refTo=" + m_refToName);
		try
		{
			// recover any saved state
			if ( (savedInstanceState != null) && (!savedInstanceState.isEmpty()))
			{
				m_iStat 				= (Integer) savedInstanceState.getSerializable("stat");
				m_refFromName 			= (String) savedInstanceState.getSerializable("stattypeFrom");
				m_refToName 			= (String) savedInstanceState.getSerializable("stattypeTo");
				Log.i(TAG, "onCreate retrieved saved state: refFrom=" + m_refFromName + " refTo=" + m_refToName);
	 			
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
		if ((extras != null) && !extras.isEmpty())
		{
			// Override if some values were passed to the intent
			if (extras.containsKey(StatsActivity.STAT)) m_iStat = extras.getInt(StatsActivity.STAT);
			if (extras.containsKey(StatsActivity.STAT_TYPE_FROM)) m_refFromName = extras.getString(StatsActivity.STAT_TYPE_FROM);
			if (extras.containsKey(StatsActivity.STAT_TYPE_TO)) m_refToName = extras.getString(StatsActivity.STAT_TYPE_TO);
			
			Log.i(TAG, "onCreate state from extra: refFrom=" + m_refFromName + " refTo=" + m_refToName);
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
    	        String sinceText = DateUtils.formatDurationShort(sinceMs);
    			boolean bShowBatteryLevels = sharedPrefs.getBoolean("show_batt", true);
    	        if (bShowBatteryLevels)
    	        {
    	        		sinceText += " " + StatsProvider.getInstance(this).getBatteryLevelFromTo(myReferenceFrom, myReferenceTo, true);
    	        }
    	        tvSince.setText(sinceText);
    	    	Log.i(TAG, "Since " + sinceText);
            }
            else
            {
    	        tvSince.setText("n/a");
    	    	Log.i(TAG, "Since: n/a ");
            	
            }
        }
        
		// Spinner for selecting the stat
		Spinner spinnerStat = (Spinner) findViewById(R.id.spinnerStat);
		
		ArrayAdapter spinnerStatAdapter = ArrayAdapter.createFromResource(
	            this, R.array.stats, R.layout.bbs_spinner_layout); //android.R.layout.simple_spinner_item);
		spinnerStatAdapter.setDropDownViewResource(R.layout.bbs_spinner_dropdown_item); // android.R.layout.simple_spinner_dropdown_item);
	    
		spinnerStat.setAdapter(spinnerStatAdapter);
		// setSelection MUST be called after setAdapter
		spinnerStat.setSelection(m_iStat);
		spinnerStat.setOnItemSelectedListener(this);
		
		///////////////////////////////////////////////
		// Spinner for Selecting the Stat type
		///////////////////////////////////////////////
		Spinner spinnerStatType = (Spinner) findViewById(R.id.spinnerStatType);
		m_spinnerFromAdapter = new ReferencesAdapter(this, R.layout.bbs_spinner_layout); //android.R.layout.simple_spinner_item);
		m_spinnerFromAdapter.setDropDownViewResource(R.layout.bbs_spinner_dropdown_item); //android.R.layout.simple_spinner_dropdown_item);
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
		m_spinnerToAdapter = new ReferencesAdapter(this, R.layout.bbs_spinner_layout); //android.R.layout.simple_spinner_item);
		m_spinnerToAdapter.setDropDownViewResource(R.layout.bbs_spinner_dropdown_item); //android.R.layout.simple_spinner_dropdown_item);

		
		boolean bShowSpinner = sharedPrefs.getBoolean("show_to_ref", true);
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
    	
    	Log.i(TAG, "onCreate final state: refFrom=" + m_refFromName + " refTo=" + m_refToName);
    	Log.i(TAG, "OnCreated end");
		
	}
    
	/* Request updates at startup */
	@Override
	protected void onResume()
	{
		super.onResume();
		Log.i(TAG, "OnResume called");
		
		Log.i(TAG, "onResume references state: refFrom=" + m_refFromName + " refTo=" + m_refToName);
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
                Log.i(TAG, "Received broadcast, reference was updated:" + refName);
                
                // reload the spinners to make sure all refs are in the right sequence when current gets refreshed
//                if (refName.equals(Reference.CURRENT_REF_FILENAME))
//                {
                	refreshSpinners();
//                }
            }
        };
        
        //registering our receiver
        this.registerReceiver(m_referenceSavedReceiver, intentFilter);
        
		// the service is always started as it handles the widget updates too
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean serviceShouldBeRunning = sharedPrefs.getBoolean("ref_for_screen_off", false);
		
		// we need to run the service also if we are on kitkat without root
		boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);

		// if on kitkat make sure that we always collect screen on time: if no root then count the time
		if ( !rootEnabled && !SysUtils.hasBatteryStatsPermission(this) )
		{
			serviceShouldBeRunning = true;
		}

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
		
		// check if active monitoring is on: if yes make sure the alarm is scheduled
		if (sharedPrefs.getBoolean("active_mon_enabled", false))
		{
			if (!StatsProvider.isActiveMonAlarmScheduled(this))
			{
				StatsProvider.scheduleActiveMonAlarm(this);
			}
		}
		Log.i(TAG, "OnResume end");

		

		
		
	}

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause()
	{
		super.onPause();
		
		Log.i(TAG, "OnPause called");
		Log.i(TAG, "onPause reference state: refFrom=" + m_refFromName + " refTo=" + m_refToName);
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
        
    	Log.i(TAG, "onSaveInstanceState references: refFrom=" + m_refFromName + " refTo=" + m_refToName);
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
	        	
	        case R.id.rawstats:  
	        	Intent intentRaw = new Intent(this, RawStatsActivity.class);
	        	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_RAW);
	            this.startActivity(intentRaw);
	        	break;	
	        case R.id.refresh:
            	// Refresh
//	        	ReferenceStore.rebuildCache(this);
	        	doRefresh(true);
            	break;	
            case R.id.custom_ref:
            	// Set custom reference
            	GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTION_SET_CUSTOM_REF);

            	// start service to persist reference
        		Intent serviceIntent = new Intent(this, WriteCustomReferenceService.class);
        		this.startService(serviceIntent);
            	break;
            case R.id.credits:
	        	Intent intentCredits = new Intent(this, CreditsActivity.class);
	            this.startActivity(intentCredits);
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

            case R.id.share:
            	// Share
            	getShareDialog().show();
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
		// this method is fired even if nothing has changed so we nee to find that out
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		boolean bChanged = false;
		
		// id is in the order of the spinners, 0 is stat, 1 is stat_type
		if (parent == (Spinner) findViewById(R.id.spinnerStatType))
		{
			// detect if something changed
			String newStat = (String) ( (ReferencesAdapter) parent.getAdapter()).getItemName(position);
			if ((m_refFromName != null) && ( !m_refFromName.equals(newStat) ))
			{
				Log.i(TAG, "Spinner from changed from " + m_refFromName + " to " + newStat);
				m_refFromName = newStat;
				bChanged = true;
				// we need to update the second spinner
				m_spinnerToAdapter.filterToSpinner(newStat, this);
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
			if ((m_refFromName != null) && ( !m_refToName.equals(newStat) ))
			{
				Log.i(TAG, "Spinner to changed from " + m_refToName + " to " + newStat);
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
	        String sinceText = DateUtils.formatDuration(sinceMs);
			boolean bShowBatteryLevels = sharedPrefs.getBoolean("show_batt", true);
	        if (bShowBatteryLevels)
	        {

        		sinceText += " " + StatsProvider.getInstance(this).getBatteryLevelFromTo(myReferenceFrom, myReferenceTo, true);
	        }
	        tvSince.setText(sinceText);
	    	Log.i(TAG, "Since " + sinceText);
        }
        else
        {
	        tvSince.setText("n/a ");
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
//		Log.i(TAG, "OnNothingSelected called");
		// do nothing
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
    	if (key.equals("show_to_ref"))
    	{
    		Spinner spinnerStatSampleEnd = (Spinner) findViewById(R.id.spinnerStatSampleEnd);	
    		boolean bShowSpinner = prefs.getBoolean("show_to_ref", true);
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
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if ((m_refFromName == null) && (m_refToName == null))
		{
			Toast.makeText(this, "Fallback to default references", Toast.LENGTH_SHORT).show();
			m_refFromName	= sharedPrefs.getString("default_stat_type", Reference.UNPLUGGED_REF_FILENAME);
			m_refToName	= Reference.CURRENT_REF_FILENAME;
			

			if (!ReferenceStore.hasReferenceByName(m_refFromName, this))
			{
				if (sharedPrefs.getBoolean("fallback_to_since_boot", false))
				{
					m_refFromName = Reference.BOOT_REF_FILENAME;
		    		
				}
			}
			Log.e(TAG, "refreshSpinners: reset null references: from='" + m_refFromName + "', to='" + m_refToName + "'");
					
		}
		// reload the spinners to make sure all refs are in the right sequence
		m_spinnerFromAdapter.refreshFromSpinner(this);
		m_spinnerToAdapter.filterToSpinner(m_refFromName, this);
		// after we reloaded the spinners we need to reset the selections
		Spinner spinnerStatTypeFrom = (Spinner) findViewById(R.id.spinnerStatType);
		Spinner spinnerStatTypeTo = (Spinner) findViewById(R.id.spinnerStatSampleEnd);
		Log.i(TAG, "refreshSpinners: reset spinner selections: from='" + m_refFromName + "', to='" + m_refToName + "'");
		Log.i(TAG, "refreshSpinners Spinner values: SpinnerFrom=" + m_spinnerFromAdapter.getNames() + " SpinnerTo=" + m_spinnerToAdapter.getNames());
		Log.i(TAG, "refreshSpinners: request selections: from='" + m_spinnerFromAdapter.getPosition(m_refFromName) + "', to='" + m_spinnerToAdapter.getPosition(m_refToName) + "'");

		// restore positions
		spinnerStatTypeFrom.setSelection(m_spinnerFromAdapter.getPosition(m_refFromName), true);
		if (spinnerStatTypeTo.isShown())
		{
			spinnerStatTypeTo.setSelection(m_spinnerToAdapter.getPosition(m_refToName), true);
		}
		else
		{
			spinnerStatTypeTo.setSelection(m_spinnerToAdapter.getPosition(Reference.CURRENT_REF_FILENAME), true);
		}
		Log.i(TAG, "refreshSpinners result positions: from='" + spinnerStatTypeFrom.getSelectedItemPosition() + "', to='" + spinnerStatTypeTo.getSelectedItemPosition() + "'");
		
		if ((spinnerStatTypeTo.isShown()) 
				&& ((spinnerStatTypeFrom.getSelectedItemPosition() == -1)||(spinnerStatTypeTo.getSelectedItemPosition() == -1)))
		{
			Toast.makeText(StatsActivity.this,
					"Selected 'from' or 'to' reference could not be loaded. Please refresh",
					Toast.LENGTH_LONG).show();
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

		if (SysUtils.hasBatteryStatsPermission(this)) BatteryStatsProxy.getInstance(this).invalidate();
		
		refreshSpinners();
		new LoadStatData().execute(updateCurrent);	
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
				Log.i(TAG, "LoadStatData: refreshing display for stats " + m_refFromName + " to " + m_refToName);
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
		        String sinceText = DateUtils.formatDuration(sinceMs);
		        
				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(StatsActivity.this);
				boolean bShowBatteryLevels = sharedPrefs.getBoolean("show_batt", true);
		        if (bShowBatteryLevels)
		        {
		        		sinceText += " " + StatsProvider.getInstance(StatsActivity.this).getBatteryLevelFromTo(myReferenceFrom, myReferenceTo, true);
		        }
		        tvSince.setText(sinceText);
		    	Log.i(TAG, "Since " + sinceText);
	        }
	        else
	        {
		        tvSince.setText("n/a");
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
	

	public Dialog getShareDialog()
	{
	
		final ArrayList<Integer> selectedSaveActions = new ArrayList<Integer>();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean saveAsText = sharedPrefs.getBoolean("save_as_text", true);
		boolean saveAsJson = sharedPrefs.getBoolean("save_as_json", false);
		boolean saveLogcat = sharedPrefs.getBoolean("save_logcat", false);
		boolean saveDmesg = sharedPrefs.getBoolean("save_dmesg", false);

		if (saveAsText)
		{
			selectedSaveActions.add(0);
		}
		if (saveAsJson)
		{
			selectedSaveActions.add(1);
		}
		if (saveLogcat)
		{
			selectedSaveActions.add(2);
		}
		if (saveDmesg)
		{
			selectedSaveActions.add(3);
		}

		
		// Set the dialog title
		builder.setTitle(R.string.title_share_dialog)
				.setMultiChoiceItems(R.array.saveAsLabels, new boolean[]{saveAsText, saveAsJson, saveLogcat, saveDmesg}, new DialogInterface.OnMultiChoiceClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked)
					{
						if (isChecked)
						{
							// If the user checked the item, add it to the
							// selected items
							selectedSaveActions.add(which);
						} else if (selectedSaveActions.contains(which))
						{
							// Else, if the item is already in the array,
							// remove it
							selectedSaveActions.remove(Integer.valueOf(which));
						}
					}
				})
				// Set the action buttons
				.setPositiveButton(R.string.label_button_share, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int id)
					{
		            	GoogleAnalytics.getInstance(StatsActivity.this).trackPage(GoogleAnalytics.ACTION_DUMP);            	

		            	ArrayList<Uri> attachements = new ArrayList<Uri>();

		            	Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, StatsActivity.this);
			    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, StatsActivity.this);

			    		Reading reading = new Reading(StatsActivity.this, myReferenceFrom, myReferenceTo);

						// save as text is selected
						if (selectedSaveActions.contains(0))
						{
							attachements.add(reading.writeToFileText(StatsActivity.this));
						}
						// save as JSON if selected
						if (selectedSaveActions.contains(1))
						{
							attachements.add(reading.writeToFileJson(StatsActivity.this));
						}
						// save logcat if selected
						if (selectedSaveActions.contains(2))
						{
							attachements.add(StatsProvider.getInstance(StatsActivity.this).writeLogcatToFile());
						}
						// save dmesg if selected
						if (selectedSaveActions.contains(3))
						{
							attachements.add(StatsProvider.getInstance(StatsActivity.this).writeDmesgToFile());
						}


						if (!attachements.isEmpty())
						{
							Intent shareIntent = new Intent();
							shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
							shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachements);
							shareIntent.setType("text/*");
							startActivity(Intent.createChooser(shareIntent, "Share info to.."));
						}
					}
				})
				.setNeutralButton(R.string.label_button_save, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int id)
					{
		            	GoogleAnalytics.getInstance(StatsActivity.this).trackPage(GoogleAnalytics.ACTION_DUMP);            	


		            	Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, StatsActivity.this);
			    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, StatsActivity.this);

			    		Reading reading = new Reading(StatsActivity.this, myReferenceFrom, myReferenceTo);

						// save as text is selected
						// save as text is selected
						if (selectedSaveActions.contains(0))
						{
							reading.writeToFileText(StatsActivity.this);
						}
						// save as JSON if selected
						if (selectedSaveActions.contains(1))
						{
							reading.writeToFileJson(StatsActivity.this);
						}
						// save logcat if selected
						if (selectedSaveActions.contains(2))
						{
							StatsProvider.getInstance(StatsActivity.this).writeLogcatToFile();
						}
						// save dmesg if selected
						if (selectedSaveActions.contains(3))
						{
							StatsProvider.getInstance(StatsActivity.this).writeDmesgToFile();
						}
						
					}
				}).setNegativeButton(R.string.label_button_cancel, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int id)
						{
							// do nothing
						}
					});
	
		return builder.create();
	}
	
	@Override
    public void onDownMotionEvent() {
        mScrollSettleHandler.setSettleEnabled(false);
    }
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
    public void onScrollChanged(int scrollY) {
        scrollY = Math.min(mMaxScrollY, scrollY);

        mScrollSettleHandler.onScroll(scrollY);

        int rawY = mPlaceholderView.getTop() - scrollY;
        int translationY = 0;

        switch (mState) {
            case STATE_OFFSCREEN:
                if (rawY <= mMinRawY) {
                    mMinRawY = rawY;
                } else {
                    mState = STATE_RETURNING;
                }
                translationY = rawY;
                break;

            case STATE_ONSCREEN:
                if (rawY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                translationY = rawY;
                break;

            case STATE_RETURNING:
                translationY = (rawY - mMinRawY) - mQuickReturnHeight;
                if (translationY > 0) {
                    translationY = 0;
                    mMinRawY = rawY - mQuickReturnHeight;
                }

                if (rawY > 0) {
                    mState = STATE_ONSCREEN;
                    translationY = rawY;
                }

                if (translationY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                break;
        }
        mQuickReturnView.animate().cancel();
        mQuickReturnView.setTranslationY(translationY + scrollY);
    }
	
	@Override
    public void onUpOrCancelMotionEvent() {
        mScrollSettleHandler.setSettleEnabled(true);
        mScrollSettleHandler.onScroll(mObservableScrollView.getScrollY());
    }
	
	
	private class ScrollSettleHandler extends Handler {
        private static final int SETTLE_DELAY_MILLIS = 100;

        private int mSettledScrollY = Integer.MIN_VALUE;
        private boolean mSettleEnabled;

        public void onScroll(int scrollY) {
            if (mSettledScrollY != scrollY) {
                 // Clear any pending messages and post delayed
                removeMessages(0);
                sendEmptyMessageDelayed(0, SETTLE_DELAY_MILLIS);
                mSettledScrollY = scrollY;
            }
        }

        public void setSettleEnabled(boolean settleEnabled) {
            mSettleEnabled = settleEnabled;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
		@Override
        public void handleMessage(Message msg) {
            // Handle the scroll settling.
            if (STATE_RETURNING == mState && mSettleEnabled) {
                int mDestTranslationY;
                if (mSettledScrollY - mQuickReturnView.getTranslationY() > mQuickReturnHeight / 2) {
                    mState = STATE_OFFSCREEN;
                    mDestTranslationY = Math.max(
                            mSettledScrollY - mQuickReturnHeight,
                            mPlaceholderView.getTop());
                } else {
                    mDestTranslationY = mSettledScrollY;
                }

                mMinRawY = mPlaceholderView.getTop() - mQuickReturnHeight - mDestTranslationY;
                mQuickReturnView.animate().translationY(mDestTranslationY);
            }
            mSettledScrollY = Integer.MIN_VALUE; // reset
        }
    }
}
