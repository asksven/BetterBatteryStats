package com.noshufou.android.su; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TabHost;

public class Su extends TabActivity {
    private static final String TAG = "Su";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;
		
		intent = new Intent().setClass(this, AppListActivity.class);
		spec = tabHost.newTabSpec("apps").setIndicator(getString(R.string.tab_apps),
				res.getDrawable(R.drawable.ic_tab_permissions))
				.setContent(intent);
		tabHost.addTab(spec);
		
		intent = new Intent().setClass(this, LogActivity.class);
		spec = tabHost.newTabSpec("log").setIndicator(getString(R.string.tab_log),
				res.getDrawable(R.drawable.ic_tab_log))
				.setContent(intent);
		tabHost.addTab(spec);
		
		intent = new Intent().setClass(this, SuPreferences.class);
		spec = tabHost.newTabSpec("settings").setIndicator(getString(R.string.tab_settings),
				res.getDrawable(R.drawable.ic_tab_settings))
				.setContent(intent);
		tabHost.addTab(spec);
		
		tabHost.setCurrentTab(0);
		
		firstRun();
	}
	
	private void firstRun() {
		int versionCode = 0;
		try {
			versionCode = getPackageManager()
					.getPackageInfo("com.noshufou.android.su", PackageManager.GET_META_DATA)
					.versionCode;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Package not found... Odd, since we're in that package...", e);
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int lastFirstRun = prefs.getInt("last_run", 0);
		
		if (lastFirstRun >= versionCode) {
			Log.d(TAG, "Not first run");
			return;
		}
		Log.d(TAG, "First run for version " + versionCode);
		
		String suVer = getSuVersion();
		Log.d(TAG, "su version: " + suVer);
		new Updater(this, suVer).doUpdate();
		
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt("last_run", versionCode);
		editor.commit();
	}

	public static String getSuVersion()
    {
    	Process process = null;
    	try {
    		process = Runtime.getRuntime().exec("su -v");
    		BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String suVersion = stdInput.readLine();
    		stdInput.close();
    		return suVersion;
    	} catch (IOException e) {
    		Log.e(TAG, "Call to su failed. Perhaps the wrong version of su is present", e);
    		return null;
    	}
    }
}
