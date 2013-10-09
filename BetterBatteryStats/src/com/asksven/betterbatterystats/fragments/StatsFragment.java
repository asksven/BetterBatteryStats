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
package com.asksven.betterbatterystats.fragments;

/**
 * @author sven
 *
 */
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.net.Uri;
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
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.asksven.android.common.AppRater;
import com.asksven.android.common.CommonLogSettings;
import com.asksven.android.common.ReadmeActivity;
import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.privateapiproxies.BatteryInfoUnavailableException;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.betterbatterystats.AboutActivity;
import com.asksven.betterbatterystats.BbsApplication;
//import com.asksven.betterbatterystats.BatteryGraphActivity;
import com.asksven.betterbatterystats.FirstLaunch;
import com.asksven.betterbatterystats.HelpActivity;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.PreferencesActivity;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.adapters.ReferencesAdapter;
import com.asksven.betterbatterystats.adapters.StatsAdapter;
import com.asksven.betterbatterystats.data.GoogleAnalytics;
import com.asksven.betterbatterystats.data.Reading;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.services.EventWatcherService;
import com.asksven.betterbatterystats.services.WriteCurrentReferenceService;
import com.asksven.betterbatterystats.services.WriteCustomReferenceService;
import com.asksven.betterbatterystats.services.WriteUnpluggedReferenceService;
import com.asksven.betterbatterystats.services.WriteBootReferenceService;

