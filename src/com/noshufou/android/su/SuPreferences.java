package com.noshufou.android.su;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SuPreferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        Preference versionPreference = (Preference)getPreferenceScreen().findPreference("preference_version");
        
        versionPreference.setTitle(getString(R.string.preference_version_title, getSuperuserVersion()));
        versionPreference.setSummary(getString(R.string.preference_version_summary, getSuVersion()));
    }
    
    private String getSuperuserVersion()
    {
    	String versionName = "";
    	
    	try
        {
        	PackageInfo pInfo = getPackageManager().getPackageInfo("com.noshufou.android.su", PackageManager.GET_META_DATA);
        	versionName = pInfo.versionName;
        } catch (NameNotFoundException e)
        {
        	e.printStackTrace();
        }
        
        return versionName;
    }
    
    private String getSuVersion()
    {
    	String suVersion = "";
    	Process process = null;
    	
	    try {
			process = Runtime.getRuntime().exec("su -v");
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			suVersion = stdInput.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
    	
    	return suVersion;
    }
}
