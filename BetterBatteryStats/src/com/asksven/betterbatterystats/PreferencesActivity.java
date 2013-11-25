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

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.asksven.android.common.CommonLogSettings;
import com.asksven.android.common.RootShell;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.services.EventWatcherService;
import com.asksven.betterbatterystats.widgetproviders.BbsWidgetProvider;
import com.asksven.betterbatterystats.widgetproviders.LargeWidgetProvider;
import com.asksven.betterbatterystats.widgetproviders.MediumWidgetProvider;
import com.asksven.betterbatterystats.widgetproviders.SmallWidgetProvider;

/**
 * Activity for managing preferences using Android's preferences framework
 * @see http://www.javacodegeeks.com/2011/01/android-quick-preferences-tutorial.html
 * 
 * Access prefs goes like this:
 *   SharedPreferences sharedPrefs = 
 *   	PreferenceManager.getDefaultSharedPreferences(this);
 *   sharedPrefs.getBoolean("perform_updates", false));
 *   
 * @author sven
 *
 */
public class PreferencesActivity extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener
{
	
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		// refresh widgets
		Intent intent = new Intent(this.getApplicationContext(),
				LargeWidgetProvider.class);
		intent.setAction(BbsWidgetProvider.WIDGET_PREFS_REFRESH);
		this.sendBroadcast(intent);

		intent = new Intent(this.getApplicationContext(),
				MediumWidgetProvider.class);
		intent.setAction(BbsWidgetProvider.WIDGET_PREFS_REFRESH);
		this.sendBroadcast(intent);

		intent = new Intent(this.getApplicationContext(),
				SmallWidgetProvider.class);
		intent.setAction(BbsWidgetProvider.WIDGET_PREFS_REFRESH);
		this.sendBroadcast(intent);
		
	}
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
        if (key.equals("ref_for_screen_off"))
        {
    		boolean serviceShouldBeRunning = sharedPreferences.getBoolean(key, false);
    		
    		if (serviceShouldBeRunning)
    		{
    			if (!EventWatcherService.isServiceRunning(this))
    			{
    				Intent i = new Intent(this, EventWatcherService.class);
    				this.startService(i);
    			}    				
    		}
    		else
    		{
    			if (EventWatcherService.isServiceRunning(this))
    			{
    				Intent i = new Intent(this, EventWatcherService.class);
    		        this.stopService(i);

    			}
    			
    		}
    		
    		// enable / disable sliders
			// enable sliders 
    		findPreference("watchdog_awake_threshold").setEnabled(serviceShouldBeRunning);
    		findPreference("watchdog_duration_threshold").setEnabled(serviceShouldBeRunning);
    		

        }
        
        if (key.equals("debug_logging"))
        {
    		boolean enabled = sharedPreferences.getBoolean(key, false);
    		if (enabled)
    		{
    			LogSettings.DEBUG=true;
    			CommonLogSettings.DEBUG=true;
    		}
    		else
    		{
    			LogSettings.DEBUG=true;
    			CommonLogSettings.DEBUG=true;
    		}
        }

        if (key.equals("root_features"))
        {
    		boolean enabled = sharedPreferences.getBoolean(key, false);
    		if (enabled)
    		{
		        AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
		        builder.setMessage("Enabling root features assumes that your phone is rooted.\n"
		        		+ "Please make sure to grant su rights to Alarms and Network stats.\n"
		        		+ "if those rights do not stick blame the su app, not BBS.\n"
		        		+ "Continue?")
		               .setCancelable(false)
		               .setPositiveButton("Yes", new DialogInterface.OnClickListener()
		               {
		                   public void onClick(DialogInterface dialog, int id)
		                   {		                        
		                	   RootShell.getInstance().run("ls /");
		                   }
		               })
		               .setNegativeButton("No", new DialogInterface.OnClickListener()
		               {
		                   public void onClick(DialogInterface dialog, int id)
		                   {
		                	   SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this);
			           	        SharedPreferences.Editor editor = sharedPrefs.edit();	        
			        	        editor.putBoolean("root_features", false);
			        			editor.commit();
				    	        CheckBoxPreference checkboxPref = (CheckBoxPreference) getPreferenceManager().findPreference("root_features");
				    	        checkboxPref.setChecked(false);

		                        dialog.cancel();
		                   }
		               });
		        builder.create().show();
    		}
        }
        
        if (key.equals("active_mon_enabled"))
        {
    		boolean enabled = sharedPreferences.getBoolean(key, false);

    		if (enabled)
    		{
		        AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
		        builder.setMessage("Active monitoring results in an overhead in terms of wakeups and processing and should be used with care.\n"
		        		+ "Continue?")
		               .setCancelable(false)
		               .setPositiveButton("Yes", new DialogInterface.OnClickListener()
		               {
		                   public void onClick(DialogInterface dialog, int id)
		                   {		           
		                	   // Fire the alarms
		                	   StatsProvider.scheduleActiveMonAlarm(PreferencesActivity.this);
		                	   dialog.cancel();
		                   }
		               })
		               .setNegativeButton("No", new DialogInterface.OnClickListener()
		               {
		                   public void onClick(DialogInterface dialog, int id)
		                   {
		                	   SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this);
			           	        SharedPreferences.Editor editor = sharedPrefs.edit();	        
			        	        editor.putBoolean("active_mon_enabled", false);
			        			editor.commit();
				    	        CheckBoxPreference checkboxPref = (CheckBoxPreference) getPreferenceManager().findPreference("root_features");
				    	        checkboxPref.setChecked(false);

		                        dialog.cancel();
		                   }
		               });
		        builder.create().show();

    		}
    		else
    		{
    			// cancel any existing alarms
         	   StatsProvider.cancelActiveMonAlarm(PreferencesActivity.this);

    		}
    			
        }

	}


}