public class StatsFragment extends SherlockListFragment implements OnSharedPreferenceChangeListener
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
	 * The application
	 */
	private BbsApplication m_app = null;
	
	/**
	 * The logfile TAG
	 */
	private static final String LOGFILE = "BetterBatteryStats_Dump.log";
	
	/**
	 * The ArrayAdpater for rendering the ListView
	 */
	private StatsAdapter m_listViewAdapter;
	/**
	 * The Type of Stat to be displayed (default is "Since charged")
	 */
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
	
	public static StatsFragment newInstance(int position)
	{
		StatsFragment fragment = new StatsFragment();
	    Bundle args=new Bundle();

	    args.putInt(STAT, position);
	    fragment.setArguments(args);

		return fragment;
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState); 
        setHasOptionsMenu(true);
		
		m_iStat = getArguments().getInt(STAT, 0);
		new LoadStatData().execute(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.stats, container, false);
		Log.i(TAG, "OnCreated called");
		super.onCreate(savedInstanceState);
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
				
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
		BatteryStatsProxy stats = BatteryStatsProxy.getInstance(getActivity());
				
		if (stats.initFailed())
		{
			Toast.makeText(getActivity(), "The 'batteryinfo' service could not be accessed. If this error persists after a reboot please contact the dev and provide your ROM/Kernel versions.", Toast.LENGTH_SHORT).show();			
		}
		
		///////////////////////////////////////////////
		// check if we have a new release
		///////////////////////////////////////////////
		// if yes do some migration (if required) and show release notes
		String strLastRelease	= sharedPrefs.getString("last_release", "0");
		
		String strCurrentRelease = "";
		try
		{
			PackageInfo pinfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
			
	    	strCurrentRelease = Integer.toString(pinfo.versionCode);
		}
		catch (Exception e)
		{
			// nop strCurrentRelease is set to ""
		}
		
		if (strLastRelease.equals("0"))
		{
			// show the initial run screen
			FirstLaunch.app_launched(getActivity());

		}
		else if (!strLastRelease.equals(strCurrentRelease))
    	{
	        // save the current release to properties so that the dialog won't be shown till next version
	        SharedPreferences.Editor updater = sharedPrefs.edit();
	        updater.putString("last_release", strCurrentRelease);
	        updater.commit();

    		// show the readme
	    	Intent intentReleaseNotes = new Intent(getActivity(), ReadmeActivity.class);
	    	intentReleaseNotes.putExtra("filename", "readme.html");
	        this.startActivity(intentReleaseNotes);
	        
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
	            AlertDialog.Builder alertbox = new AlertDialog.Builder(getActivity());
	 
	            // set the message to display
	            alertbox.setMessage("BetterBatteryStats makes use of Google Analytics to collect usage statitics. If you disagree or do not want to participate you can opt-out by disabling \"Google Analytics\" in the \"Advanced Preferences\"");
	 
	            // add a neutral button to the alert box and assign a click listener
	            alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener()
	            {
	 
	                // click listener on the alert box
	                public void onClick(DialogInterface arg0, int arg1)
	                {
	        	        // opt out info was displayed
	            		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
		    	AppRater.app_launched(getActivity());

			}
    	}
    	
		///////////////////////////////////////////////
    	// retrieve default selections for spinners
    	// if none were passed
		///////////////////////////////////////////////
    	m_app = (BbsApplication) getActivity().getApplication(); 
		m_refFromName	= m_app.getRefFromName();
		m_refToName		= m_app.getRefToName();

		try
		{
			// recover any saved state
			if ( (savedInstanceState != null) && (!savedInstanceState.isEmpty()))
			{
				m_iStat 				= (Integer) savedInstanceState.getSerializable("stat");
				m_refFromName 			= (String) savedInstanceState.getSerializable("stattypeFrom");
				m_refToName 			= (String) savedInstanceState.getSerializable("stattypeTo");
				
				m_app.setStat(m_iStat);
				m_app.setRefFromName(m_refFromName);
				m_app.setRefToName(m_refToName);
	 			
			}			
		}
		catch (Exception e)
		{
			m_iStat		= Integer.valueOf(sharedPrefs.getString("default_stat", "0"));
			m_refFromName	= sharedPrefs.getString("default_stat_type", Reference.UNPLUGGED_REF_FILENAME);

			m_app.setStat(m_iStat);
			m_app.setRefFromName(m_refFromName);
			m_app.setRefToName(m_refToName);

    		Log.e(TAG, "Exception: " + e.getMessage());
    		DataStorage.LogToFile(LOGFILE, "Exception in onCreate restoring Bundle");
    		DataStorage.LogToFile(LOGFILE, e.getMessage());
    		DataStorage.LogToFile(LOGFILE, e.getStackTrace());
    		
    		Toast.makeText(getActivity(), "An error occured while recovering the previous state", Toast.LENGTH_SHORT).show();
		}

		// Handle the case the Activity was called from an intent with paramaters
	    Bundle extras = getArguments();

		if (extras != null)
		{

			boolean bCalledFromNotification = extras.getBoolean(StatsFragment.FROM_NOTIFICATION, false);
			
			// Clear the notifications that was clicked to call the activity
			if (bCalledFromNotification)
			{
		    	NotificationManager nM = (NotificationManager)getActivity().getSystemService(Service.NOTIFICATION_SERVICE);
		    	nM.cancel(EventWatcherService.NOTFICATION_ID);
			}
		}

		// Display the reference of the stat
        TextView tvSince = (TextView) rootView.findViewById(R.id.TextViewSince);
        if (tvSince != null)
        {
    		Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, getActivity());
    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, getActivity());

            long sinceMs = StatsProvider.getInstance(getActivity()).getSince(myReferenceFrom, myReferenceTo);
            if (sinceMs != -1)
            {
    	        String sinceText = DateUtils.formatDuration(sinceMs);
    			boolean bShowBatteryLevels = sharedPrefs.getBoolean("show_batt", true);
    	        if (bShowBatteryLevels)
    	        {
    	        		sinceText += " " + StatsProvider.getInstance(getActivity()).getBatteryLevelFromTo(myReferenceFrom, myReferenceTo);
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
        
		try
		{
			this.setListViewAdapter();
		}
		catch (BatteryInfoUnavailableException e)
		{
//			Log.e(TAG, e.getMessage(), e.fillInStackTrace());
			Log.e(TAG, "Exception: "+Log.getStackTraceString(e));
			Toast.makeText(getActivity(),
					"BatteryInfo Service could not be contacted.",
					Toast.LENGTH_LONG).show();
			
		}
		catch (Exception e)
		{
			//Log.e(TAG, e.getMessage(), e.fillInStackTrace());
			Log.e(TAG, "Exception: "+Log.getStackTraceString(e));
			Toast.makeText(getActivity(),
					"An unhandled error occured. Please check your logcat",
					Toast.LENGTH_LONG).show();
		}
		

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
		GoogleAnalytics.getInstance(getActivity()).trackStats(getActivity(), GoogleAnalytics.ACTIVITY_STATS, m_iStat, m_refFromName, m_refToName, m_iSorting);

        // Set up a listener whenever a key changes
    	PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
		
    	// log reference store
    	ReferenceStore.logReferences(getActivity());
		
    	return rootView;
	}
	
	/* Request updates at startup */
	@Override
	public void onResume()
	{
		Log.i(TAG, "OnResume called");
		super.onResume();

		// read the currently selected references
		m_refFromName = m_app.getRefFromName();
		m_refToName = m_app.getRefToName();
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
                
            }
        };
        
        //registering our receiver
        getActivity().registerReceiver(m_referenceSavedReceiver, intentFilter);
        
		// the service is always started as it handles the widget updates too
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		boolean serviceShouldBeRunning = sharedPrefs.getBoolean("ref_for_screen_off", false);
		if (serviceShouldBeRunning)
		{
			if (!EventWatcherService.isServiceRunning(getActivity()))
			{
				Intent i = new Intent(getActivity(), EventWatcherService.class);
				getActivity().startService(i);
			}    				
		}
		
		// make sure to create a valid "current" stat if none exists
		// or if prefs re set to auto refresh
		
// do not auto refresh		
//		boolean bAutoRefresh = sharedPrefs.getBoolean("auto_refresh", true);
//
//		if ((bAutoRefresh) || (!ReferenceStore.hasReferenceByName(Reference.CURRENT_REF_FILENAME, getActivity())))
//		{
//			Intent serviceIntent = new Intent(getActivity(), WriteCurrentReferenceService.class);
//			getActivity().startService(serviceIntent);
//			doRefresh(true);
//
//		}
//		else
//		{	
//			doRefresh(false);
//			
//		}
//		
		// check if active monitoring is on: if yes make sure the alarm is scheduled
		if (sharedPrefs.getBoolean("active_mon_enabled", false))
		{
			if (!StatsProvider.isActiveMonAlarmScheduled(getActivity()))
			{
				StatsProvider.scheduleActiveMonAlarm(getActivity());
			}
		}


		

		
		
	}

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	public void onPause()
	{
		super.onPause();
		
		// unregister boradcast receiver for saved references
		getActivity().unregisterReceiver(this.m_referenceSavedReceiver);
		
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
        
	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {  
		// as this fragment is nested it requires a patch to ABS: https://github.com/JakeWharton/ActionBarSherlock/issues/828
		// the patch is here: https://github.com/purdyk/ActionBarSherlock/commit/cab97d6a33685963b402b61db1343b3fea802598
		super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.statsmenu, menu);
    }  

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
    {  
        switch (item.getItemId())
        {  
	        case R.id.refresh:
            	// Refresh
	        	ReferenceStore.rebuildCache(getActivity());
	        	doRefresh(true);
            	break;	
            case R.id.custom_ref:
            	// Set custom reference
            	GoogleAnalytics.getInstance(getActivity()).trackPage(GoogleAnalytics.ACTION_SET_CUSTOM_REF);

            	// start service to persist reference
        		Intent serviceIntent = new Intent(getActivity(), WriteCustomReferenceService.class);
        		getActivity().startService(serviceIntent);
            	break;
            case R.id.share:
            	// Share
            	getShareDialog().show();
            	break;
        }  
        return false;  
    }    
    


	public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
    	if (key.equals("show_to_ref"))
    	{
    		Spinner spinnerStatSampleEnd = (Spinner) getView().findViewById(R.id.spinnerStatSampleEnd);	
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
			m_listViewAdapter = new StatsAdapter(getActivity(), 
					StatsProvider.getInstance(getActivity()).getStatList(m_iStat, m_refFromName, m_iSorting, m_refToName));
		
			setListAdapter(m_listViewAdapter);
		}
	}

	private void doRefresh(boolean updateCurrent)
	{

		BatteryStatsProxy.getInstance(getActivity()).invalidate();
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
				StatsProvider.getInstance(getActivity()).setCurrentReference(m_iSorting);		
			}
			//super.doInBackground(params);
			m_listViewAdapter = null;
			try
			{
				Log.i(TAG, "LoadStatData: refreshing display for stats " + m_refFromName + " to " + m_refToName);
				m_listViewAdapter = new StatsAdapter(
						getActivity(),
						StatsProvider.getInstance(getActivity()).getStatList(m_iStat, m_refFromName, m_iSorting, m_refToName));
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
		
//	    @Override
		protected void onPreExecute()
		{
		    // update hourglass
			getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
		}

		//		@Override
		protected void onPostExecute(StatsAdapter o)
	    {
			// update hourglass
			getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
	        
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
	        TextView tvSince = (TextView) getView().findViewById(R.id.TextViewSince);
    		Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, getActivity());
    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, getActivity());

	        long sinceMs = StatsProvider.getInstance(getActivity()).getSince(myReferenceFrom, myReferenceTo);

	        if (sinceMs != -1)
	        {
		        String sinceText = DateUtils.formatDuration(sinceMs);
		        
				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
				boolean bShowBatteryLevels = sharedPrefs.getBoolean("show_batt", true);
		        if (bShowBatteryLevels)
		        {
		        		sinceText += " " + StatsProvider.getInstance(getActivity()).getBatteryLevelFromTo(myReferenceFrom, myReferenceTo);
		        }
		        tvSince.setText(sinceText);
		    	Log.i(TAG, "Since " + sinceText);
	        }
	        else
	        {
		        tvSince.setText("n/a");
		    	Log.i(TAG, "Since: n/a ");
	        	
	        }

	    	StatsFragment.this.setListAdapter(o);
	    }
	}	

	public Dialog getShareDialog()
	{
	
		final ArrayList<Integer> selectedSaveActions = new ArrayList<Integer>();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
		            	GoogleAnalytics.getInstance(getActivity()).trackPage(GoogleAnalytics.ACTION_DUMP);            	

		            	ArrayList<Uri> attachements = new ArrayList<Uri>();

		            	Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, getActivity());
			    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, getActivity());

			    		Reading reading = new Reading(getActivity(), myReferenceFrom, myReferenceTo);

						// save as text is selected
						if (selectedSaveActions.contains(0))
						{
							attachements.add(reading.writeToFileText(getActivity()));
						}
						// save as JSON if selected
						if (selectedSaveActions.contains(1))
						{
							attachements.add(reading.writeToFileJson(getActivity()));
						}
						// save logcat if selected
						if (selectedSaveActions.contains(2))
						{
							attachements.add(StatsProvider.getInstance(getActivity()).writeLogcatToFile());
						}
						// save dmesg if selected
						if (selectedSaveActions.contains(3))
						{
							attachements.add(StatsProvider.getInstance(getActivity()).writeDmesgToFile());
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
		            	GoogleAnalytics.getInstance(getActivity()).trackPage(GoogleAnalytics.ACTION_DUMP);            	


		            	Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, getActivity());
			    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, getActivity());

			    		Reading reading = new Reading(getActivity(), myReferenceFrom, myReferenceTo);

						// save as text is selected
						// save as text is selected
						if (selectedSaveActions.contains(0))
						{
							reading.writeToFileText(getActivity());
						}
						// save as JSON if selected
						if (selectedSaveActions.contains(1))
						{
							reading.writeToFileJson(getActivity());
						}
						// save logcat if selected
						if (selectedSaveActions.contains(2))
						{
							StatsProvider.getInstance(getActivity()).writeLogcatToFile();
						}
						// save dmesg if selected
						if (selectedSaveActions.contains(3))
						{
							StatsProvider.getInstance(getActivity()).writeDmesgToFile();
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
	
}
